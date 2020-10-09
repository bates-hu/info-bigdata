package edu.neu.info7255.bates.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    private static final Logger LOG =  LoggerFactory.getLogger(RedisService.class);

    @Autowired
    protected StringRedisTemplate redisTemplate;

    public boolean set(final String key, String value){
        boolean result = false;
        try {
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            result = true;
        } catch (Exception e) {
            LOG.error("write redis fail" + e.getMessage());
        }
        return result;
    }

    public String get(final String key){
        String result = null;
        try {
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            result = operations.get(key);
        } catch (Exception e) {
            LOG.error("read redis fail：" + e.getMessage());
        }
        return result;
    }

    public boolean delete(final String key){
        try {
            return redisTemplate.delete(key) == Boolean.TRUE;
        } catch (Exception e) {
            LOG.error("read redis fail：" + e.getMessage());
        }
        return false;
    }

    public boolean isKeyExist(final String key) {
        return redisTemplate.hasKey(key) == Boolean.TRUE;
    }
}
