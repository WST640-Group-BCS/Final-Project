package querylogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import clustering.Clustering;

public class suggestions {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String test = "benjamin bling";
		getSuggestions(test);
		
	}
	
	
	
	public static TreeMap<String, Float> getSuggestions(String query){
		TreeMap<String, Float> word_counts = new TreeMap<String, Float>();
		try{
			String path_to_trec = "";
			File sub_file = null;
			if (Clustering.isWindows()) {
				sub_file = new File("E:\\Dropbox\\Dataset\\user-ct-test-collection-01.txt");
			} else if (Clustering.isMac()) {
				sub_file = new File("/Users/wingair/Dropbox/Dataset/user-ct-test-collection-01.txt");
			}
			
			StringBuilder builder = new StringBuilder();
			//BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(sub_file.getAbsolutePath()))));
			BufferedReader in = new BufferedReader(new FileReader(sub_file.getAbsolutePath()));
			String content;
			//System.out.println(new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory).getName() + ":");
			String[] splited_query = query.split("\\s+");
			String regex = "";
			int counter = 0;
			for (String token:splited_query){
				if(counter == 0){
					regex = "(.*)" + token + "(.*)";
				}
				if(counter>0){
					regex += "|(.*)" + token + "(.*)";
				}
				counter += 1;
			}
			
			String prev_query_log = "";
			String query_log_string = "";
			while ((content = in.readLine()) != null) {
				//get the lines that contain the query tokens
				if(content.matches(regex)){
					//get the specific query string
					query_log_string = content.split("\\t+")[1];
					//make sure all entries are new entries
					if(!query_log_string.equals(prev_query_log)){
						//System.out.println("content " + query_log_string + " prevlog: " + prev_query_log + " boolean: " + (!query_log_string.equals(prev_query_log)));
						String[] query_log_tokens = query_log_string.split("\\s+");
						for(String token: query_log_tokens){
							//avoid the tokens that the query consists of
							if(!Arrays.asList(splited_query).contains(token)){
								try{
									Float word_count = word_counts.get(token);
									word_counts.put(token, word_count+1);
								}catch(Exception e){
									word_counts.put(token, (float) 1);
								}
							}
						}
					}
				}
				
				prev_query_log = query_log_string;
				//builder.append(content);
			}
			
			System.out.println(entriesSortedByValues(word_counts));
			String sub_file_text = builder.toString();
			
			in.close();
		}catch(Exception e){
			System.out.println(e);
		}
		
		//System.out.println("original: " + word_counts.get("franklin") + " normalized: " + normalize_treemap(word_counts).get("franklin"));
		
		return normalize_treemap(word_counts);
	}
	
	public static TreeMap<String, Float> normalize_treemap(TreeMap<String, Float> treemap){
		float total_weight = 0;
		for(Map.Entry<String,Float> entry : treemap.entrySet()) {
			total_weight += entry.getValue();
		}
		for(Map.Entry<String,Float> entry : treemap.entrySet()) {
			entry.setValue(entry.getValue()/total_weight);
		}
		
		return treemap;
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
