/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.index.Instance;
import de.citec.sc.utils.FileUtil;
import de.citec.sc.utils.KnowledgeBaseLinker;
import java.util.List;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class CreateIBODataset {
    
    private static final int entityMaxNGramSize = 6;
    private static final int predicateMaxNGramSize = 4;
    private static final int topK = 10;
    
    public static void main(String[] args) {
        
    }
    
    private static void preprocess(String filePath){
        
        Set<String> dataPoints = FileUtil.readFile("src/main/resources/simplequestions/annotated_fb_data_" + filePath + ".txt");

        int count = 0;
        int correctCount = 0;
        int correctEntityCount = 0;
        int correctPredicateCount = 0;

        for (String s : dataPoints) {

            count++;

//            if (count == 10000) {
//                break;
//            }

            if (count % 1000 == 0) {
                double progr = count / (double) dataPoints.size();
                String strDouble = String.format("%.4f", progr);
                System.out.println(filePath + ": " + strDouble);
            }

            String[] c = s.split("\t");

            String text = c[3];
            text = text.replaceAll("\\s+", " ").replace("?", "").replace(".", "").replace("'s", "").trim();
            String subject = "m." + c[0].replace("www.freebase.com/m/", "");
            String predicate = c[1].replace("www.freebase.com/", "").replace("/", ".");

            List<List<Instance>> matches = KnowledgeBaseLinker.getMatches(text, topK, entityMaxNGramSize, predicateMaxNGramSize);

            // entity matches are always in 0 index, predicates are in index 1
            List<Instance> entityMatches = matches.get(0);
            List<Instance> predicateMatches = matches.get(1);
            
            boolean subjectFound = false;
            String entityNgram = "";
            
            for(Instance i : entityMatches){
                if(i.getUri().equals(subject)){
                    subjectFound = true;
                    entityNgram = i.getNgram();
                    break;
                }
            }
            
            if(subjectFound){
                
            }
        }
    }
}
