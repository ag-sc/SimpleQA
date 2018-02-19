/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author sherzod
 */
@Data
@AllArgsConstructor
public class RandomWalkSequence {

    private int maxIterations;
    private int sequenceSize;

    /**
     * generates random walks and save the result into a file
     */
    public StringBuffer generateRandomSequenceWithRestart(int startNodeID, Map<Integer, List<Pair>> map, double alpha) {

        int currentNode = startNodeID;

        StringBuffer sequences = new StringBuffer("");

        /*
		 * A loop over the number of iterations.
         */
        for (int i = 0; i < maxIterations; i++) {

            StringBuffer sequence = new StringBuffer("");

            for (int j = 0; j < sequenceSize; j++) {

                /*
			 * If the a random number between 0 and 1 is greater than lambda,
			 * set current node to the original starting node.
                 */
                if (Math.random() > alpha) {
                    currentNode = startNodeID;
                    // depth = 0;
                    continue;
                }

                Pair randomPair = getRandomPair(map.get(currentNode));

                if (randomPair == null) {
                    //no neighbors
                    continue;
                }

                Integer nextNodeID = randomPair.getObjectID();
                // System.out.println(depth);
                if (nextNodeID != startNodeID) {

                    sequence.append("E" + currentNode + " " + randomPair.toString() + " ");

                    currentNode = nextNodeID;
                }
            }
            //add the sequence as new line
            sequences.append(sequence).append("\n");
        }

        return sequences;
    }

    /**
     * generates random walks and save the result into a file
     */
    public StringBuffer generateRandomSequence(int startNodeID, Map<Integer, List<Pair>> map) {

        StringBuffer result = new StringBuffer("");
        /*
		 * A loop over the number of iterations.
         */
        List<String> set = new ArrayList<>();

        for (int i = 0; i < maxIterations; i++) {

            StringBuffer sequence = new StringBuffer("");
            int currentNode = startNodeID;

            for (int j = 0; j < sequenceSize; j++) {

                Pair randomPair = getRandomPair(map.get(currentNode));

                if (randomPair == null) {
                    //no neighbors, stop here
                    break;
                }

                Integer nextNodeID = randomPair.getObjectID();
                // System.out.println(depth);
                if (nextNodeID != startNodeID) {

                    sequence.append("E" + currentNode + " " + randomPair.toString() + " ");

                    currentNode = nextNodeID;
                }
            }

            if (sequence.length() > 2) {
                set.add(sequence.toString());
            }
        }

        for (String s : set) {
            result.append(s).append("\n");
        }

        return result;
    }

    /**
     * Returns a new random node that can be reached from the given node.
     *
     * @param currentNode the current node.
     * @param relatedNodes all nodes that can be reached from the current node.
     * @return a random chosen node from the list of related nodes.
     */
    private static Pair getRandomPair(List<Pair> relatedNodes) {
        if (relatedNodes == null) {
            return null;
        }
        /*
		 * A random index between 0 and the size of related nodes.
         */
        int index;

        index = new Random().nextInt(relatedNodes.size());
        // depth++;
        return relatedNodes.get(index);
    }
}
