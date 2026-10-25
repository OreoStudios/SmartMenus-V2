package com.oreo.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

public class RedisCooldownProvider implements CooldownProvider {

    private final JedisPool pool;

    public RedisCooldownProvider(String host, int port, String password, int database) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);

        if (password == null || password.isEmpty()) {
            this.pool = new JedisPool(config, host, port, 2000, null, database);
        } else {
            this.pool = new JedisPool(config, host, port, 2000, password, database);
        }
    }

    private String buildKey(String id, UUID uuid) {
        return "smartmenus:cooldown:" + id + ":" + uuid;
    }

    @Override
    public long remainingMillis(String id, UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            long pttl = jedis.pttl(buildKey(id, uuid));
            return Math.max(0L, pttl);
        }
    }

    @Override
    public void apply(String id, UUID uuid, long durationMillis) {
        try (Jedis jedis = pool.getResource()) {
            jedis.psetex(buildKey(id, uuid), durationMillis, "1");
        }
    }

    @Override
    public void clear(String id, UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(buildKey(id, uuid));
        }
    }

    @Override
    public void shutdown() {
        pool.close();
    }
}
