/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class PreprocessFreebaseDump {

//    private static final String freebaseOutputFilePath = "freebaseFiles/freebasePreprocessedDump.txt";
//    private static final String freebaseEntitiesOutputFilePath = "freebaseFiles/freebaseEntities.txt";
//    private static final String freebasePredicatesOutputFilePath = "freebaseFiles/freebasePredicates.txt";
    private static final String freebaseOutputFilePath = "../freebaseFiles/freebasePreprocessedDump.txt";
    private static final String freebaseEntitiesOutputFilePath = "../freebaseFiles/freebaseEntities.txt";
    private static final String freebasePredicatesOutputFilePath = "../freebaseFiles/freebasePredicates.txt";

    public static void main(String[] args) {

//        test();
////        
//        System.exit(0);


        File f = new File(freebaseOutputFilePath);
        if (f.exists()) {
            f.delete();
        }

        
        extractValidTriples("../freebaseFiles/preprocessed.txt");
//        extractValidTriples("freebaseDump");
//
    }

    private static void extractValidTriples(String filePath) {

        System.out.println("Read freebase dump to extract valid triples ... ");

        Map<String, Integer> mapOfEntities = new HashMap<>();
        Map<String, Integer> mapOfPredicates = new HashMap<>();

        String patternString = "<http://rdf.freebase.com/ns/(m\\..*?)>\t<http://rdf.freebase.com/ns/(.*?)>\t<http://rdf.freebase.com/ns/(m\\..*?)>\t.";
        Pattern patternLabel = Pattern.compile(patternString);

//        Set<String> processedTriples = new ConcurrentHashSet<>();
        final StringBuffer output = new StringBuffer("");

        float maxNumberOfLines = 3130753066f;//the line numbers of the file
        final AtomicInteger lineCount = new AtomicInteger(0);

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.forEach(item -> {

                if (lineCount.incrementAndGet() % 10000000 == 0) {//3130753066
                    double s = lineCount.get() / (double) maxNumberOfLines;
//                    System.out.println("Done = " + s + " size:" + processedTriples.size());
                }

                Matcher m = patternLabel.matcher(item);

                while (m.find()) {
                    String subject = m.group(1).intern();
                    String predicate = m.group(2).intern();
                    String object = m.group(3).intern();

                    //add the predicate
//                    synchronized (mapOfPredicates) {
                    if (!mapOfPredicates.containsKey(predicate)) {
                        mapOfPredicates.put(predicate, mapOfPredicates.size());
                    }
//                    }

                    //add the subject
//                    synchronized (mapOfEntities) {
                    if (!mapOfEntities.containsKey(subject)) {
                        mapOfEntities.put(subject, mapOfEntities.size());
                    }
//                    }



                    //add the object
//                    synchronized (mapOfEntities) {
                    if (!mapOfEntities.containsKey(object)) {
                        mapOfEntities.put(object, mapOfEntities.size());
                    }
//                    }

                    String preprocessedTriple = mapOfEntities.get(subject) + "\t" + mapOfPredicates.get(predicate) + "\t" + mapOfEntities.get(object);
                    output.append(preprocessedTriple).append("\n");

                    if (lineCount.incrementAndGet() % 10000 == 0) {
                        FileUtil.writeStringBufferToFile(freebaseOutputFilePath, output, true);
                        //clear the buffer
                        output.setLength(0);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Saving maps into files");
        //save the remaining
        FileUtil.writeStringBufferToFile(freebaseOutputFilePath, output, true);

        //save entities and predicates
        System.out.println("Entity size:  " + mapOfEntities.size());
        writeMapToFile(mapOfEntities, freebaseEntitiesOutputFilePath);

        mapOfEntities.clear();

        System.out.println("Predicate size:  " + mapOfPredicates.size());
        writeMapToFile(mapOfPredicates, freebasePredicatesOutputFilePath);

    }

    private static void writeMapToFile(Map<String, Integer> map, String filePath) {

        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }

        AtomicInteger countOfKeys = new AtomicInteger();

        final StringBuffer output = new StringBuffer("");

        map.keySet().stream().forEach(key -> {

            output.append("\n" + key + "\t" + map.get(key));
            //write every 10K lines
            if (countOfKeys.incrementAndGet() % 10000 == 0) {

                FileUtil.writeStringBufferToFile(filePath, output, true);
                output.setLength(0);
            }
        });

        FileUtil.writeStringBufferToFile(filePath, output, true);
    }

    private static void test() {
        Set<String> c = FileUtil.readFile(freebaseEntitiesOutputFilePath);
        
        for(String c1 : c){
            String[] da = c1.split("\t");
            if(da[1].equals("0")){
                System.out.println(c1);
            }
        }

        System.out.println(c.size());

    }
}
