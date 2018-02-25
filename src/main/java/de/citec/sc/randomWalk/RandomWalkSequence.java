/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import java.util.ArrayList;
import java.util.HashMap;
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
    private Random random;

    private static final String entityLabel = "E";
    private static final String predicateLabel = " P";
    private static final String spaceLabel = " ";
    private static final String lineLabel = "\n";

//    /**
//     * generates random walks and save the result into a file
//     */
//    public StringBuffer generateRandomSequenceWithRestart(int startNodeID, Map<Integer, List<Pair>> map, double alpha) {
//
//        int currentNode = startNodeID;
//
//        StringBuffer sequences = new StringBuffer("");
//
//        /*
//		 * A loop over the number of iterations.
//         */
//        for (int i = 0; i < maxIterations; i++) {
//
//            StringBuffer sequence = new StringBuffer("");
//
//            for (int j = 0; j < sequenceSize; j++) {
//
//                /*
//			 * If the a random number between 0 and 1 is greater than lambda,
//			 * set current node to the original starting node.
//                 */
//                if (Math.random() > alpha) {
//                    currentNode = startNodeID;
//                    // depth = 0;
//                    continue;
//                }
//
//                Pair randomPair = getRandomPair(map.get(currentNode));
//
//                if (randomPair == null) {
//                    //no neighbors
//                    continue;
//                }
//
//                Integer nextNodeID = randomPair.getObjectID();
//                // System.out.println(depth);
//                if (nextNodeID != startNodeID) {
//
//                    sequence.append("E" + currentNode + " " + randomPair.toString() + " ");
//
//                    currentNode = nextNodeID;
//                }
//            }
//            //add the sequence as new line
//            sequences.append(sequence).append("\n");
//        }
//
//        return sequences;
//    }
    /**
     * generates random walks and save the result into a file
     */
    public List<String> generateRandomSequences2(int startNodeID, Map<Integer, Map<Integer, List<Integer>>> map) {
        /*
		 * A loop over the number of iterations.
         */
        List<String> sequences = new ArrayList<>();

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

                if (nextNodeID != startNodeID) {
                    sequence.append("E" + currentNode + " P" + randomPair.getPredicateID() + " ");
                    currentNode = nextNodeID;
                }
            }

            //add the last node
            sequence.append("E" + currentNode);

            if (sequence.length() > 2) {
                sequences.add(sequence.toString());
            }
        }

        return sequences;
    }

    public String generateRandomSequences(int startNodeID, Map<Integer, Map<Integer, List<Integer>>> map) {
        /*
		 * A loop over the number of iterations.
         */
        StringBuffer sequences = new StringBuffer();

        for (int i = 0; i < maxIterations; i++) {

            int currentNode = startNodeID;

            for (int j = 0; j < sequenceSize; j++) {

                Map<Integer, List<Integer>> relatedNodes = map.get(currentNode);

                if (relatedNodes == null) {
                    continue;
                }

                //random predicate
                int randomPredicateID = getRandomFromSet(relatedNodes.keySet());

                if (randomPredicateID == -1) {
                    //no neighbors, stop here
                    break;
                }

                //random object
                List<Integer> objectIDs = relatedNodes.get(randomPredicateID);
                int indexObject = random.nextInt(objectIDs.size());
                Integer randomObjectID = objectIDs.get(indexObject);

                if (randomObjectID != startNodeID) {
                    sequences.append(entityLabel).append(currentNode).append(predicateLabel).append(randomPredicateID).append(spaceLabel);
                    currentNode = randomObjectID;
                }
            }

            //add the last node
            sequences.append(entityLabel).append(currentNode).append(lineLabel);
        }

        return sequences.toString();
    }

    /**
     * Returns a new random node that can be reached from the given node.
     *
     * @param relatedNodes all nodes that can be reached from the current node.
     * @return a Pair of random chosen predicateID and objectID
     */
    private static Pair getRandomPair(Map<Integer, List<Integer>> relatedNodes) {
        if (relatedNodes == null) {
            return null;
        }
        /*
		 * A random index between 0 and the size of related nodes.
         */
        List<Integer> predicateIDs = new ArrayList<>(relatedNodes.keySet());
        int indexPredicate = new Random().nextInt(predicateIDs.size());

        Integer randomPredicateID = predicateIDs.get(indexPredicate);

        List<Integer> objectIDs = relatedNodes.get(randomPredicateID);

        int indexObject = new Random().nextInt(objectIDs.size());
        Integer randomObjectID = objectIDs.get(indexObject);

        // depth++;
        Pair pair = new Pair(randomPredicateID, randomObjectID);

        return pair;
    }

    private int getRandomFromSet(Set<Integer> keySet) {

        int randomPredicateIndex = random.nextInt(keySet.size());

        int randomPredicateID = -1;

        for (int i = 0; i < randomPredicateIndex; i++) {
            keySet.iterator().next();
        }
        if (keySet.iterator().hasNext()) {
            randomPredicateID = keySet.iterator().next();
        }

        return randomPredicateID;
    }
}
