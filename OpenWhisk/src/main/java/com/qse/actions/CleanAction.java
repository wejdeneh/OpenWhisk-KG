package com.qse.actions;

import com.google.gson.JsonObject;
import com.qse.models.ActionResult;
import com.qse.models.JedisClass;
import io.minio.*;
import io.minio.messages.Item;

public class CleanAction {
    
    private static MinioClient minioClient;
    private static final String BUCKET_NAME = "qse-storage";
    
    static {
        try {
            minioClient = MinioClient.builder()
                .endpoint(System.getenv("MINIO_ENDPOINT"))
                .credentials(System.getenv("MINIO_ACCESS_KEY"), System.getenv("MINIO_SECRET_KEY"))
                .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static JsonObject main(JsonObject args) {
        long startTime = System.currentTimeMillis();
        
        try {
            String workflowId = args.get("workflow_id").getAsString();
            
            // Clean Redis
            JedisClass jedis = new JedisClass();
            jedis.clean();
            
            // Clean MinIO storage
            CleanResult cleanResult = cleanMinIOStorage();
            
            // Create successful result
            ActionResult result = ActionResult.success("Cleanup completed successfully")
                .withData("workflow_id", workflowId)
                .withData("redis_cleaned", true)
                .withData("objects_deleted", cleanResult.objectsDeleted)
                .withData("prefixes_cleaned", cleanResult.prefixesCleaned)
                .withData("total_size_freed", cleanResult.totalSizeFreed)
                .withMetadata("action", "clean")
                .withMetadata("timestamp", String.valueOf(System.currentTimeMillis()))
                .withExecutionTime(System.currentTimeMillis() - startTime);
            
            return result.toJsonObject();
            
        } catch (Exception e) {
            // Create error result
            ActionResult result = ActionResult.error("Cleanup failed", e)
                .withData("workflow_id", args.has("workflow_id") ? args.get("workflow_id").getAsString() : "unknown")
                .withMetadata("action", "clean")
                .withExecutionTime(System.currentTimeMillis() - startTime);
            
            return result.toJsonObject();
        }
    }
    
    private static CleanResult cleanMinIOStorage() {
        CleanResult result = new CleanResult();
        
        try {
            // List and delete objects that need cleaning
            String[] prefixes = {"classEntityCount/", "CTP/", "merged", "shapeTripletSupport/"};
            
            for (String prefix : prefixes) {
                int deletedCount = 0;
                long sizeFreed = 0;
                
                Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .prefix(prefix)
                        .build()
                );
                
                for (Result<Item> itemResult : results) {
                    Item item = itemResult.get();
                    sizeFreed += item.size();
                    
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(item.objectName())
                            .build()
                    );
                    
                    deletedCount++;
                }
                
                result.objectsDeleted += deletedCount;
                result.totalSizeFreed += sizeFreed;
                result.prefixesCleaned++;
                
                System.out.println("Deleted " + deletedCount + " objects with prefix: " + prefix);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to clean MinIO storage", e);
        }
        
        return result;
    }
    
    private static class CleanResult {
        int objectsDeleted = 0;
        int prefixesCleaned = 0;
        long totalSizeFreed = 0;
    }
}