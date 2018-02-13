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
public class ExtractSurfaceForm {

    public static void main(String[] args) {

        //read all triples from freebaseSubset2million dataset
        Set<String> entities = new ConcurrentHashSet<>();
        Set<String> predicates = new ConcurrentHashSet<>();

        System.out.println("Reading Freebase2M file into memory ... ");
        String filePath = "freebase-FB2M.txt";
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
                String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");
                String[] objects = c[2].replace("www.freebase.com/", "").replace("/", ".").split("\\s");

                entities.add(subject);
                predicates.add(predicate);
                
                for (String object : objects) {
                    entities.add(object);
                }

                

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Entities: "+entities.size() + " Predicates: "+predicates.size());
        extractSurfaceForms("../freebase-rdf-latest", entities, predicates);
//        loadFreebaseToWikidataLinks("subset.txt");
    }

    private static void extractSurfaceForms(String filePath, Set<String> entities, Set<String> predicates) {

        System.out.println("Read freebase dump to extract surface forms ... ");

        //delete previous surface form file
        File surfaceFromFile = new File("entitySurfaceForms.txt");
        if (surfaceFromFile.exists()) {
            surfaceFromFile.delete();
        }
        surfaceFromFile = new File("predicateSurfaceForms.txt");
        if (surfaceFromFile.exists()) {
            surfaceFromFile.delete();
        }

        String patternString = "<http://rdf.freebase.com/ns/(.*?)>\t<http://rdf.freebase.com/ns/type.object.name>\t\"(.*?)\"@en\t.";
        Pattern patternLabel = Pattern.compile(patternString);

        Set<String> entitySurfaceForms = new ConcurrentHashSet<>();
        Set<String> predicateSurfaceForms = new ConcurrentHashSet<>();

        final AtomicInteger count = new AtomicInteger(0);

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.split("\t");

                if (count.incrementAndGet() % 100000 == 0) {
                    //save 10K results

                    FileUtil.writeListToFile("entitySurfaceForms.txt", entitySurfaceForms, true);
                    entitySurfaceForms.clear();

                    FileUtil.writeListToFile("predicateSurfaceForms.txt", predicateSurfaceForms, true);
                    predicateSurfaceForms.clear();

                    System.out.println("count = " + count.get());

                }
                Matcher m = patternLabel.matcher(item);

                while (m.find()) {
                    String uri = m.group(1);
                    String label = m.group(2);

                    //take only those that appear in Freebase2M dataset
                    if (entities.contains(uri)) {

                        try {
                            label = StringEscapeUtils.unescapeJava(label);
                            label = URLDecoder.decode(label, "UTF-8");

                            label = label.toLowerCase().trim();
                        } catch (Exception e) {

                        }

                        uri = "www.freebase.com/" + uri.replace(".", "/");

                        entitySurfaceForms.add(uri + "\t" + label);
                    }
                    else if (predicates.contains(uri)) {

                        try {
                            label = StringEscapeUtils.unescapeJava(label);
                            label = URLDecoder.decode(label, "UTF-8");

                            label = label.toLowerCase().trim();
                        } catch (Exception e) {

                        }

                        uri = "www.freebase.com/" + uri.replace(".", "/");

                        predicateSurfaceForms.add(uri + "\t" + label);
                    }

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        FileUtil.writeListToFile("entitySurfaceForms.txt", entitySurfaceForms, true);
        FileUtil.writeListToFile("predicateSurfaceForms.txt", predicateSurfaceForms, true);
        

    }
}
