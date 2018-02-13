/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import java.util.Set;
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
public class DataPoint {

    private String question;
    private String query;
    private Set<String> answers;
    private String id;

    public String getResult() {
        return query;
    }

    @Override
    public String toString() {

        String s = "Id: " + id + "\nQuestion: " + question + "\nQuery: " + query;

        if (answers != null) {
            s += "\nAnswers:";

            int counter = 0;
            for (String a : answers) {
                s += "\n\t" + a;
                counter++;

                if (counter >= 10) {
                    s += " ...";
                    break;
                }
            }
        }

        return s;
    }
}
