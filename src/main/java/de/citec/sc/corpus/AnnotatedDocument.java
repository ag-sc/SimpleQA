/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import corpus.LabeledInstance;
import de.citec.sc.syntax.DependencyParse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sherzod
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class AnnotatedDocument implements LabeledInstance<AnnotatedDocument, String> {

    private DependencyParse dependencyParse;
    private DataPoint dataPoint;

    @Override
    public AnnotatedDocument getInstance() {
        return this;
    }

    @Override
    public String getResult() {
        return dataPoint.getResult();
    }

    @Override
    public String toString() {
        String s = "";

        s += "Instance:\n\n" + dataPoint.toString();
        s += "\n\nDependency parse tree:\n\t" + dependencyParse.toString();

        return s;
    }

}
