/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.scoring;

import java.util.List;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class BagOfLinksEvaluator {

    public static double evaluate(Set<String> derived, Set<String> goldStandard) {

        double recall = getIntersectionScore(derived, goldStandard);
        double precision = getIntersectionScore(goldStandard, derived);

        double f1 = (2 * precision * recall) / (precision + recall);

//        double f1 = recall;
        if (Double.isNaN(f1) || Double.isInfinite(f1)) {
            f1 = 0;
        }

        return f1;
    }

    private static double getIntersectionScore(Set<String> derived, Set<String> goldStandard) {
        double score = 0;

        int c = 0;
        for (String d : derived) {
            if (goldStandard.contains(d)) {
                    c++;
                }
            }

        score = c / (double) derived.size();

        return score;
    }

}
