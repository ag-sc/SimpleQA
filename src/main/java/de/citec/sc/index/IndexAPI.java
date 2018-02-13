/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.index;

import arq.query;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import spark.Spark;

/**
 *
 * @author sherzod
 */
public class IndexAPI {

    public static void main(String[] args) {
        
        int port = 8080;
        Spark.port(port);

        System.out.println("Starting the service with port :" + port);
        
//        String path = "/home/sherzod/NetBeansProjects/LITD/rawFiles/en/resourceFiles";
        String path = "indexFiles";
        
        OnMemoryIndex index = new OnMemoryIndex();

        Spark.get("/findEntities", "application/json", (request, response) -> {
            //get input from client
            String query = request.queryParams("query");
            String k = request.queryParams("k");
            
            int topK = 0;

            try {
                query = URLDecoder.decode(query, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            
            try {
                k = URLDecoder.decode(k, "UTF-8");
                
                topK = Integer.parseInt(k);
                
            } catch (UnsupportedEncodingException ex) {
            }

            
            List<Instance> instances = index.getMatches(query.toLowerCase(), topK, Language.lang.EN, true);

            return instances;

        }, new JsonTransformer());
        
        Spark.get("/findPredicates", "application/json", (request, response) -> {
            //get input from client
            String query = request.queryParams("query");
            String k = request.queryParams("k");
            
            int topK = 0;

            try {
                query = URLDecoder.decode(query, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            
            try {
                k = URLDecoder.decode(k, "UTF-8");
                
                topK = Integer.parseInt(k);
                
            } catch (UnsupportedEncodingException ex) {
            }

            
            List<Instance> instances = index.getMatches(query.toLowerCase(), topK, Language.lang.EN, false);

            return instances;

        }, new JsonTransformer());
    }
}
