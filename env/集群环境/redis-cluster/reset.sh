#!/bin/bash

# 停止并删除所有Redis容器
docker stop redis-1 redis-2 redis-3
docker rm redis-1 redis-2 redis-3

# 删除所有Redis数据卷
docker volume rm redis-1-data redis-2-data redis-3-data

# 重新创建Redis容器
docker-compose up -d redis-1 redis-2 redis-3