import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.InputSource;

public class trec_indexing_local {
	
	private static String OS = System.getProperty("os.name").toLowerCase();

	public static void main(String[] args) {
		try {
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching

			String path_to_trec = "";
			if (isWindows()) {
				path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";	
			} else if (isMac()) {
				path_to_trec = "/Users/wingair/Dropbox/Dataset/WT10G/";	
			}

			int number_of_documents_to_index = 1000;
			Directory index = indexSpecificNumberOfDocuments(path_to_trec, number_of_documents_to_index, args);
			// Text to search
			//index.openInput(arg0, arg1)
			/*
			String querystr = "Christian Slater (William)";

			// field is explicitly specified in the query
			Query q = new QueryParser(Version.LUCENE_48, "title", analyzer).parse(querystr);

			// Searching code
			int hitsPerPage = 10;
		
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// Code to display the results of search
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("isbn") + "\t"
						+ d.get("title"));
			}
			// reader can only be closed when there is no need to access the
			// documents any more
			reader.close();
			*/
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static Directory indexSpecificNumberOfDocuments(String path_to_trec,int number_of_documents_to_index, String[] args) {
		Directory index = new RAMDirectory();
		try{

			
			File indexDir = new File("E:\\Dropbox\\DTU\\advanced_web_search_technology\\Final-Project\\trec_index");
			// Code to create the index
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching
			@SuppressWarnings("deprecation")
	        Version luceneVersion = Version.LUCENE_CURRENT;
			Analyzer analyzer = new StandardAnalyzer(luceneVersion);
	        IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
	        IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), config);
			
			//System.out.println(args[0]);
			
			int counter = 0;
			
			String symbol = "";
			if (isWindows()) {
				symbol = "\\";	
			} else if (isMac()) {
				symbol = "/";
			}
			
			
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
										
										Document doc = new Document();
							            /*
							             * We will create Lucene documents with searchable "fullContent" field and "title", 
							             * "url" and "snippet" fields for clustering.
							             */
										
							            //doc.add(new TextField("fullContent", doc_content, Store.YES));
							            //doc.add(new TextField("title", doc_no, Store.YES));
							            doc.add(new TextField("fullContent", doc_content, Store.NO));
							            doc.add(new TextField("title", doc_no, Store.YES));
							            doc.add(new TextField("snippet", doc_content, Store.YES));
							            doc.add(new StringField("url", doc_no, Store.YES));
							            writer.addDocument(doc);
									}
									counter += 1;
								}
							}
						}
					}
				}

			}
			String hej = "a";
			writer.close();

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
