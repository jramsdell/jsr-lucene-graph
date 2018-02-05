package unh.edu.cs;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jooq.lambda.Seq;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class LuceneQueryBuilder {
    private IndexSearcher indexSearcher;
    private Analyzer analyzer;
    private QueryType queryType;

    LuceneQueryBuilder(QueryType qType, Analyzer ana, Similarity sim, String indexPath) throws IOException {
        analyzer = ana;
        queryType = qType;
        createIndexSearcher(indexPath);
        indexSearcher.setSimilarity(sim);
    }

    private class TokenGenerator implements Supplier<String> {
        final TokenStream tokenStream;

        TokenGenerator(TokenStream ts) throws IOException {
            tokenStream = ts;
            ts.reset();
        }

        @Override
        public String get() {
            try {
                if (!tokenStream.incrementToken()) {
                    tokenStream.end();
                    tokenStream.close();
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
            return tokenStream.getAttribute(CharTermAttribute.class).toString();
        }
    }

    // delete
    private static IndexSearcher setupIndexSearcher(String indexPath, String typeIndex) throws IOException {
        Path path = FileSystems.getDefault().getPath(indexPath);
        Directory indexDir = FSDirectory.open(path);
        IndexReader reader = DirectoryReader.open(indexDir);
        return new IndexSearcher(reader);
    }

    // delete
    static class MyQueryBuilder {
        private final StandardAnalyzer analyzer;
        private List<String> tokens;

        public MyQueryBuilder(StandardAnalyzer standardAnalyzer){
            analyzer = standardAnalyzer;
            tokens = new ArrayList<>(128);
        }

        public BooleanQuery toQuery(String queryStr) throws IOException {

            TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(queryStr));
            tokenStream.reset();
            tokens.clear();

            while (tokenStream.incrementToken()) {
                final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
                tokens.add(token);
            }
            tokenStream.end();
            tokenStream.close();
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            for (String token : tokens) {
                booleanQuery.add(new TermQuery(new Term("text", token)), BooleanClause.Occur.SHOULD);
            }
            return booleanQuery.build();
        }
    }

    private BooleanQuery createQuery(String query) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(query));
        TokenGenerator tg = new TokenGenerator(tokenStream);
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
//        Optional.ofNullable(tg)
        Seq.generate(tg).limitWhile(Objects::nonNull)
                .map(s -> new TermQuery(new Term("text", s)))
                .forEach(termQuery -> queryBuilder.add(termQuery, BooleanClause.Occur.SHOULD));
        return queryBuilder.build();
    }

    private static String createQueryString(Data.Page page, List<Data.Section> sectionPath) {
        StringBuilder queryStr = new StringBuilder();
        String results = page.getPageName() +
                sectionPath.stream()
                        .map(section -> " " + section.getHeading() )
                        .collect(Collectors.joining(" "));

        return results;
    }

    void writeRankings(String queryLocation, String rankingsOutput) throws IOException {
        final BufferedWriter out = new BufferedWriter(new FileWriter(rankingsOutput));
        final FileInputStream inputStream = new FileInputStream(new File(queryLocation));

        if (queryType == QueryType.PAGE) {
            writePageRankings(inputStream, out);
        } else if (queryType == QueryType.SECTION) {
            writeSectionRankings(inputStream, out);
        }

        out.flush();
        out.close();
    }

    void writeRankingsToFile(ScoreDoc[] scoreDoc, String queryId, BufferedWriter out, HashSet<String> ids) throws IOException {
        for (int i = 0; i < scoreDoc.length; i++) {
            ScoreDoc score = scoreDoc[i];
            final Document doc = indexSearcher.doc(score.doc);
            final String paragraphid = doc.getField("paragraphid").stringValue();
            final float searchScore = score.score;
            final int searchRank = i + 1;

//            if (!ids.add(paragraphid)) {
//                continue;
//            }

            out.write(queryId + " Q0 " + paragraphid + " "
                    + searchRank + " " + searchScore + " Lucene-BM25" + "\n");
        }
    }


    void writePageRankings(FileInputStream inputStream, BufferedWriter out) throws IOException {
        HashSet<String> ids = new HashSet<>();

        for (Data.Page page : DeserializeData.iterableAnnotations(inputStream)) {
            final String queryId = page.getPageId();

            String queryStr = createQueryString(page, Collections.<Data.Section>emptyList());

            TopDocs tops = indexSearcher.search(createQuery(queryStr), 100);
            ScoreDoc[] scoreDoc = tops.scoreDocs;
            writeRankingsToFile(scoreDoc, queryId, out, ids);
        }
    }

    void writeSectionRankings(FileInputStream inputStream, BufferedWriter out) throws IOException {
        HashSet<String> ids = new HashSet<>();
        for (Data.Page page : DeserializeData.iterableAnnotations(inputStream)) {
            for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
                final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
                String queryStr = createQueryString(page, sectionPath);
                if (!ids.add(queryId)) {
                    continue;
                }

                TopDocs tops = indexSearcher.search(createQuery(queryStr), 100);
                ScoreDoc[] scoreDoc = tops.scoreDocs;
                writeRankingsToFile(scoreDoc, queryId, out, ids);
            }
        }

    }


    private void createIndexSearcher(String iPath) throws IOException {
        Path indexPath = Paths.get(iPath);
        Directory indexDir = FSDirectory.open(indexPath);
        IndexReader indexReader = DirectoryReader.open(indexDir);
        indexSearcher = new IndexSearcher(indexReader);
    }

}
