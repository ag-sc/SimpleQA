/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import net.ricecode.similarity.StringSimilarityMeasures;

/**
 *
 * @author sherzod
 */
public class StringSimilarityUtils {
    /**
     * levenstein sim
     */
    public static double getSimilarityScore(String node, String uri) {

        String label = convertURI2Label(uri);

        //compute levenstein edit distance similarity and normalize
        final double weightedEditSimilarity = StringSimilarityMeasures.score(label, node);

        return weightedEditSimilarity;
    }
    
    private static String convertURI2Label(String uri) {

        uri = uri.replace("http://dbpedia.org/resource/", "");
        uri = uri.replace("http://dbpedia.org/property/", "");
        uri = uri.replace("http://dbpedia.org/ontology/", "");
        uri = uri.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#type###", "");

        //remove the paranthese endings  e.g. The_South_(film)
        
        if(uri.contains("_(")){
            uri = uri.substring(0, uri.indexOf("_("));
        }
        uri = uri.replaceAll("@en", "");
        uri = uri.replaceAll("\"", "");
        uri = uri.replaceAll("_", " ");

        //replace capital letters with space
        //to tokenize compount classes e.g. ProgrammingLanguage => Programming Language
        String temp = "";
        for (int i = 0; i < uri.length(); i++) {
            String c = uri.charAt(i) + "";
            if (c.equals(c.toUpperCase())) {
                temp += " ";
            }
            temp += c;
        }
        //_(film)
        temp = temp.replaceAll("\\s+", " ");
        uri = temp.trim().toLowerCase();

        return uri;
    }
}
