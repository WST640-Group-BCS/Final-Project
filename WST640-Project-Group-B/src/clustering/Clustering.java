
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2014, Dawid Weiss, Stanis��aw Osi��ski.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */
package clustering;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
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
public class Clustering
{
	private static String OS = System.getProperty("os.name").toLowerCase();
	private ArrayList<Document> documents;
	private ArrayList<ArrayList<org.apache.lucene.document.Document>> clustersWithLuceneDocuments;
	
	public ArrayList<ArrayList<org.apache.lucene.document.Document>> getClustersWithLuceneDocuments()
	{
		return clustersWithLuceneDocuments;
	}
	
	public ArrayList<Directory> createLuceneIndexesFromClusters() throws IOException
	{
		ArrayList<Directory> clusterIndexes = new ArrayList<Directory>();		
		
		for (ArrayList<org.apache.lucene.document.Document> cluster : this.clustersWithLuceneDocuments) {
			
			Directory index = new RAMDirectory();

			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			IndexWriter indexWriter = new IndexWriter(index, config);

			for (org.apache.lucene.document.Document document : cluster) {
				indexWriter.addDocument(document);
			}
			clusterIndexes.add(index);
			indexWriter.close();
		}	
		return clusterIndexes;
	}
	
	public Clustering()
	{
		
	}
	
	public void startIndexing() throws FileNotFoundException, IOException
	{
        this.documents = new ArrayList<Document>();
        
        String path_to_trec = "";
		if (isWindows()) {
			path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";	
		} else if (isMac()) {
			path_to_trec = "/Users/wingair/Dropbox/Dataset/WT10G/";	
		}
		
		
		String symbol = "";
		if (isWindows()) {
			symbol = "\\";	
		} else if (isMac()) {
			symbol = "/";
		}
		int numberOfFoldersToUse = 1;
		int numberOfFilesToIndex = 10;
		int numberOfDOCTagsToIndexInONEFile = 30;

		int numberOfDOCTagIndexing = 0;
		int numberOfFilesIndexing = 0;
		int numberOfFolderUsing = 0;
		
		File file = new File(path_to_trec);
		String[] wtx_folders = file.list();
		
		/*
		 * Find the correct folders and files to use.
		 */
		for (String wtx_folder : wtx_folders) {
			if ((new File(path_to_trec + symbol + wtx_folder).isDirectory())) {
				if (numberOfFolderUsing < numberOfFoldersToUse) {
					System.out.println("Using the folder: " + new File(path_to_trec + symbol + wtx_folder).getName());
					String[] sub_directories = new File(path_to_trec + symbol + wtx_folder).list();
					for (String sub_directory : sub_directories) 
					{
						if (numberOfFilesIndexing < numberOfFilesToIndex) 
						{

							StringBuilder builder = new StringBuilder();

							File sub_file = new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory);
							
							System.out.println("Using the specific path: " + sub_file.getAbsolutePath());
							BufferedReader bufferedReader;
							bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(sub_file.getAbsolutePath()))));
							String content;
							
							System.out.println("Indexing the specific file: " + 
							new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory).getName() + ":");

							while ((content = bufferedReader.readLine()) != null) {
								builder.append(content);
							}
							bufferedReader.close();
							
							String sub_file_text = builder.toString();

							String docno_pattern = "(<DOCNO>(.*?)</DOCNO>)(?<DOC>(.*?)</DOC>)";

							Pattern docno_r = Pattern.compile(docno_pattern);
							Matcher docno_m = docno_r.matcher(sub_file_text);
							while (docno_m.find()) {
								if (numberOfDOCTagIndexing < numberOfDOCTagsToIndexInONEFile) {
									String doc_no = docno_m.group(2);
									System.out.println("Indexing a <DOC> tag, with the title: " + doc_no);
									String doc_content = docno_m.group(3);
									
									this.documents.add(new Document(doc_no, doc_content, doc_content));
								}
								numberOfDOCTagIndexing += 1;
							}
							numberOfFilesIndexing += 1;
						}
						numberOfDOCTagIndexing = 0;
					}
					numberOfFolderUsing += 1;
				}
			}
		}            

	}
		
	public void startClusteringWithQuery(String query)
    {
    	try{    
            final Controller controller = ControllerFactory.createSimple();
                        
            final ProcessingResult byTopicClusters = controller.process(documents, query, STCClusteringAlgorithm.class);
            
            final List<Cluster> clustersByTopic = byTopicClusters.getClusters();  
            
            System.out.println("Number of Clusters Made: " + clustersByTopic.size());
            
//            Cluster firstCluster = clustersByTopic.get(0);
//            List<Document> documentsOfFirstCluster = firstCluster.getDocuments();
            
//            for (Document document : documentsOfFirstCluster)
//            {
//                System.out.println(document.getScore());
//            }
            
            /* Perform clustering by domain. In this case query is not useful, hence it is null. */
            //final ProcessingResult byDomainClusters = controller.process(documents, null, ByUrlClusteringAlgorithm.class);
            //final List<Cluster> clustersByDomain = byDomainClusters.getClusters();
            // [[[end:clustering-document-list]]]
            
            //ConsoleFormatter.displayClusters(clustersByTopic);
            
            Iterator<Cluster> clustersByTopicIterator = clustersByTopic.iterator();
            
            //iterating through all clusters
            this.clustersWithLuceneDocuments = new ArrayList<ArrayList<org.apache.lucene.document.Document>>();  
            while(clustersByTopicIterator.hasNext()) {
            	System.out.println(clustersByTopicIterator.next());
                Cluster cluster = (Cluster) clustersByTopicIterator.next();
                List<Document> document_clusters = cluster.getAllDocuments();
                
                ArrayList<org.apache.lucene.document.Document> cluster_documents_list = new ArrayList<>();
                for(Document doc: document_clusters){
                	
                	FieldType type = new FieldType();
            		type.setIndexed(true);
            		type.setStored(true);
            		type.setStoreTermVectors(true);
            		
            		org.apache.lucene.document.Document doc_to_insert = new org.apache.lucene.document.Document();
                    Field field = new Field("body", doc.getContentUrl(), type);
                    doc_to_insert.add(field);
                    cluster_documents_list.add(doc_to_insert);
                }
                this.clustersWithLuceneDocuments.add(cluster_documents_list);
                //System.out.println(element.getClass());
                //System.out.print(element.toString());
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