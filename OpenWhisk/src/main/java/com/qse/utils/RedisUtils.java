package com.qse.utils;

import redis.clients.jedis.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RedisUtils {
    
    private static JedisPool jedisPool = null;
    private static final String ETD_HASH_NAME = "ETD";
    private static final String COUNTER_KEY_NAME = "counter";
    private static final String ID_TO_VAL_NAME = "StringEncoderTable";
    private static final String VAL_TO_ID_NAME = "StringEncoderRev";
    private static final boolean USE_SSL = Boolean.parseBoolean(System.getenv("REDIS_USE_SSL"));
    
    // Cache for frequently accessed data
    private static final Map<String, String> stringEncoderCache = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> etdCache = new ConcurrentHashMap<>();
    
    private static final String LUA_SCRIPT = 
        "if redis.call('HEXISTS', KEYS[2],ARGV[1]) == 0 then\n" +
        "    local i = redis.call('INCR', KEYS[1])\n" +
        "    redis.call('HSET', KEYS[2], ARGV[1], i)\n" +
        "    redis.call('HSET', KEYS[3], i, ARGV[1])\n" +
        "    return i\n" +
        "else\n" +
        "    return redis.call('HGET', KEYS[2], ARGV[1])\n" +
        "end";
    
    static {
        initializeJedisPool();
    }
    
    private static void initializeJedisPool() {
        String redisHost = System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST") : "redis";
        int redisPort = Integer.parseInt(System.getenv("REDIS_PORT") != null ? System.getenv("REDIS_PORT") : "6379");
        String redisPassword = System.getenv("REDIS_PASSWORD");
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(3);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        
        // Fixed Duration constructor issues
        if (redisPassword != null && !redisPassword.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword, USE_SSL);
        } else {
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000);
        }
    }
    
    // ETD Operations
    public static byte[] getETDValue(byte[] key) {
        String keyStr = Base64.getEncoder().encodeToString(key);
        
        // Check cache first
        if (etdCache.containsKey(keyStr)) {
            return etdCache.get(keyStr);
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            byte[] value = jedis.hget(ETD_HASH_NAME.getBytes(), key);
            if (value != null) {
                etdCache.put(keyStr, value);
            }
            return value;
        }
    }
    
    public static List<byte[]> getETDValues(List<byte[]> keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            List<Response<byte[]>> responses = new ArrayList<>();
            
            for (byte[] key : keys) {
                responses.add(pipeline.hget(ETD_HASH_NAME.getBytes(), key));
            }
            
            pipeline.sync();
            
            List<byte[]> results = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                byte[] value = responses.get(i).get();
                results.add(value);
                
                // Update cache
                if (value != null) {
                    etdCache.put(Base64.getEncoder().encodeToString(keys.get(i)), value);
                }
            }
            
            return results;
        }
    }
    
    public static void setETDValue(byte[] key, byte[] value) {
        String keyStr = Base64.getEncoder().encodeToString(key);
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(ETD_HASH_NAME.getBytes(), key, value);
            etdCache.put(keyStr, value);
        }
    }
    
    public static void setETDValues(List<byte[]> keys, List<byte[]> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values must have the same size");
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            
            for (int i = 0; i < keys.size(); i++) {
                pipeline.hset(ETD_HASH_NAME.getBytes(), keys.get(i), values.get(i));
                // Update cache
                etdCache.put(Base64.getEncoder().encodeToString(keys.get(i)), values.get(i));
            }
            
            pipeline.sync();
        }
    }
    
    public static boolean containsETDKey(byte[] key) {
        String keyStr = Base64.getEncoder().encodeToString(key);
        
        // Check cache first
        if (etdCache.containsKey(keyStr)) {
            return true;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists(ETD_HASH_NAME.getBytes(), key);
        }
    }
    
    // String Encoder Operations
    public static String decodeString(Integer id) {
        String key = id.toString();
        
        // Check cache first
        if (stringEncoderCache.containsKey(key)) {
            return stringEncoderCache.get(key);
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.hget(ID_TO_VAL_NAME, key);
            if (value != null) {
                stringEncoderCache.put(key, value);
            }
            return value;
        }
    }
    
    public static List<String> decodeStrings(List<Integer> ids) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            List<Response<String>> responses = new ArrayList<>();
            
            for (Integer id : ids) {
                responses.add(pipeline.hget(ID_TO_VAL_NAME, id.toString()));
            }
            
            pipeline.sync();
            
            List<String> results = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                String value = responses.get(i).get();
                results.add(value);
                
                // Update cache
                if (value != null) {
                    stringEncoderCache.put(ids.get(i).toString(), value);
                }
            }
            
            return results;
        }
    }
    
    public static Integer encodeString(String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            String sha = jedis.scriptLoad(LUA_SCRIPT);
            Object result = jedis.evalsha(sha, 3, COUNTER_KEY_NAME, VAL_TO_ID_NAME, ID_TO_VAL_NAME, value);
            
            if (result == null) {
                throw new RuntimeException("Error in encoding string");
            }
            
            Integer id = Integer.parseInt(result.toString());
            
            // Update cache
            stringEncoderCache.put(id.toString(), value);
            stringEncoderCache.put(value, id.toString());
            
            return id;
        }
    }
    
    public static List<Integer> encodeStrings(List<String> values) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            String sha = jedis.scriptLoad(LUA_SCRIPT);
            List<Response<Object>> responses = new ArrayList<>();
            
            for (String value : values) {
                responses.add(pipeline.evalsha(sha, 3, COUNTER_KEY_NAME, VAL_TO_ID_NAME, ID_TO_VAL_NAME, value));
            }
            
            pipeline.sync();
            
            List<Integer> results = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                Object result = responses.get(i).get();
                if (result == null) {
                    results.add(null);
                } else {
                    Integer id = Integer.parseInt(result.toString());
                    results.add(id);
                    
                    // Update cache
                    stringEncoderCache.put(id.toString(), values.get(i));
                    stringEncoderCache.put(values.get(i), id.toString());
                }
            }
            
            return results;
        }
    }
    
    public static boolean isStringEncoded(String value) {
        // Check cache first
        if (stringEncoderCache.containsKey(value)) {
            return true;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists(VAL_TO_ID_NAME, value);
        }
    }
    
    // Utility Methods
    public static void flushAll() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
            // Clear caches
            stringEncoderCache.clear();
            etdCache.clear();
        }
    }
    
    public static void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    // Get connection statistics
    public static Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        if (jedisPool != null) {
            stats.put("active_connections", jedisPool.getNumActive());
            stats.put("idle_connections", jedisPool.getNumIdle());
            stats.put("waiting_threads", jedisPool.getNumWaiters());
            stats.put("cache_size_string_encoder", stringEncoderCache.size());
            stats.put("cache_size_etd", etdCache.size());
        }
        return stats;
    }
    
    // Execute custom Redis operations
    public static <T> T executeWithJedis(JedisOperation<T> operation) {
        try (Jedis jedis = jedisPool.getResource()) {
            return operation.execute(jedis);
        }
    }
    
    @FunctionalInterface
    public interface JedisOperation<T> {
        T execute(Jedis jedis);
    }
}