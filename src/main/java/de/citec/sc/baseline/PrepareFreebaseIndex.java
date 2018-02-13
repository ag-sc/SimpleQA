/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.StringPreprocessor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class PrepareFreebaseIndex {

    private static Map<String, Map<String, Integer>> entitySurfaceFormMap;
    private static Map<String, Map<String, Integer>> predicateSurfaceFormMap;

    public static void main(String[] args) {

        entitySurfaceFormMap = new HashMap<>();
        predicateSurfaceFormMap = new HashMap<>();

        //read all triples from freebaseSubset2million dataset
        Set<String> entities = new ConcurrentHashSet<>();
        Set<String> predicates = new ConcurrentHashSet<>();

        System.out.println("Reading Freebase2M file into memory ... ");
        String filePath = "freebase-FB2M.txt";
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
                String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

                entities.add(subject);
                predicates.add(predicate);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> freebaseToDBpediaMap = loadFreebaseToDBpediaMapping("freebaseFiles/freebaseToDBpediaLinks.txt");

        //load dbpedia index and replace the dbpedia URIs with freebase MIDs
        //add entities that appear in subject position of Freebase2M triples
        loadDBpediaIndex("indexFiles/merged.ttl2", entitySurfaceFormMap, freebaseToDBpediaMap, entities);
        System.out.println("Map size: " + entitySurfaceFormMap.size());

        //load freebase surface forms and merge with prev index
        //add entities that appear in subject position of Freebase2M triples
        loadFreebaseEntityIndex("freebaseFiles/entitySurfaceForms.txt", entitySurfaceFormMap, entities);
        System.out.println("Map size: " + entitySurfaceFormMap.size());

        //save the index
        saveIndex("indexFiles/freebaseEntityIndex.txt", entitySurfaceFormMap);
        
        
        //preprocess the predicate files
        loadFreebasePredicateIndex("freebaseFiles/freebase_Property_Labels.tsv", predicates, predicateSurfaceFormMap);
        System.out.println("Map size: " + predicateSurfaceFormMap.size());

        //save the index
        saveIndex("indexFiles/freebasePredicateIndex.txt", predicateSurfaceFormMap);
    }

    private static Map<String, String> loadFreebaseToDBpediaMapping(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> map = new HashMap<>();

        System.out.println("Loading freebase to dbpedia map");
        try {
            map = objectMapper.readValue(new File(filePath), Map.class);
        } catch (IOException ex) {
            Logger.getLogger(Baseline.class.getName()).log(Level.SEVERE, null, ex);
        }

        Map<String, String> freebaseToDBpediaMap = new HashMap<>();

        System.out.println("Reversing freebase to dbpedia map");
        for (String key : map.keySet()) {
            String dbpediaURI = map.get(key);

            String mid = key.replace("/", ".");

            freebaseToDBpediaMap.put(dbpediaURI, mid);
        }

        return freebaseToDBpediaMap;
    }

    private static void loadDBpediaIndex(String filePath, Map<String, Map<String, Integer>> map, Map<String, String> freebaseToDBpediaMap, Set<String> validEntities) {

        System.out.println("Reading DBpedia index file");
        Set<String> content = FileUtil.readFile(filePath);

        System.out.println("Looping in dbpedia index file: " + filePath);

        for (String item : content) {
            String[] c = item.split("\t");
            String label = "";

            if (c.length == 3) {

                label = c[0].toLowerCase().trim();
                label = StringPreprocessor.preprocess(label, Language.lang.EN).replace("_", " ").trim();
                String uri = c[1];
                int freq = Integer.parseInt(c[2]);

                if (freq >= 2 && freebaseToDBpediaMap.containsKey(uri) && label.length() > 1) {

                    //replace the uri
                    String freebaseMID = freebaseToDBpediaMap.get(uri);

                    //add entities that appear in subject position of Freebase2M triples
                    if (validEntities.contains(freebaseMID)) {

                        if (map.containsKey(label)) {
                            Map<String, Integer> addedMap = map.get(label);

                            if (addedMap.containsKey(freebaseMID)) {
                                addedMap.put(freebaseMID, addedMap.get(freebaseMID) + freq);
                            } else {
                                addedMap.put(freebaseMID, freq);
                            }

                            map.put(label, addedMap);
                        } else {
                            Map<String, Integer> addedMap = new HashMap<>();
                            addedMap.put(freebaseMID, freq);

                            map.put(label, addedMap);
                        }

                    }
                }
            }
        }
    }

    private static void loadFreebaseEntityIndex(String filePath, Map<String, Map<String, Integer>> map, Set<String> validEntities) {

        System.out.println("Reading Freebase index file");
        Set<String> content = FileUtil.readFile(filePath);

        System.out.println("Looping in Freebase index file: " + filePath);

        for (String item : content) {
            String[] c = item.split("\t");

            if (c.length == 2) {

                String lemma = c[1].trim();
                String uri = "m." + c[0].trim().replace("www.freebase.com/m/", "");
                String preprocessedLemma = StringPreprocessor.preprocess(lemma, Language.lang.EN).replace("_", " ");

                if (!preprocessedLemma.equals(lemma)) {
                    lemma = preprocessedLemma.trim();
                }

                if (lemma.length() > 1) {

                    //add entities that appear in subject position of Freebase2M triples
                    if (validEntities.contains(uri)) {

                        if (map.containsKey(lemma)) {
                            Map<String, Integer> addedMap = map.get(lemma);

                            if (addedMap.containsKey(uri)) {
                                addedMap.put(uri, addedMap.get(uri) + 1);
                            } else {
                                addedMap.put(uri, 1);
                            }

                            map.put(lemma, addedMap);
                        } else {
                            Map<String, Integer> addedMap = new HashMap<>();
                            addedMap.put(uri, 1);

                            map.put(lemma, addedMap);
                        }
                    }
                }
            }
        }
    }

    private static void loadFreebasePredicateIndex(String filePath, Set<String> predicates, Map<String, Map<String, Integer>> map) {

        System.out.println("Loading freebase predicates file: " + filePath);

        Set<String> content = FileUtil.readFile(filePath);

        for (String item : content) {
//        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
//            stream.parallel().forEach(item -> {

            String[] c = item.split("\t");

            if (c.length == 4) {
                String lemma = c[3].trim();
                String uri = c[2];

                uri = uri.substring(1, uri.length()).replace("/", ".");
                int frequency = Integer.parseInt(c[1]);

                if (!predicates.contains(uri)) {
                    continue;
                }

                lemma = StringPreprocessor.preprocess(lemma, Language.lang.EN).replace("_", " ").trim();

//                    Instance instance1 = null;
                //only predicates in Freebase2M triples
//                    if (!uri.isEmpty() && lemma.length() > 1 && predicates.contains(uri)) {
                if (!uri.isEmpty() && lemma.length() > 1) {

                    if (map.containsKey(lemma)) {
                        Map<String, Integer> addedMap = map.get(lemma);

                        if (!addedMap.containsKey(uri)) {
                            addedMap.put(uri, frequency);

                            map.put(lemma, addedMap);
                        }
                    } else {
                        Map<String, Integer> addedMap = new HashMap<>();
                        addedMap.put(uri, frequency);
                        map.put(lemma, addedMap);
                    }

                    //84785248        836910  /people/person/nationality      "Country of nationality"
                    //convert such uris to labels as well, remove up to the last index of /
                    String convertedLemma = uri.substring(uri.lastIndexOf(".") + 1);
                    convertedLemma = StringPreprocessor.preprocess(convertedLemma, Language.lang.EN).replace("_", " ").trim();

                    if (!convertedLemma.equals(lemma) && convertedLemma.length() > 1) {

                        if (map.containsKey(convertedLemma)) {
                            Map<String, Integer> addedMap = map.get(convertedLemma);

                            if (!addedMap.containsKey(uri)) {
                                addedMap.put(uri, frequency);

                                map.put(convertedLemma, addedMap);
                            }
                        } else {
                            Map<String, Integer> addedMap = new HashMap<>();
                            addedMap.put(uri, frequency);
                            map.put(convertedLemma, addedMap);
                        }

                    }

                }
            }
        }
    }

    private static void saveIndex(String filePath, Map<String, Map<String, Integer>> map) {
        //save the map
        String s = "";
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("Saving the map with size: " + map.size() + " to: " + filePath);
        try {
            if (!map.isEmpty()) {
                s = objectMapper.writeValueAsString(map);

                FileUtil.writeListToFile(filePath, s, false);
            }
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

    }

}
