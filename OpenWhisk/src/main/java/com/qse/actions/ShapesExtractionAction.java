package com.qse.actions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qse.models.*;
import com.qse.utils.StorageUtils;
import com.qse.encoders.StringEncoder;
import com.qse.serialization.Serialize;
import com.qse.utils.Constants;
import com.qse.utils.Utils;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;

import java.io.*;
import java.util.*;

import static org.eclipse.rdf4j.model.util.Values.bnode;

public class ShapesExtractionAction {
    
    private ValueFactory factory = SimpleValueFactory.getInstance();
    private StringEncoder encoder;
    private CEC classInstanceCount;
    private String typePredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private ShapeTripletSupport shapeTripletSupport;
    
    public static JsonObject main(JsonObject args) {
        try {
            ShapesExtractionAction extractor = new ShapesExtractionAction();
            return extractor.extractShapes(args);
        } catch (Exception e) {
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            error.addProperty("stack_trace", getStackTrace(e));
            return error;
        }
    }
    
    private JsonObject extractShapes(JsonObject args) {
        String SUPPShardName = args.get("SUPPShardName").getAsString();
        String CECShardName = args.get("CECShardName").getAsString();
        String CTPShardName = args.get("CTPShardName").getAsString();
        
        // Read the support confidence of shape triplets
        shapeTripletSupport = readShapeTripletSupportConfidence(SUPPShardName);
        // Read the class entity count map
        classInstanceCount = readClassEntityCount(CECShardName);
        
        // Compute Confidence
        for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : shapeTripletSupport.entrySet()) {
            SupportConfidence value = entry.getValue();
            double confidence = (double) value.getSupport() / classInstanceCount.get(entry.getKey()._1);
            value.setConfidence(confidence);
        }
        
        encoder = new StringEncoder();
        CTP classToPropWithObjTypes = readClassToPropWithObjTypes(CTPShardName);
        
        // Build shapes without pruning
        Model model = null;
        ModelBuilder builder = new ModelBuilder();
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> entry : classToPropWithObjTypes.entrySet()) {
            Integer encodedClassIRI = entry.getKey();
            Map<Integer, Set<Integer>> propToObjectType = entry.getValue();
            buildShapes(builder, encodedClassIRI, propToObjectType);
        }
        model = builder.build();
        
        // Collect all statements
        List<String> statements = new ArrayList<>();
        model.forEach(st -> {
            statements.add(st.toString());
        });
        
        // Save shapes to storage
        String shapesPath = "output_shapes/shapes_" + System.currentTimeMillis() + ".ttl";
        
        try {
            // Save as Turtle format
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Rio.write(model, baos, RDFFormat.TURTLE);
            StorageUtils.uploadBlob(shapesPath, baos.toByteArray());
            
            // Also save formatted version
            String formattedPath = "output_shapes/formatted_shapes_" + System.currentTimeMillis() + ".ttl";
            TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
            org.apache.jena.rdf.model.Model jenaModel = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            jenaModel.read(bais, "", "TURTLE");
            
            ByteArrayOutputStream formattedBaos = new ByteArrayOutputStream();
            formatter.accept(jenaModel, formattedBaos);
            StorageUtils.uploadBlob(formattedPath, formattedBaos.toByteArray());
            
            JsonObject result = new JsonObject();
            result.addProperty("shapes_file", shapesPath);
            result.addProperty("formatted_shapes_file", formattedPath);
            result.addProperty("statement_count", statements.size());
            result.addProperty("shapes_content", statements.toString());
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to save shapes: " + e.getMessage());
            return error;
        }
    }
    
    private void buildShapes(ModelBuilder b, Integer encodedClassIRI, Map<Integer, Set<Integer>> propToObjectType) {
        if (Utils.isValidIRI(encoder.decode(encodedClassIRI))) {
            IRI subj = factory.createIRI(encoder.decode(encodedClassIRI));
            String nodeShape = Constants.SHAPES_NAMESPACE + subj.getLocalName() + "Shape";
            b.subject(nodeShape)
                .add(RDF.TYPE, SHACL.NODE_SHAPE)
                .add(SHACL.TARGET_CLASS, subj);
            
            // Annotate with support
            b.subject(nodeShape).add(Constants.SUPPORT, classInstanceCount.get(encodedClassIRI));
            
            if (propToObjectType != null) {
                constructPropertyShapes(b, subj, encodedClassIRI, nodeShape, propToObjectType);
            }
        } else {
            System.out.println("INVALID SUBJECT IRI: " + encoder.decode(encodedClassIRI));
        }
    }
    
    private void constructPropertyShapes(ModelBuilder b, IRI subj, Integer subjEncoded, String nodeShape, 
                                         Map<Integer, Set<Integer>> propToObjectTypesLocal) {
        Map<String, Integer> propDuplicateDetector = new HashMap<>();
        
        propToObjectTypesLocal.forEach((prop, propObjectTypes) -> {
            ModelBuilder localBuilder = new ModelBuilder();
            IRI property = factory.createIRI(encoder.decode(prop));
            String localName = property.getLocalName();
            
            boolean isInstanceTypeProperty = property.toString().equals(remAngBrackets(typePredicate));
            if (isInstanceTypeProperty) {
                localName = "instanceType";
            }
            
            if (propDuplicateDetector.containsKey(localName)) {
                int freq = propDuplicateDetector.get(localName);
                propDuplicateDetector.put(localName, freq + 1);
                localName = localName + "_" + freq;
            }
            propDuplicateDetector.putIfAbsent(localName, 1);
            
            IRI propShape = factory.createIRI(Constants.SHAPES_NAMESPACE + localName + subj.getLocalName() + "ShapeProperty");
            
            b.subject(nodeShape).add(SHACL.PROPERTY, propShape);
            b.subject(propShape)
                .add(RDF.TYPE, SHACL.PROPERTY_SHAPE)
                .add(SHACL.PATH, property);
            
            if (isInstanceTypeProperty) {
                Resource head = bnode();
                List<Resource> members = Arrays.asList(new Resource[]{subj});
                Model tempModel = RDFCollections.asRDF(members, head, new LinkedHashModel());
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                });
                tempModel.add(propShape, SHACL.IN, head);
                b.build().addAll(tempModel);
                b.build().addAll(localBuilder.build());
            }
            
            int numberOfObjectTypes = propObjectTypes.size();
            
            if (numberOfObjectTypes == 1 && !isInstanceTypeProperty) {
                propObjectTypes.forEach(encodedObjectType -> {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                        }
                    }
                    String objectType = encoder.decode(encodedObjectType);
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {objectType = objectType.replace("<", "").replace(">", "");}
                            IRI objectTypeIri = factory.createIRI(objectType);
                            b.subject(propShape).add(SHACL.DATATYPE, objectTypeIri);
                            b.subject(propShape).add(SHACL.NODE_KIND, SHACL.LITERAL);
                            annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                        } else {
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                b.subject(propShape).add(SHACL.CLASS, objectTypeIri);
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                            } else {
                                b.subject(propShape).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(propShape, localBuilder, tuple3);
                                if (objectType.equals(Constants.OBJECT_UNDEFINED_TYPE))
                                    b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                            }
                        }
                    } else {
                        b.subject(propShape).add(SHACL.DATATYPE, XSD.STRING);
                    }
                });
                
                b.build().addAll(localBuilder.build());
            }
            if (numberOfObjectTypes > 1) {
                List<Resource> members = new ArrayList<>();
                Resource headMember = bnode();
                
                for (Integer encodedObjectType : propObjectTypes) {
                    Tuple3<Integer, Integer, Integer> tuple3 = new Tuple3<>(encoder.encode(subj.stringValue()), prop, encodedObjectType);
                    String objectType = encoder.decode(encodedObjectType);
                    Resource currentMember = bnode();
                    
                    if (shapeTripletSupport.containsKey(tuple3)) {
                        if (shapeTripletSupport.get(tuple3).getSupport().equals(classInstanceCount.get(encoder.encode(subj.stringValue())))) {
                            b.subject(propShape).add(SHACL.MIN_COUNT, factory.createLiteral(XMLDatatypeUtil.parseInteger("1")));
                        }
                    }
                    
                    if (objectType != null) {
                        if (objectType.contains(XSD.NAMESPACE) || objectType.contains(RDF.LANGSTRING.toString())) {
                            if (objectType.contains("<")) {objectType = objectType.replace("<", "").replace(">", "");}
                            IRI objectTypeIri = factory.createIRI(objectType);
                            localBuilder.subject(currentMember).add(SHACL.DATATYPE, objectTypeIri);
                            localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.LITERAL);
                            annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
                        } else {
                            if (Utils.isValidIRI(objectType) && !objectType.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                                IRI objectTypeIri = factory.createIRI(objectType);
                                localBuilder.subject(currentMember).add(SHACL.CLASS, objectTypeIri);
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
                            } else {
                                localBuilder.subject(currentMember).add(SHACL.NODE_KIND, SHACL.IRI);
                                annotateWithSupportAndConfidence(currentMember, localBuilder, tuple3);
                            }
                        }
                    } else {
                        localBuilder.subject(currentMember).add(SHACL.DATATYPE, XSD.STRING);
                    }
                    members.add(currentMember);
                }
                Model localModel = RDFCollections.asRDF(members, headMember, new LinkedHashModel());
                localModel.add(propShape, SHACL.OR, headMember);
                localModel.addAll(localBuilder.build());
                b.build().addAll(localModel);
            }
        });
    }
    
    private ShapeTripletSupport readShapeTripletSupportConfidence(String SUPPShardName) {
        byte[] shapeTripletSupportBytes = StorageUtils.downloadBlob(SUPPShardName);
        return (ShapeTripletSupport) Serialize.deserialize(shapeTripletSupportBytes);
    }
    
    private CEC readClassEntityCount(String CECShardName) {
        byte[] CECBytes = StorageUtils.downloadBlob(CECShardName);
        return (CEC) Serialize.deserialize(CECBytes);
    }
    
    private CTP readClassToPropWithObjTypes(String CTPShardName) {
        byte[] CTPBytes = StorageUtils.downloadBlob(CTPShardName);
        return (CTP) Serialize.deserialize(CTPBytes);
    }
    
    private String remAngBrackets(String typePredicate) {
        return typePredicate.replace("<", "").replace(">", "");
    }
    
    private void annotateWithSupportAndConfidence(Resource currentMember, ModelBuilder localBuilder, 
                                                  Tuple3<Integer, Integer, Integer> tuple3) {
        if (shapeTripletSupport.containsKey(tuple3)) {
            Literal entities = Values.literal(shapeTripletSupport.get(tuple3).getSupport());
            localBuilder.subject(currentMember).add(Constants.SUPPORT, entities);
            Literal confidence = Values.literal(shapeTripletSupport.get(tuple3).getConfidence());
            localBuilder.subject(currentMember).add(Constants.CONFIDENCE, confidence);
        }
    }
    
    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}