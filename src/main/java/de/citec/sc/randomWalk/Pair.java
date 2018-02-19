/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author sherzod
 */
@Data
@AllArgsConstructor
public class Pair {
    private int predicateID;
    private int objectID;

    @Override
    public String toString() {
        return "P"+predicateID + " E" + objectID;
    }
    
}
