/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.template;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.variable.SlotVariable;

import de.citec.sc.variable.State;
import de.citec.sc.variable.URIVariable;
import factors.Factor;
import java.util.ArrayList;
import java.util.List;
import learning.Vector;
import templates.AbstractTemplate;

/**
 *
 * @author sherzod
 */
public class BaselineTemplate extends AbstractTemplate<AnnotatedDocument, State, StateFactorScope<State>> {

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

        for (Integer tokenID : state.getUriVariables().keySet()) {
            URIVariable var = state.getUriVariables().get(tokenID);

            String token = state.getDocument().getDependencyParse().getToken(tokenID);

            featureVector.addToValue("URIVar - Lemma: " + token + "  URI: " + var.getUri() + " DUDE: " + var.getDudeId(), 1.0);
        }
    }
}
