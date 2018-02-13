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
public class URIVariable {
    private Integer tokenId;
    private Integer dudeId;
    private String uri;
    
    @Override
    public URIVariable clone(){
        return new URIVariable(tokenId, dudeId, uri);
    }
    
    @Override
    public String toString() {
        return "TokenID: " + tokenId + " DUDE: " + dudeId + " URI: " + uri ;
    }
}
