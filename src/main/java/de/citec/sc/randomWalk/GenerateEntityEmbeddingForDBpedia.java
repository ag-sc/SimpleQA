/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import de.citec.sc.utils.FileUtil;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class GenerateEntityEmbeddingForDBpedia {

//    private static final String sequenceDir = "dbpediaEmbeddingFiles";
//    private static final String freebaseRawTriplePath = "mappingbased_objects_en.ttl";
    private static final String sequenceDir = "../dbpediaEmbeddingFiles";
    private static final String freebaseRawTriplePath = "../dbpediaFiles/mappingbased_objects_en.ttl";
    

    public static void main(String[] args) throws FileNotFoundException {

        //delete old content
        FileUtil.deleteFolderWithContent(sequenceDir);

        File dir = new File(sequenceDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        System.out.println("Loading dbpedia object properties file ...");

//        generateEmbeddingFilesFromPreprocessed(freebasePreprocessedTriplePath);
        generateEmbeddingFilesFromRaw(freebaseRawTriplePath);

    }

    private static void generateEmbeddingFilesFromRaw(String filePath) {

        long startTime = System.currentTimeMillis();

        List<String> outputs = new ArrayList<String>();

        String patternString = "<http://dbpedia.org/resource/(.*?)>\\s<http://dbpedia.org/ontology/(.*?)>\\s<http://dbpedia.org/resource/(.*?)>\\s.";
        Pattern patternLabel = Pattern.compile(patternString);

        int maxLineNumber = 18746176;
        int count = 0;

        try {
            FileInputStream fstream = new FileInputStream(filePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;

            while ((strLine = br.readLine()) != null) {

                Matcher m = patternLabel.matcher(strLine);

                while (m.find()) {

                    count++;

                    String subject = m.group(1).intern();
                    String predicate = m.group(2).intern();
                    String object = m.group(3).intern();

                    String l1 = "__label__Subj_" + subject + " Subj_P_" + predicate + " Obj_" + object;
                    String l2 = "__label__Obj_" + object + " Obj_P_" + predicate + " Subj_" + subject;

                    outputs.add(l1);
                    outputs.add(l2);

                    if (outputs.size() >= 10000) {
                        FileUtil.writeListToFile(sequenceDir + "/train.txt", outputs, true);
                        outputs.clear();
                    }

                    if (count % 1800000 == 0) {
                        double s = count / (double) maxLineNumber;
                        System.out.println("Done: " + s);
                    }
                }
            }
            fstream.close();
            in.close();
            br.close();

            FileUtil.writeListToFile(sequenceDir + "/train.txt", outputs, true);

        } catch (Exception e) {
            System.err.println("Error reading the file: " + filePath + "\n" + e.getMessage());
        }

        long endTime = System.currentTimeMillis();

        System.out.printf("Generated in %s ms", (endTime - startTime));
        System.out.println("");
    }
}
