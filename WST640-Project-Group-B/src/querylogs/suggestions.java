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
	
	/*
	 * Method for returning suggestions based on a query. Suggestions found from a query log file
	 */
	
	public static TreeMap<String, Float> getSuggestions(String query){
		TreeMap<String, Float> word_counts = new TreeMap<String, Float>();
		try{
			File sub_file = null;
			//get path of the query log collection
			if (Clustering.isWindows()) {
				sub_file = new File("E:\\Dropbox\\Dataset\\user-ct-test-collection-01.txt");
			} else if (Clustering.isMac()) {
				sub_file = new File("/Users/wingair/Dropbox/Dataset/user-ct-test-collection-01.txt");
			}
			
			BufferedReader in = new BufferedReader(new FileReader(sub_file.getAbsolutePath()));
			String content;
			//split the query by space
			String[] splited_query = query.split("\\s+");
			String regex = "";
			int counter = 0;
			
			//create the regex looking for the individual terms of the query
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
			//read every line in the query log file
			while ((content = in.readLine()) != null) {
				//get the lines that contain the query tokens
				if(content.matches(regex)){
					//get the specific query string
					query_log_string = content.split("\\t+")[1];
					//make sure all entries are new entries
					if(!query_log_string.equals(prev_query_log)){
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
			}
			in.close();
		}catch(Exception e){
			System.out.println(e);
		}
		
		//return normalized query log scores
		return normalize_treemap(word_counts);
	}
	
	/*
	 * Method for normalizing the query log scores
	 */
	public static TreeMap<String, Float> normalize_treemap(TreeMap<String, Float> treemap){
		float total_weight = 0;
		//get total value of all tokens found in the query log
		for(Map.Entry<String,Float> entry : treemap.entrySet()) {
			total_weight += entry.getValue();
		}
		//calculate individual token score
		for(Map.Entry<String,Float> entry : treemap.entrySet()) {
			entry.setValue(entry.getValue()/total_weight);
		}
		
		return treemap;
	}
}
