#!/bin/bash

# 用于后台启动一个jar包，并在启动前释放 7094 端口
# 执行命令为: ./start.sh jar包名 [日志目录]
# 示例: ./start.sh myapp.jar /data/log-gxh

PORT=7094

# 检查是否提供了jar包名参数
if [ $# -eq 0 ]; then
    echo "用法: $0 jar包名 [日志目录]"
    echo "示例: $0 myapp.jar"
    echo "示例: $0 myapp.jar /data/log-gxh"
    exit 1
fi

JAR_FILE=$1
LOG_PATH="${2:-log-gxh}"  # 如果没有提供第二个参数，使用默认值 log-gxh

# 检查jar文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 找不到jar文件 $JAR_FILE"
    exit 1
fi

# 检查 netstat 是否可用
if ! command -v netstat &> /dev/null; then
    echo "警告: netstat 命令未找到，跳过端口检查。请确保 $PORT 端口未被占用。"
else
    echo "正在检查端口 $PORT 是否被占用..."

    # 获取占用 7094 端口的 PID（只取数字部分）
    PID=$(netstat -tulnp 2>/dev/null | grep ":$PORT " | awk '{print $7}' | cut -d'/' -f1)

    if [ -n "$PID" ] && [[ "$PID" =~ ^[0-9]+$ ]]; then
        echo "检测到端口 $PORT 被进程 PID=$PID 占用，正在终止..."
        kill -9 "$PID" 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "已成功终止 PID=$PID"
        else
            echo "警告: 无法终止 PID=$PID（可能权限不足）"
        fi
        # 等待1秒让端口释放
        sleep 1
    else
        echo "端口 $PORT 未被占用，继续启动..."
    fi
fi

# 获取jar文件名（不含路径）用于日志和pid文件命名
JAR_NAME=$(basename "$JAR_FILE" .jar)

# 启动jar包在后台运行（日志文件按 jar 名命名）
LOG_FILE="gxh_kj.log"
PID_FILE="java_start.pid"

# 创建日志目录（如果不存在）
echo "创建日志目录: $LOG_PATH"
mkdir -p "$LOG_PATH"
if [ $? -ne 0 ]; then
    echo "警告: 无法创建日志目录 $LOG_PATH，请检查权限。"
    echo "将使用默认相对路径 log-gxh"
    LOG_PATH="log-gxh"
    mkdir -p "$LOG_PATH"
fi

# 启动 jar 包，指定日志路径
echo "正在启动 $JAR_FILE，日志目录: $LOG_PATH"
nohup java -DLOG_PATH="$LOG_PATH" -jar "$JAR_FILE" > ./nohup.out 2>&1 &
PID=$!

echo "已启动 jar 包 $JAR_FILE，进程ID: $PID"
echo "$PID" > "$PID_FILE"
echo "进程ID已保存到 $PID_FILE，日志文件：$LOG_FILE"
echo "应用日志目录: $LOG_PATH"
