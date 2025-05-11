package com.qse.actions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrchestratorAction {
    
    public static JsonObject main(JsonObject args) {
        // Parse input parameters
        String inputparams = args.has("input") ? args.get("input").getAsString() : "";
        String[] params = inputparams.split(" ");
        
        String graphFileName = params.length > 0 ? params[0] : "lubm-mini.nt";
        Integer numberOfSubset = params.length > 1 ? Integer.parseInt(params[1]) : 0;
        
        // Create workflow result
        JsonObject result = new JsonObject();
        result.addProperty("workflow_id", "qse-" + System.currentTimeMillis());
        result.addProperty("filename", graphFileName);
        result.addProperty("subset_count", numberOfSubset);
        result.addProperty("start_time", System.currentTimeMillis());
        
        // Create workflow steps
        JsonArray workflowSteps = new JsonArray();
        
        // Step 1: Clean
        JsonObject cleanStep = new JsonObject();
        cleanStep.addProperty("action", "qse/clean");
        cleanStep.addProperty("phase", "clean");
        JsonObject cleanInput = new JsonObject();
        cleanInput.addProperty("workflow_id", result.get("workflow_id").getAsString());
        cleanStep.add("input", cleanInput);
        workflowSteps.add(cleanStep);
        
        // Step 2: Entity Extraction (parallel)
        JsonObject entityExtractionStep = new JsonObject();
        entityExtractionStep.addProperty("action", "parallel");
        entityExtractionStep.addProperty("phase", "entity_extraction");
        JsonArray entityExtractionTasks = new JsonArray();
        
        for (int i = 0; i < numberOfSubset; i++) {
            JsonObject task = new JsonObject();
            task.addProperty("action", "qse/entity-extraction");
            JsonObject taskInput = new JsonObject();
            taskInput.addProperty("filename", graphFileName);
            taskInput.addProperty("subset_index", i);
            task.add("input", taskInput);
            entityExtractionTasks.add(task);
        }
        entityExtractionStep.add("tasks", entityExtractionTasks);
        workflowSteps.add(entityExtractionStep);
        
        // Step 3: Entity Constraints Extraction (parallel)
        JsonObject entityConstraintsStep = new JsonObject();
        entityConstraintsStep.addProperty("action", "parallel");
        entityConstraintsStep.addProperty("phase", "entity_constraints_extraction");
        JsonArray entityConstraintsTasks = new JsonArray();
        
        for (int i = 0; i < numberOfSubset; i++) {
            JsonObject task = new JsonObject();
            task.addProperty("action", "qse/entity-constraints-extraction");
            JsonObject taskInput = new JsonObject();
            taskInput.addProperty("filename", graphFileName);
            taskInput.addProperty("subset_index", i);
            task.add("input", taskInput);
            entityConstraintsTasks.add(task);
        }
        entityConstraintsStep.add("tasks", entityConstraintsTasks);
        workflowSteps.add(entityConstraintsStep);
        
        // Step 4: Merge (parallel)
        JsonObject mergeStep = new JsonObject();
        mergeStep.addProperty("action", "parallel");
        mergeStep.addProperty("phase", "merge");
        JsonArray mergeTasks = new JsonArray();
        
        // Merge Support
        JsonObject mergeSupportTask = new JsonObject();
        mergeSupportTask.addProperty("action", "qse/merge-support");
        JsonObject mergeInput = new JsonObject();
        mergeInput.addProperty("subset_count", numberOfSubset);
        mergeSupportTask.add("input", mergeInput);
        mergeTasks.add(mergeSupportTask);
        
        // Merge CEC
        JsonObject mergeCECTask = new JsonObject();
        mergeCECTask.addProperty("action", "qse/merge-cec");
        mergeCECTask.add("input", mergeInput);
        mergeTasks.add(mergeCECTask);
        
        // Merge CTP
        JsonObject mergeCTPTask = new JsonObject();
        mergeCTPTask.addProperty("action", "qse/merge-ctp");
        mergeCTPTask.add("input", mergeInput);
        mergeTasks.add(mergeCTPTask);
        
        mergeStep.add("tasks", mergeTasks);
        workflowSteps.add(mergeStep);
        
        // Step 5: Shapes Extraction
        JsonObject shapesExtractionStep = new JsonObject();
        shapesExtractionStep.addProperty("action", "qse/shapes-extraction");
        shapesExtractionStep.addProperty("phase", "shapes_extraction");
        JsonObject shapesInput = new JsonObject();
        shapesExtractionStep.add("input", shapesInput);
        workflowSteps.add(shapesExtractionStep);
        
        result.add("workflow_steps", workflowSteps);
        
        // Return the workflow definition
        return result;
    }
}