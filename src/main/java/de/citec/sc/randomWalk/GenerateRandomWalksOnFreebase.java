/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.randomWalk;

import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class GenerateRandomWalksOnFreebase {

    private static Map<Integer, List<Pair>> freebaseMap;
//    private static final String sequenceDir = "freebaseSequenceFiles";
//    private static final String freebaseTriplePath = "freebaseFiles/dummy.txt";
    private static final String sequenceDir = "../freebaseSequenceFiles";
    private static final String freebaseTriplePath = "../freebaseFiles/freebasePreprocessedDump.txt";
    private static final int sequenceSize = 10;
    private static final int maxIterations = 1000;
    private static final double alpha = 0.15d;

    public static void main(String[] args) {

        //delete old content
        FileUtil.deleteFolderWithContent(sequenceDir);
        
        File dir = new File(sequenceDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        System.out.println("Loading freebase file ...");
        freebaseMap = loadData(freebaseTriplePath);

        RandomWalkSequence sequenceGenerator = new RandomWalkSequence(maxIterations, sequenceSize);

        final AtomicInteger lineCount = new AtomicInteger(0);

        System.out.println("Generating sequences for each node with #node: "+freebaseMap.size());
        
        StringBuffer output = new StringBuffer();

        freebaseMap.keySet().stream().forEach(nodeID -> {

            StringBuffer sequence = sequenceGenerator.generateRandomSequence(nodeID, freebaseMap);
            output.append(sequence);
            

            if (lineCount.incrementAndGet() % 10000 == 0) {
                double s = lineCount.get() / (double) freebaseMap.size();
                
                //write to file
                FileUtil.writeStringBufferToFile(sequenceDir + "/" + lineCount.get() + ".txt", output, false);
                output.setLength(0);
                System.out.println("Done = " + s);
            }

        });
        
        FileUtil.writeStringBufferToFile(sequenceDir + "/remaining.txt", output, false);

    }

    /**
     * load the freebase index into memory
     */
    private static Map<Integer, List<Pair>> loadData(String filePath) {
        Map<Integer, List<Pair>> map = new ConcurrentHashMap<>(50000000);

        long startTime = System.currentTimeMillis();

        float maxNumberOfLines = 338552952f;
        //the line numbers of the file
        final AtomicInteger lineCount = new AtomicInteger(0);

        StringBuffer readLine = new StringBuffer();

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.forEach(line -> {

                readLine.append(line);

                if (lineCount.incrementAndGet() % 40000000 == 0) {//3130753066
                    double s = lineCount.get() / (double) maxNumberOfLines;
                    System.out.println("Loading done = " + s);
                }

                String[] data = line.split("\t");
                if (data.length == 3) {
                    Integer subjectID = Integer.parseInt(data[0]);
                    Integer predicateID = Integer.parseInt(data[1]);
                    Integer objectID = Integer.parseInt(data[2]);

                    synchronized (map) {
                        map.putIfAbsent(subjectID, new ArrayList<>());
                    }

                    Pair pair = new Pair(predicateID, objectID);
                    map.get(subjectID).add(pair);
                }

            });

        } catch (IOException e) {
            e.printStackTrace();

            System.out.println(readLine);
            System.exit(0);
        }

        long endTime = System.currentTimeMillis();

        System.out.printf("Loaded in %s ms", (endTime - startTime));
        System.out.println("");

        return map;
    }
}
