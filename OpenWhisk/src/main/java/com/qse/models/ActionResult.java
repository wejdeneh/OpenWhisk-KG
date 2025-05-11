package com.qse.models;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import java.util.Map;
import java.util.HashMap;

/**
 * Standard format for OpenWhisk action results
 * Provides a consistent structure for returning data from actions
 */
public class ActionResult {
    
    private boolean success;
    private String message;
    private JsonObject data;
    private JsonArray errors;
    private Map<String, String> metadata;
    private long timestamp;
    private long executionTime;
    
    public ActionResult() {
        this.success = false;
        this.data = new JsonObject();
        this.errors = new JsonArray();
        this.metadata = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public ActionResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }
    
    // Static factory methods
    public static ActionResult success() {
        return new ActionResult(true, "Success");
    }
    
    public static ActionResult success(String message) {
        return new ActionResult(true, message);
    }
    
    public static ActionResult success(String message, JsonObject data) {
        ActionResult result = new ActionResult(true, message);
        result.setData(data);
        return result;
    }
    
    public static ActionResult error(String message) {
        return new ActionResult(false, message);
    }
    
    public static ActionResult error(String message, Exception e) {
        ActionResult result = new ActionResult(false, message);
        result.addError(e.getMessage());
        if (e.getCause() != null) {
            result.addError("Cause: " + e.getCause().getMessage());
        }
        return result;
    }
    
    public static ActionResult error(String message, String errorDetail) {
        ActionResult result = new ActionResult(false, message);
        result.addError(errorDetail);
        return result;
    }
    
    // Builder pattern methods
    public ActionResult withData(String key, JsonElement value) {
        if (this.data == null) {
            this.data = new JsonObject();
        }
        this.data.add(key, value);
        return this;
    }
    
    public ActionResult withData(String key, String value) {
        if (this.data == null) {
            this.data = new JsonObject();
        }
        this.data.addProperty(key, value);
        return this;
    }
    
    public ActionResult withData(String key, Number value) {
        if (this.data == null) {
            this.data = new JsonObject();
        }
        this.data.addProperty(key, value);
        return this;
    }
    
    public ActionResult withData(String key, Boolean value) {
        if (this.data == null) {
            this.data = new JsonObject();
        }
        this.data.addProperty(key, value);
        return this;
    }
    
    public ActionResult withMetadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }
    
    public ActionResult withExecutionTime(long executionTime) {
        this.executionTime = executionTime;
        return this;
    }
    
    public ActionResult addError(String error) {
        this.errors.add(error);
        return this;
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public JsonObject getData() {
        return data;
    }
    
    public void setData(JsonObject data) {
        this.data = data;
    }
    
    public JsonArray getErrors() {
        return errors;
    }
    
    public void setErrors(JsonArray errors) {
        this.errors = errors;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
    
    // Convert to JsonObject for OpenWhisk
    public JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        result.addProperty("success", success);
        result.addProperty("message", message);
        result.addProperty("timestamp", timestamp);
        result.addProperty("executionTime", executionTime);
        
        if (data != null && data.size() > 0) {
            result.add("data", data);
        }
        
        if (errors != null && errors.size() > 0) {
            result.add("errors", errors);
        }
        
        if (metadata != null && !metadata.isEmpty()) {
            JsonObject metadataJson = new JsonObject();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                metadataJson.addProperty(entry.getKey(), entry.getValue());
            }
            result.add("metadata", metadataJson);
        }
        
        return result;
    }
    
    // Create from JsonObject
    public static ActionResult fromJsonObject(JsonObject json) {
        ActionResult result = new ActionResult();
        
        if (json.has("success")) {
            result.setSuccess(json.get("success").getAsBoolean());
        }
        
        if (json.has("message")) {
            result.setMessage(json.get("message").getAsString());
        }
        
        if (json.has("timestamp")) {
            result.setTimestamp(json.get("timestamp").getAsLong());
        }
        
        if (json.has("executionTime")) {
            result.setExecutionTime(json.get("executionTime").getAsLong());
        }
        
        if (json.has("data")) {
            result.setData(json.getAsJsonObject("data"));
        }
        
        if (json.has("errors")) {
            result.setErrors(json.getAsJsonArray("errors"));
        }
        
        if (json.has("metadata")) {
            JsonObject metadataJson = json.getAsJsonObject("metadata");
            Map<String, String> metadata = new HashMap<>();
            for (String key : metadataJson.keySet()) {
                metadata.put(key, metadataJson.get(key).getAsString());
            }
            result.setMetadata(metadata);
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return toJsonObject().toString();
    }
}