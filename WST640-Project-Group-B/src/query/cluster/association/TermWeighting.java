package query.cluster.association;
import java.io.IOException;
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
	
	public ArrayList<NavigableSet<Map.Entry<String, Float>>> calculateTFIDFForClusters(ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersInLuceneDocuments) {		
		ArrayList<NavigableSet<Map.Entry<String, Float>>> termClustersList = new ArrayList<NavigableSet<Map.Entry<String, Float>>>();  
		for (ArrayList<Document> cluster : clustersInLuceneDocuments) {
//			TreeMap<String, Float> idf_weights = get_important_words(cluster, "idf");
//			termClustersList.add(idf_weights);
			TreeMap<String, Float> tf_weights = get_important_words(cluster, "tf");
			NavigableSet<Map.Entry<String, Float>> set = entriesSortedByValues(tf_weights);

			termClustersList.add(set);
		}
		return termClustersList;
	}
	
	public TreeMap<String, Float> get_important_words(ArrayList<Document> documents, String weight_type){

	    TreeMap<String, Float> idf_weights = new TreeMap<>();
	    TreeMap<String, Float> df_weights = new TreeMap<>();
		try {
			Directory index = new RAMDirectory();
			CharArraySet luceneStopwords = getStopwords();

			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46, luceneStopwords);

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
	
	public CharArraySet getStopwords()
	{
		CharArraySet luceneStopwords = new CharArraySet(Version.LUCENE_46, 20, true); 
		luceneStopwords.add("1.0");
		luceneStopwords.add("200");
		luceneStopwords.add("80");
		luceneStopwords.add("000001");
		luceneStopwords.add("text");
		luceneStopwords.add("a");
		luceneStopwords.add("and");
		luceneStopwords.add("body");
		luceneStopwords.add("doc");
		luceneStopwords.add("dochdr");
		luceneStopwords.add("docoldno");
		luceneStopwords.add("for");
		luceneStopwords.add("html");
		luceneStopwords.add("http");
		luceneStopwords.add("ia001");
		luceneStopwords.add("the");
		luceneStopwords.add("to");
		luceneStopwords.add("type");
		luceneStopwords.add("01");
		luceneStopwords.add("head");
		luceneStopwords.add("in");
		luceneStopwords.add("of");
		luceneStopwords.add("p");
		luceneStopwords.add("title");
		luceneStopwords.add("on");
		luceneStopwords.add("by");
		luceneStopwords.add("is");
		luceneStopwords.add("as");
		luceneStopwords.add("with");
		luceneStopwords.add("an");
		luceneStopwords.add("length");
		luceneStopwords.add("this");
		luceneStopwords.add("at");
		luceneStopwords.add("from");
		luceneStopwords.add("modified");
		luceneStopwords.add("are");
		luceneStopwords.add("be");
		luceneStopwords.add("57.0");
		luceneStopwords.add("center");
		luceneStopwords.add("have");
		luceneStopwords.add("or");
		luceneStopwords.add("their");
		luceneStopwords.add("b");
		luceneStopwords.add("gmt");
		luceneStopwords.add("src");
		luceneStopwords.add("or");
		luceneStopwords.add("doctype");
		luceneStopwords.add("or");
		luceneStopwords.add("href");
		luceneStopwords.add("dtd");
		luceneStopwords.add("en");
		luceneStopwords.add("gmtcontent");
		luceneStopwords.add("htmlcontent");
		luceneStopwords.add("img");
		luceneStopwords.add("okserver");
		luceneStopwords.add("000000");
		luceneStopwords.add("that");
		luceneStopwords.add("has");
		luceneStopwords.add("which");
		luceneStopwords.add("i");
		luceneStopwords.add("it");
		luceneStopwords.add("2");
		luceneStopwords.add("name");
		luceneStopwords.add("other");
		luceneStopwords.add("also");
		luceneStopwords.add("more");
		luceneStopwords.add("will");
		luceneStopwords.add("all");
		luceneStopwords.add("1");
		luceneStopwords.add("jan");
		luceneStopwords.add("br");
		luceneStopwords.add("size");
		luceneStopwords.add("3");
		luceneStopwords.add("5");
		luceneStopwords.add("if");
		luceneStopwords.add("00");
		luceneStopwords.add("netscape");
		luceneStopwords.add("1.0.1content");
		luceneStopwords.add("1996");
		luceneStopwords.add("1997");
		luceneStopwords.add("hr");
		luceneStopwords.add("gmtserver");
		luceneStopwords.add("okdate");
		luceneStopwords.add("gif");
		luceneStopwords.add("aads");
		luceneStopwords.add("apache");
		luceneStopwords.add("font");
		luceneStopwords.add("ul");
		luceneStopwords.add("banner.gif");
		luceneStopwords.add("35.1.1.47");
		luceneStopwords.add("content");


		return luceneStopwords;
	}
}
