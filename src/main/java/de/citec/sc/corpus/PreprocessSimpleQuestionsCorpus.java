/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import de.citec.sc.index.Instance;
import de.citec.sc.index.Language;
import de.citec.sc.utils.EntityExtractorUtils;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.NGramExtractor;
import de.citec.sc.utils.StringPreprocessor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class PreprocessSimpleQuestionsCorpus {

    public static void main(String[] args) {

        List<String> fileNames = new ArrayList<>();
        fileNames.add("subset");
        fileNames.add("small");
//        fileNames.add("valid");
        fileNames.add("train");
        fileNames.add("test");

        for (String f : fileNames) {
            String filePath = PreprocessSimpleQuestionsCorpus.class.getClassLoader().getResource("simplequestions").getPath() + "/annotated_fb_data_" + f + ".txt";
            Set<String> content = FileUtil.readFile(filePath);

            Set<String> replacedContent = new HashSet<>();

            int entityCounter = 0;
            int predicateCounter = 0;
            for (String c : content) {
                
                String[] s = c.split("\t");
                String text = s[3];
                text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();

                boolean hasResult = false;

                int maxNGramSize = 6;
                boolean isEntity = true;

                String replacedText = text;

                for (int ngramSize = maxNGramSize; ngramSize > 0; ngramSize--) {

                    if (hasResult) {
                        break;
                    }

                    //extract ngrams based on the size given
                    //ngrams that size equal to ngramSize as
                    List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

                    for (String ngram : ngrams) {

                        String lemma = StringPreprocessor.preprocess(ngram, Language.lang.EN);
                        List<Instance> instances = retrieveMatches(lemma, 100, isEntity);

                        if (!instances.isEmpty()) {

                            String replacement = ngram.replace(" ", "@").replace("-", "@");
                            replacedText = replacedText.replace(ngram, replacement);
                            entityCounter ++;
                            hasResult = true;
                        }

                        if (hasResult) {
                            break;
                        }
                    }
                }
                
                isEntity = false;
                hasResult = false;
                maxNGramSize = 3;
                        
                for (int ngramSize = maxNGramSize; ngramSize > 0; ngramSize--) {

                    if (hasResult) {
                        break;
                    }

                    //extract ngrams based on the size given
                    //ngrams that size equal to ngramSize as
                    List<String> ngrams = NGramExtractor.extractNGrams(text, ngramSize);

                    for (String ngram : ngrams) {

                        String lemma = StringPreprocessor.preprocess(ngram, Language.lang.EN);
                        List<Instance> instances = retrieveMatches(lemma, 100, isEntity);

                        if (!instances.isEmpty()) {

                            String replacement = ngram.replace(" ", "@@").replace("-", "@@");
                            replacedText = replacedText.replace(ngram, replacement);
                            predicateCounter++;
                            hasResult = true;
                        }

                        if (hasResult) {
                            break;
                        }
                    }
                }

                String newContent = s[0] + "\t" + s[1] + "\t" + s[2] + "\t" + replacedText;
                replacedContent.add(newContent);

//                String preprocessedText = text;
//                
//                int maxNGramSize = 6;
//                double minStringSimilarityThreshold = 0.8;
//                List<String> namedEntities = EntityExtractorUtils.extractNamedEntities(preprocessedText, maxNGramSize, minStringSimilarityThreshold);
//
//                String replacedText = text;
//                for (String n1 : namedEntities) {
//
//                    String replacement = n1.replace(" ", "@").replace("-", "@");
//                    replacedText = replacedText.replace(n1, replacement);
//                }
//
//                //add the changed text, if possible
//                String newContent = s[0] + "\t" + s[1] + "\t" + s[2] + "\t" + replacedText;
//                replacedContent.add(newContent);
//
//                if (!text.equals(replacedText)) {
//                    counter++;
//                    System.out.println(text);
//                    System.out.println(replacedText);
//                    System.out.println("======================================================\n");
//                }
//                else{
////                    System.out.println(f+"\t"+c);
//                }
            }

            System.out.println(f + " entity count : " + entityCounter + "/" + content.size());
            System.out.println(f + " predicate count : " + predicateCounter + "/" + content.size());
            String filePath2 = "src/main/resources/simplequestions/annotated_fb_data_" + f + "_replaced.txt";

            FileUtil.writeSetToFile(filePath2, replacedContent, false);
        }
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

    private static Set<DataPoint> readSimpleQuestions(String filePath) {
        Set<DataPoint> dataPoints = new HashSet<>();

        Set<String> content = FileUtil.readFile(filePath);

        int counter = 0;
        for (String c : content) {

            counter++;
            String[] s = c.split("\t");

            String id = counter + "";
            String question = s[3];
            String query = "SELECT ?x WHERE { ?x <http://dbpedia.org/ontology/" + s[1] + "> <http://dbpedia.org/resource/" + s[2] + "> }";
            Set<String> answers = new HashSet<>();
            answers.add(s[0]);

            DataPoint dataPoint = new DataPoint(question, query, answers, id);

            dataPoints.add(dataPoint);
        }

        return dataPoints;
    }
}
