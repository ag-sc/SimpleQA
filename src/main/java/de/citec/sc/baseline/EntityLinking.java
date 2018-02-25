/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.NGramExtractor;
import de.citec.sc.utils.StringPreprocessor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class EntityLinking {

    private static boolean normalizeFeatures = false;
    
    public static void main(String[] args) {
        String nodeScoresfilePath = "freebaseFiles/freebase-nodes-scores.tsv";
        String nodeEdgeCountsfilePath = "freebaseFiles/freebase-nodes-in-out-name.tsv";
        String nodeSurfaceFormsfilePath = "freebaseFiles/entitySurfaceForms.txt";

        System.out.println("Loading scores in memory ...");
        Map<String, Set<String>> nodeSurfaceForms = loadSurfaceForms(nodeSurfaceFormsfilePath);
        Map<String, List<Integer>> nodeDegreeCounts = loadEdgeCounts(nodeEdgeCountsfilePath);
        Map<String, Double> nodeScores = loadNodeScores(nodeScoresfilePath);

        boolean isArffFile = false;
        createTrainingFile(nodeSurfaceForms, nodeDegreeCounts, nodeScores, isArffFile);

        createTestingFile(nodeSurfaceForms, nodeDegreeCounts, nodeScores, isArffFile);

    }

    public static void createTrainingFile(Map<String, Set<String>> nodeSurfaceForms, Map<String, List<Integer>> nodeDegreeCounts, Map<String, Double> nodeScores, boolean isArffFile) {

        System.out.println("Looping over training instances ... ");

        Set<String> trainContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");

        //save to arff file
        String trainingDataFilePath = "entityLinkingTrainingData";
        if (isArffFile) {
            trainingDataFilePath += ".arff";
        } else {
            trainingDataFilePath += ".csv";
        }

        File file = new File(trainingDataFilePath);
        if (file.exists()) {
            file.delete();
        }

        String headData = "";

        if (isArffFile) {
            headData = "@RELATION entityLinking\n"
                    + "\n"
                    + "@ATTRIBUTE class        {0,1}\n"
                    + "@ATTRIBUTE nodeScore  NUMERIC\n"
                    + "@ATTRIBUTE nodeInDegree   NUMERIC\n"
                    + "@ATTRIBUTE nodeOutDegree  NUMERIC\n"
                    + "@ATTRIBUTE ngramSize  NUMERIC\n"
                    + "@DATA\n";
        }

        //write the head of the file
        FileUtil.writeStringToFile(trainingDataFilePath, headData, true);

        //loop over all training instances and extract uris with their features
        int count = 0;
        for (String s : trainContent) {

            count++;

            if (count % 1000 == 0) {
                System.out.println(count + "/" + trainContent.size());
            }

            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = "m." + c[0].replace("www.freebase.com/m/", "");

            boolean entityIsFound = false;
            for (int ngramSize = 8; ngramSize > 0; ngramSize--) {

                if (entityIsFound) {
                    break;
                }

                //extract ngrams based on the size given
                //ngrams that size equal to ngramSize as
                List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

                for (String ngram : ngrams) {

                    String lemma = ngram.toLowerCase();
                    String preprocessedLemma = StringPreprocessor.preprocess(lemma, Language.lang.EN);

                    if (!entityIsFound) {

                        Set<String> uris = new HashSet<>();

                        if (nodeSurfaceForms.containsKey(lemma)) {
                            uris = nodeSurfaceForms.get(lemma);
                        } else if (nodeSurfaceForms.containsKey(preprocessedLemma)) {
                            uris = nodeSurfaceForms.get(preprocessedLemma);
                        }

                        if (uris.contains(subject)) {
                            entityIsFound = true;

                            Map<String, Double> foundNodeScores = calculateNormalizedScores(uris, nodeScores, normalizeFeatures);
                            Map<String, List<Double>> foundNodeEdgeScores = calculateNormalizedEdgeCounts(uris, nodeDegreeCounts, normalizeFeatures);

                            String positiveDataPoint = "";
                            List<String> negativeDataPoints = new ArrayList<>();

                            double positiveNodeScore = foundNodeScores.get(subject);
                            double positiveNodeInDegreeScore = foundNodeEdgeScores.get(subject).get(0);
                            double positiveNodeOutDegreeScore = foundNodeEdgeScores.get(subject).get(1);

                            positiveDataPoint = "1," + positiveNodeScore + "," + positiveNodeInDegreeScore + "," + positiveNodeOutDegreeScore + "," + ngramSize;

                            //extract data points
                            for (String uri : uris) {

                                if (!uri.equals(subject)) {
                                    double nodeScore = foundNodeScores.get(uri);
                                    double nodeInDegreeScore = foundNodeEdgeScores.get(uri).get(0);
                                    double nodeOutDegreeScore = foundNodeEdgeScores.get(uri).get(1);

                                    String output = "0," + nodeScore + "," + nodeInDegreeScore + "," + nodeOutDegreeScore + "," + ngramSize;

                                    negativeDataPoints.add(output);
                                }

                                if (negativeDataPoints.size() >= 50) {
                                    break;
                                }
                            }

                            //write datapoints to file
                            String dataPoints = positiveDataPoint + "\n";
                            for (String n : negativeDataPoints) {
                                dataPoints += n + "\n";
                            }

                            //write to file
                            //write the head of the file
                            FileUtil.writeStringToFile(trainingDataFilePath, dataPoints, true);

                            break;
                        }
                    }
                }
            }
        }

    }

    public static void createTestingFile(Map<String, Set<String>> nodeSurfaceForms, Map<String, List<Integer>> nodeDegreeCounts, Map<String, Double> nodeScores, boolean isArffFile) {

        System.out.println("Looping over testing instances ... ");

        Set<String> testContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_test.txt");

        //save to arff file
        String testingDataDir = "testData";

        File dir = new File(testingDataDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        //loop over all training instances and extract uris with their features
        int count = 0;
        for (String s : testContent) {

            count++;

            //save each test instance separately
            String testingFilePath = testingDataDir + "/" + count + ".csv";

            if (count % 1000 == 0) {
                System.out.println(count + "/" + testContent.size());
            }

            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = "m." + c[0].replace("www.freebase.com/m/", "");

            Set<String> dataPoints = new LinkedHashSet<>();

            for (int ngramSize = 8; ngramSize > 0; ngramSize--) {

                //extract ngrams based on the size given
                //ngrams that size equal to ngramSize as
                List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

                for (String ngram : ngrams) {

                    String lemma = ngram.toLowerCase();
                    String preprocessedLemma = StringPreprocessor.preprocess(lemma, Language.lang.EN);

                    Set<String> uris = new HashSet<>();

                    if (nodeSurfaceForms.containsKey(lemma)) {
                        uris = nodeSurfaceForms.get(lemma);
                    } else if (nodeSurfaceForms.containsKey(preprocessedLemma)) {
                        uris = nodeSurfaceForms.get(preprocessedLemma);
                    }

                    if (!uris.isEmpty()) {

                        Map<String, Double> foundNodeScores = calculateNormalizedScores(uris, nodeScores, normalizeFeatures);
                        Map<String, List<Double>> foundNodeEdgeScores = calculateNormalizedEdgeCounts(uris, nodeDegreeCounts, normalizeFeatures);

                        //extract data points
                        for (String uri : uris) {

                            double nodeScore = foundNodeScores.get(uri);
                            double nodeInDegreeScore = foundNodeEdgeScores.get(uri).get(0);
                            double nodeOutDegreeScore = foundNodeEdgeScores.get(uri).get(1);

                            String output = "0," + nodeScore + "," + nodeInDegreeScore + "," + nodeOutDegreeScore + "," + ngramSize;

                            if (uri.equals(subject)) {
                                //change the class label to 1
                                output = "1" + output.substring(1);
                            }

                            dataPoints.add(output);
                        }
                    }
                }
            }

            //write to file
            //write the head of the file
            FileUtil.writeSetToFile(testingFilePath, dataPoints, false);
        }
    }

    //returns node scores normalized
    private static Map<String, Double> calculateNormalizedScores(Set<String> uris, Map<String, Double> nodeScores, boolean normalize) {
        //calculate node scores for each uri by normalizing the values
        Map<String, Double> foundNodeScores = new HashMap<>();

        //compute the sum for normalizing scores
        if (normalize) {
            double sumOfScores = 0;
            for (String uri : uris) {
                double score = 0;

                if (nodeScores.containsKey(uri)) {
                    score = nodeScores.get(uri);
                }

                sumOfScores += score;
            }

            //normalize scores and add to a map
            for (String uri : uris) {
                double score = 0;

                if (nodeScores.containsKey(uri)) {
                    score = nodeScores.get(uri);
                }

                score = score / sumOfScores;

                if (Double.isNaN(score)) {
                    score = 0;
                }

                foundNodeScores.put(uri, score);
            }
        } else {
            for (String uri : uris) {
                double score = 0;

                if (nodeScores.containsKey(uri)) {
                    score = nodeScores.get(uri);
                    
                    if(score > 0){
                        score = Math.round(score);
                    }
                }

                foundNodeScores.put(uri, score);
            }
        }
        return foundNodeScores;
    }

    //returns node edge counts normalized
    private static Map<String, List<Double>> calculateNormalizedEdgeCounts(Set<String> uris, Map<String, List<Integer>> nodeEdgeCounts, boolean normalize) {
        //calculate node scores for each uri by normalizing the values
        Map<String, List<Double>> foundNodeCounts = new HashMap<>();

        if (normalize) {
            //compute the sum for normalizing scores
            int sumOfInDegrees = 0;
            int sumOfOutDegrees = 0;
            for (String uri : uris) {
                int inDegree = 0;
                int outDegree = 0;

                if (nodeEdgeCounts.containsKey(uri)) {
                    List<Integer> edgeCounts = nodeEdgeCounts.get(uri);

                    //in degree at index 0, out degree at 1
                    inDegree = edgeCounts.get(0);
                    outDegree = edgeCounts.get(1);
                }

                sumOfInDegrees += inDegree;
                sumOfOutDegrees += outDegree;
            }

            //normalize scores and add to a map
            for (String uri : uris) {
                int inDegree = 0;
                int outDegree = 0;

                if (nodeEdgeCounts.containsKey(uri)) {
                    List<Integer> edgeCounts = nodeEdgeCounts.get(uri);

                    //in degree at index 0, out degree at 1
                    inDegree = edgeCounts.get(0);
                    outDegree = edgeCounts.get(1);
                }

                double inDegreeScore = inDegree / (double) sumOfInDegrees;
                double outDegreeScore = outDegree / (double) sumOfOutDegrees;

                if (Double.isNaN(inDegreeScore)) {
                    inDegreeScore = 0;
                }

                if (Double.isNaN(outDegreeScore)) {
                    outDegreeScore = 0;
                }

                List<Double> edgeScores = new ArrayList<>();
                edgeScores.add(inDegreeScore);
                edgeScores.add(outDegreeScore);

                foundNodeCounts.put(uri, edgeScores);
            }
        } else {
            for (String uri : uris) {
                double inDegree = 0;
                double outDegree = 0;

                if (nodeEdgeCounts.containsKey(uri)) {
                    List<Integer> edgeCounts = nodeEdgeCounts.get(uri);

                    //in degree at index 0, out degree at 1
                    inDegree = edgeCounts.get(0);
                    outDegree = edgeCounts.get(1);
                }


                List<Double> edgeScores = new ArrayList<>();
                edgeScores.add(inDegree);
                edgeScores.add(outDegree);

                foundNodeCounts.put(uri, edgeScores);
            }
        }

        return foundNodeCounts;
    }

    private static Map<String, Double> loadNodeScores(String filePath) {
        Map<String, Double> map = new ConcurrentHashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 2) {
                    long freebaseLongID = Long.parseLong(c[0]);
                    double score = Double.parseDouble(c[1]);

                    String freebaseID = convertLongToMid(freebaseLongID);

                    map.put(freebaseID, score);

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    private static Map<String, List<Integer>> loadEdgeCounts(String filePath) {
        Map<String, List<Integer>> map = new ConcurrentHashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 4) {
                    long freebaseLongID = Long.parseLong(c[0]);
                    //in degree
                    int inDegree = Integer.parseInt(c[1]);
                    int outDegree = Integer.parseInt(c[2]);

                    if (inDegree > 5 && outDegree > 5) {
                        String freebaseID = convertLongToMid(freebaseLongID);

                        List<Integer> degrees = new ArrayList<>();
                        degrees.add(inDegree);
                        degrees.add(outDegree);

                        map.put(freebaseID, degrees);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    private static Map<String, Set<String>> loadSurfaceForms(String filePath) {
        Map<String, Set<String>> map = new ConcurrentHashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 2) {
                    String lemma = c[1].trim();
                    String uri = "m." + c[0].trim().replace("www.freebase.com/m/", "");

                    if (!uri.isEmpty() && lemma.length() > 1) {

                        String preprocessedLemma = StringPreprocessor.preprocess(lemma, Language.lang.EN);

                        if (map.containsKey(lemma)) {
                            Set<String> uris = map.get(lemma);
                            uris.add(uri);

                            map.put(lemma, uris);
                        } else {
                            Set<String> uris = new HashSet<>();
                            uris.add(uri);

                            map.put(lemma, uris);
                        }

                        //non-empty and different than lemma
                        if (preprocessedLemma.length() > 1 && !preprocessedLemma.equals(lemma)) {
                            //add preprocessed lemma as well
                            if (map.containsKey(preprocessedLemma)) {
                                Set<String> uris = map.get(preprocessedLemma);

                                uris.add(uri);

                                map.put(preprocessedLemma, uris);
                            } else {
                                Set<String> uris = new HashSet<>();
                                uris.add(uri);

                                map.put(preprocessedLemma, uris);
                            }
                        }
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    /**
     * Opposite of <code>convertMidToBigInt</code>
     *
     * @param decimal
     * @return
     * @throws NullPointerException
     * @throws IndexOutOfBoundsException
     */
    private static String convertLongToMid(long decimal)
            throws NullPointerException, IndexOutOfBoundsException {

        String mid = "";
        String decimalString = decimal + "";
        for (int i = 0; i < decimalString.length(); i += 2) {
            mid = (char) Integer.parseInt(decimalString.substring(i, i + 2)) + mid;
        }
        return "m." + mid.toLowerCase();
    }
}
