/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import de.citec.sc.index.Language;
import de.citec.sc.utils.EntityExtractorUtils;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.SortUtils;
import de.citec.sc.utils.Stopwords;
import de.citec.sc.utils.StringPreprocessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class PostprocessSimpleQuestionsCorpus {

    public static void main(String[] args) {

        List<String> fileNames = new ArrayList<>();
        fileNames.add("subset");
        fileNames.add("train");
        fileNames.add("test");

        Map<Object, Integer> freqMap = new HashMap<>();

        for (String f : fileNames) {
            String filePath = PostprocessSimpleQuestionsCorpus.class.getClassLoader().getResource("simplequestions").getPath() + "/annotated_fb_data_" + f + "_replaced.txt";
            Set<String> content = FileUtil.readFile(filePath);

            for (String c : content) {
                String[] s = c.split("\t");
                String text = s[3].trim();
                text = text.replaceAll("\\s+", " ");
                text = text.replace("?", " ?");

                text = text.trim();

                String[] tokens = text.split(" ");
                for (String t : tokens) {
                    if (t.contains("@")) {
                        freqMap.put(t, freqMap.getOrDefault(t, 1) + 1);
                    }

                }
            }
        }
        
        freqMap = SortUtils.sortByIntegerValue(freqMap);
        Map<String, Integer> topWords = new HashMap<>();
        
        int count = 0;
        for(Object o1 : freqMap.keySet()){
            count ++;
            
            topWords.put(o1.toString(), freqMap.get(o1.toString()));
            
            if(count >= 100){
                break;
            }
        }

        for (String f : fileNames) {
            String filePath = PostprocessSimpleQuestionsCorpus.class.getClassLoader().getResource("simplequestions").getPath() + "/annotated_fb_data_" + f + "_replaced.txt";
            Set<String> content = FileUtil.readFile(filePath);

            Set<String> replacedContent = new HashSet<>();
            
            for (String c : content) {
                String[] s = c.split("\t");
                String text = s[3].trim();
                text = text.replaceAll("\\s+", " ");
                text = text.replace("?", " ?");

                text = text.trim();

                String[] tokens = text.split(" ");
                for (String t : tokens) {
                    if (topWords.containsKey(t)) {

                        String[] ngrams = t.split("@");
                        int stopWordCount = 0;
                        for (String n1 : ngrams) {
                            if (Stopwords.isStopWord(n1, Language.lang.EN)) {
                                stopWordCount++;
                            }
                        }

                        double percentage = stopWordCount / (double) ngrams.length;

                        if (percentage >= 0.5) {
                            
                            //replace the @ sign with space
                            String replaced = t.replace("@", " ");
                            text = text.replace(t, replaced);
                        }
                    }
                }
                
                replacedContent.add(s[0]+"\t"+s[1]+"\t"+s[2]+"\t"+text);
            }
//
            System.out.println(f);
            String filePath2 = "src/main/resources/simplequestions/annotated_fb_data_" + f + "_replaced2.txt";

            FileUtil.writeSetToFile(filePath2, replacedContent, false);
        }

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
