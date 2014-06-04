
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2014, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.synthetic.ByUrlClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.IDocumentSource;
import org.carrot2.core.ProcessingResult;
import org.carrot2.examples.ConsoleFormatter;

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
public class clustering
{
	private static String OS = System.getProperty("os.name").toLowerCase();
    public static void main(String [] args)
    {
    	try{
        
            final ArrayList<Document> documents = new ArrayList<Document>();
            
            String path_to_trec = "";
			if (isWindows()) {
				path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";	
			} else if (isMac()) {
				path_to_trec = "/Users/wingair/Dropbox/Dataset/WT10G/";	
			}
			

			int number_of_documents_to_index = 20;
            
            

			String symbol = "";
			if (isWindows()) {
				symbol = "\\";	
			} else if (isMac()) {
				symbol = "/";
			}
			int counter = 0;
			
			File file = new File(path_to_trec);
			String[] wtx_folders = file.list();
			for (String wtx_folder : wtx_folders) {
				// excluding the info folder
				if ((new File(path_to_trec + symbol + wtx_folder).isDirectory())
						&& !(new File(path_to_trec + symbol + wtx_folder).getName().equals("info"))) {
					if (counter < number_of_documents_to_index) {
						System.out.println(new File(path_to_trec + symbol + wtx_folder).getName());
						String[] sub_directories = new File(path_to_trec + symbol + wtx_folder).list();
						for (String sub_directory : sub_directories) {
							if (counter < number_of_documents_to_index) {

								StringBuilder builder = new StringBuilder();

								File sub_file = new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory);
								System.out.println(sub_file.getAbsolutePath());
								BufferedReader in;
								in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(sub_file.getAbsolutePath()))));
								String content;
								System.out.println(new File(path_to_trec + symbol
										+ wtx_folder + symbol + sub_directory)
								.getName() + ":");

								while ((content = in.readLine()) != null) {
									builder.append(content);
								}

								String sub_file_text = builder.toString();

								String docno_pattern = "(<DOCNO>(.*?)</DOCNO>)(?<DOC>(.*?)</DOC>)";

								Pattern docno_r = Pattern.compile(docno_pattern);
								Matcher docno_m = docno_r.matcher(sub_file_text);
								while (docno_m.find()) {
									if (counter < number_of_documents_to_index) {
										String doc_no = docno_m.group(2);
										String doc_content = docno_m.group(3);
										//addDoc(w, doc_content, doc_no);
										
										//Document doc = new Document();
							            //doc.add(new TextField("fullContent", doc_content, Store.YES));
							            //doc.add(new TextField("title", doc_no, Store.YES));
										documents.add(new Document(doc_no, doc_content, doc_content));
									}
									counter += 1;
								}
							}
						}
					}
				}

			}
            
            
            
            
            
            
            /* A controller to manage the processing pipeline. */
            final Controller controller = ControllerFactory.createSimple();

            /*
             * Perform clustering by topic using the Lingo algorithm. Lingo can 
             * take advantage of the original query, so we provide it along with the documents.
             */
            //final ProcessingResult byTopicClusters = controller.process(documents, "data mining",LingoClusteringAlgorithm.class);
            final ProcessingResult byTopicClusters = controller.process(documents, "data mining",BisectingKMeansClusteringAlgorithm.class);
            //byTopicClusters.get
            final List<Cluster> clustersByTopic = byTopicClusters.getClusters();
            Cluster hej  = clustersByTopic.get(1);
            
            /* Perform clustering by domain. In this case query is not useful, hence it is null. */
            //final ProcessingResult byDomainClusters = controller.process(documents, null, ByUrlClusteringAlgorithm.class);
            //final List<Cluster> clustersByDomain = byDomainClusters.getClusters();
            // [[[end:clustering-document-list]]]
            
            //ConsoleFormatter.displayClusters(clustersByTopic);
            
            Iterator clustersByTopicIterator = clustersByTopic.iterator();
            
            //iterating through all clusters
            while(clustersByTopicIterator.hasNext()) {
            	System.out.println(clustersByTopicIterator.next());
                Cluster cluster = (Cluster) clustersByTopicIterator.next();
                List<Document> document_clusters = cluster.getAllDocuments();
                
                ArrayList<Document> cluster_documents_list = new ArrayList<>();
                for(Document doc: document_clusters){
                	
                	FieldType type = new FieldType();
            		type.setIndexed(true);
            		type.setStored(true);
            		type.setStoreTermVectors(true);
            		
            		Document doc_to_insert = new Document();
                    Field field = new Field("body", doc.getContentUrl(), type);
                    //doc_to_insert.add(field);
                    cluster_documents_list.add(doc_to_insert);
                    
                    
                	
                }
                //System.out.println(element.getClass());
                //System.out.print(element.toString());
                //
             }
            
            
            
            //ConsoleFormatter.displayClusters(clustersByDomain);
       

    	} catch (Exception e) {
    		
    	}
    }
    
    public static boolean isWindows() {
		 
		return (OS.indexOf("win") >= 0);
 
	}
 
	public static boolean isMac() {
 
		return (OS.indexOf("mac") >= 0);
 
	}
}
