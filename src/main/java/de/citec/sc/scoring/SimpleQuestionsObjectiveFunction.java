/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.scoring;

import de.citec.sc.query.SPARQLParser;
import de.citec.sc.semantics.QueryConstructor;
import de.citec.sc.utils.ProjectConfiguration;

import de.citec.sc.variable.State;
import de.citec.sc.variable.URIVariable;

import java.io.Serializable;
import learning.ObjectiveFunction;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

/**
 *
 * @author sherzod
 */
public class SimpleQuestionsObjectiveFunction extends ObjectiveFunction<State, String> implements Serializable {

    private boolean useQueryEvaluator = true;

    public void setUseQueryEvaluator(boolean useQueryEvaluator) {
        this.useQueryEvaluator = useQueryEvaluator;
    }

    public static double computeValue(String query, String goldState) {

        String constructedQuery = query;

        if (constructedQuery.trim().isEmpty()) {
            return 0;
        }

        if (SPARQLParser.extractTriplesFromQuery(constructedQuery).isEmpty()) {
            return 0;
        }

        double score = QueryEvaluator.evaluate(constructedQuery, goldState, false);

        return score;
    }

    @Override
    protected double computeScore(State state, String goldState) {

        String subject = "";
        String predicate = "";
        
        for(Integer tokenID : state.getUriVariables().keySet()){
            URIVariable var = state.getUriVariables().get(tokenID);
            
            if(var.getUri().startsWith("http://m.")){
                subject = var.getUri();
            }
            else{
               predicate = var.getUri();
            }
        }

        String constructedQuery = "SELECT ?x WHERE {<" + subject + "> <" + predicate + "> ?x .}";

//        String constructedQuery = QueryConstructor.getSPARQLQuery(state);
//
//        //get the query type from the query
//        int constructedQueryType = 1;
//        if (constructedQuery.contains("SELECT")) {
//            if (constructedQuery.contains("COUNT")) {
//                constructedQueryType = 2;
//            }
//        } else {
//            constructedQueryType = 3;
//        }
//
//        if (constructedQuery.trim().isEmpty()) {
//            return 0;
//        }
//
//        if (SPARQLParser.extractTriplesFromQuery(constructedQuery).isEmpty()) {
//            return 0;
//        }
        double score = QueryEvaluator.evaluate(constructedQuery, goldState, false);

        return score;
    }
}
