#!/bin/bash

# 备份目录
BACKUP_DIR="/path/to/backup/directory"

# 获取当前日期
DATE=$(date +%Y-%m-%d)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份每个Redis节点的数据
for i in 1 2 3; do
  docker exec redis-$i redis-cli save
  docker cp redis-$i:/data $BACKUP_DIR/redis-$i-$DATE
done

# 压缩备份文件
tar -czvf $BACKUP_DIR/redis-backup-$DATE.tar.gz $BACKUP_DIR/redis-*-$DATE

# 删除未压缩的备份文件
rm -rf $BACKUP_DIR/redis-*-$DATE