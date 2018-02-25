/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author sherzod
 */
public class GenerateFreebaseToDBpediaMapping {
    
    private static final String redirectsFilePath = "../dbpediaFiles/redirects_en.nt";
    private static final String freebaseDBpediaMappingFilePath = "../dbpediaFiles/freebase_links_en.nt";
    private static final String freebaseWikidataMappingFilePath = "../dbpediaFiles/fb2w.nt";
    private static final String dbpediaWikidataMappingFilePath = "../dbpediaFiles/interlanguage_links_en.ttl";
    private static final String mergedFilePath = "../freebaseFiles/freebaseToDBpediaMapping.txt";
    
    public static void main(String[] args) {
        
        Map<String, String> wikiDataToFreebaseMap = loadWikidataToFreebaseMap();
        System.out.println("WikiData to Freebase map: " + wikiDataToFreebaseMap.size());
        
        Map<String, String> wikiDataToDBpediaMap = loadWikidataToDBpediaMap();
        System.out.println("WikiData to DBpedia map: " + wikiDataToDBpediaMap.size());
        
        Map<String, String> dbpediaRedirectMap = loadDBpediaRedirectMap();
        System.out.println("DBpedia Redirects map: " + dbpediaRedirectMap.size());
        
        Map<String, String> freebaseToDBpediaMap = loadFreebaseToDBpediaMap(dbpediaRedirectMap);
        System.out.println("Freebase to DBpedia map: " + freebaseToDBpediaMap.size());

        //merge wikiData2DBpedia and wikiData2Freebase
        System.out.println("Before merging freebase2DBpedia: " + freebaseToDBpediaMap.size());
        
        for (String wikiDataURI : wikiDataToDBpediaMap.keySet()) {
            if (wikiDataToFreebaseMap.containsKey(wikiDataURI)) {
                
                String dbpediaURI = wikiDataToDBpediaMap.get(wikiDataURI);
                String freebaseURI = wikiDataToFreebaseMap.get(wikiDataURI);
                
                freebaseToDBpediaMap.put(freebaseURI, dbpediaURI);
            }
        }
        
        System.out.println("After merging freebase2DBpedia: " + freebaseToDBpediaMap.size());
        
        System.out.println("Saving the file ... ");
        
        try {
            File file = new File(mergedFilePath);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            for (String freebaseURI : freebaseToDBpediaMap.keySet()) {
                pw.println(freebaseURI + "\t" + freebaseToDBpediaMap.get(freebaseURI));
            }            
            
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private static Map<String, String> loadFreebaseLinks(String filePath) {
//
//        Map<String, String> map = new HashMap<>();
//
//        File f = new File(filePath);
//
//        if (!f.exists()) {
//
//            Map<String, String> freebaseMap = new ConcurrentHashMap<>(5000000);
//            Map<String, String> redirectLinksMap = loadRedirectLinks("/home/sherzod/NetBeansProjects/LITDProcessor/dbpediaResourceRawData/redirects_en.nt");
//
//            System.out.println("Loading links ...");
//
//            try (Stream<String> stream = Files.lines(Paths.get("/home/sherzod/NetBeansProjects/SimpleQuestions.DBpedia/freebase_links_en.nt"))) {
//                stream.parallel().forEach(item -> {
//
//                    String[] c = item.split(" ");
//
//                    String dbpediaURI = c[0];
//                    String freebaseURI = c[2];
//
//                    if (dbpediaURI.contains("http")) {
//
//                        dbpediaURI = dbpediaURI.replace("<http://dbpedia.org/resource/", "").replace(">", "");
//                        freebaseURI = freebaseURI.replace("<http://rdf.freebase.com/ns/", "").replace(">", "");
//                        freebaseURI = freebaseURI.replace(".", "/");
//
//                        if (redirectLinksMap.containsKey(dbpediaURI)) {
//                            dbpediaURI = redirectLinksMap.get(dbpediaURI);
//                        }
//
//                        //handle the redirects
////                    if (map.containsKey(freebaseURI)) {
////                        if(!map.get(freebaseURI).equals(dbpediaURI)){
////                            boolean isRedirect = redirectLinksMap.containsKey(dbpediaURI);
////                            System.out.println(freebaseURI + " " + dbpediaURI + "  "+ isRedirect);
////                        }
////                    }
//                        freebaseMap.put(freebaseURI, dbpediaURI);
//                    }
//
//                });
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            //load freebase to wikidata map
//            Map<String, String> wikidataToFreebaseMap = loadFreebaseToWikidataLinks("/home/sherzod/Downloads/fb2w.nt");
//            Map<String, String> wikidataToDBpediaMap = loadDBpediaToWikidataLinks("/home/sherzod/NetBeansProjects/SimpleQuestions.DBpedia/dbpedia/interlanguage_links_en.ttl.bz2");
//
//            System.out.println("Using mapping from wikidata->freebase & wikidata->dbpedia\n");
//            System.out.println("Map size before : " + freebaseMap.size());
//
//            for (String wikidataURI : wikidataToFreebaseMap.keySet()) {
//                if (wikidataToDBpediaMap.containsKey(wikidataURI)) {
//
//                    String freebaseURI = wikidataToFreebaseMap.get(wikidataURI);
//
//                    String dbpediaURI = wikidataToDBpediaMap.get(wikidataURI);
//
//                    //replace redirects
//                    if (redirectLinksMap.containsKey(dbpediaURI)) {
//                        dbpediaURI = redirectLinksMap.get(dbpediaURI);
//                    }
//
//                    freebaseMap.put(freebaseURI, dbpediaURI);
//                }
//            }
//
//            System.out.println("Map size after : " + freebaseMap.size());
//
//            //save the map
//            String s = "";
//            System.out.println("Saving the file size: " + freebaseMap.size());
//            try {
//                if (!freebaseMap.isEmpty()) {
//                    s = objectMapper.writeValueAsString(freebaseMap);
//
//                    FileUtil.writeStringToFile(filePath, s, false);
//                }
//            } catch (JsonProcessingException ex) {
//                ex.printStackTrace();
//            }
//        } else {
//            try {
//                map = objectMapper.readValue(f, Map.class);
//            } catch (IOException ex) {
//                Logger.getLogger(Baseline.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        return map;
//    }
    private static Map<String, String> loadDBpediaRedirectMap() {
        
        Map<String, String> map = new HashMap<>(5000000);
        
        System.out.println("Loading redirect links ...");
        
        String patternString = "<http://dbpedia.org/resource/(.*?)>\\s<http://dbpedia.org/ontology/wikiPageRedirects>\\s<http://dbpedia.org/resource/(.*?)>\\s.";
        Pattern patternLabel = Pattern.compile(patternString);
        
        try {
            FileInputStream fstream = new FileInputStream(redirectsFilePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;
            
            while ((strLine = br.readLine()) != null) {
                
                Matcher m = patternLabel.matcher(strLine);
                
                while (m.find()) {
                    String redirectURI = m.group(1).intern();
                    String pageURI = m.group(2).intern();
                    
                    map.put(redirectURI, pageURI);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }
    
    private static Map<String, String> loadWikidataToFreebaseMap() {
        
        Map<String, String> map = new HashMap<>(5000000);
        
        System.out.println("Loading wikidata to freebase links ...");
        
        String patternString = "<http://rdf.freebase.com/ns/(.*?)>\t<http://www.w3.org/2002/07/owl#sameAs>\t<http://www.wikidata.org/entity/(.*?)>\\s.";
        Pattern patternLabel = Pattern.compile(patternString);
        
        try {
            FileInputStream fstream = new FileInputStream(freebaseWikidataMappingFilePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;
            
            while ((strLine = br.readLine()) != null) {
                
                Matcher m = patternLabel.matcher(strLine);
                
                while (m.find()) {
                    String freebaseURI = m.group(1).intern();
                    String wikiDataURI = m.group(2).intern();
                    
                    map.put(wikiDataURI, freebaseURI);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }
    
    private static Map<String, String> loadWikidataToDBpediaMap() {
        
        Map<String, String> map = new HashMap<>(5000000);
        
        System.out.println("Loading wikidata to dbpedia links ...");

        //<http://dbpedia.org/resource/Wales> <http://www.w3.org/2002/07/owl#sameAs> <http://wikidata.dbpedia.org/resource/Q25> .
        String patternString = "<http://dbpedia.org/resource/(.*?)>\\s<http://www.w3.org/2002/07/owl#sameAs>\\s<http://www.wikidata.org/entity/(.*?)>\\s.";
        Pattern patternLabel = Pattern.compile(patternString);
        
        try {
            FileInputStream fstream = new FileInputStream(dbpediaWikidataMappingFilePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;
            
            while ((strLine = br.readLine()) != null) {
                
                Matcher m = patternLabel.matcher(strLine);
                
                while (m.find()) {
                    String dbpediaURI = m.group(1).intern();
                    String wikiDataURI = m.group(2).intern();
                    
                    map.put(wikiDataURI, dbpediaURI);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }
    
    private static Map<String, String> loadFreebaseToDBpediaMap(Map<String, String> redirectsMap) {
        
        Map<String, String> map = new HashMap<>(5000000);
        
        System.out.println("Loading freebase to wikidata links ...");

        //<http://dbpedia.org/resource/Dysdera_ancora> <http://www.w3.org/2002/07/owl#sameAs> <http://rdf.freebase.com/ns/m.01009ly3>
        String patternString = "<http://dbpedia.org/resource/(.*?)>\\s<http://www.w3.org/2002/07/owl#sameAs>\\s<http://rdf.freebase.com/ns/(.*?)>\\s.";
        Pattern patternLabel = Pattern.compile(patternString);
        
        try {
            FileInputStream fstream = new FileInputStream(freebaseDBpediaMappingFilePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;
            
            while ((strLine = br.readLine()) != null) {
                
                Matcher m = patternLabel.matcher(strLine);
                
                while (m.find()) {
                    String dbpediaURI = m.group(1).intern();
                    String freebaseURI = m.group(2).intern();
                    
                    if (redirectsMap.containsKey(dbpediaURI)) {
                        dbpediaURI = redirectsMap.get(dbpediaURI);
                    }
                    
                    map.put(freebaseURI, dbpediaURI);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }
}
