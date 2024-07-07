#!/bin/sh

# 设置日志文件路径
LOG_FILE="/var/log/create-cluster.log"

# 记录日志函数
log() {
  timestamp=$(date "+%Y-%m-%d %H:%M:%S")
  echo "[${timestamp}] $1" | tee -a "${LOG_FILE}"
}

# 等待Redis服务启动
sleep 10

# 检查集群状态
is_cluster_configured() {
  redis-cli -h 172.55.0.2 -p 6379 --user admin --pass redis123 cluster info | grep -q 'cluster_state:ok'
}

# 如果集群已经配置,则退出
if is_cluster_configured; then
  log "Redis cluster is already configured."
  exit 0
fi

# 如果集群未配置,则创建一个新集群
log "Redis cluster is not configured. Creating a new cluster..."

# 创建集群并捕获输出
output=$(yes yes | redis-cli --cluster create 172.55.0.2:6379 172.55.0.3:6379 172.55.0.4:6379 --cluster-replicas 0 --user admin --pass redis123 2>&1)

# 检查集群创建是否成功
if echo "${output}" | grep -q 'OK'; then
  log "Redis cluster created successfully."
else
  log "Failed to create Redis cluster. Output:"
  log "${output}"
  exit 1
fi