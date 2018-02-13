/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import com.google.gson.Gson;
import de.citec.sc.index.Language;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 *
 * @author sherzod
 */
public class WordEmbeddingUtil {

    public static double computeSimilarity(String word1, String word2, Language.lang lang) {
        double sim = 0;

        try {

            //preprocess the input text
            word1 = StringPreprocessor.preprocess(word1, lang);
            word1 = URLEncoder.encode(word1, "UTF-8");

            word2 = StringPreprocessor.preprocess(word2, lang);
            word2 = URLEncoder.encode(word2, "UTF-8");

            String url = "http://purpur-v10:8081/similarity?word1=" + word1 + "&word2=" + word2 + "&lang=" + lang.name();

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
//            System.out.println("\nSending 'GET' request to URL : " + url);
//            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
//            System.out.println(response.toString());
            Gson gson = new Gson();

            sim = gson.fromJson(response.toString(), Double.class);
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return sim;
    }
}
