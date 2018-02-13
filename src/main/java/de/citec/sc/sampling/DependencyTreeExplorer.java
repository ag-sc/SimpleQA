/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.sampling;

import de.citec.sc.query.SPARQLParser;
import de.citec.sc.query.Triple;
import de.citec.sc.utils.ProjectConfiguration;
import de.citec.sc.utils.TrainingLexiconUtil;
import de.citec.sc.variable.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sampling.Explorer;

/**
 *
 * @author sherzod
 */
public class DependencyTreeExplorer implements Explorer<State> {

    private Map<Integer, String> dudesMap;
    private Set<String> validPOSTags;

    public DependencyTreeExplorer(Map<Integer, String> dudesMap) {
        this.dudesMap = dudesMap;

        validPOSTags = new HashSet<>();
        validPOSTags.add("PROPN");
        validPOSTags.add("VERB");
        validPOSTags.add("PRON");
        validPOSTags.add("NOUN");
        validPOSTags.add("ADJ");
        validPOSTags.add("ADV");
        validPOSTags.add("ADP");
    }

    @Override
    public List<State> getNextStates(State inputState) {

        //check if the training procedure
        if (ProjectConfiguration.getProcessName().equals("train")) {
            return getStatesDuringTraining(inputState);
        } else {
            return getStatesDuringPrediction(inputState);
        }

    }

    /**
     * extracts URIs from the query and maps them to DUDE types the returned map
     * contains dude ID as key and all URIs extracted for that specific type
     *
     * @param query
     * @return Map<Integer. Set<String>> dudes2URIMap
     */
    private Map<Integer, Set<String>> getDUDESMappedToURIs(String query) {
        Map<Integer, Set<String>> dudes2URIMap = new HashMap<>();
        //get triples from query
        Set<Triple> triples = SPARQLParser.extractTriplesFromQuery(query);
        //create a map of dudes ID to URIs from the query
        for (Triple t : triples) {
            if (t.IsReturnVariable()) {

                String uri = "What_URI";
                for (Integer dudeId : dudesMap.keySet()) {
                    if (dudesMap.get(dudeId).equals("What")) {
                        Set<String> uris = new HashSet<>();
                        //add previously added ones
                        if (dudes2URIMap.containsKey(dudeId)) {
                            uris.addAll(dudes2URIMap.get(dudes2URIMap));
                        }
                        //add the current predicate uri
                        uris.add(uri);

                        dudes2URIMap.put(dudeId, uris);
                    }
                }

            } //get uris from sub, obj, predicate
            else {
                String predicateURI = t.getPredicate().getPredicateName();

                //find the dude id where the dude name equals to Property
                for (Integer dudeId : dudesMap.keySet()) {
                    if (dudesMap.get(dudeId).equals("Property")) {
                        Set<String> uris = new HashSet<>();
                        //add previously added ones
                        if (dudes2URIMap.containsKey(dudeId)) {
                            uris.addAll(dudes2URIMap.get(dudes2URIMap));
                        }
                        //add the current predicate uri
                        uris.add(predicateURI);

                        dudes2URIMap.put(dudeId, uris);
                    }
                }

                if (!t.getObject().isVariable()) {
                    //find the dude id where the dude name equals to Individual
                    String uri = t.getObject().toString();
                    for (Integer dudeId : dudesMap.keySet()) {
                        if (dudesMap.get(dudeId).equals("Individual")) {
                            Set<String> uris = new HashSet<>();
                            //add previously added ones
                            if (dudes2URIMap.containsKey(dudeId)) {
                                uris.addAll(dudes2URIMap.get(dudes2URIMap));
                            }
                            //add the current predicate uri
                            uris.add(uri);

                            dudes2URIMap.put(dudeId, uris);
                        }
                    }
                }
                if (!t.getSubject().isVariable()) {
                    //find the dude id where the dude name equals to Individual
                    String uri = t.getSubject().toString();
                    for (Integer dudeId : dudesMap.keySet()) {
                        if (dudesMap.get(dudeId).equals("Individual")) {
                            Set<String> uris = new HashSet<>();
                            //add previously added ones
                            if (dudes2URIMap.containsKey(dudeId)) {
                                uris.addAll(dudes2URIMap.get(dudes2URIMap));
                            }
                            //add the current predicate uri
                            uris.add(uri);

                            dudes2URIMap.put(dudeId, uris);
                        }
                    }
                }
            }
        }

        return dudes2URIMap;
    }

    private List<State> getStatesDuringTraining(State inputState) {

        String query = inputState.getDocument().getDataPoint().getQuery();
        Map<Integer, Set<String>> dudes2URIMap = getDUDESMappedToURIs(query);

        boolean continueExploring = true;

        //add the inital state
        Set<State> nextStates = new HashSet<>();
        nextStates.add(inputState);

        while (continueExploring) {

            int hiddenVariableSize = 0;
            for (Integer dudeID : dudes2URIMap.keySet()) {

                Set<String> uris = dudes2URIMap.get(dudeID);

                hiddenVariableSize += uris.size();
            }

            Set<State> generatedStates = new HashSet<>();

            for (State currentState : nextStates) {

                //check if the uri variables are equal to the number of expected uri variables
                //no need to explore, because the uri variables can at most as the dude_types* uris
                //strategy to exit the nextStates loop
                if (currentState.getUriVariables().size() == hiddenVariableSize) {
                    continueExploring = false;
                    //break the loop from exploring states in nextStates Set
                    break;
                }

                for (Integer tokenID : currentState.getDocument().getDependencyParse().getTokenMap().keySet()) {

                    String pos = currentState.getDocument().getDependencyParse().getPOSTag(tokenID);

                    //don't assign any URI and DUDE to non-valid postags such as 
                    if (!validPOSTags.contains(pos)) {
                        continue;
                    }

                    for (Integer dudeID : dudes2URIMap.keySet()) {

                        Set<String> uris = dudes2URIMap.get(dudeID);

                        for (String uri : uris) {

                            if (currentState.exploredBefore(dudeID, uri)) {
                                continue;
                            }

                            //add slots as well
                            if (dudesMap.get(dudeID).equals("Property")) {

                                //assign all possible slots to all nodes
                                Set<Integer> candidateTokenIDs = new HashSet<>();
                                for (Integer t1 : currentState.getDocument().getDependencyParse().getTokenMap().keySet()) {

                                    String candidateTokenPOS = currentState.getDocument().getDependencyParse().getPOSTag(t1);

                                    //don't assign slots to tokens without valid postag
                                    if (!validPOSTags.contains(candidateTokenPOS)) {
                                        continue;
                                    }

                                    //get all nodes except the current tokenID
                                    if (!t1.equals(tokenID)) {
                                        candidateTokenIDs.add(t1);
                                    }
                                }

                                //assign slot 1 to the first token and then loop over other remaining tokenIDs and assign 2
                                for (Integer candidateTokenID1 : candidateTokenIDs) {

                                    for (Integer candidateTokenID2 : candidateTokenIDs) {
                                        //don't add slot 1 and 2 to the same tokenID
                                        if (candidateTokenID1.equals(candidateTokenID2)) {
                                            continue;
                                        }

                                        State newStateWithSlot = new State(currentState);
                                        newStateWithSlot.addURIVariable(tokenID, dudeID, uri);
                                        //add the slot 1
                                        newStateWithSlot.addSlotVariable(candidateTokenID1, tokenID, 1);
                                        //add the slot 2
                                        newStateWithSlot.addSlotVariable(candidateTokenID2, tokenID, 2);

                                        //add to generated states
                                        generatedStates.add(newStateWithSlot);
                                    }
                                }

                            } else {
                                //other dude types don't assign slots
                                State newState = new State(currentState);
                                newState.addURIVariable(tokenID, dudeID, uri);

                                generatedStates.add(newState);
                            }
                        }
                    }
                }
            }

            if (!continueExploring) {
                //if no need for further exploration then just return the generated states

                Set<State> newStates = new HashSet<>();
                for (State s1 : nextStates) {
                    if (s1.getUriVariables().size() == hiddenVariableSize) {
                        newStates.add(s1);
                    }
                }
                return new ArrayList<>(newStates);
            } else {
                //clear the next states, add states in generatedStates
                nextStates.clear();
                for (State s1 : generatedStates) {
                    nextStates.add(new State(s1));
                }
                //clear the generatedStates as well
                generatedStates.clear();
            }
        }

        return new ArrayList<>();
    }

    private List<State> getStatesDuringPrediction(State inputState) {

        boolean continueExploring = true;

        //add the inital state
        Set<State> nextStates = new HashSet<>();
        nextStates.add(inputState);

        while (continueExploring) {

            int hiddenVariableSize = 3;

            Set<State> generatedStates = new HashSet<>();

            for (State currentState : nextStates) {

                //check if the uri variables are equal to the number of expected uri variables
                //no need to explore, because the uri variables can at most as the dude_types* uris
                //strategy to exit the nextStates loop
                if (currentState.getUriVariables().size() == hiddenVariableSize) {
                    continueExploring = false;
                    //break the loop from exploring states in nextStates Set
                    break;
                }

                for (Integer tokenID : currentState.getDocument().getDependencyParse().getTokenMap().keySet()) {

                    String lemma = currentState.getDocument().getDependencyParse().getToken(tokenID);
                    String pos = currentState.getDocument().getDependencyParse().getPOSTag(tokenID);

                    //don't assign any URI and DUDE to non-valid postags such as 
                    if (!validPOSTags.contains(pos)) {
                        continue;
                    }

                    for (Integer dudeID : dudesMap.keySet()) {

                        Set<String> uris = TrainingLexiconUtil.getURIs(lemma, dudeID);

                        for (String uri : uris) {

                            if (currentState.exploredBefore(dudeID, uri)) {
                                continue;
                            }

                            //add slots as well
                            if (dudesMap.get(dudeID).equals("Property")) {

                                //assign all possible slots to all nodes
                                Set<Integer> candidateTokenIDs = new HashSet<>();
                                for (Integer t1 : currentState.getDocument().getDependencyParse().getTokenMap().keySet()) {

                                    String candidateTokenPOS = currentState.getDocument().getDependencyParse().getPOSTag(t1);

                                    //don't assign slots to tokens without valid postag
                                    if (!validPOSTags.contains(candidateTokenPOS)) {
                                        continue;
                                    }

                                    //get all nodes except the current tokenID
                                    if (!t1.equals(tokenID)) {
                                        candidateTokenIDs.add(t1);
                                    }
                                }

                                //assign slot 1 to the first token and then loop over other remaining tokenIDs and assign 2
                                for (Integer candidateTokenID1 : candidateTokenIDs) {

                                    for (Integer candidateTokenID2 : candidateTokenIDs) {
                                        //don't add slot 1 and 2 to the same tokenID
                                        if (candidateTokenID1.equals(candidateTokenID2)) {
                                            continue;
                                        }

                                        State newStateWithSlot = new State(currentState);
                                        newStateWithSlot.addURIVariable(tokenID, dudeID, uri);
                                        //add the slot 1
                                        newStateWithSlot.addSlotVariable(candidateTokenID1, tokenID, 1);
                                        //add the slot 2
                                        newStateWithSlot.addSlotVariable(candidateTokenID2, tokenID, 2);

                                        //add to generated states
                                        generatedStates.add(newStateWithSlot);
                                    }
                                }

                            } else {
                                //other dude types don't assign slots
                                State newState = new State(currentState);
                                newState.addURIVariable(tokenID, dudeID, uri);

                                generatedStates.add(newState);
                            }
                        }
                    }
                }
            }

            if (!continueExploring) {
                //if no need for further exploration then just return the generated states

                Set<State> newStates = new HashSet<>();
                for (State s1 : nextStates) {
                    if (s1.getUriVariables().size() == hiddenVariableSize) {
                        newStates.add(s1);
                    }
                }
                return new ArrayList<>(newStates);
            } else {
                //clear the next states, add states in generatedStates
                nextStates.clear();
                for (State s1 : generatedStates) {
                    nextStates.add(new State(s1));
                }
                //clear the generatedStates as well
                generatedStates.clear();
            }
        }

        return new ArrayList<>();
    }
}
