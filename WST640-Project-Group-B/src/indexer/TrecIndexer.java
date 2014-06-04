package indexer;
import gui.MainView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class TrecIndexer {
	
	static
    Version luceneVersion = Version.LUCENE_46;
	private static String OS = System.getProperty("os.name").toLowerCase();
	private static MainView mainView;
	private static StandardAnalyzer analyzer;
	private Directory index;
	
	public Directory startIndexingFiles() {
		// Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching

		analyzer = new StandardAnalyzer(luceneVersion);
		String path_to_trec = "";
		if (isWindows()) {
			path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";	
		} else if (isMac()) {
			path_to_trec = "/Users/wingair/Dropbox/Dataset/WT10G/";	
		}

		this.index = indexSpecificNumberOfDocuments(path_to_trec);
		return this.index;
	}
	
	public ArrayList<Document> search(String queryString)
	{
		ArrayList<Document> stringArray = new ArrayList<Document>();

		try {
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching

			String querystr = queryString;

			// field is explicitly specified in the query
			Query query = new QueryParser(luceneVersion, "fullContent", analyzer).parse(querystr);

			// Searching code
			int hitsPerPage = 10;
			IndexReader reader = DirectoryReader.open(this.index);
			IndexSearcher indexSearcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			indexSearcher.search(query, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// Code to display the results of search
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				System.out.println(hits[i].score);
				Document document = indexSearcher.doc(docId);
				stringArray.add(document);
			}

			// reader can only be closed when there is no need to access the
			// documents any more
			reader.close();

		} catch (Exception e) {
			//System.out.println(e.getMessage());
		}
		return stringArray;
	}

	public static Directory indexSpecificNumberOfDocuments(String path_to_trec) {
		Directory index = new RAMDirectory();
		try{

			File file = new File(path_to_trec);
			String[] wtx_folders = file.list();
			
			// Code to create the index
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching
			
			StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion);
			IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
			IndexWriter indexWriter = new IndexWriter(index, config);
			
			String symbol = "";
			if (isWindows()) {
				symbol = "\\";	
			} else if (isMac()) {
				symbol = "/";	
			}
			int numberOfFoldersToUse = 1;
			int numberOfFilesToIndex = 1;
			int numberOfDOCTagsToIndexInONEFile = 30;

			int numberOfFolderUsing = 0;
			int numberOfFilesIndexing = 0;
			int numberOfDOCTagIndexing = 0;
			
			for (String wtx_folder : wtx_folders) {
				// excluding the info folder
				if ((new File(path_to_trec + symbol + wtx_folder).isDirectory())
						&& !(new File(path_to_trec + symbol + wtx_folder).getName()
								.equals("info"))) {
					if (numberOfFolderUsing < numberOfFoldersToUse) {
						System.out.println("Using the folder: " + new File(path_to_trec + symbol
								+ wtx_folder).getName());
						String[] sub_directories = new File(path_to_trec + symbol
								+ wtx_folder).list();
						for (String sub_directory : sub_directories) {
							if (numberOfFilesIndexing < numberOfFilesToIndex) {

								StringBuilder builder = new StringBuilder();

								File sub_file = new File(path_to_trec + symbol
										+ wtx_folder + symbol + sub_directory);
								System.out.println("Using the file: " + sub_file.getAbsolutePath());
								BufferedReader in = new BufferedReader(new InputStreamReader(
										new GZIPInputStream(
												new FileInputStream(sub_file
														.getAbsolutePath()))));
								String content;
								//System.out.println(new File(path_to_trec + symbol + wtx_folder + symbol + sub_directory).getName() + ":");
								
								while ((content = in.readLine()) != null) {
									builder.append(content);
								}

								String sub_file_text = builder.toString();
								
								in.close();
								
								String docno_pattern = "(<DOCNO>(.*?)</DOCNO>)(?<DOC>(.*?)</DOC>)";

								Pattern docno_r = Pattern.compile(docno_pattern);
								Matcher docno_m = docno_r.matcher(sub_file_text);
								while (docno_m.find()) {
									if (numberOfDOCTagIndexing < numberOfDOCTagsToIndexInONEFile) {
										String doc_no = docno_m.group(2);
										String doc_content = docno_m.group(3);
										
										Document doc = new Document();
							            /*
							             * We will create Lucene documents with searchable "fullContent" field and "title", 
							             * "url" and "snippet" fields for clustering.
							             */
							            doc.add(new TextField("fullContent", doc_content, Store.NO));
							            doc.add(new TextField("title", doc_no, Store.YES));
							            indexWriter.addDocument(doc);
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
			indexWriter.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return index;
	}	

	public static boolean isWindows() {
		 
		return (OS.indexOf("win") >= 0);
 
	}
 
	public static boolean isMac() {
 
		return (OS.indexOf("mac") >= 0);
 
	}

}
