package com.qse.actions;

import com.google.gson.JsonObject;
import com.qse.models.*;
import com.qse.utils.StorageUtils;
import com.qse.encoders.StringEncoder;
import com.qse.serialization.Serialize;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityExtractionAction {
    
    private static final String typePredicate = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    
    public static JsonObject main(JsonObject args) {
        try {
            String filename = args.get("filename").getAsString();
            int subsetIndex = args.get("subset_index").getAsInt();
            
            // Create subfile path
            String subfilePath = getSubfilePath(filename, subsetIndex);
            
            Instant startTime = Instant.now();
            ETD entityDataHashMap = new ETD();
            CEC classEntityCount = new CEC();
            StringEncoder stringEncoder = new StringEncoder();
            
            // Read from storage
            BufferedReader reader = StorageUtils.getBufferedReader(subfilePath);
            if (reader == null) {
                throw new RuntimeException("Could not read file: " + subfilePath);
            }
            
            // Skip entities line
            reader.readLine();
            
            // Process in batches
            List<String> lines;
            while ((lines = readNLines(reader, 1000)).size() > 0) {
                List<Node[]> nodes = lines.stream().map(line -> {
                    try {
                        return NxParser.parseNodes(line);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());
                
                // Filter by type predicate
                nodes = nodes.stream()
                    .filter(node -> node != null && node.length > 1 && node[1].toString().equals(typePredicate))
                    .collect(Collectors.toList());
                
                List<Node> subjects = nodes.stream().map(node -> node[0]).collect(Collectors.toList());
                List<String> objects = nodes.stream().map(node -> node[2].getLabel()).collect(Collectors.toList());
                List<Integer> objIDs = stringEncoder.encode(objects);
                
                for (int i = 0; i < subjects.size(); i++) {
                    EntityData entityData = entityDataHashMap.get(subjects.get(i));
                    if (entityData == null) {
                        entityData = new EntityData();
                    }
                    entityData.addClassType(objIDs.get(i));
                    entityDataHashMap.put(subjects.get(i), entityData);
                    classEntityCount.merge(objIDs.get(i), 1, Integer::sum);
                }
            }
            
            reader.close();
            
            // Save CEC to storage
            String CECShardName = "classEntityCount/shard" + UUID.randomUUID() + ".ser";
            byte[] serializedCEC = Serialize.serialize(classEntityCount);
            StorageUtils.uploadBlob(CECShardName, serializedCEC);
            
            // Save ETD to Redis
            entityDataHashMap.pushInRedis();
            
            // Create result
            JsonObject result = new JsonObject();
            result.addProperty("CECShardName", CECShardName);
            result.addProperty("processing_time", Instant.now().toEpochMilli() - startTime.toEpochMilli());
            result.addProperty("subset_index", subsetIndex);
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return error;
        }
    }
    
    private static String getSubfilePath(String filename, int subsetIndex) {
        String folder = filename.substring(0, filename.lastIndexOf('.'));
        return folder + "/subfile" + subsetIndex + ".nt";
    }
    
    private static List<String> readNLines(BufferedReader reader, int n) throws IOException {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String line = reader.readLine();
            if (line == null) {
                return lines;
            }
            lines.add(line);
        }
        return lines;
    }
}