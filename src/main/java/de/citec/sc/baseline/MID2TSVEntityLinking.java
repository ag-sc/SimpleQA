/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.StringPreprocessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class MID2TSVEntityLinking {

    public static void main(String[] args) {
        String filePath = "mid2name.tsv";

       Set<String> setOfURIs = loadURIs(filePath);
       
        System.out.println("URIs: "+ setOfURIs);

        Set<String> trainContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");
        
        

        int count = 0;
        for (String s : trainContent) {
            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = c[1];
            String object = c[2];
            
            if(setOfURIs.contains(subject)){
                count ++;
            }
        }
        
        double score = count/ (double) trainContent.size();
        
        System.out.println(score);
    }

    private static Map<String, Set<String>> loadIndex(String filePath) {
        Map<String, Set<String>> map = new ConcurrentHashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 2) {
                    String lemma = c[1].trim();
                    String uri = c[0].trim();

                    if (!uri.isEmpty() && lemma.length() > 1) {

                        String preprocessedLemma = StringPreprocessor.preprocess(lemma, Language.lang.EN);

                        if (map.containsKey(lemma)) {
                            Set<String> uris = map.get(lemma);
                            uris.add(uri);

                            map.put(lemma, uris);
                        } else {
                            Set<String> uris = new HashSet<>();
                            uris.add(uri);

                            map.put(lemma, uris);
                        }

                        //non-empty and different than lemma
                        if (preprocessedLemma.length() > 1 && !preprocessedLemma.equals(lemma)) {
                            //add preprocessed lemma as well
                            if (map.containsKey(preprocessedLemma)) {
                                Set<String> uris = map.get(preprocessedLemma);

                                uris.add(uri);

                                map.put(preprocessedLemma, uris);
                            } else {
                                Set<String> uris = new HashSet<>();
                                uris.add(uri);

                                map.put(preprocessedLemma, uris);
                            }
                        }
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    private static Set<String> loadURIs(String filePath) {
        Set<String> set = new ConcurrentHashSet<>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (c.length == 2) {
                    String freebaseID = c[0];
                    String dbpediaLabel = c[1];
                    
                    freebaseID = freebaseID.substring(1, freebaseID.length()).replace("/", ".");
                    
                    set.add(freebaseID);

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return set;
    }
}
