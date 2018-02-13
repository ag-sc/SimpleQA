/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.index;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author sherzod
 */
@Data
@AllArgsConstructor
public class Instance implements Comparable<Instance> {

    private String ngram;
    private String uri;
    private int frequency;

    @Override
    public int compareTo(Instance o) {
        if (o.getFrequency() > this.frequency) {
            return 1;
        }
        if (o.getFrequency() < this.frequency) {
            return -1;
        }
        return 0;
    }

}
