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
import de.citec.sc.utils.StringPreprocessor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author sherzod
 */
public class UpperBound {

    public static void main(String[] args) {

        System.out.println("Entity Linking upperbound for Test");
        computeUpperBound("test", true, 6);
        System.out.println("Predicate Linking upperbound Test");
        computeUpperBound("test", false, 3);

        System.out.println("Entity Linking upperbound for Train");
        computeUpperBound("train", true, 6);
        System.out.println("Predicate Linking upperbound Train");
        computeUpperBound("train", false, 3);

    }

    public static void computeUpperBound(String fileName, boolean isEntity, int maxNGramSize) {

//        System.out.println("Looping over instances ... ");
        Set<String> dataPoints = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_" + fileName + ".txt");

        //loop over all training instances and extract uris with their features
        Map<Integer, Integer> topKCounts = new LinkedHashMap<>();
        topKCounts.put(1, 0);
        topKCounts.put(3, 0);
        topKCounts.put(5, 0);
        topKCounts.put(8, 0);
        topKCounts.put(10, 0);
        topKCounts.put(15, 0);
        topKCounts.put(20, 0);
        topKCounts.put(30, 0);
        topKCounts.put(50, 0);
        topKCounts.put(80, 0);
        topKCounts.put(100, 0);
        topKCounts.put(1000, 0);

        int count = 0;

        for (String s : dataPoints) {

            count++;

//            if (count == 100) {
//                break;
//            }
            if (count % 3000 == 0) {

                double progr = count / (double) dataPoints.size();

                String strDouble = String.format("%.4f", progr);
                System.out.println(fileName+": "+strDouble);
//                System.out.println("\n" + count + "/" + dataPoints.size());
//                for (int topK : topKCounts.keySet()) {
//                    double overall = topKCounts.get(topK) / (double) count;
//                    System.out.println("\tTopK:" + topK + "  Recall: " + overall);
//                }
            }

            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = "m." + c[0].replace("www.freebase.com/m/", "");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

            boolean hasResult = false;
            for (int ngramSize = maxNGramSize; ngramSize > 0; ngramSize--) {

                if (hasResult) {
                    break;
                }

                //extract ngrams based on the size given
                //ngrams that size equal to ngramSize as
                List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

                for (String ngram : ngrams) {

                    String lemma = StringPreprocessor.preprocess(ngram, Language.lang.EN);

                    //loop over each topK count and check if the subject is in the list
                    for (Integer topK : topKCounts.keySet()) {

                        if (hasResult) {
                            topKCounts.put(topK, topKCounts.get(topK) + 1);
                            continue;
                        }

                        List<Instance> instances = retrieveMatches(lemma, topK, isEntity);

                        for (Instance i : instances) {

                            if (isEntity) {

                                if (i.getUri().equals(subject)) {
                                    topKCounts.put(topK, topKCounts.getOrDefault(topK, 1) + 1);
                                    hasResult = true;
                                    break;
                                }
                            } else {
                                if (i.getUri().equals(predicate)) {
                                    topKCounts.put(topK, topKCounts.getOrDefault(topK, 1) + 1);
                                    hasResult = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (hasResult) {
                        break;
                    }
                }
            }
        }

        System.out.println("\nEnd ...\n");

        String output = fileName + "\n";

        for (int topK : topKCounts.keySet()) {
            double overall = topKCounts.get(topK) / (double) count;

            output += "\tTopK:" + topK + "  Recall: " + overall + "\n";

            System.out.println("TopK:" + topK + "  Recall: " + overall);
        }

        String fileNamePath = "upperBound_" + fileName;
        if (isEntity) {
            fileNamePath += "_entity.txt";
        } else {
            fileNamePath += "_predicate.txt";
        }

        FileUtil.writeListToFile(fileNamePath, output, false);

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
}
