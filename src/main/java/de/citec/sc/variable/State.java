/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.variable;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.scoring.FeatureMapData;
import de.citec.sc.scoring.FeatureMapData.FeatureDataPoint;
import exceptions.MissingFactorException;
import factors.Factor;
import factors.FactorScope;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import variables.AbstractState;

/**
 *
 * @author sherzod
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class State extends AbstractState<AnnotatedDocument> {

    private AnnotatedDocument document;
    private Map<Integer, URIVariable> uriVariables;
    private Map<Integer, SlotVariable> slotVariables;

    public State(AnnotatedDocument doc) {
        super(doc);
        this.document = (AnnotatedDocument) doc;
        this.uriVariables = new HashMap<>();
        this.slotVariables = new HashMap<>();
    }

    public State(State state) {
        super(state);
        this.document = state.document;
        this.uriVariables = new HashMap<>();
        this.slotVariables = new HashMap<>();

        //clone uri variables
        for (Integer d : state.uriVariables.keySet()) {
            uriVariables.put(d, state.uriVariables.get(d).clone());
        }
        //clone slots
        for (Integer d : state.slotVariables.keySet()) {
            slotVariables.put(d, state.slotVariables.get(d).clone());
        }
    }

    public boolean exploredBefore(Integer dudeID, String uri) {

        for (Integer tokenID : uriVariables.keySet()) {
            URIVariable v = uriVariables.get(tokenID);

            if (v.getDudeId().equals(dudeID) && v.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    public void addURIVariable(Integer tokenID, Integer dudeID, String uri) {

        URIVariable v = new URIVariable(tokenID, dudeID, uri);
        this.uriVariables.put(tokenID, v);
    }

    public void addSlotVariable(Integer tokenID, Integer parentTokenID, Integer slotNumber) {

        SlotVariable s = new SlotVariable(slotNumber, tokenID, parentTokenID);
        this.slotVariables.put(tokenID, s);
    }

    @Override
    public String toString() {
        String state = document.toString() + "\n";

        state += "\nURIVariables:\n";

        for (Integer d : uriVariables.keySet()) {
            state += uriVariables.get(d).toString() + "\n";
        }

        state += "\nSlotVariables:\n";

        for (Integer d : slotVariables.keySet()) {
            state += slotVariables.get(d).toString() + "\n";
        }

        state += "\nObjectiveScore: " + getObjectiveScore();
        state += "\nModelScore: " + getModelScore() + "\n";

        return state;
    }

    public String getSlot(int depTokenID, int parentID) {
        if (this.slotVariables.containsValue(depTokenID)) {
            if (this.slotVariables.get(depTokenID).getParentTokenID() == parentID) {
                return this.slotVariables.get(depTokenID).getSlotNumber() + "";
            } else {
                return "";
            }
        }
        return "";
    }

    public FeatureDataPoint toTrainingPoint(FeatureMapData data, boolean training) {

        final Map<String, Double> features = new HashMap<>();
        try {
            for (Factor<? extends FactorScope> factor : getFactorGraph().getFactors()) {
                for (Map.Entry<String, Double> f : factor.getFeatureVector().getFeatures().entrySet()) {
                    features.put(f.getKey(), features.getOrDefault(f.getKey(), 0d) + f.getValue());
                }
            }
        } catch (MissingFactorException e) {
            e.printStackTrace();
        }
        return new FeatureDataPoint(data, features, getObjectiveScore(), training);
    }
}
