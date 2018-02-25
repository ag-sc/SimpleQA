/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import de.citec.sc.utils.FileUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class OverlapWithEntities {
    public static void main(String[] args) {
        Set<String> entities = new HashSet<>();
        
        String filePath = "freebaseFiles/freebase-FB2M.txt";
        Set<String> content = FileUtil.readFile(filePath);

        for (String item : content) {

            String[] c = item.split("\t");
            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

            entities.add(subject);
        }
        
        
        Set<String> processedNodes = loadProcessedNodes();
        
        int count = 0;
        for(String e : entities){
            if(processedNodes.contains(e)){
                count++;
            }
        }
        
        double d = count/(double) entities.size();
        System.out.println(d+"  "+count+"--"+entities.size());
    }
    private static Set<String> loadProcessedNodes(){
        String filePath1 = "freebaseSequenceFiles/processedNodes.txt";
        Set<String> processedNodeIDs = FileUtil.readFile(filePath1);
        
        String filePath2 = "freebaseFiles/freebaseEntities.txt";
        Set<String> content = FileUtil.readFile(filePath2);
        
        Map<String, String> map = new HashMap<>();
        for (String item : content) {

            String[] c = item.split("\t");
            String uri = c[0];
            String id = c[1];
            
            map.put(id, uri);
        }
        
       Set<String> processedNodes = new HashSet<>();
       for(String id : processedNodeIDs){
           if(map.containsKey(id)){
               processedNodes.add(map.get(id));
           }
       }
       
        System.out.println("Processed node ids: "+processedNodeIDs.size());
        System.out.println("Processed nodes: "+processedNodes.size());
       
       return processedNodes;
    }
}
