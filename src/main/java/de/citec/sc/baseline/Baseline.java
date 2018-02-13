/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.citec.sc.index.Instance;
import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.NGramExtractor;
import de.citec.sc.utils.SortUtils;
import de.citec.sc.utils.Stopwords;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 *
 * @author sherzod
 */
public class Baseline {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static int maxNgramSize = 3;

    public static void main(String[] args) {
        
//        Map<String, String> wikidataToFreebaseMap = loadFreebaseToWikidataLinks("/home/sherzod/Downloads/fb2w.nt");
//            getURIs();
//        computeOverlap();
//        computeLinkingOverlap();
//        computeLinkingOverlap();

//        Map<String, Map<String, Double>> tfIDFMap = trainTFIDF();
//        test(tfIDFMap);
    }

//    private static Map<String, Map<String, Double>> loadTfIdfMap() {
//        Map<String, Map<String, Double>> tfIdfMap = new HashMap<>();
//
//        File f = new File("tfIdfMap.txt");
//        if (f.exists()) {
//
//            try {
//                tfIdfMap = objectMapper.readValue(f, Map.class);
//            } catch (IOException ex) {
//                Logger.getLogger(Baseline.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        return tfIdfMap;
//    }

//    private static Map<String, Map<String, Double>> trainTFIDF() {
//
//        Map<String, Map<String, Double>> map = loadTfIdfMap();
//
//        if (!map.isEmpty()) {
//            return map;
//        }
//
//        Set<String> content = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");
//
//        System.out.println("Training docs: " + content.size());
//
//        int counter = 0;
//
//        Map<String, List<String>> mapOfDocuments = new HashMap<>();
//        for (String s : content) {
//            counter++;
//            String[] c = s.split("\t");
//
//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
//
//            String subject = c[0];
//            String predicate = c[1];
//            String object = c[2];
//
//            List<String> mappings = createMappings(text, maxNgramSize, subject, predicate);
//
//            mapOfDocuments.put("Doc" + counter, mappings);
//        }
//
//        Map<String, Set<String>> sparseMapOfDocuments = new HashMap<>();
//        //loop over each doc and all mappings as set
//        for (String docName : mapOfDocuments.keySet()) {
//            Set<String> mappings = new HashSet<>(mapOfDocuments.get(docName));
//
//            sparseMapOfDocuments.put(docName, mappings);
//        }
//        //compute idf scores for all mappings in all documents
//        Map<String, Double> idfMap = TFIDF.getIDFs(sparseMapOfDocuments);
//
//        Map<String, Double> tfIdfMap = new HashMap<>();
//
//        //compute tf-idf score for each mapping
//        for (String docName : mapOfDocuments.keySet()) {
//            List<String> mappings = mapOfDocuments.get(docName);
//
//            Map<String, Double> tfMap = TFIDF.getTFs(mappings, false);
//
//            for (String m : tfMap.keySet()) {
//                Double tf = tfMap.get(m);
//                Double idf = idfMap.get(m);
//
//                Double tfIdf = tf * idf;
//
//                tfIdfMap.put(m, tfIdf);
//            }
//        }
//
//        //sort
//        tfIdfMap = SortUtils.sortByDoubleValue((HashMap<String, Double>) tfIdfMap);
//
//        //convert lemma=URI TFIDF score into ->>> lemma -> (URI, TFIDF Score)
//        for (String l : tfIdfMap.keySet()) {
//            String lemma = l.substring(0, l.indexOf("="));
//            String uri = l.substring(l.indexOf("=") + 1);
//            Double score = tfIdfMap.get(l);
//
//            Map<String, Double> uriMap = new HashMap<>();
//            uriMap.put(uri, score);
//
//            map.put(lemma, uriMap);
//        }
//
//        //save the map
//        String s = "";
//
//        try {
//            if (!map.isEmpty()) {
//                s = objectMapper.writeValueAsString(map);
//
//                FileUtil.writeListToFile("tfIdfMap.txt", s, false);
//            }
//        } catch (JsonProcessingException ex) {
//            ex.printStackTrace();
//        }
//
//        return map;
//    }

//    private static void test(Map<String, Map<String, Double>> tfIDFMap) {
//        Set<String> content = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_test.txt");
//
//        int counter = 0;
//
//        int correctSubjectCount = 0;
//        int correctPredicateCount = 0;
//
//        for (String s : content) {
//            counter++;
//            String[] c = s.split("\t");
//
//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
//
//            String subject = c[0];
//            String predicate = c[1];
//            String object = c[2];
//
//            Map<String, Double> mappings = scoreMappings(text, maxNgramSize, tfIDFMap);
//
//            //sort
//            mappings = SortUtils.sortByDoubleValue((HashMap<String, Double>) mappings);
//
//            for (String m1 : mappings.keySet()) {
//                String predicatedPredicateURI = m1.substring(m1.indexOf("=") + 1, m1.length());
//                String predicatedSubjectURI = m1.substring(m1.indexOf("="));
//
//                if (predicatedPredicateURI.equals(predicate)) {
//                    correctPredicateCount++;
//                }
//                if (predicatedSubjectURI.equals(subject)) {
//                    correctSubjectCount++;
//                }
//
//                //no need loop over, the max mappings is in the first position
//                break;
//            }
//        }
//
//        double subjectScore = correctSubjectCount / (double) counter;
//        double predicateScore = correctPredicateCount / (double) counter;
//
//        System.out.println("Predicate score: " + predicateScore);
//        System.out.println("Subject score: " + subjectScore);
//    }

//    private static Map<String, Double> scoreMappings(String text, int maxNgramSize, Map<String, Map<String, Double>> tfIdfMap) {
//
//        text = text.replaceAll("\\s+", " ").trim();
//        String[] unigrams = text.split("\\s");
//
//        String tokenizedText = "";
//        for (int i = 0; i < unigrams.length; i++) {
//            tokenizedText += "Token:" + i + "=" + unigrams[i] + " ";
//        }
//        tokenizedText = tokenizedText.trim();
//
//        List<String> ngrams = NGramExtractor.extractNamedEntities(tokenizedText, maxNgramSize);
//
//        Map<String, Double> scoredMappings = new HashMap<>();
//
//        for (String ngram : ngrams) {
//            //replace the ngram in the tokenizedText and extract all possible ngrams to assign the remaining part of the sentence
//            String remainingText = tokenizedText.replace(ngram, "");
//            List<String> remainingNgrams = NGramExtractor.extractNamedEntities(remainingText, maxNgramSize);
//
//            //assign the predicate all possible remaining ngrams
//            for (String remainingNgram : remainingNgrams) {
//
//                //also remove up to the = sign
//                //convert Token:0=what Token:1=songs Token:2=have=Subj    Token:5=produced?=Pred
//                //into what songs have=Subj    produced?=Pred
//                String subjectMapping = removeTokenAnnotation(ngram);
//                String predicateMapping = removeTokenAnnotation(remainingNgram);
//
//                String subjectURI = "";
//                String predicateURI = "";
//                Double subjectScore = 0d;
//                Double predicateScore = 0d;
//
//                if (tfIdfMap.containsKey(subjectMapping) && tfIdfMap.containsKey(predicateMapping)) {
//                    Map<String, Double> subjectMap = tfIdfMap.get(subjectMapping);
//                    Map<String, Double> predicateMap = tfIdfMap.get(predicateMapping);
//
//                    //sort the uri maps
//                    subjectMap = SortUtils.sortByDoubleValue((HashMap<String, Double>) subjectMap);
//                    predicateMap = SortUtils.sortByDoubleValue((HashMap<String, Double>) predicateMap);
//
//                    for (String s : subjectMap.keySet()) {
//                        //check if the uri is of entity type
//                        //entities start with m -> www.freebase.com/m/01smmxsd
//                        if (s.startsWith("www.freebase.com/m/")) {
//                            subjectURI = s;
//                            subjectScore = subjectMap.get(s);
//                        }
//                    }
//
//                    for (String s : predicateMap.keySet()) {
//                        //check if the uri is of entity type
//                        //predicate doesn't start with m -> www.freebase.com/m/01smmxsd
//                        if (!s.startsWith("www.freebase.com/m/")) {
//                            predicateURI = s;
//                            predicateScore = predicateMap.get(s);
//                        }
//                    }
//                }
//
//                if (!subjectURI.isEmpty() && !predicateURI.isEmpty()) {
//                    String s = subjectURI + "=" + predicateURI;
//                    Double avgScore = (subjectScore + predicateScore) / 2;
//
//                    scoredMappings.put(s, avgScore);
//                }
//            }
//        }
//        return scoredMappings;
//    }

    private static List<String> createMappings(String text, int maxNgramSize, String subject, String predicate) {

        text = text.replaceAll("\\s+", " ").trim();
        String[] unigrams = text.split("\\s");

        String tokenizedText = "";
        for (int i = 0; i < unigrams.length; i++) {
            tokenizedText += "Token:" + i + "=" + unigrams[i] + " ";
        }
        tokenizedText = tokenizedText.trim();

        Set<String> mappings = new HashSet<>();
        List<String> ngrams = NGramExtractor.extractAllNGrams(tokenizedText, maxNgramSize);

        for (String ngram : ngrams) {
            //replace the ngram in the tokenizedText and extract all possible ngrams to assign the remaining part of the sentence
            String remainingText = tokenizedText.replace(ngram, "");
            List<String> remainingNgrams = NGramExtractor.extractAllNGrams(remainingText, maxNgramSize);

            String subjectMapping = removeTokenAnnotation(ngram) + "=" + subject;
            mappings.add(subjectMapping);
            //assign the predicate all possible remaining ngrams
            for (String remainingNgram : remainingNgrams) {

                //also remove up to the = sign
                //convert Token:0=what Token:1=songs Token:2=have=Subj    Token:5=produced?=Pred
                //into what songs have=Subj    produced?=Pred
                String predicateMapping = removeTokenAnnotation(remainingNgram) + "=" + predicate;

                mappings.add(predicateMapping);
            }
        }

        return new ArrayList<>(mappings);
    }

    private static String removeTokenAnnotation(String ngram) {
        String r = "";

        String[] tokens = ngram.split("\\s");

        for (String t1 : tokens) {
            r += t1.substring(t1.indexOf("=") + 1, t1.length()) + " ";
        }

        return r.trim();
    }

    private static void computeOverlap() {

        Set<String> trainContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");
        Set<String> testContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_test.txt");

        Set<String> trainSubjects = new HashSet<>();
        Set<String> trainObjects = new HashSet<>();
        Set<String> trainPredicates = new HashSet<>();
        for (String s : trainContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0];
            String predicate = c[1];
            String object = c[2];

            trainPredicates.add(predicate);
            trainSubjects.add(subject);
            trainObjects.add(object);

            int z = 1;
        }

        Set<String> testSubjects = new HashSet<>();
        Set<String> testPredicates = new HashSet<>();
        Set<String> testObjects = new HashSet<>();
        for (String s : testContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0];
            String predicate = c[1];
            String object = c[2];

            testPredicates.add(predicate);
            testSubjects.add(subject);

            testObjects.add(object);

            int z = 1;
        }

        int subjectCount = 0;
        for (String s1 : testSubjects) {
            if (trainSubjects.contains(s1)) {
                subjectCount++;
            }
        }
        int predicateCount = 0;
        for (String s1 : testPredicates) {
            if (trainPredicates.contains(s1)) {
                predicateCount++;
            }
        }
        int objectCount = 0;
        for (String s1 : testObjects) {
            if (trainObjects.contains(s1)) {
                objectCount++;
            }
        }

        double subjectScore = subjectCount / (double) testSubjects.size();
        double objectScore = objectCount / (double) testObjects.size();
        double predicateScore = predicateCount / (double) testPredicates.size();

        System.out.println("Subject overlap: " + subjectScore);
        System.out.println("Object overlap: " + objectScore);
        System.out.println("Predicate overlap: " + predicateScore);
    }

    private static void computeLinkingOverlap() {

        Set<String> trainContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");
        Set<String> testContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_test.txt");

        Set<String> trainSubjects = new HashSet<>();
        Set<String> trainObjects = new HashSet<>();
        Set<String> trainPredicates = new HashSet<>();
        for (String s : trainContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0];
            String predicate = c[1];
            String object = c[2];

            trainPredicates.add(predicate);
            trainSubjects.add(subject.replace("www.freebase.com/", ""));
            trainObjects.add(object.replace("www.freebase.com/", ""));

            int z = 1;
        }

        Set<String> testSubjects = new HashSet<>();
        Set<String> testPredicates = new HashSet<>();
        Set<String> testObjects = new HashSet<>();
        for (String s : testContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0];
            String predicate = c[1];
            String object = c[2];

            testPredicates.add(predicate);
            testSubjects.add(subject.replace("www.freebase.com/", ""));
            testObjects.add(object.replace("www.freebase.com/", ""));
        }

        //load DBpedia Links to Freebase
        Map<String, String> freebaseToDBpedia = loadFreebaseLinks("freebaseToDBpediaLinks.txt");

        System.out.println("Links: " + freebaseToDBpedia.size());

        //merge subject uris
        trainSubjects.addAll(testSubjects);
        trainPredicates.addAll(testPredicates);
        trainObjects.addAll(testObjects);

        System.out.println("Subject size: " + trainSubjects.size());
        System.out.println("Predicates size: " + trainPredicates.size());
        System.out.println("Object size: " + trainObjects.size());

        int subjectCount = 0;
        for (String s1 : trainSubjects) {
            if (freebaseToDBpedia.containsKey(s1)) {
                subjectCount++;
            }
        }
        int predicateCount = 0;
        for (String s1 : trainPredicates) {
            if (freebaseToDBpedia.containsKey(s1)) {
                predicateCount++;
            }
        }
        int objectCount = 0;
        for (String s1 : trainObjects) {
            if (freebaseToDBpedia.containsKey(s1)) {
                objectCount++;
            }
        }

        double subjectScore = subjectCount / (double) trainSubjects.size();
        double objectScore = objectCount / (double) trainObjects.size();
        double predicateScore = predicateCount / (double) trainPredicates.size();

        System.out.println("Subject overlap: " + subjectScore);
        System.out.println("Object overlap: " + objectScore);
        System.out.println("Predicate overlap: " + predicateScore);
    }
    private static void getURIs() {

        Set<String> trainContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");
        Set<String> testContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_test.txt");
        Set<String> validContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_valid.txt");

        Set<String> entities = new HashSet<>();
        Set<String> predicates = new HashSet<>();
        for (String s : trainContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");
            String object = c[2].replace("www.freebase.com/", "").replace("/", ".");

            predicates.add("http://rdf.freebase.com/ns/"+predicate);
            entities.add("http://rdf.freebase.com/ns/"+subject);
            entities.add("http://rdf.freebase.com/ns/"+object);
        }
        
        for (String s : testContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");
            String object = c[2].replace("www.freebase.com/", "").replace("/", ".");

            predicates.add("http://rdf.freebase.com/ns/"+predicate);
            entities.add("http://rdf.freebase.com/ns/"+subject);
            entities.add("http://rdf.freebase.com/ns/"+object);
        }
        for (String s : validContent) {
            String[] c = s.split("\t");

//            String text = c[3];
//            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");
            String object = c[2].replace("www.freebase.com/", "").replace("/", ".");

            predicates.add("http://rdf.freebase.com/ns/"+predicate);
            entities.add("http://rdf.freebase.com/ns/"+subject);
            entities.add("http://rdf.freebase.com/ns/"+object);
        }
        
        FileUtil.writeListToFile("predicates.txt", predicates, false);
        FileUtil.writeListToFile("entities.txt", entities, false);
        

        
    }

    private static Map<String, String> loadFreebaseLinks(String filePath) {

        Map<String, String> map = new HashMap<>();

        File f = new File(filePath);

        if (!f.exists()) {

            Map<String, String> freebaseMap = new ConcurrentHashMap<>(5000000);
            Map<String, String> redirectLinksMap = loadRedirectLinks("/home/sherzod/NetBeansProjects/LITDProcessor/dbpediaResourceRawData/redirects_en.nt");

            System.out.println("Loading links ...");

            try (Stream<String> stream = Files.lines(Paths.get("/home/sherzod/NetBeansProjects/SimpleQuestions.DBpedia/freebase_links_en.nt"))) {
                stream.parallel().forEach(item -> {

                    String[] c = item.split(" ");

                    String dbpediaURI = c[0];
                    String freebaseURI = c[2];

                    if (dbpediaURI.contains("http")) {

                        dbpediaURI = dbpediaURI.replace("<http://dbpedia.org/resource/", "").replace(">", "");
                        freebaseURI = freebaseURI.replace("<http://rdf.freebase.com/ns/", "").replace(">", "");
                        freebaseURI = freebaseURI.replace(".", "/");

                        if (redirectLinksMap.containsKey(dbpediaURI)) {
                            dbpediaURI = redirectLinksMap.get(dbpediaURI);
                        }

                        //handle the redirects
//                    if (map.containsKey(freebaseURI)) {
//                        if(!map.get(freebaseURI).equals(dbpediaURI)){
//                            boolean isRedirect = redirectLinksMap.containsKey(dbpediaURI);
//                            System.out.println(freebaseURI + " " + dbpediaURI + "  "+ isRedirect);
//                        }
//                    }
                        freebaseMap.put(freebaseURI, dbpediaURI);
                    }

                });

            } catch (IOException e) {
                e.printStackTrace();
            }

            //load freebase to wikidata map
            Map<String, String> wikidataToFreebaseMap = loadFreebaseToWikidataLinks("/home/sherzod/Downloads/fb2w.nt");
            Map<String, String> wikidataToDBpediaMap = loadDBpediaToWikidataLinks("/home/sherzod/NetBeansProjects/SimpleQuestions.DBpedia/dbpedia/interlanguage_links_en.ttl.bz2");

            
            System.out.println("Using mapping from wikidata->freebase & wikidata->dbpedia\n");
            System.out.println("Map size before : "+ freebaseMap.size());
            

            for (String wikidataURI : wikidataToFreebaseMap.keySet()) {
                if (wikidataToDBpediaMap.containsKey(wikidataURI)) {

                    String freebaseURI = wikidataToFreebaseMap.get(wikidataURI);

                    String dbpediaURI = wikidataToDBpediaMap.get(wikidataURI);

                    //replace redirects
                    if (redirectLinksMap.containsKey(dbpediaURI)) {
                        dbpediaURI = redirectLinksMap.get(dbpediaURI);
                    }

                    freebaseMap.put(freebaseURI, dbpediaURI);
                }
            }
            
            System.out.println("Map size after : "+ freebaseMap.size());

            //save the map
            String s = "";
            System.out.println("Saving the file size: " + freebaseMap.size());
            try {
                if (!freebaseMap.isEmpty()) {
                    s = objectMapper.writeValueAsString(freebaseMap);

                    FileUtil.writeListToFile(filePath, s, false);
                }
            } catch (JsonProcessingException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                map = objectMapper.readValue(f, Map.class);
            } catch (IOException ex) {
                Logger.getLogger(Baseline.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return map;
    }

    private static Map<String, String> loadRedirectLinks(String filePath) {

        Map<String, String> map = new ConcurrentHashMap<>(5000000);

        System.out.println("Loading redirect links ...");

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split(" ");

                String redirectURI = c[0];
                String pageURI = c[2];

                if (pageURI.contains("http")) {
                    pageURI = pageURI.replace("<http://dbpedia.org/resource/", "").replace(">", "");
                    redirectURI = redirectURI.replace("<http://dbpedia.org/resource/", "").replace(">", "");

                    map.put(redirectURI, pageURI);
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static Map<String, String> loadFreebaseToWikidataLinks(String filePath) {

        Map<String, String> map = new ConcurrentHashMap<>(5000000);

        System.out.println("Loading freebase to wikidata links ...");

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (item.startsWith("<http://")) {

                    String freebaseURI = c[0];
                    String wikidataURI = c[2];

                    freebaseURI = freebaseURI.replace("<http://rdf.freebase.com/ns/", "").replace(">", "");
                    freebaseURI = freebaseURI.replace(".", "/");
                    
                    
                    wikidataURI = wikidataURI.replace("<http://www.wikidata.org/entity/", "").replace(">", "");
                    if(wikidataURI.indexOf(".") == wikidataURI.length()-1){
                        wikidataURI = wikidataURI.substring(0, wikidataURI.length()-1);
                    }

                    map.put(wikidataURI.trim(), freebaseURI.trim());

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static Map<String, String> loadDBpediaToWikidataLinks(String filePath) {

        Map<String, String> map = new ConcurrentHashMap<>(5000000);

        System.out.println("Loading dbpedia to wikidata links ...");

        try {

            FileInputStream fin = new FileInputStream(filePath);
            BufferedInputStream bis = new BufferedInputStream(fin);
            CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
            BufferedReader br2 = new BufferedReader(new InputStreamReader(input));

            String strLine;

            while ((strLine = br2.readLine()) != null) {
                String[] triple = strLine.split("\\s");

                if (triple.length < 4) {
                    continue;
                }

                if (!triple[2].contains("wikidata")) {
                    continue;
                }
                if (triple[2].contains("dbpedia")) {
                    continue;
                }

                String wikidataEntity = triple[2].replace("<", "").replace(">", "").replace("http://www.wikidata.org/entity/", "");
                String dbpediaEntity = triple[0].replace("<", "").replace(">", "").replace("http://dbpedia.org/resource/", "");

                map.put(wikidataEntity, dbpediaEntity);
            }

            br2.close();
            fin.close();
            bis.close();
            input.close();

        } catch (Exception e) {
        }
        return map;
    }
}
