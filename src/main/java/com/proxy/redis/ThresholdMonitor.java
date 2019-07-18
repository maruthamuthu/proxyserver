package com.proxy.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Calendar;

public class ThresholdMonitor
{
    private static final long CONNECTION_THRESHOLD = 50;
    private static final JedisPool JEDIS_POOL;

    static
    {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(3);
        jedisPoolConfig.setMaxTotal(5);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPoolConfig.setMaxWaitMillis(60000);

        JEDIS_POOL = new JedisPool(jedisPoolConfig, "localhost", 6379, 60000, null, 10);
    }

    public static boolean canAllow(String clientID)
    {
        String key = getThresholdKey(clientID);
        Long currentCount = null;

        try (Jedis jedis = JEDIS_POOL.getResource())
        {
            // We can use get then compare and then set. But it requires two redis calls.
            currentCount = jedis.incr(key);
            if (currentCount != null && currentCount == 1)
            {
                jedis.expire(key, 70);
            }
        }

        return  (currentCount == null || currentCount <= CONNECTION_THRESHOLD);
    }

    private static String getThresholdKey(String clientID)
    {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int mins = calendar.get(Calendar.MINUTE);

        return clientID + '_'+ day+ '_'+ hour + '_' + mins;
    }
}
