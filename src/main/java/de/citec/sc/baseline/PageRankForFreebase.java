/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class PageRankForFreebase {

    public static void main(String[] args) {

        Map<String, Integer> node2Integer = new ConcurrentHashMap<>(500000);
        Map<Integer, String> int2Node = new ConcurrentHashMap<>(500000);
        Map<Integer, Map<Integer, Integer>> adjacencyMap = new ConcurrentHashMap<>(5000000);

        String dirPath = "pageRankFiles";
        String node2IntegerFilePath = dirPath + "/node2Integer.txt";
        String adjacencyFilePath = dirPath + "/nodeAdjacencies.txt";

        //read files node2Int file
        try (Stream<String> stream = Files.lines(Paths.get(node2IntegerFilePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");
                //node2Int
                node2Integer.put(c[0], Integer.parseInt(c[1]));
                //inverted int2Node
                int2Node.put(Integer.parseInt(c[1]), c[0]);

            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        //read files adjacencyMap file
        try (Stream<String> stream = Files.lines(Paths.get(adjacencyFilePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");
                
                //the first element is the nodeID
                Integer nodeID = Integer.parseInt(c[0]);
                
                //the rest of line is :neightborNodeID1 <tab> frequency1   <tab> neightborNodeID2 <tab> frequency2
                
                Map<Integer, Integer> neighborMap = new ConcurrentHashMap<>();
                for(int i = 1; i<c.length; i=i+2){
                    //add the element 
                    neighborMap.put(Integer.parseInt(c[i]), Integer.parseInt(c[i+1]));
                }
                
                //add to adjacency map
                adjacencyMap.put(nodeID, neighborMap);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        calculatePageRankScore(node2Integer, int2Node, adjacencyMap);
    }

    private static void calculatePageRankScore(Map<String, Integer> node2Integer, Map<Integer, String> int2Node, Map<Integer, Map<Integer, Integer>> adjacencyMap) {

        System.out.println("Read freebase dump to calculate pagerank scores ... ");

//        Map<String, Integer> node2Integer = new ConcurrentHashMap<>(500000);
//        Map<Integer, String> int2Node = new ConcurrentHashMap<>(500000);
//        Map<Integer, Map<Integer, Integer>> adjacencyMap = new ConcurrentHashMap<>(5000000);
        //delete previous surface form file
        String dirPath = "pageRankFiles";
        String pageRankFilePath = dirPath+"/pageRankScores.txt";
        File pageRankScoreFile = new File(pageRankFilePath);
        if (pageRankScoreFile.exists()) {
            pageRankScoreFile.delete();
        }

        final AtomicInteger count = new AtomicInteger(0);

        System.out.println("Loaded into memory # resources: " + node2Integer.size());

        double alpha = 0.80;
        int randomWalkSteps = 10000;
        int minNodeFreq = 100;
        count.set(0);

        Random rand = new Random();

//        int startNode = 1;
        Map<Integer, Integer> hitCountMap = new ConcurrentHashMap<>();

        //loop over each node and select it as start node
        adjacencyMap.keySet().parallelStream().forEach((startNode) -> {
            //do random walks

            Integer node = startNode;

            if (count.incrementAndGet() % 10000 == 0) {

                double s = count.get() / (double) +adjacencyMap.size();
                BigDecimal bd = new BigDecimal(s);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                System.out.println(bd.doubleValue());
            }

            for (int w = 0; w <= randomWalkSteps; w++) {

                //if it's the dead end, no further nodes to explore from that 
                if (!adjacencyMap.containsKey(node)) {
                    //restart
                    node = startNode;
                } else {

                    if (rand.nextDouble() > alpha) {
                        //get neighbors and pick 1 randomly using the transition probabilities
                        //neighbors map contains how many time each neighbor appears in the dataset
                        //using these hits pick 1 randomly, => transition probability

                        Map<Integer, Integer> neighbors = adjacencyMap.get(node);

                        Integer randomNeighbor = getRandomNeighbor(neighbors);
                        //save the hit of each individual node
                        hitCountMap.put(randomNeighbor, hitCountMap.getOrDefault(randomNeighbor, 1) + 1);

                        node = randomNeighbor;
                    } else {
                        node = startNode;
                    }
                }
            }
        });

        //delete the prev version
        File pageRankFile = new File(pageRankFilePath);
        if (pageRankFile.exists()) {
            pageRankFile.delete();
        }

        System.out.println("Calculating probability of each node");
        //calculate probability of each node based on hit counts
        int sumOfHits = hitCountMap.values().stream().mapToInt(i -> i).sum();
        Set<String> pageRankScores = new ConcurrentHashSet<>();
        final AtomicInteger counter = new AtomicInteger(0);

        hitCountMap.keySet().parallelStream().forEach((node) -> {
            //do random walks

            //only hits higher than threshold
            if (hitCountMap.get(node) > minNodeFreq) {
                double prob = hitCountMap.get(node) / (double) sumOfHits;

                String uri = int2Node.get(node);

                pageRankScores.add(uri + "\t" + prob);
            }

            if (counter.incrementAndGet() % 10000 == 0) {

                FileUtil.writeListToFile(pageRankFilePath, pageRankScores, true);
                pageRankScores.clear();
            }

        });

        //save the remaining parts as well
        FileUtil.writeListToFile(pageRankFilePath, pageRankScores, true);

    }

    private static Integer getRandomNeighbor(Map<Integer, Integer> neighborNodes) {
        Integer randomNeighbor = -1;

        double p = Math.random();
        double cumulativeProbability = 0.0;

        //sum all frequency values of each node
        int sum = neighborNodes.values().stream().mapToInt(i -> i).sum();

        for (Integer neighbor : neighborNodes.keySet()) {

            //calculate probability of each node
            double probability = neighborNodes.get(neighbor) / (sum);

            cumulativeProbability += probability;
            if (p <= cumulativeProbability) {
                randomNeighbor = neighbor;
                break;
            }
        }

        if (randomNeighbor == -1) {
            for (Integer neighbor : neighborNodes.keySet()) {
                randomNeighbor = neighborNodes.get(neighbor);
                break;
            }
        }

        return randomNeighbor;
    }

}
