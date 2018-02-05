package unh.edu.cs;
import javafx.util.Pair;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.jooq.lambda.Seq;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BigramAnalyzer {
    private final NGramTokenizer tokenizer;

    BigramAnalyzer(String content) {
        tokenizer = new NGramTokenizer(2, 2);
        tokenizer.setReader(new StringReader(content));
    }

    Double getScore(String bigram, HashMap<String, Double> bigramCounts, HashMap<Character, Double> monogramCounts) {
        Double pBigram = bigramCounts.get(bigram);
        Double p1 = monogramCounts.get(bigram.charAt(0));
        Double p2 = monogramCounts.get(bigram.charAt(1));

        try {
            return (pBigram / (p1 * p2));
        } catch (NullPointerException e) {
            return 0.0;
        }
    }

    public List<String> run() throws IOException {
        HashMap<String, Double> bigramCounts = new HashMap<>();
        HashMap<Character, Double> monogramCounts = new HashMap<>();
        int totalBigrams = 0;

//        CharTermAttribute charTerm = tokenizer.addAttribute(CharTermAttribute.class);
        NGramTokenFilter filter = new NGramTokenFilter(tokenizer, 2, 2);
        CharTermAttribute charTerm = filter.addAttribute(CharTermAttribute.class);
//        tokenizer.reset();
        filter.reset();

        // Count number of times each bigram occur and co-occur
        String previous = "";
        while (filter.incrementToken()) {
            String token = charTerm.toString();
            totalBigrams += 1;

            bigramCounts.put(token, bigramCounts.getOrDefault(token, 0.0) + 1.0);
            monogramCounts.put(token.charAt(0),
                    monogramCounts.getOrDefault(token.charAt(1), 0.0) + 1.0);

//            if (!previous.isEmpty()) {
//                String comb = previous + token;
//                totalEdges += 1;
//                bigramEdges.put(comb, bigramCounts.getOrDefault(comb, 0.0) + 1.0);
//            }

//            previous = token;
        }

        // Convert to probabilities
        ArrayList<Integer> stuff = new ArrayList<>();

        final int finalCounts = totalBigrams;
        bigramCounts.entrySet()
                .forEach(entry -> entry.setValue(entry.getValue() / finalCounts));
        monogramCounts.entrySet()
                .forEach(entry -> entry.setValue(entry.getValue() / finalCounts * 2));

        // Create list sorted by bigram scores
        return Seq.seq(bigramCounts.entrySet())
                .map(entry ->
                        new Pair<>(entry.getKey(), getScore(entry.getKey(), bigramCounts, monogramCounts)))
                .sorted(Pair::getValue)
                .reverse()
                .take(finalCounts / 10)
                .map(Pair::getKey)
                .toList();

    }

    public static void main(String[] args) throws IOException {
        BigramAnalyzer ba = new BigramAnalyzer("This is a test. I wonder if it will work.");
//        String field = ba.run();
    }

}

