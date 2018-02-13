/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.index.Language;
import de.citec.sc.syntax.DependencyParse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class DependencyParseTreeTest {

    @Test
    public void test() {

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

        //dep parse tree tests
        Assert.assertTrue(depParse.toString().length() > 10);
        Assert.assertTrue(depParse.getParentNode(1).equals(2));
    }

//    @Test
//    public void test2() {
//        Set<AnnotatedDocument> documents = CorpusLoader.load(CorpusLoader.Dataset.simpleQuestionsSubset, Language.lang.EN, true);
//
//        
//        for (AnnotatedDocument doc : documents) {
//
//            System.out.println(doc);
//            
//            String path1 = doc.getDependencyParse().getMergedDependencyRelation(2, 3, true);
//            String path2 = doc.getDependencyParse().getMergedDependencyRelation(3, 2, true);
//            
//            System.out.println("Path: "+path1);
//            System.out.println("Path: "+path2);
//            
//            Assert.assertEquals(path1, path2);
//            
//            
//            path1 = doc.getDependencyParse().getMergedDependencyRelation(2, 3, false);
//            path2 = doc.getDependencyParse().getMergedDependencyRelation(3, 2, false);
//            
//            System.out.println("Path without POS tags: "+path1);
//            System.out.println("Path without POS tags: "+path2);
//            
//            Assert.assertEquals(path1, path2);
//        }
//    }
}
