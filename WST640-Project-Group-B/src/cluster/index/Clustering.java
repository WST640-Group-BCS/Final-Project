
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2014, Dawid Weiss, Stanis��aw Osi��ski.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */
package cluster.index;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.clustering.synthetic.ByUrlClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.IDocumentSource;
import org.carrot2.core.ProcessingResult;

/**
 * This example shows how to cluster a set of documents available as an {@link ArrayList}.
 * This setting is particularly useful for quick experiments with custom data for which
 * there is no corresponding {@link IDocumentSource} implementation. For production use,
 * it's better to implement a {@link IDocumentSource} for the custom document source, so
 * that e.g., the {@link Controller} can cache its results, if needed.
 * 
 * @see ClusteringDataFromDocumentSources
 * @see UsingCachingController
 */
public class Clustering
{
	private static String OS = System.getProperty("os.name").toLowerCase();
	private ArrayList<Document> documents;
	private ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersWithLuceneDocuments;
				
	public Clustering()
	{
		
	}
	
	public Directory startLuceneIndexing(
			int numberOfFoldersToUse, 
			int numberOfFilesToIndex,
			int numberOfDOCTagsToIndexInONEFile, 
			String TRECPath) throws FileNotFoundException, IOException
	{
        this.documents = new ArrayList<Document>();
        
        /*
         * Path to the Trec Files, make sure to change these to the correct path.
         */
        String path_to_trec = TRECPath;
//		if (isWindows()) {
//			path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";	
//		} else if (isMac()) {
//			path_to_trec = "/Users/wingair/Dropbox/Dataset/WT10G/";	
//		}
		
		/*
		 * Use the correct symbol according to the OS.
		 */
		String symbol = "";
		if (isWindows()) {
			symbol = "\\";	
		} else if (isMac()) {
			symbol = "/";
		}

		/*
		 * Counters indicating which folder, files, <DOC> Tag indexing.
		 */
		int numberOfDOCTagIndexing = 0;
		int numberOfFilesIndexing = 0;
		int numberOfFolderUsing = 0;
		
		File file = new File(path_to_trec);
		String[] wtx_folders = file.list();

		/*
		 * Initialization of objects needed to index. 
		 * We are going to store our index in RAM, so we are going to use a |RAMDirectory|.
		 */
		Directory index = new RAMDirectory();

		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
		IndexWriter indexWriter = new IndexWriter(index, config);

		/*
		 * Find the correct folders and files to use.
		 */
		for (String wtx_folder : wtx_folders) {
			numberOfDOCTagIndexing = 0;
			numberOfFilesIndexing = 0;

			if ((new File(path_to_trec + symbol + wtx_folder).isDirectory())) {

				if (numberOfFolderUsing < numberOfFoldersToUse) {
					System.out.println("Using the folder: " + new File(path_to_trec + symbol + wtx_folder).getName());
					String[] sub_directories = new File(path_to_trec + symbol + wtx_folder).list();
					for (String sub_directory : sub_directories) 
					{
						if (numberOfFilesIndexing < numberOfFilesToIndex) 
						{
							
							StringBuilder builder = new StringBuilder();

							File sub_file = new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory);
							
							System.out.println("Using the specific path: " + sub_file.getAbsolutePath());
							BufferedReader bufferedReader;
							bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(sub_file.getAbsolutePath()))));
							String content;
							
							System.out.println("Indexing the specific file: " + 
							new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory).getName() + ":");

							while ((content = bufferedReader.readLine()) != null) {
								builder.append(content);
							}
							bufferedReader.close();
							
							String sub_file_text = builder.toString();
							
							/*
							 * The regular expression used to search for the part we want from
							 * the files.
							 */
							String docno_pattern = "(<DOCNO>(.*?)</DOCNO>)(?<DOC>(.*?)</DOC>)";

							Pattern docno_r = Pattern.compile(docno_pattern);
							Matcher docno_m = docno_r.matcher(sub_file_text);
							while (docno_m.find()) {
								if (numberOfDOCTagIndexing < numberOfDOCTagsToIndexInONEFile) {
									/*
									 * Get the interesting parts retrieved from our regular expression.
									 * That is the document number and the contents between the tags
									 * <DOC></DOC>.
									 */
									String doc_no = docno_m.group(2);
									String doc_content = docno_m.group(3);
									
									/*
									 * Create a new Lucene Document, add the content and the document
									 * number to this. Then add it to the index writer so it can be
									 * indexed.
									 */
									org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();

									this.documents.add(new Document(doc_no, doc_content, doc_content));
						            doc.add(new TextField("content", doc_content, Store.YES));
						            doc.add(new TextField("title", doc_no, Store.YES));
						            indexWriter.addDocument(doc);
								}
								numberOfDOCTagIndexing += 1;
							}
							numberOfFilesIndexing += 1;
						}
						numberOfDOCTagIndexing = 0;
					}
					numberOfFolderUsing += 1;
				}
			}
		} 
		indexWriter.close();
		return index;
	}
	
	public ArrayList<org.apache.lucene.document.Document> searchForDocuments(String searchString, Directory index)
	{
		ArrayList<org.apache.lucene.document.Document> searchResults = new ArrayList<org.apache.lucene.document.Document>();

		try {
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			Query query = new QueryParser(Version.LUCENE_46, "content", analyzer).parse(searchString);

			IndexReader indexReader = DirectoryReader.open(index);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(99, true);
			indexSearcher.search(query, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				org.apache.lucene.document.Document document = indexSearcher.doc(docId);
				searchResults.add(document);
			}
			
			indexReader.close();
		} catch (Exception e) {

		}
		return searchResults;
	}

	
	public ArrayList<ArrayList<org.apache.lucene.document.Document>> startClusteringWithResults(ArrayList<org.apache.lucene.document.Document> results, String query)
	{
		ArrayList<ArrayList<org.apache.lucene.document.Document>> searchResultClustersCreatedFromQuery = new ArrayList<ArrayList<org.apache.lucene.document.Document>>();
		//Convert the collection of Lucene documents to Carrot2 documents.
		ArrayList<Document> convertedResults = new ArrayList<Document>();
		for (org.apache.lucene.document.Document luceneDocument : results) {
			convertedResults.add(new Document(luceneDocument.get("title"), luceneDocument.get("content"), luceneDocument.get("content")));
		}
		
        Controller controller = ControllerFactory.createSimple();
        
        /*
         * Feed the controller with the converted results, choose the algorithm we want to use and
         * start clustering the results according to the search query.
         */
        ProcessingResult byTopicClusters = controller.process(convertedResults, query, STCClusteringAlgorithm.class);
        
        List<Cluster> clustersByTopic = byTopicClusters.getClusters();  

        /*
         * Convert the Carrot2 documents back to Lucene Documents so we can use Lucene for searching.
         */
        for (Cluster cluster : clustersByTopic) {
        	System.out.println(cluster.getScore());
            List<Document> carrot2ClusterDocumentsList = cluster.getAllDocuments();
            ArrayList<org.apache.lucene.document.Document> luceneDocumentClustersList = new ArrayList<org.apache.lucene.document.Document>();            		

            for (Document document : carrot2ClusterDocumentsList) {
            	FieldType type = new FieldType();
        		type.setIndexed(true);
        		type.setStored(true);
        		type.setStoreTermVectors(true);

                org.apache.lucene.document.Document luceneDocument = new org.apache.lucene.document.Document();
                luceneDocument.add(new Field("body", document.getContentUrl(), type));
                luceneDocument.add(new Field("title", document.getTitle(), type));
                luceneDocumentClustersList.add(luceneDocument);
			}
            searchResultClustersCreatedFromQuery.add(luceneDocumentClustersList);
		}
        return searchResultClustersCreatedFromQuery;
	}	
	
	/*
	 * Functions used to determine the OS being used.
	 */
    public static boolean isWindows() {
		 
		return (OS.indexOf("win") >= 0);
 
	}
 
	public static boolean isMac() {
 
		return (OS.indexOf("mac") >= 0);
 
	}
}
