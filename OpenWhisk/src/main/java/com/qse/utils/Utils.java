package com.qse.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility methods for QSE OpenWhisk application
 */
public class Utils {
    
    private static long secondsTotal;
    private static long minutesTotal;
    
    /**
     * Get current timestamp in formatted string
     */
    public static void getCurrentTimeStamp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now));
        dtf.format(now);
    }
    
    /**
     * Check if a string is a valid IRI
     */
    public static boolean isValidIRI(String iri) {
        return iri != null && iri.indexOf(':') > 0;
    }
    
    /**
     * Convert string to IRI
     */
    public static IRI toIri(String value) {
        if (!isValidIRI(value)) {
            throw new IllegalArgumentException("Invalid IRI: " + value);
        }
        ValueFactory factory = SimpleValueFactory.getInstance();
        return factory.createIRI(value);
    }
    
    /**
     * Convert IRI string to Node
     */
    public static Node IriToNode(String Iri) {
        Resource subjRes = new Resource("<" + Iri + ">", true);
        Resource predRes = new Resource("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", true);
        BNode bn = new BNode("_:bnodeId", true);
        Node[] triple = new Node[]{subjRes, predRes, bn};
        return triple[0];
    }
    
    /**
     * Write a line to a file
     */
    public static void writeLineToFile(String line, String fileAddress) {
        try {
            FileWriter fileWriter = new FileWriter(fileAddress, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(line);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Simple test for IRI validation
     */
    public static void simpleTest() {
        ValueFactory factory = SimpleValueFactory.getInstance();
        String iri = "http://www.wikidata.org/entity/Q829554";
        IRI subj = factory.createIRI(iri);
        if (Utils.isValidIRI(iri)) {
            System.out.println("True");
        } else {
            System.out.println("False");
        }
    }
    
    /**
     * Get node with minimum scope from three nodes
     */
    public static BinaryNode getNodeWithMinimumScope(BinaryNode a, BinaryNode b, BinaryNode c) {
        BinaryNode smallest;
        if (a.scope < b.scope) {
            if (c.scope < a.scope) {
                smallest = c;
            } else {
                smallest = a;
            }
        } else {
            if (b.scope < c.scope) {
                smallest = b;
            } else {
                smallest = c;
            }
        }
        return smallest;
    }
    
    /**
     * Calculate log base 2
     */
    public static int logWithBase2(int x) {
        return (int) (Math.log(x) / Math.log(2) + 1e-10);
    }
    
    /**
     * Remove angle brackets from URI
     */
    public static String removeAngleBrackets(String uri) {
        if (uri.startsWith("<") && uri.endsWith(">")) {
            return uri.substring(1, uri.length() - 1);
        }
        return uri;
    }
    
    /**
     * Add angle brackets to URI if missing
     */
    public static String addAngleBrackets(String uri) {
        if (!uri.startsWith("<") || !uri.endsWith(">")) {
            return "<" + uri + ">";
        }
        return uri;
    }
    
    /**
     * Extract local name from IRI
     */
    public static String getLocalName(String iri) {
        String cleanIri = removeAngleBrackets(iri);
        int lastHash = cleanIri.lastIndexOf('#');
        int lastSlash = cleanIri.lastIndexOf('/');
        int splitPoint = Math.max(lastHash, lastSlash);
        
        if (splitPoint > 0 && splitPoint < cleanIri.length() - 1) {
            return cleanIri.substring(splitPoint + 1);
        }
        return cleanIri;
    }
    
    /**
     * Extract namespace from IRI
     */
    public static String getNamespace(String iri) {
        String cleanIri = removeAngleBrackets(iri);
        int lastHash = cleanIri.lastIndexOf('#');
        int lastSlash = cleanIri.lastIndexOf('/');
        int splitPoint = Math.max(lastHash, lastSlash);
        
        if (splitPoint > 0) {
            return cleanIri.substring(0, splitPoint + 1);
        }
        return cleanIri;
    }
    
    /**
     * Create shape IRI from class IRI
     */
    public static String createShapeIRI(String classIRI) {
        String localName = getLocalName(classIRI);
        return Constants.SHAPES_NAMESPACE + localName + "Shape";
    }
    
    /**
     * Create property shape IRI
     */
    public static String createPropertyShapeIRI(String propertyIRI, String classIRI) {
        String propLocalName = getLocalName(propertyIRI);
        String classLocalName = getLocalName(classIRI);
        return Constants.SHAPES_NAMESPACE + propLocalName + classLocalName + "ShapeProperty";
    }
    
    /**
     * Safe string conversion
     */
    public static String safeToString(Object obj) {
        return obj != null ? obj.toString() : "null";
    }
    
    /**
     * Format execution time
     */
    public static String formatExecutionTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.2fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
    
    /**
     * Get memory usage in MB
     */
    public static double getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024.0 * 1024.0);
    }
}