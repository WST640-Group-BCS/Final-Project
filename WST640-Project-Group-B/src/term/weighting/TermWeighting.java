package term.weighting;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.Version;
//import org.carrot2.core.Document;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.document.Document;

import querylogs.suggestions;


public class TermWeighting {
	static Version luceneVersion = Version.LUCENE_46;
		
	/*
	 * The method used to determine which weighting scheme to use for the terms in the clusters.
	 */
	public ArrayList<NavigableSet<Map.Entry<String, Float>>> calculateTFIDFForClusters(ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersInLuceneDocuments, String term_weight_type, String searchString, Boolean queryLog) {		
		ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = new ArrayList<NavigableSet<Map.Entry<String, Float>>>();
		if(term_weight_type == "tfidf"){
			termClustersList = calculate_tfidf(clustersInLuceneDocuments, searchString, queryLog);
		}
		else if(term_weight_type == "df"){
			//Get all suggestions from the quer log
			TreeMap<String, Float> query_log_treemap = suggestions.getSuggestions(searchString);
			//run through every found cluster
			for (ArrayList<Document> cluster : clustersInLuceneDocuments) {
				TreeMap<String, Float> tf_weights = get_important_words(cluster, "df", searchString, query_log_treemap, queryLog);
				//sort the map
				NavigableSet<Map.Entry<String, Float>> set = entriesSortedByValues(tf_weights);
				termClustersList.add(set);
			}
		}
		return termClustersList;
	}
	
	
	/*
	 * Method that takes the documents of all clusters and finds the tokens with highest tfidf value together with query log weighting
	 */
	public ArrayList<NavigableSet<Map.Entry<String, Float>>> calculate_tfidf(ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersInLuceneDocuments, String searchString, Boolean queryLog){
		//Get the idf weights of all tokens
		TreeMap<String, Float> idf_weights = calculate_idf_weights(clustersInLuceneDocuments);
		ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = new ArrayList<NavigableSet<Map.Entry<String, Float>>>(); 
		try {
			System.out.println("-- analyzing query logs --");
			//get token weight from query log
			TreeMap<String, Float> query_log_treemap = suggestions.getSuggestions(searchString);
			//run through all found cluster
			for(ArrayList<org.apache.lucene.document.Document> cluster: clustersInLuceneDocuments){
				//Calculating idf weights
				Directory index = new RAMDirectory();
				File stopwords = new File("src/stopwords.en");
				//use words found in stopwords.en as stopwords in the indexer
				Reader reader = new FileReader(stopwords.getAbsolutePath());
				StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion, reader);
				IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
				IndexWriter w = new IndexWriter(index, config);
				//iterating through all documents in all clusters
				for (Document doc: cluster){
		            w.addDocument(doc);
				}
				
				w.close();
				
				IndexReader index_reader = DirectoryReader.open(index);
				float number_of_documents = index_reader.numDocs();
				
			    //iterating through all terms in the collection
			    LuceneDictionary ld = new LuceneDictionary( index_reader, "body" );
			    BytesRefIterator iterator = ld.getEntryIterator();
			    BytesRef byteRef = null;
			    
			    TreeMap<String, Float> term_tfidf = new TreeMap<String, Float>();
			    
			    //used for setting total idf weight
			    float total_weight = 0;
			    
			    //used for normalizing the tfidf weights
			    while ((byteRef = iterator.next()) != null)
			    {
			    	String term = byteRef.utf8ToString();
			    	//get the idf weight for the specific term
			        float idf_weight = idf_weights.get(term);
				    Term termInstance = new Term("body", term);
				    //get term freq from the indexReader
				    long total_term_Freq = index_reader.totalTermFreq(termInstance);
				    float tfidf_weight = (float) ((1 + Math.log10(total_term_Freq)) * idf_weight);
				    total_weight += tfidf_weight;
				}
			    
			    iterator = ld.getEntryIterator();
			    byteRef = null;
			    
			    //calculating the individual term tfidf score
			    while ((byteRef = iterator.next()) != null)
			    {
			        String term = byteRef.utf8ToString();
			        float idf_weight = idf_weights.get(term);
				    Term termInstance = new Term("body", term);
				    long total_term_Freq = index_reader.totalTermFreq(termInstance);
				    float tfidf_weight = (float) ((1 + Math.log10(total_term_Freq)) * idf_weight);
				    //normalize tfidf score
				    float normalized_tfidf = tfidf_weight/total_weight;
			        try{
			        	float query_log_tfidf_weight;
			        	//check if the query log should be enabled
			    		if (queryLog) {
			    			//try if the term is found from query log
				    		float query_log_score = query_log_treemap.get(term);
				    		//define beta
				    		float beta = 1/10;
				    		//calculate weight with query log score
				    		query_log_tfidf_weight = normalized_tfidf + beta * query_log_score;
						} else {
				    		query_log_tfidf_weight = normalized_tfidf;
						}
			    		term_tfidf.put(term, query_log_tfidf_weight);
			    	}catch(Exception e){
						//if term not found simply return normalized tfidf
			    		term_tfidf.put(term, normalized_tfidf);
			    	}
				    
				}
			    index_reader.close();
		    
				NavigableSet<Map.Entry<String, Float>> set = entriesSortedByValues(term_tfidf);
				termClustersList.add(set);
			}
		    
		} catch (IOException e) {
			e.printStackTrace();
		}
		return termClustersList;
	}
	
	/*
	 * Method for calculating idf weights
	 */
	
	public TreeMap<String, Float> calculate_idf_weights(ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersInLuceneDocuments){
		TreeMap<String, Float> term_idf = null;
		
		try {
			
			//Calculating idf weights
			Directory index = new RAMDirectory();
			File stopwords = new File("src/stopwords.en");
			Reader reader = new FileReader(stopwords.getAbsolutePath());
			StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion, reader);
			IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
			IndexWriter w = new IndexWriter(index, config);
			//iterating through all documents in all clusters to get df
			for(ArrayList<org.apache.lucene.document.Document> cluster: clustersInLuceneDocuments){
				StringBuilder builder = new StringBuilder();
				for (Document doc: cluster){
					builder.append(doc.get("body"));
				}
				FieldType type = new FieldType();
        		type.setIndexed(true);
        		type.setStored(true);
        		type.setStoreTermVectors(true);

                org.apache.lucene.document.Document luceneDocument = new org.apache.lucene.document.Document();
                luceneDocument.add(new Field("body", builder.toString(), type));
				w.addDocument(luceneDocument);
			}
			
			float N = clustersInLuceneDocuments.size();
			w.close();
			
			IndexReader index_reader = DirectoryReader.open(index);
			
		    
		    LuceneDictionary ld = new LuceneDictionary( index_reader, "body" );
		    BytesRefIterator iterator = ld.getEntryIterator();
		    BytesRef byteRef = null;
		    
		    term_idf = new TreeMap<String, Float>();
		    
		  //iterating through all terms in the collection
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
			    Term termInstance = new Term("body", term);
			    float doc_freq = index_reader.docFreq(termInstance);
			    //calculate idf weight
			    float idf_weight = (float) Math.log10(N/doc_freq);
			    //
			    term_idf.put(term, (float)idf_weight);
			    //System.out.println("term: " + term + ". Doc freq: " +  doc_freq + ". idf: " + idf_weight);  
			}
		    index_reader.close();
		    
		} catch (IOException e) {
			e.printStackTrace();
		}
		return term_idf;
	}
	
	/*
	 * Method for returning df weights
	 */
	
	public TreeMap<String, Float> get_important_words(ArrayList<Document> documents, String weight_type, String searchString,TreeMap<String, Float> query_log_treemap, Boolean queryLog){
		
	    TreeMap<String, Float> df_weights = new TreeMap<>();
		try {
			Directory index = new RAMDirectory();
			
			File stopwords = new File("src/stopwords.en");
			Reader reader = new FileReader(stopwords.getAbsolutePath());
			StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion, reader);
			
			IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
			IndexWriter w = new IndexWriter(index, config);
			
			for (Document doc: documents){
	            w.addDocument(doc);
			}
			w.close();
			
			IndexReader index_reader = DirectoryReader.open(index);
			
		    //iterating through all terms in the collection
		    LuceneDictionary ld = new LuceneDictionary( index_reader, "body" );
		    
		    BytesRefIterator iterator = ld.getEntryIterator();
		    BytesRef byteRef = null;
		    
		    float total_df_weight = 0;
		    //used for finding total df weights
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
			    Term termInstance = new Term("body", term);
			    float doc_freq = index_reader.docFreq(termInstance);
			    total_df_weight += doc_freq;
			 }
		    
		    iterator = ld.getEntryIterator();
		    byteRef = null;
		    
		    //find the individual 
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
			    Term termInstance = new Term("body", term);
			    //get document frequency
			    float doc_freq = index_reader.docFreq(termInstance);
			    float normalized_df_weight = doc_freq/total_df_weight;
			    //as before try and find term in query log and add the query log score
			    try{
		        	float query_log_df_weight;
		    		if (queryLog) {
				    	float query_log_score = query_log_treemap.get(term);
			    		query_log_df_weight = normalized_df_weight + query_log_score/10;
					} else {
						query_log_df_weight = normalized_df_weight;
					}

			    	df_weights.put(term, query_log_df_weight);
			    }catch(Exception e){
			    	df_weights.put(term, normalized_df_weight);
			    }
			 }
		    index_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (weight_type == "df") {
			return df_weights;
		}
		return null;
	}
	
	/*
	 * Method for sorting a map based on values
	 */
	
	static <K,V extends Comparable<? super V>> NavigableSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
		NavigableSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                @Override public int compare(Map.Entry<K,V> e2, Map.Entry<K,V> e1) {
                    int res = e1.getValue().compareTo(e2.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
