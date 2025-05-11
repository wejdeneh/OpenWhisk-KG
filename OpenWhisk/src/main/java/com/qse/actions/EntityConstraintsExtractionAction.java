package com.qse.actions;

import com.google.gson.JsonObject;
import com.qse.models.*;
import com.qse.utils.StorageUtils;
import com.qse.encoders.StringEncoder;
import com.qse.serialization.Serialize;
import com.qse.utils.Constants;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class EntityConstraintsExtractionAction {
    
    private StringEncoder stringEncoder;
    private ETD entityDataHashMap;
    private CTP classToPropWithObjTypes = new CTP();
    private ShapeTripletSupport shapeTripletSupport;
    
    public static JsonObject main(JsonObject args) {
        try {
            EntityConstraintsExtractionAction extractor = new EntityConstraintsExtractionAction();
            return extractor.extractEntityConstraints(args);
        } catch (Exception e) {
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return error;
        }
    }
    
    private JsonObject extractEntityConstraints(JsonObject args) {
        Instant startTime = Instant.now();
        
        String filename = args.get("filename").getAsString();
        int subsetIndex = args.get("subset_index").getAsInt();
        
        // Create subfile path
        String subfilePath = getSubfilePath(filename, subsetIndex);
        
        Set<String> entities = null;
        stringEncoder = new StringEncoder();
        
        try {
            BufferedReader reader = StorageUtils.getBufferedReader(subfilePath);
            if (reader == null) {
                throw new RuntimeException("Could not read file: " + subfilePath);
            }
            
            // Split first line into entities set
            String entitiesLine = reader.readLine();
            entities = Set.of(entitiesLine.split(","));
            entityDataHashMap = new ETD(entities);
            
            // Read and process file in chunks
            List<String> lines;
            while ((lines = readNLines(reader, 1000)).size() > 0) {
                List<Node[]> nodesList = lines.stream().map(line -> {
                    try {
                        return NxParser.parseNodes(line);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                
                List<Node> entityNodes = nodesList.stream().map(nodes -> nodes[0]).collect(Collectors.toList());
                List<String> objectTypes = nodesList.stream().map(nodes -> extractObjectType(nodes[2].toString())).collect(Collectors.toList());
                List<Integer> propIDs = stringEncoder.encode(nodesList.stream().map(nodes -> nodes[1].getLabel()).collect(Collectors.toList()));
                List<Node> objectNodes = nodesList.stream().map(nodes -> nodes[2]).collect(Collectors.toList());
                
                // Load entity data from Redis
                entityDataHashMap.get(objectNodes);
                
                for (int i = 0; i < lines.size(); i++) {
                    Set<Integer> objTypesIDs = new HashSet<>(10);
                    Set<Tuple2<Integer, Integer>> prop2objTypeTuples = new HashSet<>(10);
                    
                    Node[] nodes = nodesList.get(i);
                    Node entityNode = entityNodes.get(i);
                    String objectType = objectTypes.get(i);
                    int propID = propIDs.get(i);
                    
                    // Process based on object type
                    if (objectType.equals("IRI")) {
                        objTypesIDs = parseIriTypeObject(objTypesIDs, prop2objTypeTuples, nodes, entityNode, propID);
                    } else {
                        objTypesIDs = parseLiteralTypeObject(objTypesIDs, entityNode, objectType, propID);
                    }
                    
                    // Update class to property with object types map
                    updateClassToPropWithObjTypesMap(objTypesIDs, entityNode, propID);
                }
            }
            
            reader.close();
            
            // Compute support
            shapeTripletSupport = new ShapeTripletSupport();
            entityDataHashMap.forEach((entity, entityData) -> {
                Set<Integer> instanceClasses = entityData.getClassTypes();
                if (instanceClasses != null) {
                    for (Integer c : instanceClasses) {
                        for (Tuple2<Integer, Integer> propObjTuple : entityData.getPropertyConstraints()) {
                            Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(c, propObjTuple._1, propObjTuple._2);
                            SupportConfidence sc = this.shapeTripletSupport.get(tuple3);
                            if (sc == null) {
                                this.shapeTripletSupport.put(tuple3, new SupportConfidence(1));
                            } else {
                                Integer newSupp = sc.getSupport() + 1;
                                sc.setSupport(newSupp);
                                this.shapeTripletSupport.put(tuple3, sc);
                            }
                        }
                    }
                }
            }, entities);
            
            // Save results
            String CTPFilename = "CTP/shard" + UUID.randomUUID() + ".ser";
            byte[] ctpData = Serialize.serialize(classToPropWithObjTypes);
            StorageUtils.uploadBlob(CTPFilename, ctpData);
            
            String shapeTripletSupportShardName = "shapeTripletSupport/shard" + UUID.randomUUID() + ".ser";
            byte[] supportData = Serialize.serialize(shapeTripletSupport);
            StorageUtils.uploadBlob(shapeTripletSupportShardName, supportData);
            
            JsonObject result = new JsonObject();
            result.addProperty("CTPFilename", CTPFilename);
            result.addProperty("shapeTripletSupportShardName", shapeTripletSupportShardName);
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
    
    private String extractObjectType(String literalIri) {
        Literal theLiteral = new Literal(literalIri, true);
        String type = null;
        if (theLiteral.getDatatype() != null) {
            type = theLiteral.getDatatype().toString();
        } else if (theLiteral.getLanguageTag() != null) {
            type = "<" + RDF.LANGSTRING + ">";
        } else {
            if (isValidIRI(literalIri)) {
                if (SimpleValueFactory.getInstance().createIRI(literalIri).isIRI()) {
                    type = "IRI";
                }
            } else {
                type = "<" + XSD.STRING + ">";
            }
        }
        return type;
    }
    
    private Set<Integer> parseIriTypeObject(Set<Integer> objTypesIDs, Set<Tuple2<Integer, Integer>> prop2objTypeTuples, 
                                            Node[] nodes, Node subject, int propID) {
        EntityData currEntityData = entityDataHashMap.get(nodes[2]);
        if (currEntityData != null && currEntityData.getClassTypes().size() != 0) {
            objTypesIDs = currEntityData.getClassTypes();
            for (Integer node : objTypesIDs) {
                prop2objTypeTuples.add(new Tuple2<>(propID, node));
            }
            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
        } else {
            int objID = stringEncoder.encode(Constants.OBJECT_UNDEFINED_TYPE);
            objTypesIDs.add(objID);
            prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
            addEntityToPropertyConstraints(prop2objTypeTuples, subject);
        }
        return objTypesIDs;
    }
    
    private void addEntityToPropertyConstraints(Set<Tuple2<Integer, Integer>> prop2objTypeTuples, Node subject) {
        EntityData currentEntityData = entityDataHashMap.get(subject);
        if (currentEntityData == null) {
            currentEntityData = new EntityData();
        }
        for (Tuple2<Integer, Integer> tuple2 : prop2objTypeTuples) {
            currentEntityData.addPropertyConstraint(tuple2._1, tuple2._2);
        }
        entityDataHashMap.put(subject, currentEntityData);
    }
    
    private Set<Integer> parseLiteralTypeObject(Set<Integer> objTypes, Node subject, String objectType, int propID) {
        Set<Tuple2<Integer, Integer>> prop2objTypeTuples;
        int objID = stringEncoder.encode(objectType);
        objTypes.add(objID);
        prop2objTypeTuples = Collections.singleton(new Tuple2<>(propID, objID));
        addEntityToPropertyConstraints(prop2objTypeTuples, subject);
        return objTypes;
    }
    
    private void updateClassToPropWithObjTypesMap(Set<Integer> objTypesIDs, Node entityNode, int propID) {
        EntityData entityData = entityDataHashMap.get(entityNode);
        if (entityData != null) {
            for (Integer entityTypeID : entityData.getClassTypes()) {
                Map<Integer, Set<Integer>> propToObjTypes = classToPropWithObjTypes.computeIfAbsent(entityTypeID, k -> new HashMap<>());
                Set<Integer> classObjTypes = propToObjTypes.computeIfAbsent(propID, k -> new HashSet<>());
                classObjTypes.addAll(objTypesIDs);
                propToObjTypes.put(propID, classObjTypes);
                classToPropWithObjTypes.put(entityTypeID, propToObjTypes);
            }
        }
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
    
    private boolean isValidIRI(String iri) {
        return iri.indexOf(':') > 0;
    }
    
    private String getSubfilePath(String filename, int subsetIndex) {
        String folder = filename.substring(0, filename.lastIndexOf('.'));
        return folder + "/subfile" + subsetIndex + ".nt";
    }
}