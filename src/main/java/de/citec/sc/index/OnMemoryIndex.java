/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.citec.sc.baseline.Baseline;
import static de.citec.sc.index.Language.lang.EN;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class OnMemoryIndex {

    private static Map<String, Map<String, Integer>> resourceIndexEN;
    private static Map<String, Map<String, Integer>> propertyIndexEN;

    public OnMemoryIndex() {

        long startTime = System.currentTimeMillis();
        System.out.println("Loading index files ...");
        //English files
        resourceIndexEN = loadIndexFile("../indexFiles/freebaseEntityIndex.txt");
        propertyIndexEN = loadIndexFile("../indexFiles/freebasePredicateIndex.txt");
        long endTime = System.currentTimeMillis();
        System.out.println("Loading finished in " + (endTime - startTime) + " ms.");

    }

    public List<Instance> getMatches(String searchTerm, int topK, Language.lang lang, boolean isEntity) {
        
        List<Instance> result = new ArrayList<>();

        Map<String, Integer> matches = new HashMap<>();

        if (isEntity) {
            switch (lang) {
                case EN:
                    if (resourceIndexEN.containsKey(searchTerm)) {
                        matches = resourceIndexEN.get(searchTerm);
                    }
                    break;
            }
        } else {
            switch (lang) {
                case EN:
                    if (propertyIndexEN.containsKey(searchTerm)) {
                        matches = propertyIndexEN.get(searchTerm);
                    }
                    break;
            }
        }

        for (String uri : matches.keySet()) {
            Instance i = new Instance("", uri, matches.get(uri));
            result.add(i);
        }

        if (!result.isEmpty()) {
            Collections.sort(result);

            if (result.size() > topK) {
                return result.subList(0, topK);
            }
        }

        return result;
    }

//    public List<Instance> getAllResources(String searchTerm, int topK, Language.lang lang) {
//
//        Map<String, Integer> matches = new HashMap<>();
//
//        switch (lang) {
//            case EN:
//                if (resourceIndexEN.containsKey(searchTerm)) {
//                    matches = resourceIndexEN.get(searchTerm);
//                }
//                break;
//        }
//
//        List<Instance> result = new ArrayList<>();
//
//        if (!matches.isEmpty()) {
//
//            for (Object i : matches) {
//                Map<String, Object> map = (Map<String, Object>) i;
//
//                Instance instance = new Instance(map.get("uri").toString(), (int) map.get("frequency"));
//
//                result.add(instance);
//            }
//            //sort by frequency
//            Collections.sort(result);
//
//            if (result.size() > topK) {
//                return result.subList(0, topK);
//            }
//        }
//
//        return result;
//    }
//
//    public List<Instance> getAllPredicates(String searchTerm, int topK, Language.lang lang) {
//
//        Set<Instance> matches = new HashSet<>();
//
//        switch (lang) {
//            case EN:
//                if (propertyIndexEN.containsKey(searchTerm)) {
//                    matches.addAll(propertyIndexEN.get(searchTerm));
//                }
//                break;
//        }
//
//        List<Instance> result = new ArrayList<>();
//
//        if (!matches.isEmpty()) {
//
//            for (Object i : matches) {
//                Map<String, Object> map = (Map<String, Object>) i;
//
//                Instance instance = new Instance(map.get("uri").toString(), (int) map.get("frequency"));
//
//                result.add(instance);
//            }
//            //sort by frequency
//            Collections.sort(result);
//
//            if (result.size() > topK) {
//                return result.subList(0, topK);
//            }
//
////            for (Instance i : matches) {
////
////                Instance instance1 = new Instance(i.getUri(), i.getFrequency());
////                result.add(instance1);
////
////                if (result.size() == topK) {
////                    break;
////                }
////            }
//        }
//
//        return result;
//    }

    private Map<String, Map<String, Integer>> loadIndexFile(String filePath) {

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Map<String, Integer>> map = new HashMap<>();
        try {
            map = objectMapper.readValue(new File(filePath), Map.class);
        } catch (IOException ex) {
            Logger.getLogger(Baseline.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Loaded map with size: " + map.size() + " from: " + filePath);

        return map;
    }

    private void loadFiles(String filePath, ConcurrentHashMap<String, List<Instance>> map) {

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");
                Instance instance = null;
                String label = "";

                if (c.length == 3) {

                    label = c[0].toLowerCase();
                    String uri = c[1];
                    int freq = Integer.parseInt(c[2]);

//                                if(freq >= 2){
                    instance = new Instance("", uri, freq);
//                                }
                }

                if (instance != null) {
                    if (map.containsKey(label)) {

                        List<Instance> instances = map.get(label);
                        List<Instance> newInstances = new ArrayList<>();

                        if (!instances.contains(instance)) {
                            newInstances.add(instance);
                            newInstances.addAll(instances);
                        } else {
                            //update frequency by adding the new freq to the prev. one
                            for (Instance i : instances) {
                                if (i.equals(instance)) {
                                    //update freq.
                                    instance.setFrequency(i.getFrequency() + instance.getFrequency());

                                    newInstances.add(instance);
                                } else {
                                    newInstances.add(i);
                                }
                            }
                        }

                        map.put(label, newInstances);

                    } else {

                        List<Instance> instances = new ArrayList<>();

                        instances.add(instance);

                        map.put(label, instances);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Map size: " + map.size());
    }

}
