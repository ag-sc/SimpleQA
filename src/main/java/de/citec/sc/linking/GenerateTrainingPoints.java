/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.linking;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import de.citec.sc.index.Instance;
import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.NGramExtractor;
import de.citec.sc.utils.SortUtils;
import de.citec.sc.utils.StringPreprocessor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class GenerateTrainingPoints {

    public static void main(String[] args) {

//        Map<String, Set<String>> entity2PredicateMap = new HashMap<>();
//
//        System.out.println("Reading Freebase2M file into memory ... ");
//
//        String filePath = "/home/sherzod/NetBeansProjects/SimpleQuestions.DBpedia/SimpleQuestions_v2/freebase-subsets/freebase-FB2M.txt";
//        Set<String> content = FileUtil.readFile(filePath);
//
//        for (String item : content) {
//
//            String[] c = item.split("\t");
//
//            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
//            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");
//
//            if (entity2PredicateMap.containsKey(subject)) {
//                Set<String> predicates = entity2PredicateMap.get(subject);
//                predicates.add(predicate);
//
//                entity2PredicateMap.put(subject, predicates);
//            } else {
//                Set<String> predicates = new HashSet<>();
//                predicates.add(predicate);
//
//                entity2PredicateMap.put(subject, predicates);
//            }
//
//        }
//
//        String nodeScoresfilePath = "freebaseFiles/freebase-nodes-scores.tsv";
//        String nodeEdgeCountsfilePath = "freebaseFiles/freebase-nodes-in-out-name.tsv";
//
//        System.out.println("Loading scores in memory ...");
//        Map<String, Integer> nodeDegreeCounts = loadEdgeCounts(nodeEdgeCountsfilePath, entity2PredicateMap.keySet());
//        Map<String, Double> nodeScores = loadNodeScores(nodeScoresfilePath, entity2PredicateMap.keySet());
//
//        
//        System.out.println("Entity Linking upperbound for Train");
//        createTrainingSet("train", true, 6, nodeScores, nodeDegreeCounts);
//        collectAll("train", true, 6);
       
//        collectAllAndRemoveProperties("train", true, 6);
//        dynamicThreshold("train", true, 6);


//        createTrainingSet("train", true, 6, new HashMap<>(), new HashMap<>());

    }

    


    public static void createTrainingSet(String fileName, boolean isEntity, int maxNGramSize, Map<String, Double> nodeScores, Map<String, Integer> edgeCounts) {

//        System.out.println("Looping over instances ... ");
        Set<String> content = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_" + fileName + ".txt");

        int count = 0;

        Set<String> dataPoints = new HashSet<>();

        for (String s : content) {

            count++;

            if (count == 10000) {
                break;
            }

            if (count % 1000 == 0) {

                double progr = count / (double) 10000;

                String strDouble = String.format("%.4f", progr);
                System.out.println(fileName + ": " + strDouble);
            }

            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", "").replace(".", "").replace("'s", "").trim();
            String subject = "m." + c[0].replace("www.freebase.com/m/", "");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

            String result = "";

            boolean hasResult = false;

            Map<String, List<Instance>> matches = new HashMap<>();
            Map<Instance, Integer> instance2NgramSize = new HashMap<>();
            List<Instance> retrievedInstances = new ArrayList<>();

            for (int ngramSize = maxNGramSize; ngramSize > 0; ngramSize--) {

                if (hasResult) {
                    break;
                }

                //extract ngrams based on the size given
                //ngrams that size equal to ngramSize as
                List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

                //collect all matches in a list
                for (String ngram : ngrams) {

                    String lemma = StringPreprocessor.preprocess(ngram, Language.lang.EN);
                    List<Instance> instances = retrieveMatches(lemma, 10, isEntity);

                    if (!instances.isEmpty()) {

                        //remove matches that are already covered by bigger ngrams
                        boolean isValid = true;

                        for (String m : matches.keySet()) {
                            if (m.contains(ngram)) {
                                isValid = false;
                                break;
                            }
                        }

                        if (isValid) {
                            matches.put(ngram, instances);
                            retrievedInstances.addAll(instances);

                            //add all instance with ngramSize
                            for (Instance i : instances) {
                                instance2NgramSize.put(i, ngramSize);
                            }
                        }
                    }
                }
            }

            Collections.sort(retrievedInstances);

            int instanceCount = 0;

            List<String> negativeDataPoints = new ArrayList<>();
            String positiveDataPoint = "";

            for (Instance i : retrievedInstances) {

//                if (!edgeCounts.containsKey(i.getUri()) || !nodeScores.containsKey(i.getUri())) {
//                    continue;
//                }
                int freq = i.getFrequency();

//                int nodeEdgeScore = edgeCounts.get(i.getUri());
//                double nodePopularityScore = nodeScores.get(i.getUri());

                int ngramSize = instance2NgramSize.get(i);

                //positive datapoint
                if (i.getUri().equals(subject)) {
                    //ngramSize, edgeScore, popularityScore, mentionFreqCount
//                    dataPoints.add(ngramSize + "," + nodeEdgeScore + "," + freq + ",1");
                    positiveDataPoint = ngramSize + "," + freq + ",1";
                } //negative datapoint
                else {
                    negativeDataPoints.add(ngramSize + "," + freq + ",0");
                }
            }

            //shuffle and select 5 negative data points
            Collections.shuffle(negativeDataPoints);

            if (!positiveDataPoint.isEmpty()) {
                dataPoints.add(positiveDataPoint);
                //select at most 5 or if the negative datapoints size is lower than 5 then put all of them
                dataPoints.addAll(negativeDataPoints.subList(0, Math.min(5, negativeDataPoints.size())));
            }
        }

//        FileUtil.writeListToFile("entityLinkingTrainingData.csv", dataPoints, false);
        String wekaFileHeader = "@RELATION entityLinking\n"
                + "\n"
                + "@ATTRIBUTE ngramSize  NUMERIC\n"
                //                + "@ATTRIBUTE nodeEdgeScore   NUMERIC\n"
                + "@ATTRIBUTE mentionFreq  NUMERIC\n"
                + "@ATTRIBUTE class        {0,1}\n"
                + "@DATA\n";

        FileUtil.writeStringToFile("wekaEntityLinkingTrainingData.arff", wekaFileHeader, false);
        FileUtil.writeSetToFile("wekaEntityLinkingTrainingData.arff", dataPoints, true);
    }

   

    private static List<Instance> retrieveMatches(String query, int topK, boolean isEntity) {
        List<Instance> instances = new ArrayList<>();

        try {

            query = URLEncoder.encode(query, "UTF-8");

            String url = "http://purpur-v10:8080/findEntities?query=" + query + "&k=" + topK;
            if (!isEntity) {
                url = url.replace("findEntities", "findPredicates");
            }

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
//            System.out.println("\nSending 'GET' request to URL : " + url);
//            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
//            System.out.println(response.toString());
            Gson gson = new Gson();

            List<Object> objects = gson.fromJson(response.toString(), List.class);

            for (Object o : objects) {
                Map<String, Object> map = (LinkedTreeMap) o;

                String uri = map.get("uri").toString();
                double frequency = (double) map.get("frequency");
                Instance i1 = new Instance(query, uri, (int) frequency);

                instances.add(i1);
            }

        } catch (Exception e) {
//            e.printStackTrace();
        }

        return instances;
    }

    private static Map<String, Double> loadNodeScores(String filePath, Set<String> validEntities) {
        Map<String, Double> map = new ConcurrentHashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 2) {
                    long freebaseLongID = Long.parseLong(c[0]);
                    double score = Double.parseDouble(c[1]);

                    String freebaseID = convertLongToMid(freebaseLongID);

                    if (validEntities.contains(freebaseID)) {
                        map.put(freebaseID, score);
                    }

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    private static Map<String, Integer> loadEdgeCounts(String filePath, Set<String> validEntities) {
        Map<String, Integer> map = new ConcurrentHashMap<>();

//        DecimalFormat df2 = new DecimalFormat(".");
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 4) {
                    long freebaseLongID = Long.parseLong(c[0]);
                    String freebaseID = convertLongToMid(freebaseLongID);

                    if (validEntities.contains(freebaseID)) {

                        //in degree
                        int inDegree = Integer.parseInt(c[1]);
                        int outDegree = Integer.parseInt(c[2]);

                        if (inDegree > 5 && outDegree > 5) {
                            double average = inDegree + outDegree / (double) 2;

//                            double formatted = Double.parseDouble(df2.format(average));
                            map.put(freebaseID, (int) average);
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
