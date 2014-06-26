package gui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;

import query.cluster.association.TermWeighting;
import clustering.Clustering;

public class MainView {

	JTextField searchField;
	private static Clustering clustering;
	private static Directory luceneIndex;
	
	

	public static void main(String[] args) throws FileNotFoundException, IOException {		
		/*
		 * Create a new Clustering object and start the lucene indexing.
		 * Test the time it takes to cluster. 
		 */

		clustering = new Clustering();
		
		long startTime = System.currentTimeMillis();
		
		luceneIndex = clustering.startLuceneIndexing(5, 10, 10);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println(elapsedTime / 1000 + " seconds to index");
		
		String inputString = "";
		while(inputString != "exit"){
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        System.out.print("Enter search query\nPress enter for suggestions: ");
	        inputString = br.readLine();
	        System.out.println("Finding suggestions for " + inputString);
	        get_suggestions(inputString);
		}        
	}
		
	public static void get_suggestions(String searchString){
		//String searchString = searchField.getText();
		if (searchString.length() > 2) {
			long startTime = System.currentTimeMillis();

			ArrayList<Document> documentsResults = clustering.searchForDocuments(searchString, luceneIndex);
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println(elapsedTime + " milliseconds to search for relevant documents");
			
			long clusteringStartTime = System.currentTimeMillis();
			ArrayList<ArrayList<org.apache.lucene.document.Document>> clusteringResults = clustering.startClusteringWithResults(documentsResults, searchString);
			System.out.println("Found " + clusteringResults.size() + " clusters");
			long clusteringStopTime = System.currentTimeMillis();
			long clusteringElapsedTime = clusteringStopTime - clusteringStartTime;
			System.out.println(clusteringElapsedTime + " milliseconds for Carrot2 to create clusters in relation to query");

			long calculateTFIDFStartTime = System.currentTimeMillis();
			TermWeighting termWeighting = new TermWeighting();
			ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = termWeighting.calculateTFIDFForClusters(clusteringResults, "tfidf", searchString);
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

