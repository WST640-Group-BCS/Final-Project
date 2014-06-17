package query.cluster.association;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
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
import org.apache.lucene.document.Field.Store;

import querylogs.suggestions;


public class TermWeighting {
	static Version luceneVersion = Version.LUCENE_46;
	
	public int calculateDocumentScoreAccordingToQuery(String query, String documentTitle)
	{
		return 0;
	}
	
	public ArrayList<NavigableSet<Map.Entry<String, Float>>> calculateTFIDFForClusters(ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersInLuceneDocuments, String term_weight_type, String searchString) {		
		ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = new ArrayList<NavigableSet<Map.Entry<String, Float>>>();
		if(term_weight_type == "tfidf"){
			termClustersList = calculate_tfidf(clustersInLuceneDocuments, searchString);
		}
		else if(term_weight_type == "tf"){
			for (ArrayList<Document> cluster : clustersInLuceneDocuments) {
				TreeMap<String, Float> tf_weights = get_important_words(cluster, "tf", searchString);
				NavigableSet<Map.Entry<String, Float>> set = entriesSortedByValues(tf_weights);
	
				termClustersList.add(set);
			}
		}
		return termClustersList;
	}
	
	public ArrayList<NavigableSet<Map.Entry<String, Float>>> calculate_tfidf(ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersInLuceneDocuments, String searchString){
		TreeMap<String, Float> idf_weights = calculate_idf_weights(clustersInLuceneDocuments);
		ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = new ArrayList<NavigableSet<Map.Entry<String, Float>>>(); 
		try {
			System.out.println("-- analyzing query logs --");
			TreeMap<String, Float> query_log_treemap = suggestions.getSuggestions(searchString);
			for(ArrayList<org.apache.lucene.document.Document> cluster: clustersInLuceneDocuments){
				//Calculating idf weights
				Directory index = new RAMDirectory();
				File stopwords = new File("src/stopwords.en");
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
			    
			    float total_weight = 0;
			    
			    while ((byteRef = iterator.next()) != null)
			    {
			    	String term = byteRef.utf8ToString();
			        float idf_weight = idf_weights.get(term);
				    Term termInstance = new Term("body", term);
				    long total_term_Freq = index_reader.totalTermFreq(termInstance);
				    float tfidf_weight = (float) ((1 + Math.log10(total_term_Freq)) * idf_weight);
				    total_weight += tfidf_weight;
				}
			    
			    iterator = ld.getEntryIterator();
			    byteRef = null;
			    
			    while ((byteRef = iterator.next()) != null)
			    {
			        String term = byteRef.utf8ToString();
			        float idf_weight = idf_weights.get(term);
				    Term termInstance = new Term("body", term);
				    long total_term_Freq = index_reader.totalTermFreq(termInstance);
				    float tfidf_weight = (float) ((1 + Math.log10(total_term_Freq)) * idf_weight);
				    float normalized_tfidf = tfidf_weight/total_weight;
				    //normalizing scores
				    
			        try{
			    		float query_log_score = query_log_treemap.get(term);
			    		float query_log_tfidf_weight = normalized_tfidf + query_log_score/10;
			    		System.out.println(term + " found, old score: " + normalized_tfidf + " new score: " + query_log_tfidf_weight);
			    		term_tfidf.put(term, query_log_tfidf_weight);
			    	}catch(Exception e){
			    		//System.out.println(term + " not found");
			    		term_tfidf.put(term, normalized_tfidf);
			    	}
				    
				    //System.out.println("term: " + term + ". Total occ: " + total_term_Freq);  
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
			
		    //iterating through all terms in the collection
		    LuceneDictionary ld = new LuceneDictionary( index_reader, "body" );
		    BytesRefIterator iterator = ld.getEntryIterator();
		    BytesRef byteRef = null;
		    
		    term_idf = new TreeMap<String, Float>();
		    
		    
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
			    Term termInstance = new Term("body", term);
			    long total_term_Freq = index_reader.totalTermFreq(termInstance);
			    float doc_freq = index_reader.docFreq(termInstance);
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
	
	public TreeMap<String, Float> get_important_words(ArrayList<Document> documents, String weight_type, String searchString){
		TreeMap<String, Float> query_log_treemap = suggestions.getSuggestions(searchString);
	    TreeMap<String, Float> idf_weights = new TreeMap<>();
	    TreeMap<String, Float> df_weights = new TreeMap<>();
		try {
			Directory index = new RAMDirectory();
			
			File stopwords = new File("src/stopwords.en");
			Reader reader = new FileReader(stopwords.getAbsolutePath());
			StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion, reader);
			
			IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
			IndexWriter w = new IndexWriter(index, config);
			
			float N = documents.size();
			for (Document doc: documents){
	            w.addDocument(doc);
			}
			w.close();
			
			IndexReader index_reader = DirectoryReader.open(index);
			
		    //iterating through all terms in the collection
		    LuceneDictionary ld = new LuceneDictionary( index_reader, "body" );
		    
		    BytesRefIterator iterator = ld.getEntryIterator();
		    BytesRef byteRef = null;
		    
		    float total_idf_weight = 0;
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
			    Term termInstance = new Term("body", term);
			    float doc_freq = index_reader.docFreq(termInstance);
			    float idf_weight = (float) Math.log10(N/doc_freq);
			    total_idf_weight += idf_weight;
			    //System.out.println("term: " + term + ". Total occ: " + total_term_Freq + ". Doc freq: " +  doc_freq + ". idf: " + idf_weight);  
			 }
		    
		    iterator = ld.getEntryIterator();
		    byteRef = null;
		    
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
			    Term termInstance = new Term("body", term);
			    long total_term_Freq = index_reader.totalTermFreq(termInstance);
			    float doc_freq = index_reader.docFreq(termInstance);
			    df_weights.put(term, doc_freq);
			    float idf_weight = (float) Math.log10(N/doc_freq);
			    float normalized_idf_weight = idf_weight/total_idf_weight;
			    try{
			    	float query_log_score = query_log_treemap.get(term);
		    		float query_log_tfidf_weight = normalized_idf_weight + query_log_score/10;
		    		System.out.println(term + " found, old score: " + normalized_idf_weight + " new score: " + query_log_tfidf_weight);
			    	df_weights.put(term, query_log_tfidf_weight);
			    }catch(Exception e){
			    	idf_weights.put(term, normalized_idf_weight);
			    }
			    
			    //System.out.println("term: " + term + ". Total occ: " + total_term_Freq + ". Doc freq: " +  doc_freq + ". idf: " + idf_weight);  
			 }
		    index_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(weight_type == "idf"){
			return idf_weights;
		}else if (weight_type == "tf") {
			return df_weights;
		}
		return null;
	}
	
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
