/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class GenerateRandomWalksOnFreebase {

    private static Map<Integer, Map<Integer, List<Integer>>> freebaseMap;
//    private static final String sequenceDir = "freebaseSequenceFiles";
//    private static final String freebaseTriplePath = "freebaseFiles/dummy.txt";
    private static final String sequenceDir = "../freebaseSequenceFiles";
    private static final String freebaseTriplePath = "../freebaseFiles/freebasePreprocessedDump.txt";
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

        //load the map
        freebaseMap = loadMemoryEfficientData(freebaseTriplePath);

        RandomWalkSequence sequenceGenerator = new RandomWalkSequence(maxIterations, sequenceSize, new Random(10l));

        final AtomicInteger lineCount = new AtomicInteger(0);

        System.out.println("Generating sequences for each node with #node: " + freebaseMap.size());

        PrintStream printStream = new PrintStream(sequenceDir + "/processedNodes.txt");

        //thread safe List
//        List<String> outputs = Collections.synchronizedList(new ArrayList<String>());
        List<String> outputs = new CopyOnWriteArrayList<String>();

        freebaseMap.keySet().stream().parallel().forEach(nodeID -> {

            //entities with triples more than 10
            if (freebaseMap.get(nodeID).size() >= 10) {

                //loop over each entity and generate sequences
                //save every 10K entities into single file
                String sequences = sequenceGenerator.generateRandomSequences(nodeID, freebaseMap);

                if (!sequences.isEmpty()) {
                    outputs.add(sequences);
                }
                printStream.println(nodeID);

                //write to file
                //            output.addAll(sequences);
                if (lineCount.incrementAndGet() % 10000 == 0) {

                    synchronized (outputs) {
                        FileUtil.writeListToFile(sequenceDir + "/sequences.txt", outputs, true);
                        outputs.clear();
                    }

                    if (lineCount.get() % 1000000 == 0) {
                        double s = lineCount.get() / (double) freebaseMap.size();
                        System.out.println("Done = " + s);
                    }
                }
                //progress
            }

        });

        printStream.close();

//        //save the remaining
//        FileUtil.writeListToFile(sequenceDir + "/" + (lineCount.get() + 1) + ".txt", output, false);
    }

    /**
     * load the freebase index into memory
     */
    private static Map<Integer, Map<Integer, List<Integer>>> loadMemoryEfficientData(String filePath) {

        Map<Integer, Map<Integer, List<Integer>>> map = new HashMap<>(79000000);

        long startTime = System.currentTimeMillis();

        float maxNumberOfLines = 338552952f;
        //the line numbers of the file
        final AtomicInteger lineCount = new AtomicInteger(0);

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.forEach(line -> {

                if (lineCount.incrementAndGet() % 40000000 == 0) {//3130753066
                    double s = lineCount.get() / (double) maxNumberOfLines;
                    System.out.println("Loading done = " + s);
                }

                if (line.length() > 4) {

                    String[] data = line.split("\t");

                    if (data.length == 3) {
                        Integer subjectID = Integer.parseInt(data[0]);
                        Integer predicateID = Integer.parseInt(data[1]);
                        Integer objectID = Integer.parseInt(data[2]);

//                    synchronized (map) {
                        map.putIfAbsent(subjectID, new HashMap<>());
//                    }

                        //get added elements
                        Map<Integer, List<Integer>> predicateMap = map.get(subjectID);

                        predicateMap.putIfAbsent(predicateID, new ArrayList<>());
                        predicateMap.get(predicateID).add(objectID);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        System.out.printf("Loaded in %s ms", (endTime - startTime));
        System.out.println("");

        return map;
    }
}
