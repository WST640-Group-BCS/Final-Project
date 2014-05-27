import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
import org.xml.sax.InputSource;

public class trec_indexing {
	public static void main(String[] args) {
		try {
			// Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);

			// Code to create the index
			Directory index = new RAMDirectory();

			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_48,
					analyzer);

			

			IndexWriter w = new IndexWriter(index, config);
			addDoc(w, "Lucene in Action", "193398817");
			addDoc(w, "Lucene for Dummies", "55320055Z");
			addDoc(w, "Managing Gigabytes", "55063554A");
			addDoc(w, "The Art of Computer Science", "9900333X");
			addDoc(w, "My name is teja", "12842d99");
			addDoc(w, "Lucene demo by teja", "23k43413");
			w.close();
			
			String path_to_trec = "E:\\Dropbox\\Dataset\\WT10G";
			int number_of_documents_to_index = 1;
			indexSpecificNumberOfDocuments(path_to_trec,
					number_of_documents_to_index);

			// Text to search
			String querystr = args.length > 0 ? args[0] : "teja";

			// The \"title\" arg specifies the default field to use when no
			// field is explicitly specified in the query
			Query q = new QueryParser(Version.LUCENE_48, "title", analyzer)
					.parse(querystr);

			// Searching code
			int hitsPerPage = 10;
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(
					hitsPerPage, true);
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
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static void addDoc(IndexWriter w, String title, String isbn)
			throws IOException {
		Document doc = new Document();
		// A text field will be tokenized
		doc.add(new TextField("title", title, Field.Store.YES));
		// We use a string field for isbn because we don\'t want it tokenized
		doc.add(new StringField("isbn", isbn, Field.Store.YES));
		w.addDocument(doc);
	}

	public static void indexSpecificNumberOfDocuments(String path_to_trec,
			int number_of_documents_to_index) {

		File file = new File(path_to_trec);
		String[] wtx_folders = file.list();
		int counter = 0;
		for (String wtx_folder : wtx_folders) {
			System.out.println("isd");
			// excluding the info folder
			if ((new File(path_to_trec + "\\" + wtx_folder).isDirectory())
					&& !(new File(path_to_trec + "\\" + wtx_folder).getName()
							.equals("info"))) {
				if (counter < number_of_documents_to_index) {
					System.out.println(new File(path_to_trec + "\\"
							+ wtx_folder).getName());
					String[] sub_directories = new File(path_to_trec + "\\"
							+ wtx_folder).list();
					for (String sub_directory : sub_directories) {
						if (counter < number_of_documents_to_index) {
							
							StringBuilder builder = new StringBuilder();
							
							File sub_file = new File(path_to_trec + "\\"
									+ wtx_folder + "\\" + sub_directory);

							BufferedReader in;
							try {
								in = new BufferedReader(new InputStreamReader(
										new GZIPInputStream(
												new FileInputStream(sub_file
														.getAbsolutePath()))));
								String content;
								System.out.println(new File(path_to_trec + "\\"
										+ wtx_folder + "\\" + sub_directory)
										.getName() + ":");
								
								while ((content = in.readLine()) != null) {
									//System.out.println(content);
									builder.append(content);
									
									
								}
								String sub_file_text = builder.toString();
								
								
								String xml = "<channel>\n" +
						                "\n" +
						                "   <title>Site Name</title>\n" +
						                "\n" +
						                "   <item>  \n" +
						                "       <title>News Title!</title>       \n" +
						                "   </item>\n" +
						                "\n" +
						                "</channel>";
								XPathFactory xpf = XPathFactory.newInstance();
						        XPath xPath = xpf.newXPath();

						        InputSource inputSource = new InputSource(new StringReader(sub_file_text));
						        String result;
								try {
									result = (String) xPath.evaluate("//DOC", inputSource, XPathConstants.STRING);
									System.out.println(result);
								} catch (XPathExpressionException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						        
						        
								//System.out.println(sub_file_text);
								counter += 1;
								

							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}
				}
			}

		}

	}
}