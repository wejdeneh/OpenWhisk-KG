package com.qse.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants used throughout the QSE OpenWhisk application
 */
public class Constants {
    // Namespace constants
    public static final String SHAPES_NAMESPACE = "http://shaclshapes.org/";
    public static final String SHACL_NAMESPACE = "http://www.w3.org/ns/shacl#";
    public static final String RDF_TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    public static final String INSTANCE_OF = "<http://www.wikidata.org/prop/direct/P31>";
    public static final String SUB_CLASS_OF = "<http://www.w3.org/2000/01/rdf-schema#subClassOf>";
    
    // Custom properties
    public static final String OBJECT_UNDEFINED_TYPE = "http://shaclshapes.org/object-type/undefined";
    public static final String MEMBERSHIP_GRAPH_ROOT_NODE = "<http://www.schema.hng.root> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.schema.hng.root#HNG_Root> .";
    
    // Custom SHACL properties
    public static final IRI CONFIDENCE = SimpleValueFactory.getInstance().createIRI("http://shaclshapes.org/confidence");
    public static final IRI SUPPORT = SimpleValueFactory.getInstance().createIRI("http://shaclshapes.org/support");
    
    // Storage constants
    public static final String DEFAULT_BUCKET = "qse-storage";
    public static final String SUBFILES_PREFIX = "subfiles/";
    public static final String CLASSCOUNT_PREFIX = "classEntityCount/";
    public static final String CTP_PREFIX = "CTP/";
    public static final String SUPPORT_PREFIX = "shapeTripletSupport/";
    public static final String MERGED_PREFIX = "merged/";
    public static final String SHAPES_PREFIX = "output_shapes/";
    
    // Action names
    public static final String ACTION_ORCHESTRATOR = "qse/orchestrator";
    public static final String ACTION_HTTP_TRIGGER = "qse/http-trigger";
    public static final String ACTION_CLEAN = "qse/clean";
    public static final String ACTION_ENTITY_EXTRACTION = "qse/entity-extraction";
    public static final String ACTION_ENTITY_CONSTRAINTS = "qse/entity-constraints-extraction";
    public static final String ACTION_SHAPES_EXTRACTION = "qse/shapes-extraction";
    public static final String ACTION_MERGE_SUPPORT = "qse/merge-support";
    public static final String ACTION_MERGE_CEC = "qse/merge-cec";
    public static final String ACTION_MERGE_CTP = "qse/merge-ctp";
    
    // Redis keys
    public static final String REDIS_ETD_HASH = "ETD";
    public static final String REDIS_COUNTER_KEY = "counter";
    public static final String REDIS_ID_TO_VAL = "StringEncoderTable";
    public static final String REDIS_VAL_TO_ID = "StringEncoderRev";
    
    // Default values
    public static final int DEFAULT_CACHE_SIZE = 10000;
    public static final int DEFAULT_BATCH_SIZE = 1000;
    public static final int DEFAULT_MEMORY_LIMIT = 1024;
    public static final int DEFAULT_TIMEOUT = 300000; // 5 minutes
    
    // HTTP constants
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CORS_HEADER = "Access-Control-Allow-Origin";
    public static final String CORS_VALUE = "*";
    
    // Error messages
    public static final String ERROR_FILE_NOT_FOUND = "File not found";
    public static final String ERROR_REDIS_CONNECTION = "Redis connection failed";
    public static final String ERROR_STORAGE_CONNECTION = "Storage connection failed";
    public static final String ERROR_SERIALIZATION = "Serialization error";
    public static final String ERROR_INVALID_INPUT = "Invalid input parameters";
    
    // Utility methods for constants
    public static String getSubfilePath(String filename, int index) {
        String folder = filename.substring(0, filename.lastIndexOf('.'));
        return folder + "/subfile" + index + ".nt";
    }
    
    public static String getShardName(String prefix, String identifier) {
        return prefix + identifier + ".ser";
    }
    
    public static String getMergedName(String prefix) {
        return prefix + "merged.ser";
    }
}