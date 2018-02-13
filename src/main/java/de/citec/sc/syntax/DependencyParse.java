/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.jena.sparql.function.library.leviathan.radiansToDegrees;

/**
 *
 * @author sherzod
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class DependencyParse {

    private Map<Integer, String> tokenMap;
    private Map<Integer, Integer> nodeRelationMap;
    private Map<Integer, String> edgeMap;
    private Map<Integer, String> postagMap;
    private int headNode;

    @Override
    public String toString() {

        String r = "\nNodes:\n";
        for (int s : tokenMap.keySet()) {
            r += "\t" + s + "," + tokenMap.get(s) + " (" + postagMap.get(s) + ") \t";
        }

        r += "\nEdges:\n";
        for (int s : edgeMap.keySet()) {
            r += "\t" + s + "," + edgeMap.get(s) + "\t";
        }

        r += "\n\nParse Tree:\n";
        for (int s : nodeRelationMap.keySet()) {
            r += " (" + s + "," + nodeRelationMap.get(s) + ")\t";
        }

        r += "\nHead node: " + headNode;
        return r;
    }

    /**
     * returns dependent edges given the headNode
     *
     * @param headNode
     * @return List of dependent nodes
     */
    public List<Integer> getDependentNodes(int headNode) {

        List<Integer> list = new ArrayList<>();
        for (Integer k : nodeRelationMap.keySet()) {
            Integer v = nodeRelationMap.get(k);

            if (v == headNode) {
                list.add(k);
            }
        }

        return list;
    }

    /**
     * returns parent node of given the node
     *
     * @param node
     * @return parent node
     */
    public Integer getParentNode(int node) {

        if (nodeRelationMap.containsKey(node)) {
            Integer parentNode = nodeRelationMap.get(node);

            return parentNode;
        }
        return -1;
    }

    /**
     * returns postag for the given node
     *
     * @param dependentNodeId
     * @return String POSTag
     */
    public String getPOSTag(Integer nodeId) {
        return postagMap.get(nodeId);
    }

    /**
     * returns postag for the given node
     *
     * @param dependentNodeId
     * @return String POSTag
     */
    public String getToken(Integer nodeId) {
        return tokenMap.get(nodeId);
    }

//    /**
//     * returns dependency relation for the given dependent node and parent node
//     *
//     * @param dependentNodeId
//     * @param parentNodeID
//     * @return String dependency relation
//     */
//    public String getDependencyRelation(Integer dependentNodeId, Integer parentNodeID) {
//
//        if (this.nodeRelationMap.containsKey(dependentNodeId)) {
//            //if the same parent
//            if (this.nodeRelationMap.get(dependentNodeId).equals(parentNodeID)) {
//                return this.edgeMap.get(dependentNodeId);
//            } else {
//                return "NO-REL";
//            }
//        }
//        return "NO-REL";
//    }
    /**
     * returns dependency relation for the given dependent node and parent node
     *
     * @param dependentNodeId
     * @param parentNodeID
     * @return String dependency relation
     */
    public String getDependencyRelation(Integer dependentNodeId) {

        if (this.nodeRelationMap.containsKey(dependentNodeId)) {
            return this.edgeMap.get(dependentNodeId);
        }
        return "NO-REL";
    }

    /**
     * returns dependency relation for the given dependent node1 and node2 if
     * the nodes aren't directly connected the path between two nodes is
     * calculated by traversing the parse tree
     *
     * @param node1
     * @param node2
     * @return String dependency relation
     */
    public String getMergedDependencyRelation(Integer node1, Integer node2, boolean includePOSTags) {

        String path1 = getPathToRoot(node1);
        String path2 = getPathToRoot(node2);

        String[] pathArray1 = path1.split("-");
        String[] pathArray2 = path2.split("-");

        int max = Math.max(pathArray1.length, pathArray2.length);
        int min = Math.min(pathArray1.length, pathArray2.length);

        String[] longArray = new String[max];
        String[] shortArray = new String[min];

        if (pathArray1.length > pathArray2.length) {

            for (int i = 0; i < pathArray1.length; i++) {
                longArray[i] = pathArray1[i];
            }
            for (int i = 0; i < pathArray2.length; i++) {
                shortArray[i] = pathArray2[i];
            }
        } else {
            for (int i = 0; i < pathArray2.length; i++) {
                longArray[i] = pathArray2[i];
            }
            for (int i = 0; i < pathArray1.length; i++) {
                shortArray[i] = pathArray1[i];
            }
        }

        //remove the common subsequence
        //e.g. NOUN=nmod:poss-NOUN=nsubj
        //     PART=case-NOUN=nmod:poss-NOUN=nsubj
        //remove the part up to  PART=case coz it's the same, also remove the first POS
        String mergedPath1 = "";
        String mergedPath2 = "";
        for (int i = longArray.length - 1; i >= 0; i--) {
            String partialPath2 = longArray[i];

            int index = shortArray.length - (longArray.length - i);
            String partialPath1 = "";
            if (index >= 0 && index <= shortArray.length - 1) {
                partialPath1 = shortArray[index];
            }

            if (!partialPath1.equals(partialPath2)) {

                //if it's not equal then merge the paths up to that point
                for (int j = 0; j <= i; j++) {

                    String p = longArray[j];
                    String relation = p.substring(p.indexOf("=") + 1, p.length());

                    if (j == 0) {
                        //remove the first POS
                        mergedPath1 += relation;
                    } else {

                        if (includePOSTags) {

                            mergedPath1 += p;
                        } else {
                            mergedPath1 += relation;
                        }
                    }

                    if (j + 1 <= i) {
                        mergedPath1 += "-";
                    }
                }

                for (int j = 0; j <= index; j++) {

                    String p = shortArray[j];
                    String relation = p.substring(p.indexOf("=") + 1, p.length());

                    if (j == 0) {
                        //remove the first POS
                        mergedPath2 += relation;
                    } else {
                        if (includePOSTags) {
                            mergedPath2 += p;
                        } else {
                            mergedPath2 += relation;
                        }
                    }

                    if (j + 1 <= index) {
                        mergedPath2 += "-";
                    }
                }

                if (mergedPath1.trim().isEmpty()) {
                    return mergedPath2;
                } else if (mergedPath2.trim().isEmpty()) {
                    return mergedPath1;
                }
                return mergedPath2 + "-ROOT-" + mergedPath1;
            }
        }

        return "";
    }

    /**
     * return the path to the root
     *
     * @param tokenID
     * @return String pathToRoot
     */
    private String getPathToRoot(Integer tokenID) {

        if (tokenID.equals(headNode)) {
            return "";
        }
        Integer parentTokenID = getParentNode(tokenID);
        String depRelation = getDependencyRelation(tokenID);
        String parentPOS = getPOSTag(parentTokenID);
        String pos = getPOSTag(tokenID);

        if (!parentTokenID.equals(headNode)) {
            return pos + "=" + depRelation + "-" + getPathToRoot(parentTokenID);
        }
        return pos + "=" + depRelation;
    }

}
