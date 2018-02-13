/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sherzod
 */
public class SortUtils {

    public static Map<Object, Double> sortByDoubleValue(Map<Object, Double> unsortMap) {
        if (unsortMap == null) {
            return new HashMap<>();
        }

        List<Map.Entry<Object, Double>> list = new LinkedList<Map.Entry<Object, Double>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<Object, Double>>() {
            public int compare(Map.Entry<Object, Double> o1,
                    Map.Entry<Object, Double> o2) {

                return o2.getValue().compareTo(o1.getValue());

            }
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<Object, Double> sortedMap = new LinkedHashMap<Object, Double>();
        for (Map.Entry<Object, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
    
    public static Map<Object, Integer> sortByIntegerValue(Map<Object, Integer> unsortMap) {
        if (unsortMap == null) {
            return new HashMap<>();
        }

        List<Map.Entry<Object, Integer>> list = new LinkedList<Map.Entry<Object, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<Object, Integer>>() {
            public int compare(Map.Entry<Object, Integer> o1,
                    Map.Entry<Object, Integer> o2) {

                return o2.getValue().compareTo(o1.getValue());

            }
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<Object, Integer> sortedMap = new LinkedHashMap<Object, Integer>();
        for (Map.Entry<Object, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
