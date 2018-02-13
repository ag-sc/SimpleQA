/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import de.citec.sc.index.Language;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class NGramExtractor {

    public static List<String> extractAllNGrams(String text, int maxNgramSize) {
        List<String> ngrams = new ArrayList<>();

        text = text.replaceAll("\\s+", " ").trim();
        String[] unigrams = text.split("\\s");

        for (int i = 0; i < unigrams.length; i++) {

            for (int n = maxNgramSize; n > 0; n--) {

                if (i + n <= unigrams.length) {

                    String ngram = "";

                    for (int k = i; k < i + n; k++) {
                        ngram += unigrams[k] + " ";
                    }
                    ngram = ngram.trim();

                    //select valid ngrams
                    if (ngram.length() < 3) {
                        continue;
                    }

                    //check if the word isn't stopword
                    if (Stopwords.isStopWord(ngram, Language.lang.EN)) {
                        continue;
                    }
                    //check if unigrams in the word aren't stopwords
                    String[] tokens = ngram.trim().split(" ");
                    int numberOfStopWords = 0;
                    for (String t : tokens) {
                        if (Stopwords.isStopWord(t, Language.lang.EN)) {
                            numberOfStopWords++;
                        }
                    }

                    //ngrams that have many stopwords in it
                    double percentageOfStopWords = numberOfStopWords / (double) tokens.length;
                    if (percentageOfStopWords >= 0.7) {
                        continue;
                    }

                    ngrams.add(ngram);
                }
            }
        }

        return ngrams;
    }

    /**
     * extracts ngram that equals to the given maxNGramSize
     *
     * @param text
     * @param maxNgramSize
     * @return List<String> ngrams
     *
     */
    public static List<String> extractNGrams(String text, int maxNgramSize) {
        Set<String> ngrams = new LinkedHashSet<>();

        text = text.replaceAll("\\s+", " ").trim();
        String[] unigrams = text.split("\\s");

        for (int i = 0; i < unigrams.length; i++) {

            String ngram = "";

            if (i + maxNgramSize <= unigrams.length) {
                for (int j = i; j < i + maxNgramSize; j++) {
                    ngram += unigrams[j] + " ";
                }

                ngram = ngram.trim();

                //select valid ngrams
                if (ngram.length() < 3) {
                    continue;
                }

                //check if the word isn't stopword
                if (Stopwords.isStopWord(ngram, Language.lang.EN)) {
                    continue;
                }
//                //check if unigrams in the word aren't stopwords
//                String[] tokens = ngram.trim().split(" ");
//                int numberOfStopWords = 0;
//                for (String t : tokens) {
//                    if (Stopwords.isStopWord(t, Language.lang.EN)) {
//                        numberOfStopWords++;
//                    }
//                }
//
//                //ngrams that have many stopwords in it
//                double percentageOfStopWords = numberOfStopWords / (double) tokens.length;
//                if (percentageOfStopWords >= 0.7) {
//                    continue;
//                }

                ngrams.add(ngram);
            }
        }

        return new ArrayList<>(ngrams);
    }
}
