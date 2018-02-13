/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.query.SPARQLParser;
import de.citec.sc.query.Triple;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sherzod
 */
public class SPARQLParserTest {

    private static final Logger log = LogManager.getFormatterLogger(SPARQLParserTest.class.getName());

    @Test
    public void test() {
        String query = "SELECT ?x WHERE { ?x <http://dbpedia.org/ontology/stateOfOrigin> <http://dbpedia.org/resource/Ellen_Swallow_Richards> }";

        Set<Triple> triples = SPARQLParser.extractTriplesFromQuery(query);
        System.out.println("==========================================================");
        Assert.assertTrue(triples.size() > 0);
    }
}
