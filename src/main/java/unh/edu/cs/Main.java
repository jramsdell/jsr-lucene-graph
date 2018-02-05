package unh.edu.cs;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;

import java.io.IOException;

public class Main {

    private static void printIndexerUsage() {
        System.out.println("Indexer Usage: index indexType corpusCBOR IndexDirectory\n" +
                "Where:\n\tindexType is one of: \n" +
                "\t\tnormal (indexes text in paragraphs)\n" +
                "\t\tspotlight (also indexes entities using local spotlight server)\n" +
                "\tcorpusCBOR: the paragraph corpus file to index\n" +
                "\tIndexDirectory: the name of the index directory to create after indexing.\n"
        );
    }

    private static void printQueryUsage() {
        System.out.println("Query Usage: query queryType indexDirectory queryCbor rankOutput\n" +
                "Where:\n\tqueryType is one of: \n" +
                "\t\tpage (retrieves query results for each page)\n" +
                "\t\tsection (retrieves query results for each section)\n" +
                "\tindexLocation: the directory of the lucene index.\n" +
                "\tqueryCbor: the cbor file to be used as a query with the index.\n" +
                "\trankOutput: is the output location of the rankings (used for trec-eval)\n"
        );
    }

    private static void runIndexer(String sType, String corpusFile, String indexOutLocation) throws IOException {
        // Index Enum
        IndexType indexType = null;
        try {
            indexType = IndexType.valueOf(sType);
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown index type!");
            printIndexerUsage();
            System.exit(1);
        }

        LuceneIndexBuilder indexBuilder = new LuceneIndexBuilder(indexType, corpusFile, indexOutLocation);

        indexBuilder.initializeWriter();
        indexBuilder.run();

    }

    private static void runQuery(String qType, String indexLocation, String queryLocation,
                                 String rankingOutputLocation) throws IOException {
        QueryType queryType = null;
        try {
            queryType = QueryType.valueOf(qType);
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown query type!");
            printQueryUsage();
            System.exit(1);
        }
        LuceneQueryBuilder lqb = new LuceneQueryBuilder(
                queryType, new StandardAnalyzer(), new BM25Similarity(), indexLocation);
        lqb.writeRankings(queryLocation, rankingOutputLocation);
    }


    private static void printUsages() {
        printIndexerUsage();
        System.out.println();
        printQueryUsage();

    }

    public static void main(String[] args) throws IOException {
        String mode = "";
        try {
            mode = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            printUsages();
            System.exit(1);
        }

        switch (mode) {
            // Runs indexer command
            case "index":
                try {
                    final String indexType = args[1].toUpperCase();
                    final String corpusFile = args[2];
                    final String indexOutLocation = args[3];
                    runIndexer(indexType, corpusFile, indexOutLocation);
                } catch (IndexOutOfBoundsException e) {
                    printIndexerUsage();
                }
                break;

            // Runs query command
            case "query":
                try {
                    String queryType = args[1].toUpperCase();
                    String indexLocation = args[2];
                    String queryLocation = args[3];
                    String rankingOutputLocation = args[4];
                    runQuery(queryType, indexLocation, queryLocation, rankingOutputLocation);
                } catch (ArrayIndexOutOfBoundsException e) {
                    printQueryUsage();
                }
                break;
            default:
                printUsages();
                break;
        }

    }
}
