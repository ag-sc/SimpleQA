/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.utils.FileUtil;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author sherzod
 */
public class ExtractSurfaceForm {

    private static final String entitySurfaceFormPath = "../freebaseFiles/entitySurfaceFormsNew.txt";
    private static final String predicateSurfaceFormPath = "../freebaseFiles/predicateSurfaceFormsNew.txt";
    private static final String freebaseTriplesPath = "../freebaseFiles/freebase-rdf-latest.gz";
    private static final String freebase2MPath = "../freebaseFiles/freebase-FB2M.txt";

    public static void main(String[] args) {

        //read all triples from freebaseSubset2million dataset
        Set<String> entities = new ConcurrentHashSet<>();
        Set<String> predicates = new ConcurrentHashSet<>();

        System.out.println("Reading Freebase2M file into memory ... ");
        try {
            FileInputStream fstream = new FileInputStream(freebase2MPath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String strLine;

            while ((strLine = br.readLine()) != null) {

                String[] c = strLine.split("\t");

                String subject = c[0].replace("www.freebase.com/", "").replace("/", ".");
                String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

                entities.add(subject);
                predicates.add(predicate);

            }
        } catch (Exception e) {
        }

        System.out.println("Entities: " + entities.size() + " Predicates: " + predicates.size());

        extractSurfaceForms(freebaseTriplesPath, entities, predicates);
    }

    private static void extractSurfaceForms(String filePath, Set<String> entities, Set<String> predicates) {

        System.out.println("Processing " + filePath);

        File fileTobeRead = new File(filePath);

        String patternString = "<http://rdf.freebase.com/ns/(.*?)>\t<http://rdf.freebase.com/ns/type.object.name>\t\"(.*?)\"@en\t.";
        Pattern patternLabel = Pattern.compile(patternString);

        List<String> entitySurfaceForms = new ArrayList<>();
        List<String> predicateSurfaceForms = new ArrayList<>();

        File f1 = new File(entitySurfaceFormPath);
        File f2 = new File(predicateSurfaceFormPath);

        if (f1.exists()) {
            f1.delete();
        }
        if (f2.exists()) {
            f2.delete();
        }

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileTobeRead))));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {

                Matcher m = patternLabel.matcher(line);

                while (m.find()) {
                    String uri = m.group(1);
                    String label = m.group(2).replace("\n", "").replace("\t", " ").trim();

                    if (entities.contains(uri)) {
                        entitySurfaceForms.add(uri + "\t" + label);
                    } else if (predicates.contains(uri)) {
                        predicateSurfaceForms.add(uri + "\t" + label);
                    }

                    if (entitySurfaceForms.size() >= 10000) {
                        FileUtil.writeListToFile(entitySurfaceFormPath, entitySurfaceForms, true);
                        entitySurfaceForms.clear();
                    }
                    if (predicateSurfaceForms.size() >= 10000) {
                        FileUtil.writeListToFile(predicateSurfaceFormPath, predicateSurfaceForms, true);
                        predicateSurfaceForms.clear();
                    }
                }
            }

            FileUtil.writeListToFile(entitySurfaceFormPath, entitySurfaceForms, true);
            FileUtil.writeListToFile(predicateSurfaceFormPath, predicateSurfaceForms, true);

        } catch (IOException ex) {
            Logger.getLogger(ExtractSurfaceForm.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void extractSurfaceForms2(String filePath, Set<String> entities, Set<String> predicates) {

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

                    FileUtil.writeSetToFile("entitySurfaceForms.txt", entitySurfaceForms, true);
                    entitySurfaceForms.clear();

                    FileUtil.writeSetToFile("predicateSurfaceForms.txt", predicateSurfaceForms, true);
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
                    } else if (predicates.contains(uri)) {

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

        FileUtil.writeSetToFile("entitySurfaceForms.txt", entitySurfaceForms, true);
        FileUtil.writeSetToFile("predicateSurfaceForms.txt", predicateSurfaceForms, true);

    }
}
