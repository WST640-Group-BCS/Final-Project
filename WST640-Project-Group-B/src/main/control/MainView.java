package main.control;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import javax.swing.JTextField;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;

import term.weighting.TermWeighting;
import cluster.index.Clustering;

public class MainView {

	JTextField searchField;
	private static Clustering clustering;
	private static Directory luceneIndex;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {	
		
		/*
		 * Parameters you can change.
		 */
		
		//***************************************************
		//ONLY USE THE EXAMPLE PATHS BELOW THAT MATCH YOUR OS 
		//AND MAKE SURE IT MATCHES THE FILES ON YOUR OWN DISK
		//***************************************************
		//WINDOWS EXAMPLE PATHS
		String TRECPath = "E:\\Dropbox\\Dataset\\WT10G";
		String queryLogPath = "E:\\Dropbox\\Dataset\\user-ct-test-collection-01.txt";
		//MAC EXAMPLE PATHS
		//String TRECPath = "/Users/wingair/Dropbox/Dataset/WT10G/";
		//String queryLogPath = "/Users/wingair/Dropbox/Dataset/user-ct-test-collection-01.txt";

		int numberOfFoldersToIndex = 20;
		int numberOfFilesToIndex = 25;
		int numberOfDOCTagsToIndex = 20;

		String weightingScheme = "df";
		Boolean includeQueryLog = true;
		
		/*
		 * Create a new Clustering object and start the lucene indexing. 
		 */
		clustering = new Clustering();
		
		long startTime = System.currentTimeMillis();
		
		luceneIndex = clustering.startLuceneIndexing(numberOfFoldersToIndex, numberOfFilesToIndex, numberOfDOCTagsToIndex, TRECPath);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println(elapsedTime / 1000 + " seconds to index");
		
		/*
		 * Start asking the user to input query. Nothing fancy.
		 */
		String inputString = "";
		while(inputString != "exit"){
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        System.out.print("Enter search query\nPress enter for suggestions: ");
	        inputString = br.readLine();
	        System.out.println("Finding suggestions for " + inputString);
	        /*
	         * Get suggestions for word, the parameters are, the query string, the weighting
	         * scheme, and to include query log or not in the score.
	         */
	        get_suggestions(inputString, weightingScheme, includeQueryLog, queryLogPath);
		}        
	}
		
	public static void get_suggestions(String searchString, String weightingScheme, Boolean includeQueryLog, String queryLogPath){
		
		
		if (searchString.length() > 2) {
			long startTime = System.currentTimeMillis();
			/*
			 * From the index created from all the documents indexed. Retrieve the documents
			 * that contain the query. Then save the results in |documentsResults|.
			 */
			ArrayList<Document> documentsResults = clustering.searchForDocuments(searchString, luceneIndex);
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println(elapsedTime + " milliseconds to search for relevant documents");
			
			/*
			 * Give the |documentsResults| to the clustering object to create clusters out of these documents and the query.
			 */
			long clusteringStartTime = System.currentTimeMillis();
			ArrayList<ArrayList<org.apache.lucene.document.Document>> clusteringResults = clustering.startClusteringWithResults(documentsResults, searchString);
			System.out.println("Found " + clusteringResults.size() + " clusters");
			long clusteringStopTime = System.currentTimeMillis();
			long clusteringElapsedTime = clusteringStopTime - clusteringStartTime;
			System.out.println(clusteringElapsedTime + " milliseconds for Carrot2 to create clusters in relation to query");

			/*
			 * Add a weight to each term in the clusters. The weighting scheme can be "df" or "tfidf".
			 */
			long calculateTFIDFStartTime = System.currentTimeMillis();
			TermWeighting termWeighting = new TermWeighting();
			ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = termWeighting.calculateTFIDFForClusters(clusteringResults, weightingScheme, searchString, includeQueryLog, queryLogPath);
			
			/*
			 * Show each cluster with the term and the score of the term.
			 */
			for (NavigableSet<Map.Entry<String, Float>> termCluster : termClustersList) {
				System.out.println("******Cluster******");
				Iterator iterator = termCluster.iterator();
				int counter = 0;
				while (counter < 10) {
					Entry<String, Float> entry = (Entry<String, Float>) iterator.next();
					System.out.println(entry);
					counter += 1;
				}
			}
			long calculateTFIDFStopTime = System.currentTimeMillis();
			long calculateTFIDFElapsedTime = calculateTFIDFStopTime - calculateTFIDFStartTime;
			System.out.println(calculateTFIDFElapsedTime + " milliseconds to calculate TFIDF for every term in every cluster");
		}	  
	}
}

