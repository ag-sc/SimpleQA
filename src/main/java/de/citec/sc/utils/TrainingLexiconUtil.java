/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.citec.sc.variable.State;
import de.citec.sc.variable.URIVariable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class TrainingLexiconUtil {

    private static Map<String, Map<Integer, Set<String>>> map;
    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param lemma
     * @param dudeID
     * @return Set<String> uris
     *
     * returns uris that are contained in the map
     *
     */
    public static Set<String> getURIs(String lemma, Integer dudeID) {

        if (map == null) {
            loadCache();
        }

        Set<String> uris = new HashSet<>();

        if (map.containsKey(lemma)) {
            Map<Integer, Set<String>> dudeURIMap = map.get(lemma);

            if (dudeURIMap.containsKey(dudeID)) {
                uris = dudeURIMap.get(dudeID);
            }
        }

        return uris;
    }

    public static void indexState(State state) {
        for (Integer tokenID : state.getUriVariables().keySet()) {
            URIVariable var = state.getUriVariables().get(tokenID);
            String token = state.getDocument().getDependencyParse().getToken(tokenID);

            addToCache(token.toLowerCase(), var.getDudeId(), var.getUri());
        }
    }

    public static void addToCache(String lemma, Integer dudeID, String uri) {

        if (map == null) {
            loadCache();
        }

        if (map.containsKey(lemma)) {

            Map<Integer, Set<String>> dudeMap = map.get(lemma);

            if (dudeMap.containsKey(dudeID)) {
                Set<String> uris = dudeMap.get(dudeID);
                uris.add(uri);

                dudeMap.put(dudeID, uris);
            } else {
                Set<String> uris = new HashSet<>();
                uris.add(uri);
                dudeMap.put(dudeID, uris);
            }

            map.put(lemma, dudeMap);
        } else {
            Map<Integer, Set<String>> dudeMap = new HashMap<>();

            Set<String> uris = new HashSet<>();
            uris.add(uri);
            dudeMap.put(dudeID, uris);

            map.put(lemma, dudeMap);
        }

        writeCache();
    }

    private static void writeCache() {

        String s = "";

        try {
            if (map != null) {
                s = objectMapper.writeValueAsString(map);

                FileUtil.writeStringToFile("trainingLexicon-cache.txt", s, false);
            }
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
    }

    private static void loadCache() {

        try {

            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }

            map = new HashMap<>();

            File f = new File("trainingLexicon-cache.txt");
            if (f.exists()) {

                Map<String, Object> loadedMap = objectMapper.readValue(f, Map.class);

                for (String lemma : loadedMap.keySet()) {

                    Map<String, Object> dMap = (Map<String, Object>) loadedMap.get(lemma);

                    Map<Integer, Set<String>> dudeMap = new HashMap<>();

                    for (String dudeIDAsString : dMap.keySet()) {
                        Object o1 = dMap.get(dudeIDAsString);
                        List<String> list = (List<String>) o1;

                        Integer dudeID = Integer.parseInt(dudeIDAsString);
                        Set<String> uris = new HashSet<>(list);
                        dudeMap.put(dudeID, uris);
                    }

                    map.put(lemma, dudeMap);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
