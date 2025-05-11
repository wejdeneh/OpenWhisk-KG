package com.qse.models;

import com.qse.utils.RedisUtils;
import com.qse.serialization.Serialize;
import java.util.List;

/**
 * Wrapper class to maintain compatibility with existing code
 * Delegates all operations to RedisUtils
 */
public class JedisClass {
    
    private int callsCounter = 0;
    
    // ETD Operations
    public List<byte[]> getValuesETD(List<byte[]> keys) {
        callsCounter++;
        return RedisUtils.getETDValues(keys);
    }
    
    public byte[] getValueETD(byte[] key) {
        callsCounter++;
        return RedisUtils.getETDValue(key);
    }
    
    public void setValuesETD(List<byte[]> keys, List<byte[]> values) {
        callsCounter++;
        RedisUtils.setETDValues(keys, values);
    }
    
    public void setValueETD(byte[] key, byte[] value) {
        callsCounter++;
        RedisUtils.setETDValue(key, value);
    }
    
    public boolean containsKeyETD(byte[] key) {
        callsCounter++;
        return RedisUtils.containsETDKey(key);
    }
    
    // String Encoder Operations
    public String decode(Integer key) {
        callsCounter++;
        return RedisUtils.decodeString(key);
    }
    
    public List<String> decodeValues(List<Integer> keys) {
        callsCounter++;
        return RedisUtils.decodeStrings(keys);
    }
    
    public Integer evaluateValue(String key) {
        callsCounter++;
        return RedisUtils.encodeString(key);
    }
    
    public List<Integer> evaluateValues(List<String> keys) {
        callsCounter++;
        return RedisUtils.encodeStrings(keys);
    }
    
    public boolean isEncoded(String val) {
        callsCounter++;
        return RedisUtils.isStringEncoded(val);
    }
    
    // Other Operations
    public void clean() {
        callsCounter++;
        RedisUtils.flushAll();
    }
    
    public int getCallsCounter() {
        return callsCounter;
    }
}