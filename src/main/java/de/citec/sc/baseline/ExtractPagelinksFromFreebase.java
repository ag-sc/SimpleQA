/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class ExtractPagelinksFromFreebase {

    public static void main(String[] args) {

        Set<String> validEntities = new ConcurrentHashSet<>();

        System.out.println("Loading freebase2m into memory");

        String filePath = "freebase-FB2M.txt";
//        String filePath = "subsetFreebase2M.txt";
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
                String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");
                String[] objects = c[2].replace("www.freebase.com/", "").replace("/", ".").split("\\s");

                validEntities.add(subject);

                for (String object : objects) {
                    validEntities.add(object);
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

//        extractPageLinks("subsetFreebase.txt", validEntities);
        extractPageLinks("../freebase-rdf-latest", validEntities);

    }

    private static void extractPageLinks(String filePath, Set<String> entities) {

        System.out.println("Read freebase dump to extract links ... ");

        Map<String, Integer> node2Integer = new ConcurrentHashMap<>(500000);
        Map<Integer, String> int2Node = new ConcurrentHashMap<>(500000);
        Map<Integer, Map<Integer, Integer>> adjacencyMap = new ConcurrentHashMap<>(5000000);

        //delete previous surface form file
        File pageRankScoreFile = new File("pageRankScores.txt");
        if (pageRankScoreFile.exists()) {
            pageRankScoreFile.delete();
        }

        String patternString = "<http://rdf.freebase.com/ns/(.*?)>\t(.*?)\t<http://rdf.freebase.com/ns/(.*?)>\t.";
        Pattern patternLabel = Pattern.compile(patternString);

        final AtomicInteger count = new AtomicInteger(0);

        float maxNumberOfLines = 3130753066f;//the line numbers of the file

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (count.incrementAndGet() % 10000000 == 0) {//3130753066

                    double s = count.get() / (double) maxNumberOfLines;

                    System.out.println("Done = " + s);
                }
                Matcher m = patternLabel.matcher(item);

                while (m.find()) {
                    String subject = m.group(1);
                    String object = m.group(3);

                    if (entities.contains(subject) || entities.contains(object)) {

                        int indexSubject = -1;
                        int indexObject = -1;

                        if (!node2Integer.containsKey(subject)) {
                            indexSubject = node2Integer.size();

                            node2Integer.put(subject, indexSubject);
                            int2Node.put(indexSubject, subject);
                        } else {
                            indexSubject = node2Integer.get(subject);
                        }

                        if (!node2Integer.containsKey(object)) {
                            indexObject = node2Integer.size();

                            node2Integer.put(object, indexObject);
                            int2Node.put(indexObject, object);
                        } else {
                            indexObject = node2Integer.get(object);
                        }

                        if (indexObject != indexSubject) {
                            //add to adjacency matrix
                            if (adjacencyMap.containsKey(indexSubject)) {

                                Map<Integer, Integer> neighborNodes = adjacencyMap.get(indexSubject);
                                neighborNodes.put(indexObject, neighborNodes.getOrDefault(indexObject, 1) + 1);

                                adjacencyMap.put(indexSubject, neighborNodes);
                            } else {
                                Map<Integer, Integer> neighborNodes = new ConcurrentHashMap<>();
                                neighborNodes.put(indexObject, 1);
                                adjacencyMap.put(indexSubject, neighborNodes);
                            }
                        }
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            String dirPath = "pageRankFiles";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdir();
            }

            String node2IntegerFilePath = dirPath + "/node2Integer.txt";
            String adjacencyFilePath = dirPath + "/nodeAdjacencies.txt";

            File node2IntegerFile = new File(node2IntegerFilePath);
            if (node2IntegerFile.exists()) {
                node2IntegerFile.delete();
            }

            File adjacencyFile = new File(adjacencyFilePath);
            if (adjacencyFile.exists()) {
                adjacencyFile.delete();
            }

            System.out.println("Saving files ...");

            Set<String> output = new ConcurrentHashSet<>();

            //node2Integer file
            node2Integer.keySet().parallelStream().forEach((node) -> {
                Integer nodeID = node2Integer.get(node);

                output.add(node + "\t" + nodeID);

                if (output.size() % 10000 == 0) {
                    FileUtil.writeListToFile(node2IntegerFilePath, output, true);
                }
            });

            //save the remaining
            FileUtil.writeListToFile(node2IntegerFilePath, output, true);

            System.out.println("Node2Int file is saved.");

            output.clear();
            //save the adjacency map
            for (Integer nodeId : adjacencyMap.keySet()) {
                Map<Integer, Integer> transitionFrequencies = adjacencyMap.get(nodeId);

                String mapAsString = nodeId + "";
                for (Integer n1 : transitionFrequencies.keySet()) {
                    mapAsString += "\t" + n1 + "\t" + transitionFrequencies.get(n1);
                }

                output.add(mapAsString);
                if (output.size() % 10000 == 0) {
                    FileUtil.writeListToFile(adjacencyFilePath, output, true);
                }
            }

            //save the remaining
            FileUtil.writeListToFile(adjacencyFilePath, output, true);

            System.out.println("AdjacencyMap file is saved.");

        } catch (Exception ex) {
            Logger.getLogger(ExtractPagelinksFromFreebase.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
