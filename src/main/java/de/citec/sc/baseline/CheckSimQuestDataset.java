/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sherzod
 */
public class CheckSimQuestDataset {

    public static void main(String[] args) {
        Map<String, String> freebaseToDBpediaMap = loadFreebaseToDBpediaMapping("freebaseFiles/freebaseToDBpediaLinks.txt");

        Set<String> testContent = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_train.txt");

        //loop over all training instances and extract uris with their features
        int count = 0;
        int correctCount = 0;
        for (String s : testContent) {

            count++;

            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", " ?").replace(".", " .").replace("'s", " 's").trim();
            String subject = "m." + c[0].replace("www.freebase.com/m/", "");
            String object = "m." + c[2].replace("www.freebase.com/m/", "");
            String predicate = c[1].replace("www.freebase.com/m/", "");

            boolean replaced = false;
            if (freebaseToDBpediaMap.containsKey(subject)) {
                subject = freebaseToDBpediaMap.get(subject);
                replaced = true;
            }
            if (freebaseToDBpediaMap.containsKey(object)) {
                object = freebaseToDBpediaMap.get(object);
                replaced = true;
            }

            if (replaced) {
                System.out.println(text + "\n" + subject + "  " + predicate + "   " + object + "\n\n");
            }
        }
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

            freebaseToDBpediaMap.put(mid, dbpediaURI);
        }

        return freebaseToDBpediaMap;
    }
}
