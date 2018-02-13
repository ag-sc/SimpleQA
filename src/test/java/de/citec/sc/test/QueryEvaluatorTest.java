/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.query.SPARQLParser;
import de.citec.sc.scoring.QueryEvaluator;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class QueryEvaluatorTest {

    @Test
    public void test() {
        String query = "SELECT DISTINCT  * "
                + " WHERE"
                + "  { ?v1  <http://dbpedia.org/ontology/stateOfOrigin>  <http://dbpedia.org/resource/Ellen_Swallow_Richards>}";

        String goldQuery = "SELECT DISTINCT  ?v1 WHERE "
                + "  { ?v1  <http://dbpedia.org/ontology/stateOfOrigin>  <http://dbpedia.org/resource/Ellen_Swallow_Richards>}";
        
        double score = QueryEvaluator.evaluate(query, goldQuery, false);
        
        System.out.println(score);
        
        Assert.assertTrue(score == 1.0);
    }
}
