# lucene-jsr


# Running from Bin

The may run the precompiled jar file located in bin/jsr-lucene.jar

# Compiling Source Code
Alternatively, you may use maven to compile the source code and run the target jar.
With maven installed, run the following in the same directory as pom.xml:

mvn clean compile assembly:single

And then run the snapshot jar found in target/

# Usage

This program has two modes: index and query.
Index is used to index a paragraph corpus file, while query is used to search an indexed database using a cbor file as a query.

### Index Mode
To run the index mode, do:

java -jar jsr_lucene.jsr index indexType corpusCBOR IndexDirectory

Where:
**indexType** is one of:
    paragraph (indexes just the full text of each paragraph in the corpus)

**corpusCBOR**: the cbor file that will be used to build a Lucene index directory.

**indexDirectory**: path to the directory that the Lucene index will be created in.


#### Example
java -jar jsr_lucene.jar index paragraph dedup.articles-paragraphs.cbor myindex_directory/

### Query Mode
To run the query mode, do:

java -jar jsr_lucene.jar query queryType indexDirectory queryCbor rankOutput

Where:
**queryType** is one of:
    page (retrieves query results for each page)
    section (retrieves query results for each section)
    
**indexDirectory**: path to the Lucene index directory to be used in the search.

**queryCbor**: path to the cbor file to be used for querying the indexed database.

**rankOutput**: name of the file to create to store the results of the query (used for trec-eval).

#### Example
java -jar jsr_lucene.jar query page myindex_directory/ train.pages.cbor-outlines.cbor results.tops
