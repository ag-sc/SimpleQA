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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sherzod
 */
public class EntityExtractorUtils {

    public static List<String> extractNamedEntities(String text, int maxNgramSize, double minSimThreshold) {
        List<String> ngrams = new ArrayList<>();

        text = text.replaceAll("\\s+", " ");
        String[] unigrams = text.split("\\s");

        for (int i = 0; i < unigrams.length; i++) {

            for (int n = maxNgramSize; n > 0; n--) {

                if (i + n <= unigrams.length) {

                    String originialNgram = "";

                    for (int k = i; k < i + n; k++) {
                        originialNgram += unigrams[k] + " ";
                    }

                    originialNgram = originialNgram.replace("?", "");
                    originialNgram = originialNgram.trim();

                    String ngram = originialNgram;

                    //check if the word isn't stopword
                    if (Stopwords.isStopWord(ngram, Language.lang.EN)) {
                        continue;
                    }
                    //check if unigrams in the word aren't stopwords
                    String[] tokens = ngram.trim().split(" ");
                    int numberOfStopWords = 0;
                    for (String t : tokens) {
                        if (Stopwords.isStopWord(t, Language.lang.EN)) {
                            numberOfStopWords++;
                        }
                    }

                    double percentageOfStopWords = numberOfStopWords / (double) tokens.length;

                    if (percentageOfStopWords >= 0.7) {
                        continue;
                    }

                    List<Instance> instances = retrieveFromAPI(ngram, 10, true);
                    if (instances.isEmpty()) {
                        //preprocess the ngram

                        ngram = ngram.replaceAll("'s", "")
                                .replaceAll("í", "i")
                                .replaceAll("é", "e")
                                .replaceAll("ó", "o")
                                .replaceAll("á", "a")
                                .replaceAll("ñ", "ny")
                                .replaceAll("ü", "u")
                                .replaceAll("ú", "u")
                                .replaceAll("ö", "o")
                                .replaceAll("ä", "a")
                                .replaceAll("ß", "s")
                                .replaceAll("č", "c")
                                .replaceAll("ć", "c");

                        ngram = Normalizer.normalize(ngram, Normalizer.Form.NFD);
                        ngram = ngram.replaceAll("\\p{M}", "");
                        ngram = ngram.replaceAll("[!?,.;:'\"]", "");
                        ngram = ngram.replaceAll("\\s+", " ");
                        ngram = ngram.replaceAll("-", " ");
                        ngram = ngram.replace("\\", "");

                        ngram = ngram.replace("- ", "-");
                        ngram = ngram.replace(" - ", "-");
                        ngram = ngram.replace(" -", "-");

                        ngram = ngram.trim();

                        instances = retrieveFromAPI(ngram, 50, false);
                        if (instances.isEmpty()) {
                            continue;
                        }
                    }

                    //loop over all uris and compute similarity to the ngram
                    //if the sim > threshold then it's found
                    for (Instance instance : instances) {

                        String uri = instance.getUri();
                        uri = uri.replaceAll("'s", "")
                                .replaceAll("í", "i")
                                .replaceAll("é", "e")
                                .replaceAll("ó", "o")
                                .replaceAll("á", "a")
                                .replaceAll("ñ", "ny")
                                .replaceAll("ü", "ue")
                                .replaceAll("ú", "u");

                        double similarity = StringSimilarityUtils.getSimilarityScore(ngram, uri);
                        if (similarity >= minSimThreshold) {
                            ngrams.add(originialNgram);
                            i = i + n - 1;
                            break;
                        }
                    }
                }
            }
        }

        return ngrams;
    }

    public static List<String> extractProperties(String text, int maxNgramSize, double minSimThreshold) {
        List<String> ngrams = new ArrayList<>();

        text = text.replaceAll("\\s+", " ");
        String[] unigrams = text.split("\\s");

        for (int i = 0; i < unigrams.length; i++) {

            for (int n = maxNgramSize; n > 0; n--) {

                if (i + n <= unigrams.length) {

                    String originialNgram = "";

                    for (int k = i; k < i + n; k++) {
                        originialNgram += unigrams[k] + " ";
                    }

                    originialNgram = originialNgram.replace("?", "");
                    originialNgram = originialNgram.trim();

                    String ngram = originialNgram;

                    //check if the word isn't stopword
                    if (Stopwords.isStopWord(ngram, Language.lang.EN)) {
                        continue;
                    }
                    //check if unigrams in the word aren't stopwords
                    String[] tokens = ngram.trim().split(" ");
                    int numberOfStopWords = 0;
                    for (String t : tokens) {
                        if (Stopwords.isStopWord(t, Language.lang.EN)) {
                            numberOfStopWords++;
                        }
                    }

                    double percentageOfStopWords = numberOfStopWords / (double) tokens.length;

                    if (percentageOfStopWords >= 0.7) {
                        continue;
                    }

                    List<Instance> instances = retrieveFromAPI(ngram, 100, false);
                    if (instances.isEmpty()) {
                        //preprocess the ngram

                        ngram = ngram.replaceAll("'s", "")
                                .replaceAll("í", "i")
                                .replaceAll("é", "e")
                                .replaceAll("ó", "o")
                                .replaceAll("á", "a")
                                .replaceAll("ñ", "ny")
                                .replaceAll("ü", "u")
                                .replaceAll("ú", "u")
                                .replaceAll("ö", "o")
                                .replaceAll("ä", "a")
                                .replaceAll("ß", "s")
                                .replaceAll("č", "c")
                                .replaceAll("ć", "c");

                        ngram = Normalizer.normalize(ngram, Normalizer.Form.NFD);
                        ngram = ngram.replaceAll("\\p{M}", "");
                        ngram = ngram.replaceAll("[!?,.;:'\"]", "");
                        ngram = ngram.replaceAll("\\s+", " ");
                        ngram = ngram.replaceAll("-", " ");
                        ngram = ngram.replace("\\", "");

                        ngram = ngram.replace("- ", "-");
                        ngram = ngram.replace(" - ", "-");
                        ngram = ngram.replace(" -", "-");

                        ngram = ngram.trim();

                        instances = retrieveFromAPI(ngram, 50, false);
                        if (instances.isEmpty()) {
                            continue;
                        }
                    }

                    //loop over all uris and compute similarity to the ngram
                    //if the sim > threshold then it's found
                    for (Instance instance : instances) {

                        String uri = instance.getUri();
                        uri = uri.replaceAll("'s", "")
                                .replaceAll("í", "i")
                                .replaceAll("é", "e")
                                .replaceAll("ó", "o")
                                .replaceAll("á", "a")
                                .replaceAll("ñ", "ny")
                                .replaceAll("ü", "ue")
                                .replaceAll("ú", "u");

                        double similarity = StringSimilarityUtils.getSimilarityScore(ngram, uri);
                        if (similarity >= minSimThreshold) {
                            ngrams.add(originialNgram);
                            i = i + n - 1;
                            break;
                        }
                    }
                }
            }
        }

        return ngrams;
    }

    private static List<Instance> retrieveFromAPI(String query, int topK, boolean isEntity) {
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
}
