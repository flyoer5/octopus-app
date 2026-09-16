# Octopus App

将 [bestruirui/octopus](https://github.com/bestruirui/octopus)（为个人打造的 LLM API 聚合网关）打包为 **Android 应用**的定制版。内置 octopus 单二进制，启动后监听 `18080` 端口，通过 WebView 提供完整管理面板。

## ✨ 特性

- **无登录鉴权**：打开 App 直接进入管理面板（LLM 中转 API 的 API Key 鉴权保留）
- **内置管理面板**：WebView 加载 `http://127.0.0.1:18080`，全功能界面
- **局域网访问**：同一 Wi-Fi 下其他设备可通过 `http://<手机IP>:18080` 访问
- **Codex Header 预设**：渠道高级设置中一键追加 Codex 上游 Header（User-Agent / Originator / Version）
- **移动端优化**：底部 Dock 导航、安全区适配、沉浸式视口
- **数据本地化**：SQLite 存于 App 私有目录，随 App 隔离

## 🏗️ 架构

```
┌─────────────────────────────────────────┐
│  Android App（Kotlin 壳）                │
│  ├─ WebView（管理面板 UI）               │
│  ├─ OctopusEngine（子进程管理）          │
│  └─ liboctopus.so ← 交叉编译的 octopus   │
└─────────────────────────────────────────┘
```

- `octopus/`：上游源码（AGPL-3.0），已定制：删除管理面板登录鉴权、移除设置页原版链接与版本校验、移动端适配、Codex Header 预设
- `app/`：Android 壳（Kotlin + ViewBinding），负责二进制释放与进程生命周期
- **noexec 规避**：Android 10+ 禁止执行 App 私有目录文件，二进制以 `jniLibs/arm64-v8a/liboctopus.so` 打包，由系统提取到 `nativeLibraryDir`（可执行）后直接运行
- **配置注入**：App 启动时生成 `config.json`（端口 18080、SQLite 路径），以 `octopus start --config` 方式启动（viper 环境变量对 Unmarshal 不生效，故不走 env）

## 🚀 使用

1. 从 CI artifact 或 [Releases](https://github.com/flyoer5/octopus-app/releases) 下载 APK 安装
2. 打开「Octopus」App，自动启动服务并加载面板
3. 本机访问：`http://127.0.0.1:18080`（App 内直接可见）
4. 局域网访问：`http://<手机IP>:18080`（需同一 Wi-Fi，路由器勿开 AP 隔离）

| 项 | 值 |
|---|---|
| 监听端口 | 18080 |
| 数据目录 | App 私有目录 `files/octopus/` |
| 数据库 | SQLite（`data.db`） |
| 登录 | 无（默认关闭） |

> ⚠️ 无登录模式意味着局域网内任何设备都能操作面板，请仅在可信网络使用。

## 🛠️ 开发

本地**无需 Android SDK / Go 环境**，只维护源码，所有构建在 CI 完成：

```bash
git add . && git commit -m "..." && git push
```

- 改管理面板/后端：编辑 `octopus/` 目录（前端 `octopus/web`，后端 Go）
- 改 App 壳：编辑 `app/` 目录
- 本地交叉编译（可选，需 Node/pnpm + Go 1.26）：`./scripts/build-octopus-native.sh`

## 🔨 CI（build workflow）

每次 push 自动执行：

1. **交叉编译**：pnpm 构建前端 → `CGO_ENABLED=0 GOOS=android GOARCH=arm64` 交叉编译 → `go vet` 静态检查
2. **构建 APK**：ktlint 规范检查 → JUnit 单元测试 → `assembleRelease`
3. **发布**：推送 `v*` tag 自动生成 GitHub Release 并附带 APK

产物：Actions 运行页下载 `octopus-app-apk`。

## 📁 目录结构

```
octopus-app/
├── app/                    # Android 壳（Kotlin）
│   └── src/main/java/io/github/flyoer5/octopusapp/
│       ├── MainActivity.kt            # WebView + 状态栏
│       ├── OctopusEngine.kt           # 二进制释放、进程管理、健康检查
│       ├── OctopusConfig.kt           # 配置生成（端口/数据库路径）
│       └── OctopusForegroundService.kt # 前台服务
├── octopus/                # 上游源码（定制版，AGPL-3.0）
├── scripts/                # 本地辅助脚本
└── .github/workflows/      # CI 定义
```

## 📄 许可

- 本仓库壳代码：见 [LICENSE](./LICENSE)（未添加时默认保留上游 AGPL-3.0 约束）
- `octopus/` 目录：GNU AGPL v3.0（上游 [bestruirui/octopus](https://github.com/bestruirui/octopus)）
