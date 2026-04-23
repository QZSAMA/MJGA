#!/bin/bash

# MJGA - MicroEmulator 启动脚本
# 在电脑上模拟运行 J2ME 应用

# 找到 dist 目录中的 JAD 文件
JAD_FILE="dist/MJGA.jad"

# 检查是否构建了项目
if [ ! -f "$JAD_FILE" ]; then
    echo "❌ 找不到 $JAD_FILE，请先运行: ant clean dist"
    exit 1
fi

# 使用 large device (240x320) 匹配 W995 分辨率
echo "🚀 启动 MicroEmulator 模拟 MJGA (240x320)..."
java -cp "emulator/microemulator.jar:emulator/microemu-device-large.jar:emulator/microemu-jsr-75.jar" org.microemu.app.Main "$JAD_FILE"
