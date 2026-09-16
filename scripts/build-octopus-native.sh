#!/usr/bin/env bash
# 交叉编译 octopus 为 android/arm64 单二进制（CI 使用同一流程）
# 用法: ./scripts/build-octopus-native.sh [octopus源码目录]
set -euo pipefail

SRC_DIR="${1:-octopus}"
OUT="${2:-app/src/main/jniLibs/arm64-v8a/liboctopus.so}"

if [ ! -d "$SRC_DIR" ]; then
    echo "错误: 未找到 octopus 源码目录 $SRC_DIR，请先 clone"
    exit 1
fi

echo "[1/3] 构建前端..."
(cd "$SRC_DIR/web" && pnpm install --frozen-lockfile && pnpm run build)

echo "[2/3] 交叉编译 android/arm64..."
(cd "$SRC_DIR" && CGO_ENABLED=0 GOOS=android GOARCH=arm64 \
    go build -trimpath -ldflags "-s -w" -o octopus-android-arm64 .)

echo "[3/3] 复制为 jniLibs 二进制（liboctopus.so）..."
mkdir -p "$(dirname "$OUT")"
cp "$SRC_DIR/octopus-android-arm64" "$OUT"
chmod +x "$OUT"
file "$OUT"
echo "完成: $OUT"
