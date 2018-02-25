/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.syntax;

import de.citec.sc.index.Language;
import de.citec.sc.utils.FileUtil;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author sherzod
 */
public class UDPipe {

    private static JSONParser jsonParser;
    private static Map<String, String> cache = new HashMap<>();

    private static String requestUDPipeServer(String text, Language.lang lang) {
        String address = "";
        try {
            address = "https://lindat.mff.cuni.cz/services/udpipe/api/process?tokenizer&tagger&parser&data=" + URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(UDPipe.class.getName()).log(Level.SEVERE, null, ex);
        }

        //all language models are available here  : https://lindat.mff.cuni.cz/services/udpipe/api/models
        switch (lang) {
            case EN:
                address = address + "&model=english-ud-2.0-170801";
                break;
            case DE:
                address = address + "&model=german-ud-2.0-170801";
                break;
            case ES:
                address = address + "&model=spanish-ud-2.0-170801";
                break;
            default:
                break;
        }

        String responseAsString = "";
        try {
            URL url = new URL(address);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String inputLine;

            while ((inputLine = br.readLine()) != null) {
                responseAsString += inputLine;

            }

        } catch (IOException e) {

        }

        if (jsonParser == null) {
            jsonParser = new JSONParser();
        }

        try {
            if (responseAsString.isEmpty()) {
                return "";
            }

            JSONObject jObject = (JSONObject) jsonParser.parse(responseAsString);
            String result = (String) jObject.get("result");
            return result;
        } catch (ParseException ex) {
            Logger.getLogger(UDPipe.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    public static DependencyParse parse(String text, Language.lang lang) {

        if (cache.isEmpty()) {
            loadCache();
        }
        String result = "";
        if (cache.containsKey(text)) {
            result = cache.get(text);

            if (result.isEmpty()) {
                for (int i = 1; i <= 2; i++) {
                    result = requestUDPipeServer(text, lang);
                    if (!result.isEmpty()) {
                        break;
                    }
                }
                cache.put(text, result);
                writeCache();
            }
        } else {
            //query the api 10 times max, sometimes it doesn't work.
            for (int i = 1; i <= 10; i++) {
                result = requestUDPipeServer(text, lang);
                if (!result.isEmpty()) {
                    break;
                }
            }
            cache.put(text, result);
            writeCache();
        }

        //Dependency parse arguments
        DependencyParse depParse = null;
        Map<Integer, String> nodeMap = new HashMap<>();
        Map<Integer, String> edgeMap = new HashMap<>();
        Map<Integer, String> postagMap = new HashMap<>();
        Map<Integer, Integer> relationMap = new HashMap<>();
        int headNode = -1;

        if (result.isEmpty()) {
            return null;
        }

        String[] lines = result.split("\n");

        try {
            for (String l : lines) {
                if (!l.startsWith("#")) {

                    String[] data = l.split("\t");
                    Integer tokenID = Integer.parseInt(data[0]);
                    String label = data[1];
                    String pos = data[3];
                    int beginPosition = text.indexOf(label);
                    int endPosition = beginPosition + label.length();
                    
                    //barack@obama
                    if(label.contains("@") && label.split("@").length > 1){
                        pos = "PROPN";
                    }
                    
                    //skip punctuations
                    if(pos.equals("PUNCT")){
                        continue;
                    }

                    int parentNode = Integer.parseInt(data[6]);
                    String depRelation = data[7];

                    nodeMap.put(tokenID, label);
                    postagMap.put(tokenID, pos);

                    if (depRelation.equals("root")) {
                        headNode = tokenID;
                    } else {
                        edgeMap.put(tokenID, depRelation);
                        relationMap.put(tokenID, parentNode);
                    }
                }
            }

            depParse = new DependencyParse(nodeMap, relationMap, edgeMap, postagMap, headNode);
        } catch (Exception e) {
            return null;
        }

        return depParse;
    }

    private static void loadCache() {
        Set<String> set = FileUtil.readFile("ud-cache.txt");

        if (!set.isEmpty()) {

            String c = "";
            for (String s : set) {
                c += s + "\n";
            }

            if (jsonParser == null) {
                jsonParser = new JSONParser();
            }

            JSONObject jObject;
            try {
                jObject = (JSONObject) jsonParser.parse(c);

                JSONArray array = (JSONArray) jObject.get("parseTrees");

                for (Object o : array) {
                    JSONObject parse = (JSONObject) o;

                    String key = parse.get("text").toString();
                    String value = parse.get("tree").toString();

                    cache.put(key, value);
                }
            } catch (ParseException ex) {
                Logger.getLogger(UDPipe.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void writeCache() {
        JSONObject obj = new JSONObject();

        JSONArray parseTrees = new JSONArray();

        obj.put("parseTrees", parseTrees);

        for (String k : cache.keySet()) {
            JSONObject parseTree = new JSONObject();
            parseTree.put("text", k);
            parseTree.put("tree", cache.get(k));

            parseTrees.add(parseTree);
        }

        FileUtil.writeStringToFile("ud-cache.txt", obj.toJSONString(), false);
    }
}
