package de.citec.sc.sampling;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.variable.State;

import sampling.Initializer;

public class StateInitializer implements Initializer<AnnotatedDocument, State> {

    public StateInitializer() {
        super();
    }

    @Override
    public State getInitialState(AnnotatedDocument document) {
        State s = new State(document);
        return s;
    }

}
