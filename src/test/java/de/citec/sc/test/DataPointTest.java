/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.corpus.DataPoint;
import de.citec.sc.syntax.DependencyParse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class DataPointTest {
    
    @Test
    public void test(){
        Set<String> answers = new HashSet<>();
        answers.add("Sherzod");
        DataPoint d1 = new DataPoint("Who created Goofy?", "Query this", answers, "1");
        
        Map<Integer, String> nodeMap = new HashMap<>();
        nodeMap.put(1, "Who");
        nodeMap.put(2, "created");
        nodeMap.put(3, "Goofy");
        
        Map<Integer, String> edgeMap = new HashMap<>();
        edgeMap.put(1, "nsubj");
        edgeMap.put(3, "dobj");
        
        Map<Integer, String> postagMap = new HashMap<>();
        postagMap.put(1, "PRON");
        postagMap.put(2, "VERB");
        postagMap.put(3, "PROPN");
        
        Map<Integer, Integer> relationMap = new HashMap<>();
        relationMap.put(3, 2);
        relationMap.put(1, 2);
        
        
        DependencyParse depParse = new DependencyParse(nodeMap, relationMap, edgeMap, postagMap, 2);
        
        System.out.println("==========================================================");
        System.out.println("DataPoint:\n"+d1+"\n");
        System.out.println("Dependency parse tree:\n"+depParse);
        
        Assert.assertTrue(d1.toString().length() > 10);
        //dep parse tree tests
        Assert.assertTrue(depParse.toString().length() > 10);
        Assert.assertTrue(depParse.getParentNode(1).equals(2));
        
        
    }
}
