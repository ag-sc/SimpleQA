/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.index.Instance;
import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.KnowledgeBaseLinker;
import de.citec.sc.utils.NGramExtractor;
import de.citec.sc.utils.WordEmbeddingUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class PairwiseLinkingBaseline {

    private static final int entityMaxNGramSize = 6;
    private static final int predicateMaxNGramSize = 4;
    private static final int topK = 10;

    public static void main(String[] args) {
        train("test");
    }

    private static void train(String fileName) {

        Map<String, Set<String>> entity2PredicateMap = new HashMap<>();

        System.out.println("Reading Freebase2M file into memory ... ");

        String filePath = "/home/sherzod/NetBeansProjects/SimpleQuestions.DBpedia/SimpleQuestions_v2/freebase-subsets/freebase-FB2M.txt";
        Set<String> content = FileUtil.readFile(filePath);

        for (String item : content) {

            String[] c = item.split("\t");
            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

            if (entity2PredicateMap.containsKey(subject)) {
                Set<String> predicates = entity2PredicateMap.get(subject);
                predicates.add(predicate);

                entity2PredicateMap.put(subject, predicates);
            } else {
                Set<String> predicates = new HashSet<>();
                predicates.add(predicate);

                entity2PredicateMap.put(subject, predicates);
            }
        }

        Set<String> dataPoints = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_" + fileName + ".txt");

        int count = 0;
        int correctCount = 0;
        int correctEntityCount = 0;
        int correctPredicateCount = 0;

        for (String s : dataPoints) {

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

            List<List<Instance>> matches = KnowledgeBaseLinker.getMatches(text, topK, entityMaxNGramSize, predicateMaxNGramSize);

            // entity matches are always in 0 index, predicates are in index 1
            List<Instance> entityMatches = matches.get(0);
            List<Instance> predicateMatches = matches.get(1);

            String detectedPredicateURI = "";
            String detectedEntityURI = "";

            for (Instance entityInstance : entityMatches) {
                Set<String> entityPredicates = entity2PredicateMap.get(entityInstance.getUri());

                //loop over all detected predicates and check if the entityInstance has any of them
                //stop if there is such entity
                boolean predicateIsDetected = false;

                for (Instance predicateInstance : predicateMatches) {
                    if (entityPredicates.contains(predicateInstance.getUri())) {
                        predicateIsDetected = true;
                        detectedPredicateURI = predicateInstance.getUri();
                        break;
                    }
                }

                if (predicateIsDetected) {
                    detectedEntityURI = entityInstance.getUri();
                    break;
                }
            }

            if (!detectedEntityURI.isEmpty() && !detectedPredicateURI.isEmpty()) {
                if (detectedEntityURI.equals(subject) && detectedPredicateURI.equals(predicate)) {
                    correctCount++;
                }
                if (detectedEntityURI.equals(subject)) {
                    correctEntityCount++;
                }
                if (detectedPredicateURI.equals(predicate)) {
                    correctPredicateCount++;
                }
            } else {
                //choose one predicate based on embedding similarity

                String maxPredicate = "";
                String maxEntity = "";
                double maxPredicateSimScore = 0;

                for (Instance entityInstance : entityMatches) {
                    Set<String> entityPredicates = entity2PredicateMap.get(entityInstance.getUri());

                    for (String p : entityPredicates) {
                        String predicateLabel = p.substring(p.lastIndexOf(".") + 1).replace("_", " ").trim();

                        //get all ngrams in the text
                        List<String> ngrams = NGramExtractor.extractAllNGrams(text, predicateMaxNGramSize);

                        //compute embedding similarity with predicate label and each ngram
                        for (String ngram : ngrams) {
                            double embeddingSim = WordEmbeddingUtil.computeSimilarity(ngram, predicateLabel, Language.lang.EN);

                            if (embeddingSim > maxPredicateSimScore) {
                                maxPredicate = p;
                                maxEntity = entityInstance.getUri();
                                maxPredicateSimScore = embeddingSim;
                            }
                        }
                    }
                }

                detectedEntityURI = maxEntity;
                detectedPredicateURI = maxPredicate;

                //evaluate
                if (detectedEntityURI.equals(subject) && detectedPredicateURI.equals(predicate)) {
                    correctCount++;
                }
                if (detectedEntityURI.equals(subject)) {
                    correctEntityCount++;
                }
                if (detectedPredicateURI.equals(predicate)) {
                    correctPredicateCount++;
                }

            }
        }

        double score = correctCount / (double) count;
        double entityScore = correctEntityCount / (double) count;
        double predicateScore = correctPredicateCount / (double) count;

        System.out.println("Overall Recall: " + score);
        System.out.println("Entity Recall: " + entityScore);
        System.out.println("Predicate Recall: " + predicateScore);
    }

    private static void test() {
    }
}
