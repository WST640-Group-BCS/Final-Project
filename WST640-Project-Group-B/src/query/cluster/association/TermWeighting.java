package query.cluster.association;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
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


public class TermWeighting {
	static Version luceneVersion = Version.LUCENE_46;
	
	public int calculateDocumentScoreAccordingToQuery(String query, String documentTitle)
	{
		
		return 0;
	}
	
	public static void main(String[] args) {
		ArrayList<Document> documents = new ArrayList<>();
		
		//What is this?
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setStored(true);
		type.setStoreTermVectors(true);
		
		
		Document doc1 = new Document();
        Field field = new Field("body", "content 1 this is the hej text new text", type);
        doc1.add(field);
        documents.add(doc1);
        Document doc2 = new Document();
        Field field2 = new Field("body", "content 2 Benjamin hej hej hej text", type);
        doc2.add(field2);
        documents.add(doc2);
        Document doc3 = new Document();
        Field field3 = new Field("body", "content 2 speciel speciel speciel speciel speciel hej hej hej text", type);
        doc3.add(field3);
        documents.add(doc3);
         
		TreeMap<String, Float> idf_weights = get_important_words(documents, "idf");
		System.out.println(entriesSortedByValues(idf_weights));
		
		TreeMap<String, Float> tf_weights = get_important_words(documents, "tf");
		System.out.println(entriesSortedByValues(tf_weights));
	}
	
	public static TreeMap<String, Float> get_important_words(ArrayList<Document> documents, String weight_type){

	    TreeMap<String, Float> idf_weights = new TreeMap<>();
	    TreeMap<String, Float> df_weights = new TreeMap<>();
		try {
			Directory index = new RAMDirectory();
			StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion);
			
			IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
			IndexWriter w = new IndexWriter(index, config);
			
			float N = documents.size();
			for (Document doc: documents){
	            w.addDocument(doc);
			}
			w.close();
			
			IndexReader reader = DirectoryReader.open(index);
			
		    //iterating through all terms in the collection
		    LuceneDictionary ld = new LuceneDictionary( reader, "body" );
		    BytesRefIterator iterator = ld.getEntryIterator();
		    BytesRef byteRef = null;
		    
		    while ((byteRef = iterator.next()) != null)
		    {
		        String term = byteRef.utf8ToString();
		        
			    Term termInstance = new Term("body", term);      
			    long total_term_Freq = reader.totalTermFreq(termInstance);
			    float doc_freq = reader.docFreq(termInstance);
			    df_weights.put(term, doc_freq);
			    float idf_weight = (float) Math.log10(N/doc_freq);
			    idf_weights.put(term, idf_weight);
			    //System.out.println("term: " + term + ". Total occ: " + total_term_Freq + ". Doc freq: " +  doc_freq + ". idf: " + idf_weight);  
			 }
		    reader.close();
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
	
	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                    int res = e1.getValue().compareTo(e2.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
	
	
}
