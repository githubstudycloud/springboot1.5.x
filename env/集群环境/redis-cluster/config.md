redis.conf
```text
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
protected-mode no
```

# 设置Redis实例的密码
#masterauth redis123
#requirepass redis123

# 启用ACL (Access Control List)
aclfile /usr/local/etc/redis/users.acl

users.acl
```text
user admin on >redis123 ~* +@all
user readonly on >readonly123 ~* -@all +@read
user redis-exporter on >exporter-redis123 +@all -@dangerous
```