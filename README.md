# Octopus App

将 [bestruirui/octopus](https://github.com/bestruirui/octopus)（LLM API 聚合网关）打包为 Android 应用，内置 octopus 二进制，启动后监听 `18080` 端口，通过 WebView 提供管理界面。

## 工作原理

- CI（GitHub Actions）拉取 octopus 源码，构建前端后用 Go 交叉编译出 `android/arm64` 单二进制（前端已 embed）
- APK 启动时将二进制释放到 app 私有目录并执行，通过环境变量配置端口 `18080` 与数据存储路径
- WebView 加载 `http://127.0.0.1:18080` 作为管理面板

## 本地开发

本地无需 Android SDK / Go 环境，仅维护源码。所有构建在 CI 完成：

```bash
git add . && git commit -m "..." && git push
```

CI 产物中可下载构建好的 APK。

## 构建

触发 `build` workflow 后，在 Actions → build → 对应运行页下载 artifact `octopus-app-apk`。

## 端口与数据

| 项 | 值 |
|---|---|
| 监听端口 | 18080 |
| 数据目录 | app 私有目录 `files/octopus/` |
| 数据库 | SQLite（`data.db`） |
