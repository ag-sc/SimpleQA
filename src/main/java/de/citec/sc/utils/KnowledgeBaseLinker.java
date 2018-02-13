/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import de.citec.sc.index.Instance;
import de.citec.sc.index.Language;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sherzod
 */
public class KnowledgeBaseLinker {

    public static List<List<Instance>> getMatches(String text, int topK, int entityMaxNGramSize, int predicateMaxNGramSize) {

        List<List<Instance>> results = new ArrayList<>();

        //get property matches
        Map<String, List<Instance>> propertyMatches = new HashMap<>();
        boolean isEntity = false;

        for (int ngramSize = predicateMaxNGramSize; ngramSize > 0; ngramSize--) {

            //extract ngrams based on the size given
            //ngrams that size equal to ngramSize as
            List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

            //collect all matches in a list
            for (String ngram : ngrams) {

                //remove matches that are already covered by bigger ngrams
                boolean isValid = true;

                for (String m : propertyMatches.keySet()) {
                    if (m.contains(ngram)) {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    List<Instance> instances = retrieveMatches(ngram, 8, isEntity);

                    if (!instances.isEmpty()) {
                        propertyMatches.put(ngram, instances);
                    }
                }
            }
        }

        Map<String, List<Instance>> matches = new HashMap<>();
        Map<Instance, Integer> instance2NGramSize = new HashMap<>();
        isEntity = true;

        for (int ngramSize = entityMaxNGramSize; ngramSize > 0; ngramSize--) {

            //extract ngrams based on the size given
            //ngrams that size equal to ngramSize as
            List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

            //collect all matches in a list
            for (String ngram : ngrams) {

                String lemma = StringPreprocessor.preprocess(ngram, Language.lang.EN);

                //remove matches that are already covered by bigger ngrams
                boolean isValid = true;

                for (String m : matches.keySet()) {
                    if (m.contains(ngram)) {
                        isValid = false;
                        break;
                    }
                }

                //exclude ngrams found in properties
                for (String m : propertyMatches.keySet()) {
                    if (m.contains(ngram)) {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    List<Instance> instances = retrieveMatches(lemma, 8, isEntity);

                    if (!instances.isEmpty()) {
                        matches.put(ngram, instances);

                        for (Instance i : instances) {
                            instance2NGramSize.put(i, ngramSize);
                        }
                    }
                }
            }
        }

        Map<Object, Double> mapOfEntityInstances = new HashMap<>();

        //calculate all scores
        for (String ngram : matches.keySet()) {
            for (Instance i : matches.get(ngram)) {

                int ngramSize = instance2NGramSize.get(i);

                double score = ngramSize * 0.8 + i.getFrequency() * 0.01;

                if (mapOfEntityInstances.containsKey(i)) {
                    if (mapOfEntityInstances.get(i) < score) {
                        mapOfEntityInstances.put(i, score);
                    }
                } else {
                    mapOfEntityInstances.put(i, score);
                }
            }
        }

        //sort
        mapOfEntityInstances = SortUtils.sortByDoubleValue(mapOfEntityInstances);
        
        List<Instance> entityMatches = new ArrayList<>();
        List<Instance> predicateMatches = new ArrayList<>();
        

        //get the topK in mapOfEntityInstances
        int uriCount = 0;
        for (Object o1 : mapOfEntityInstances.keySet()) {
            uriCount++;

            entityMatches.add((Instance) o1);

            if (uriCount == topK) {
                break;
            }
        }
        
        //get all matches in propertyMatches
        for(String ngram : propertyMatches.keySet()){
            List<Instance> instances = propertyMatches.get(ngram);
            predicateMatches.addAll(instances);
        }
        
        
        results.add(entityMatches);
        results.add(predicateMatches);

        return results;
    }

    private static List<Instance> retrieveMatches(String ngram, int topK, boolean isEntity) {
        List<Instance> instances = new ArrayList<>();

        try {

            //preprocess the input text
            String query = StringPreprocessor.preprocess(ngram, Language.lang.EN);

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

                Instance i1 = new Instance(ngram, uri, (int) frequency);

                instances.add(i1);
            }

        } catch (Exception e) {
//            e.printStackTrace();
        }

        return instances;
    }
}
