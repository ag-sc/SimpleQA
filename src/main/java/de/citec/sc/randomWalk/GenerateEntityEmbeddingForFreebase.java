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
public class GenerateEntityEmbeddingForFreebase {

//    private static final String sequenceDir = "freebaseSequenceFiles";
//    private static final String freebaseTriplePath = "freebaseFiles/dummy.txt";
    private static final String sequenceDir = "../freebaseEmbeddingFiles";
    private static final String freebasePreprocessedTriplePath = "../freebaseFiles/freebasePreprocessedDump.txt";
    private static final String freebaseRawTriplePath = "../freebaseFiles/freebase_preprocessed.txt";
    private static final int sequenceSize = 10;
    private static final int maxIterations = 1000;
    private static final double alpha = 0.15d;

    public static void main(String[] args) throws FileNotFoundException {

        //delete old content
        FileUtil.deleteFolderWithContent(sequenceDir);

        File dir = new File(sequenceDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        System.out.println("Loading freebase file ...");

//        generateEmbeddingFilesFromPreprocessed(freebasePreprocessedTriplePath);
        generateEmbeddingFilesFromRaw(freebaseRawTriplePath);

    }

    /**
     * load the freebase index into memory
     */
    private static void generateEmbeddingFilesFromPreprocessed(String filePath) {

        long startTime = System.currentTimeMillis();

        float maxNumberOfLines = 338552952f;
        //the line numbers of the file
        final AtomicInteger lineCount = new AtomicInteger(0);

        List<String> outputs = new CopyOnWriteArrayList<String>();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(line -> {

                if (line.length() > 4) {

                    String[] data = line.split("\t");

                    if (data.length == 3) {
                        Integer subjectID = Integer.parseInt(data[0]);
                        Integer predicateID = Integer.parseInt(data[1]);
                        Integer objectID = Integer.parseInt(data[2]);

                        String l1 = "__label__Subj_E" + subjectID + " Subj_P" + predicateID + " Obj_E" + objectID;
                        String l2 = "__label__Obj_E" + objectID + " Obj_P" + predicateID + " Subj_E" + subjectID;

                        outputs.add(l1);
                        outputs.add(l2);

                        if (lineCount.incrementAndGet() % 10000 == 0) {

                            synchronized (outputs) {
                                FileUtil.writeListToFile(sequenceDir + "/train.txt", outputs, true);
                                outputs.clear();
                            }

                            if (lineCount.get() % 1000000 == 0) {
                                double s = lineCount.get() / (double) maxNumberOfLines;
                                System.out.println("Loading done = " + s);
                            }
                        }
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        System.out.printf("Generated in %s ms", (endTime - startTime));
        System.out.println("");
    }

    private static void generateEmbeddingFilesFromRaw(String filePath) {

        long startTime = System.currentTimeMillis();

        List<String> outputs = new ArrayList<String>();

//        String patternString = "<http://dbpedia.org/resource/(.*?)>\\s<http://dbpedia.org/ontology/(.*?)>\\s<http://dbpedia.org/resource/(.*?)>\\s.";
//        Pattern patternLabel = Pattern.compile(patternString);

        int maxLineNumber = 338586276;
        int count = 0;

        try {
            FileInputStream fstream = new FileInputStream(filePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;

            while ((strLine = br.readLine()) != null) {

                String[] data = strLine.split("\t");

                count++;

                String subject = data[0].intern();
                String predicate = data[1].intern();
                String object = data[2].intern();

                String l1 = "__label__Subj_" + subject + " Subj_P_" + predicate + " Obj_" + object;
                String l2 = "__label__Obj_" + object + " Obj_P_" + predicate + " Subj_" + subject;

                outputs.add(l1);
                outputs.add(l2);

                if (outputs.size() >= 10000) {
                    FileUtil.writeListToFile(sequenceDir + "/train.txt", outputs, true);
                    outputs.clear();
                }

                if (count % 18000000 == 0) {
                    double s = count / (double) maxLineNumber;
                    System.out.println("Done: " + s);
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
