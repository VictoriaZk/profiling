package com.example.profiling.utils;

import com.medallia.word2vec.*;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkType;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class DocumentUtilsImpl {
    private static final int numberSentencesEssay = 10;

    private final List<String> documents;
    private final String mainTerm;

    public DocumentUtilsImpl(List<String> documents, String mainTerm){
        this.mainTerm = mainTerm;
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

    private Integer getPositionInDocument(String[] sentences, String sentence){
        for (int i = 0; i < sentences.length; i++) {
            if(sentences[i].equals(sentence)){
                return i;
            }
        }

        return 0;
    }

    private Integer getPositionInParagraph(String document, String sentence){
        String[] splitTextBySentences = getSplitTextBySentences(document);

        for (int i = 0; i < splitTextBySentences.length; i++) {
            if(splitTextBySentences[i].equals(sentence)){
                return i;
            }
        }

        return 0;
    }

    private List<String> getMeaningfulSentences(){
        Map<String, Double> weightSentences = getWeightSentences();

        Optional<Double> first = weightSentences.values()
                .stream().sorted(Comparator.reverseOrder()).skip(numberSentencesEssay).findFirst();

        return first.map(aDouble -> weightSentences.entrySet().stream()
                .filter(entrySet -> entrySet.getValue() >= aDouble)
                .map(Map.Entry::getKey).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>(weightSentences.keySet()));
    }

    private Map<String, Double> getWeightSentences(){
        Map<String, Double> weightSentences = new LinkedHashMap<>();

        documents.forEach(document -> {
            String[] sentencesByDocument = getSplitTextBySentences(document);
            for (String sentence : sentencesByDocument) {
                Double weightSentence = getWeightSentence(document, sentence)
                        * getPositionInDocument(sentencesByDocument, sentence)
                        * getPositionInParagraph(document, sentence);

                weightSentences.put(sentence, weightSentence);
            }
        });

        return weightSentences;
    }

    //Score(Si)
    private Double getWeightSentence(String document, String sentence){
        String[] splitWords = getSplitWords(sentence);

        return Arrays.stream(splitWords).mapToDouble((term) ->
                getTermFrequencyByDocument(term, sentence) * getIDFTermInDocument(term, document)).sum();
    }

    //w(t,D)
    private Double getIDFTermInDocument(String term, String document){
        Double result = 0.5 * (1 + (getTermFrequencyByDocument(term, document) /
                getMaxTermFrequencyInDocument(document)));

        result *= Math.log((double)documents.size() / getNumberDocumentWithTerm(term));

        return result;
    }

    //tf(t,D)
    private Double getTermFrequencyByDocument(String term, String document){
        String[] splitWords = getSplitWords(document);
        Map<String, Integer> termsOccurrences = getTermsOccurrences(splitWords);

        if(!termsOccurrences.containsKey(term)){
            return 0.0;
        }
        return (double)termsOccurrences.get(term) / splitWords.length;
    }

    //df(t)
    private Integer getNumberDocumentWithTerm(String term){
        Integer number = documents.stream()
                .mapToInt(document -> document.toLowerCase().contains(term) ? 1 : 0).sum();

        if(number == 0){
            return 0;
        }

        return number;
    }

    //tf max(D)
    private Double getMaxTermFrequencyInDocument(String document){
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

    public List<Object> getEssayByML(){
        Word2VecModel model = getTestWord2Vec();
        String fullText = String.join("", documents);
        String[] splitTextBySentences = getSplitTextBySentences(fullText);
        List<Pair> weightSentences = Arrays.stream(splitTextBySentences).map(sentence -> {
            String[] splitWords = getSplitWords(sentence);

            double weightSentence = Arrays.stream(splitWords).mapToDouble(word -> {
                return getWeightWord(model, word);
            }).sum();

            return new Pair<String, Double>(sentence, weightSentence);
        }).collect(Collectors.toList());

        Optional<Double> first = weightSentences.stream().map(pair -> (Double)pair.getValue())
                .sorted(Comparator.reverseOrder()).skip(numberSentencesEssay).findFirst();


        return first.map(aDouble -> weightSentences.stream()
                .filter(pair -> (Double)pair.getValue() > aDouble)
                .map(Pair::getKey).collect(Collectors.toList()))
                .orElseGet(() -> weightSentences.stream().map(Pair::getKey).collect(Collectors.toList()));



    }


    private Double getWeightWord(Word2VecModel model, String term){
        try{
            return model.forSearch().cosineDistance(term, mainTerm);
        }catch (Exception ex){
            return 0.001;
        }
    }

    private Word2VecModel getTestWord2Vec() {

        try {
            String fullText = String.join("", documents);
            List<List<String>> sentences = documents.stream()
                    .map( document -> Arrays.stream(getSplitTextBySentences(document)).collect(Collectors.toList()))
                    .collect(Collectors.toList());

            String[] splitTextBySentences = getSplitTextBySentences(fullText);
            List<List<String>> collect = Arrays.stream(splitTextBySentences)
                    .map(sentence -> Arrays.stream(getSplitWords(sentence))
                            .collect(Collectors.toList())).collect(Collectors.toList());

            Word2VecModel train = Word2VecModel.trainer()
                    .setWindowSize(15)
                    .type(NeuralNetworkType.SKIP_GRAM)
                    .setLayerSize(6000)
                    .useNegativeSamples(25)
                    .setDownSamplingRate(1e-4)
                    .setNumIterations(5)
                    .train(collect);

            //Double cosineDistance = train.forSearch().cosineDistance("мозг", "полушария");

            return train;

//            List<String> vocab = train.toThrift().deepCopy().getVocab();
//
//            List<Double> vectors = train.toThrift().getVectors();
//
//            List<Searcher.Match> words = train.forSearch().getMatches("мозг", 10);
//
//            List<Searcher.Match> matches = train.forSearch().getMatches(words.get(4).match(), 10);
//
//            List<Searcher.Match> matches2 = train.forSearch().getMatches(matches.get(4).match(), 10);
//
//            NormalizedWord2VecModel normalizedWord2VecModel = NormalizedWord2VecModel.fromWord2VecModel(train);
        }catch (Exception ex){
            return null;
        }
    }
}
