package com.qse.actions;

import com.google.gson.JsonObject;
import com.qse.models.*;
import com.qse.utils.StorageUtils;
import com.qse.serialization.Serialize;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MergeActions {
    
    // Merge Support Action
    public static class MergeSupportAction {
        public static JsonObject main(JsonObject args) {
            try {
                int subsetCount = args.get("subset_count").getAsInt();
                
                ShapeTripletSupport mergedSupport = new ShapeTripletSupport();
                
                // Collect all support files
                for (int i = 0; i < subsetCount; i++) {
                    String shardName = "shapeTripletSupport/shard" + i + ".ser";
                    byte[] shardData = StorageUtils.downloadBlob(shardName);
                    if (shardData != null) {
                        ShapeTripletSupport shard = (ShapeTripletSupport) Serialize.deserialize(shardData);
                        
                        // Merge with existing data
                        for (Tuple3<Integer, Integer, Integer> tuple : shard.keySet()) {
                            if (mergedSupport.containsKey(tuple)) {
                                SupportConfidence existing = mergedSupport.get(tuple);
                                SupportConfidence current = shard.get(tuple);
                                existing.mergeSupport(current);
                                mergedSupport.put(tuple, existing);
                            } else {
                                mergedSupport.put(tuple, shard.get(tuple));
                            }
                        }
                    }
                }
                
                // Save merged result
                String mergedShardName = "mergedSupport/shapeTripletSupport.ser";
                byte[] serializedData = Serialize.serialize(mergedSupport);
                StorageUtils.uploadBlob(mergedShardName, serializedData);
                
                // Also save as text for debugging
                StringBuilder debugData = new StringBuilder();
                for (Tuple3<Integer, Integer, Integer> tuple : mergedSupport.keySet()) {
                    debugData.append("Support: ").append(tuple).append(" -> ")
                             .append(mergedSupport.get(tuple)).append("\n");
                }
                StorageUtils.uploadBlob(mergedShardName + ".txt", debugData.toString().getBytes());
                
                JsonObject result = new JsonObject();
                result.addProperty("merged_file", mergedShardName);
                result.addProperty("total_entries", mergedSupport.size());
                return result;
                
            } catch (Exception e) {
                e.printStackTrace();
                JsonObject error = new JsonObject();
                error.addProperty("error", e.getMessage());
                return error;
            }
        }
    }
    
    // Merge CEC Action
    public static class MergeCECAction {
        public static JsonObject main(JsonObject args) {
            try {
                int subsetCount = args.get("subset_count").getAsInt();
                
                CEC mergedCEC = new CEC();
                
                // Collect all CEC files
                for (int i = 0; i < subsetCount; i++) {
                    String shardName = "classEntityCount/shard" + i + ".ser";
                    byte[] shardData = StorageUtils.downloadBlob(shardName);
                    if (shardData != null) {
                        CEC shard = (CEC) Serialize.deserialize(shardData);
                        
                        // Merge with existing data
                        for (Integer classId : shard.keySet()) {
                            if (mergedCEC.containsKey(classId)) {
                                Integer existing = mergedCEC.get(classId);
                                Integer current = shard.get(classId);
                                mergedCEC.put(classId, existing + current);
                            } else {
                                mergedCEC.put(classId, shard.get(classId));
                            }
                        }
                    }
                }
                
                // Save merged result
                String mergedShardName = "mergedCEC/CEC.ser";
                byte[] serializedData = Serialize.serialize(mergedCEC);
                StorageUtils.uploadBlob(mergedShardName, serializedData);
                
                // Also save as text for debugging
                StringBuilder debugData = new StringBuilder();
                for (Integer classId : mergedCEC.keySet()) {
                    debugData.append("CEC: ").append(classId).append(" -> ")
                             .append(mergedCEC.get(classId)).append("\n");
                }
                StorageUtils.uploadBlob(mergedShardName + ".txt", debugData.toString().getBytes());
                
                JsonObject result = new JsonObject();
                result.addProperty("merged_file", mergedShardName);
                result.addProperty("total_entries", mergedCEC.size());
                return result;
                
            } catch (Exception e) {
                e.printStackTrace();
                JsonObject error = new JsonObject();
                error.addProperty("error", e.getMessage());
                return error;
            }
        }
    }
    
    // Merge CTP Action
    public static class MergeCTPAction {
        public static JsonObject main(JsonObject args) {
            try {
                int subsetCount = args.get("subset_count").getAsInt();
                
                CTP mergedCTP = new CTP();
                
                // Collect all CTP files
                for (int i = 0; i < subsetCount; i++) {
                    String shardName = "CTP/shard" + i + ".ser";
                    byte[] shardData = StorageUtils.downloadBlob(shardName);
                    if (shardData != null) {
                        CTP shard = (CTP) Serialize.deserialize(shardData);
                        
                        // Merge with existing data
                        for (Integer classId : shard.keySet()) {
                            java.util.Map<Integer, Set<Integer>> shardMap = shard.get(classId);
                            java.util.Map<Integer, Set<Integer>> mergedMap = mergedCTP.get(classId);
                            
                            if (mergedMap == null) {
                                mergedCTP.put(classId, shardMap);
                            } else {
                                // Merge property maps
                                for (Integer propId : shardMap.keySet()) {
                                    Set<Integer> shardSet = shardMap.get(propId);
                                    Set<Integer> existingSet = mergedMap.get(propId);
                                    
                                    if (existingSet == null) {
                                        mergedMap.put(propId, new HashSet<>(shardSet));
                                    } else {
                                        existingSet.addAll(shardSet);
                                        mergedMap.put(propId, existingSet);
                                    }
                                }
                                mergedCTP.put(classId, mergedMap);
                            }
                        }
                    }
                }
                
                // Save merged result
                String mergedShardName = "mergedCTP/CTP.ser";
                byte[] serializedData = Serialize.serialize(mergedCTP);
                StorageUtils.uploadBlob(mergedShardName, serializedData);
                
                // Also save as text for debugging
                StringBuilder debugData = new StringBuilder();
                for (Integer classId : mergedCTP.keySet()) {
                    debugData.append("CTP: ").append(classId).append(" -> \n");
                    java.util.Map<Integer, Set<Integer>> propMap = mergedCTP.get(classId);
                    for (Integer propId : propMap.keySet()) {
                        debugData.append("\t").append(propId).append(" -> ")
                                 .append(propMap.get(propId)).append("\n");
                    }
                }
                StorageUtils.uploadBlob(mergedShardName + ".txt", debugData.toString().getBytes());
                
                JsonObject result = new JsonObject();
                result.addProperty("merged_file", mergedShardName);
                result.addProperty("total_entries", mergedCTP.keySet().size());
                return result;
                
            } catch (Exception e) {
                e.printStackTrace();
                JsonObject error = new JsonObject();
                error.addProperty("error", e.getMessage());
                return error;
            }
        }
    }
}