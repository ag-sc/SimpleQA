/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.variable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author sherzod
 */
@Data
@AllArgsConstructor
public class SlotVariable {
    private int slotNumber;
    private int tokenID;
    private int parentTokenID;
    
    @Override
    public String toString() {
        return "Slot: " + slotNumber + ", tokenID: " + tokenID + ", parentTokenID: " + parentTokenID;
    }
    
    @Override
    public SlotVariable clone(){
        return new SlotVariable(slotNumber, tokenID, parentTokenID);
    }
    
}
