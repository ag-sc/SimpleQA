/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.template;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.variable.SlotVariable;

import de.citec.sc.variable.State;
import factors.Factor;
import java.util.ArrayList;
import java.util.List;
import learning.Vector;
import templates.AbstractTemplate;

/**
 *
 * @author sherzod
 */
public class LexicalFeatureTemplate extends AbstractTemplate<AnnotatedDocument, State, StateFactorScope<State>> {

    @Override
    public List<StateFactorScope<State>> generateFactorScopes(State state) {
        List<StateFactorScope<State>> factors = new ArrayList<>();

        factors.add(new StateFactorScope<>(this, state));
        return factors;
    }

    @Override
    public void computeFactor(Factor<StateFactorScope<State>> factor) {
        State state = factor.getFactorScope().getState();

        Vector featureVector = factor.getFeatureVector();

        for (Integer tokenID : state.getDocument().getDependencyParse().getTokenMap().keySet()) {
            String token = state.getDocument().getDependencyParse().getToken(tokenID);
            String pos = state.getDocument().getDependencyParse().getPOSTag(tokenID);

            String uri = "EMPTY_URI";
            Integer dudeID = -1;

            if (state.getUriVariables().containsKey(tokenID)) {
                uri = state.getUriVariables().get(tokenID).getUri();
                dudeID = state.getUriVariables().get(tokenID).getDudeId();
            }

//            featureVector.addToValue("URIVar: Lemma: " + token + "  URI: " + uri + "  DUDE: " + dudeID + "  POS: " + pos, 1.0);
            featureVector.addToValue("URIVar - Lemma: " + token + "  URI: " + uri, 1.0);
            featureVector.addToValue("DUDEVar - DUDE: " + dudeID + "  POS: " + pos, 1.0);
            
            //add slot features
            if(state.getSlotVariables().containsKey(tokenID)){
                SlotVariable slotVar = state.getSlotVariables().get(tokenID);
                
                String parentToken = state.getDocument().getDependencyParse().getTokenMap().get(slotVar.getParentTokenID());
                String parentPOS = state.getDocument().getDependencyParse().getPOSTag(slotVar.getParentTokenID());
                Integer parentDudeID = state.getUriVariables().get(slotVar.getParentTokenID()).getDudeId();
                String parentURI = state.getUriVariables().get(slotVar.getParentTokenID()).getUri();
                //gets relations between two nodes if exists
                String dependencyRelWithPOS = state.getDocument().getDependencyParse().getMergedDependencyRelation(tokenID, slotVar.getParentTokenID(), true);
                String dependencyRelWithoutPOS = state.getDocument().getDependencyParse().getMergedDependencyRelation(tokenID, slotVar.getParentTokenID(), false);
                int slotNumber = slotVar.getSlotNumber();
                
                featureVector.addToValue("SlotVar - Lemma: " + token + "  URI: " + uri + "  DUDE: " + dudeID + "  POS: " + pos + "  ParentLemma:"+parentToken + "  ParentPOS: "+parentPOS+"  ParentURI:"+parentURI+"  DUDE:"+parentDudeID +" DepRel: "+dependencyRelWithPOS+ "  Slot:"+slotNumber, 1.0);
                featureVector.addToValue("SlotVar - DepRel: "+dependencyRelWithPOS+ "  Slot:"+slotNumber, 1.0);
            }
        }
    }
}
