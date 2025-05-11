package com.qse.actions;

import com.google.gson.JsonObject;
import com.qse.models.ActionResult;

public class HttpTriggerAction {
    
    public static JsonObject main(JsonObject args) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract query parameters
            String filename = args.has("filename") ? args.get("filename").getAsString() : "lubm-mini.nt";
            Integer numberOfSubset = args.has("subset") ? args.get("subset").getAsInt() : 0;
            
            // Create input for orchestrator
            String inputParams = filename + " " + numberOfSubset.toString();
            
            // Create orchestrator input
            JsonObject orchestratorInput = new JsonObject();
            orchestratorInput.addProperty("input", inputParams);
            
            // Create successful result
            ActionResult result = ActionResult.success("Workflow initiated successfully")
                .withData("filename", filename)
                .withData("subset", numberOfSubset)
                .withData("workflow_started_at", System.currentTimeMillis())
                .withData("orchestrator_input", orchestratorInput)
                .withData("next_action", "qse/orchestrator")
                .withMetadata("action", "http-trigger")
                .withMetadata("version", "1.0")
                .withExecutionTime(System.currentTimeMillis() - startTime);
            
            // Convert to JsonObject for OpenWhisk
            JsonObject response = result.toJsonObject();
            
            // Set HTTP response headers
            JsonObject headers = new JsonObject();
            headers.addProperty("Content-Type", "application/json");
            headers.addProperty("Access-Control-Allow-Origin", "*");
            response.add("headers", headers);
            response.addProperty("statusCode", 200);
            
            return response;
            
        } catch (Exception e) {
            // Create error result
            ActionResult result = ActionResult.error("Failed to start workflow", e)
                .withMetadata("action", "http-trigger")
                .withExecutionTime(System.currentTimeMillis() - startTime);
            
            JsonObject response = result.toJsonObject();
            
            // Set error HTTP response
            JsonObject headers = new JsonObject();
            headers.addProperty("Content-Type", "application/json");
            headers.addProperty("Access-Control-Allow-Origin", "*");
            response.add("headers", headers);
            response.addProperty("statusCode", 500);
            
            return response;
        }
    }
}
