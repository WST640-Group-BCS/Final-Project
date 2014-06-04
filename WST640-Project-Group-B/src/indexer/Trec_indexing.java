package indexer;
import gui.MainView;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.InputSource;

public class Trec_indexing {
	
	@SuppressWarnings("deprecation")
	static
    Version luceneVersion = Version.LUCENE_CURRENT;
	private static String OS = System.getProperty("os.name").toLowerCase();
	private static MainView mainView;
	private static StandardAnalyzer analyzer;
	private static Directory directory;
	private static int numberOfFilesToIndex = 1;
	
	public void startIndexingFiles(int numberOfDOCTagsToIndex) {
		// Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching

		analyzer = new StandardAnalyzer(luceneVersion);
		String path_to_trec = "";
		if (isWindows()) {
			path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";	
		} else if (isMac()) {
			path_to_trec = "/Users/wingair/Dropbox/Dataset/WT10G/";	
		}

		directory = indexSpecificNumberOfDocuments(path_to_trec, numberOfDOCTagsToIndex);
	}
	
	public ArrayList<String> search(String queryString)
	{
		ArrayList<String> stringArray = new ArrayList<String>();

		try {
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching

			String querystr = queryString;

			// field is explicitly specified in the query
			Query q = new QueryParser(luceneVersion, "title", analyzer).parse(querystr);

			// Searching code
			int hitsPerPage = 10;
			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// Code to display the results of search
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				stringArray.add(d.get("isbn"));
			}

			// reader can only be closed when there is no need to access the
			// documents any more
			reader.close();

		} catch (Exception e) {
			//System.out.println(e.getMessage());
		}
		return stringArray;
	}

	private static void addDoc(IndexWriter w, String content, String documentNumber)
			throws IOException {
		Document doc = new Document();
		// A text field will be tokenized
		doc.add(new TextField("title", content, Field.Store.YES));
		// We use a string field for isbn because we don\'t want it tokenized
		doc.add(new StringField("isbn", documentNumber, Field.Store.YES));
		w.addDocument(doc);
	}

	public static Directory indexSpecificNumberOfDocuments(String path_to_trec,int number_of_documents_to_index) {
		Directory index = new RAMDirectory();
		try{

			File file = new File(path_to_trec);
			String[] wtx_folders = file.list();
			
			// Code to create the index
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching
			
			StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion);
			IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
			IndexWriter w = new IndexWriter(index, config);
			int counter = 0;
			
			String symbol = "";
			if (isWindows()) {
				symbol = "\\";	
			} else if (isMac()) {
				symbol = "/";	
			}
			
			for (String wtx_folder : wtx_folders) {
				// excluding the info folder
				if ((new File(path_to_trec + symbol + wtx_folder).isDirectory())
						&& !(new File(path_to_trec + symbol + wtx_folder).getName()
								.equals("info"))) {
					if (counter < numberOfFilesToIndex) {
						System.out.println("Using the folder: " + new File(path_to_trec + symbol
								+ wtx_folder).getName());
						String[] sub_directories = new File(path_to_trec + symbol
								+ wtx_folder).list();
						for (String sub_directory : sub_directories) {
							if (counter < number_of_documents_to_index) {

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
									if (counter < number_of_documents_to_index) {
										String doc_no = docno_m.group(2);
										String doc_content = docno_m.group(3);
										
										addDoc(w, doc_content, doc_no);
									}
									counter += 1;
								}
							}
						}
					}
				}

			}
			w.close();

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
