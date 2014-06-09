package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.w3c.dom.events.DocumentEvent;

import query.cluster.association.TermWeighting;
import clustering.Clustering;
import indexer.TrecIndexer;

public class MainView extends JFrame implements DocumentListener {

	JTextField searchField;
	private JLabel firstClusterResultsTextArea;
	private static TrecIndexer indexTrec;
	final JLabel secondClusterResultsTextArea;
	final JLabel thirdClusterResultsTextArea;
	private static Clustering clustering;
	private static Directory luceneIndex;
	
	private static ArrayList<Directory> clusterIndexes;
	
	public JTextField getSearchField()
	{
		return this.searchField;
	}
	
	public JLabel firstClusterResultsTextArea()
	{
		return this.firstClusterResultsTextArea;
	}
	
	public MainView()
	{
		super("WST640 - Project - Group b");
		
		JFrame frame = new JFrame("WST640 - Project - Group b");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		this.firstClusterResultsTextArea = new JLabel();	
		firstClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		firstClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		firstClusterResultsTextArea.setOpaque(true);
		firstClusterResultsTextArea.setBackground(Color.red);

		frame.getContentPane().add(firstClusterResultsTextArea, BorderLayout.LINE_START);
		firstClusterResultsTextArea.setBackground(Color.red);
		
		secondClusterResultsTextArea = new JLabel();
		secondClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		secondClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		secondClusterResultsTextArea.setBackground(Color.blue);
		secondClusterResultsTextArea.setOpaque(true);
		secondClusterResultsTextArea.setBackground(Color.blue);

		frame.getContentPane().add(secondClusterResultsTextArea, BorderLayout.CENTER);

		thirdClusterResultsTextArea = new JLabel();	
		thirdClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		thirdClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		
		frame.getContentPane().add(thirdClusterResultsTextArea, BorderLayout.LINE_END);
		thirdClusterResultsTextArea.setOpaque(true);
		thirdClusterResultsTextArea.setBackground(Color.green);

		searchField = new JTextField(10);
		frame.getContentPane().add(searchField, BorderLayout.PAGE_START);
	    searchField.setHorizontalAlignment(JTextField.CENTER);
		searchField.getDocument().addDocumentListener(this);

		frame.pack();
		frame.setSize(800, 600);
		
		frame.setVisible(true);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		//new MainView();
//		indexTrec = new TrecIndexer();
//		Directory index = indexTrec.startIndexingFiles();
//		ArrayList<Document> searchResult = indexTrec.search("william");
		//Document document = searchResult.get(0);
		//System.out.println(searchResult.get(0).getField("fullContent"));
//		System.out.println(indexTrec.search("william"));
		
		/*
		 * Create a new Clustering object and start the lucene indexing.
		 * Test the time it takes to cluster. 
		 */

		clustering = new Clustering();
		
		long startTime = System.currentTimeMillis();
		
		luceneIndex = clustering.startLuceneIndexing();
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println(elapsedTime / 1000 + " seconds to index");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter search query, press enter for suggestions: ");
        String s = br.readLine();
        System.out.println("Finding suggestions for " + s);
        get_suggestions(s);
        
        
//		clustering.createClustersWithoutQuery();
//		clusterIndexes = clustering.createLuceneIndexesFromClusters();
//		
	}
	
	/*
	 * Invoked whenever the user types in the search field.
	 */
	public void typed()
	{
		/*
		 * Get the current text from the search field and start searching for relevant documents.
		 * When the relevant documents are retrieved, start using Carrot2 to cluster the documents
		 * in relation to the query. 
		 * When the clusters are found, calculate TFIDF for each term in the clusters, and show 
		 * them to the user.
		 */
		String searchString = searchField.getText();
		if (searchString.length() > 2) {
			long startTime = System.currentTimeMillis();

			ArrayList<Document> documentsResults = clustering.searchForDocuments(searchString, luceneIndex);
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println(elapsedTime + " milliseconds to search for relevant documents");
			
			long clusteringStartTime = System.currentTimeMillis();
			ArrayList<ArrayList<org.apache.lucene.document.Document>> clusteringResults = clustering.startClusteringWithResults(documentsResults, searchString);
			
			long clusteringStopTime = System.currentTimeMillis();
			long clusteringElapsedTime = clusteringStopTime - clusteringStartTime;
			System.out.println(clusteringElapsedTime + " milliseconds for Carrot2 to create clusters in relation to query");

			
			long calculateTFIDFStartTime = System.currentTimeMillis();
			TermWeighting termWeighting = new TermWeighting();
			ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = termWeighting.calculateTFIDFForClusters(clusteringResults, "tf");
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
		
	  //clustering.startClusteringWithQuery(valueTypedByUser);

	  //clustering.searchClustersFromGeneratedLuceneClusters(valueTypedByUser, clusterIndexes);
	  
	  firstClusterResultsTextArea.setText("Result from first cluster");
	  secondClusterResultsTextArea.setText("Result from second cluster");
	  thirdClusterResultsTextArea.setText("Result from third cluster");
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
			ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = termWeighting.calculateTFIDFForClusters(clusteringResults, "tfidf");
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
		
	  //clustering.startClusteringWithQuery(valueTypedByUser);

	  //clustering.searchClustersFromGeneratedLuceneClusters(valueTypedByUser, clusterIndexes);
	  
	}
	
	@Override
	public void insertUpdate(javax.swing.event.DocumentEvent e) {
		this.typed();
	}

	@Override
	public void removeUpdate(javax.swing.event.DocumentEvent e) {
		this.typed();
	}

	@Override
	public void changedUpdate(javax.swing.event.DocumentEvent e) {
		this.typed();
	}
}

