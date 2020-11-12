package com.example.profiling.utils;

import com.example.profiling.model.Document;
import javafx.util.Pair;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentUtilsImpl {
    private static final int numberSentencesEssay = 10;

    private final List<String> documents;

    public DocumentUtilsImpl(List<String> documents){
        this.documents = documents;
    }


    public static String getCleanText(String text){
        return text
                .replaceAll("[–,.;:!?]", "")
                .replaceAll("\n", " ")
                .replaceAll("\t", " ")
                .replaceAll("  ( )*", " ").toLowerCase();
    }

    public static String[] getSplitTextBySentences(String text){
        return text.split("[.!?]");
    }

    public String getEssay(){
        List<String> meaningfulSentences = getMeaningfulSentences();

        return String.join(". ", meaningfulSentences);
    }

    public List<String> getMeaningfulSentences(){
        Map<String, Double> weightSentences = getWeightSentences();

        Optional<Double> first = weightSentences.values()
                .stream().sorted(Comparator.reverseOrder()).skip(numberSentencesEssay).findFirst();

        return first.map(aDouble -> weightSentences.entrySet().stream()
                .filter(entrySet -> entrySet.getValue() > aDouble)
                .map(Map.Entry::getKey).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>(weightSentences.keySet()));
    }

    public Map<String, Double> getWeightSentences(){
        Map<String, Double> weightSentences = new LinkedHashMap<>();

        documents.forEach(document -> {
            String[] sentencesByDocument = getSplitTextBySentences(document);
            for (String sentence : sentencesByDocument) {
                weightSentences.put(sentence, getWeightSentence(document, sentence));
            }
        });

        return weightSentences;
    }

    //Score(Si)
    public Double getWeightSentence(String document, String sentence){
        String[] splitWords = getSplitWords(sentence);

        return Arrays.stream(splitWords).mapToDouble((term) ->
                getTermFrequencyByDocument(term, sentence) * getIDFTermInDocument(term, document)).sum();
    }

    //w(t,D)
    public Double getIDFTermInDocument(String term, String document){
        Double result = 0.5 * (1 + (getTermFrequencyByDocument(term, document) /
                getMaxTermFrequencyInDocument(document)));

        result *= Math.log((double)documents.size() / getNumberDocumentWithTerm(term));

        return result;
    }

    //tf(t,D)
    public Double getTermFrequencyByDocument(String term, String document){
        String[] splitWords = getSplitWords(document);
        Map<String, Integer> termsOccurrences = getTermsOccurrences(splitWords);

        if(!termsOccurrences.containsKey(term)){
            return 0.0;
        }
        return (double)termsOccurrences.get(term) / splitWords.length;
    }

    //df(t)
    public Integer getNumberDocumentWithTerm(String term){
        Integer number = documents.stream()
                .mapToInt(document -> document.toLowerCase().contains(term) ? 1 : 0).sum();

        if(number == 0){
            return 0;
        }

        return number;
    }

    //tf max(D)
    public Double getMaxTermFrequencyInDocument(String document){
        String[] splitWords = getSplitWords(document);
        Map<String, Integer> termsOccurrences = getTermsOccurrences(splitWords);

        Double maxFrequency = termsOccurrences.values().stream()
                .mapToDouble(quantity -> (double) quantity / splitWords.length).max()
                .orElse(0.0);

        if(maxFrequency == 0.0){
            return 0.0;
        }

        return maxFrequency;
    }


    public static String[] getSplitWords(String text){
        return getCleanText(text).split(" ");
    }

    public  static Set<String> getAllTerms(String text) {
        String[] words = getCleanText(text).split(" ");

        return Arrays.stream(words).collect(Collectors.toSet());
    }

    public static Map<String, Integer> getTermsOccurrences(String[] words){

        Map<String, Integer> initialForms = new HashMap<>();

        for (String word : words) {

            if (initialForms.containsKey(word)) {
                initialForms.put(word, initialForms.get(word) + 1);
            } else {
                initialForms.put(word, 1);
            }
        }

        return initialForms;
    }

    public static Map<String, Double> getTermsFrequency(String text) {
        String[] allWords = getSplitWords(text);
        Map<String, Integer> termsOccurrences = getTermsOccurrences(allWords);
        return termsOccurrences.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Double.valueOf(entry.getValue()) / allWords.length));
    }
}
