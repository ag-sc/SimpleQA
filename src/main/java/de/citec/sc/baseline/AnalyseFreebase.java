/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class AnalyseFreebase {

    public static void main(String[] args) {

        extractSurfaceForms("../freebase-rdf-latest");
//        extractSurfaceForms("subset.txt");
    }

    private static void extractSurfaceForms(String filePath) {

        System.out.println("Read freebase dump to extract surface forms ... ");

        String patternString = "<http://rdf.freebase.com/ns/m.02mjmr>\t(.*?)\t(.*?)\t.";
        Pattern patternLabel = Pattern.compile(patternString);

        Set<String> result = new ConcurrentHashSet<>();

        final AtomicInteger count = new AtomicInteger(0);

        float maxNumberOfLines = 3130753066f;//the line numbers of the file

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                if (count.incrementAndGet() % 10000000 == 0) {//3130753066

                    FileUtil.writeSetToFile("obamaPredicates.txt", result, true);
                    double s = count.get() / (double) maxNumberOfLines;

                    System.out.println("Done = " + s);
                }

                Matcher m = patternLabel.matcher(item);

                while (m.find()) {
                    String property = m.group(1);
                    String object = m.group(2);

                    if (!object.startsWith("<http://rdf.freebase")) {
                        result.add(property + "\t" + object);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        FileUtil.writeSetToFile("obamaPredicates.txt", result, false);
    }
}
