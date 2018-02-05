package unh.edu.cs;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;

// Parses out entity links using DBpedia
public class EntityLinker {
    private final String url;

    // Accepts content (paragraph text) to form a URL query to DBpedia with
    EntityLinker(String content) {
//        String base = "http://model.dbpedia-spotlight.org/en/annotate?text=";
        String base = "localhost:9310/jsr-spotlight/annotate?text=";
        try {
            base = base + URLEncoder.encode(content, "UTF-8");
            System.out.println(base);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        url = base;
    }

    // Queries DBpedia and returns a list of entities
    ArrayList<String> run() {
        ArrayList<String> entities = new ArrayList<>();

        // Query DBpedia and parse out the titles using Jsoup
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");

            for (Element e : links) {
                String title = e.attr("title");
                title = title.substring(title.lastIndexOf("/") + 1);
                entities.add(title);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
            System.out.println(url);
        }
        return entities;
    }

    public static void main(String[] args) throws IOException {
        EntityLinker entityLinker = new EntityLinker("I think computer science is mostly okay.");
        ArrayList<String> entities = entityLinker.run();
        for (String entity : entities) {
            System.out.println(entity);
        }
    }
}
