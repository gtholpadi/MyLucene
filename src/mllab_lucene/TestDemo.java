package mllab_lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** Actions for the test demo.
 */
public class TestDemo {
	public static String FLDTYPE_PATH = "path"; 
	public static String FLDTYPE_CONTENTS = "contents"; 
	public static String FLDTYPE_TITLE = "title"; 
	public static String FLDTYPE_MODIFIED = "modified"; 
	private TestDemo() {}
	/** Parse command-line params and call the requested method. */
	public static void main (String[] args) throws Exception {
		String usage = "java mllab_lucene.TestDemo"
			+ " COMMAND [ARGS]";
		if (args.length==0) {
			System.out.println("Usage: " + usage);
			System.exit(1);
		}
		String cmd = args[0];
		int i = 1;
		if ("deletefiles".equals(cmd)) {
			String indexPath = args[i++];
			String queryterm = args[i++];
			deleteFiles(indexPath, queryterm);
		} else if ("indexfiles".equals(cmd)) {
			String indexPath = args[i++];
			String docsPath = args[i++];
			boolean update = args[i++].equals("update") ? true : false;
			String stopPath = args[i++];
			String inferencer = args[i++];
			String instancefile = args[i++];
			String indexType = args[i++];
			String docType = args[i++];//[CLINSS|WIKI|NONE]
			if (docType.equals("CLINSS")) {
				Clinss.setInstance(new Clinss(inferencer, instancefile, indexType));
			}
			indexFiles(indexPath, docsPath, update, stopPath, docType);
		} else if ("searchfiles".equals(cmd)) {
			String indexPath = args[i++];
			searchFiles(indexPath);
		} else if ("listterms".equals(cmd)) {
			String indexPath = args[i++];
			listTerms(indexPath);
		} else if ("suggestterms".equals(cmd)) {
			String indexPath = args[i++];
			suggestTerms(indexPath);
		} else if ("testing".equals(cmd)) {
			testing();
		}
	}
	/** Test code. */
	public static void testing() throws Exception{
		Set<String> fields = new HashSet<String>();
		fields.add("contents");
		fields.add("title");
		fields.add("path");
		System.out.println(fields.contains("contents"));
	}
	/** Suggest terms (from the index) for a query. */
	public static void suggestTerms(String indexPath) throws Exception {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44, new CharArraySet(Version.LUCENE_44, 0, true));
		IndexReader ireader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		TermFreqIteratorListWrapper tfit = new TermFreqIteratorListWrapper();

		List<AtomicReaderContext> readercs = ireader.leaves();
		for (AtomicReaderContext readerc : readercs) {
			AtomicReader reader = readerc.reader();

			Fields fields = reader.fields();
			for (String field : fields) {
				if ( !( field.equals(FLDTYPE_CONTENTS) || field.equals(FLDTYPE_PATH) 
						|| field.equals(FLDTYPE_TITLE) ) ) {
					continue;
				}
				Terms terms = fields.terms(field);
				TermsEnum termsEnum = terms.iterator(null);
				tfit.add(termsEnum);
			}
		}
		AnalyzingSuggester suggr = new AnalyzingSuggester(analyzer);
		suggr.build(tfit);
		ireader.close();

//		AnalyzingInfixSuggester suggr = new AnalyzingInfixSuggester(Version.LUCENE_44,
//			new File(indexPath), analyzer, analyzer, 1);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		while (true) {
			System.out.print("Enter term: ");
			String line = in.readLine();
			if (!lineOk(line)) break;
			line = line.trim();
			List<LookupResult> results = suggr.lookup(line, false, 5);
			for(LookupResult res : results) {
				System.out.println(res.key + " " +res.value);
			}
		}
	}
	private static boolean lineOk(String line) {
		boolean ok = true;
		if (line == null || line.length() == -1) {
			ok = false;
		}
		line = line.trim();
		if (line.length() == 0) {
			ok = false;
		}
		return ok;
	}
	/** List terms in an index. */
	public static void listTerms(String indexPath) throws Exception {
		IndexReader ireader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		AtomicReader reader = SlowCompositeReaderWrapper.wrap(ireader);
		Fields fields = reader.fields();
		for (String field : fields) {
			if ( !( field.equals(FLDTYPE_CONTENTS) || field.equals(FLDTYPE_PATH) || field.equals(FLDTYPE_TITLE) ) ) {
				continue;
			}
			System.out.println("#FIELD=" + field);
			Terms terms = fields.terms(field);
			for (TermsEnum termsEnum = terms.iterator(null);termsEnum.next() != null; ) {
				System.out.println(termsEnum.term().utf8ToString());
			}
		}
		reader.close();
	}
	/** Delete documents in index matching term. */
	public static void deleteFiles(String indexPath, String term) throws Exception {
		String field = FLDTYPE_CONTENTS;
		Directory indexDir = FSDirectory.open(new File(indexPath));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
		IndexWriterConfig iwconf = new IndexWriterConfig(Version.LUCENE_44, analyzer);
		iwconf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		IndexWriter writer = new IndexWriter(indexDir, iwconf);
		QueryParser parser = new QueryParser(Version.LUCENE_44, field, analyzer);
		Query query = parser.parse(term);
		writer.deleteDocuments(query);
		writer.close();
	}
	/** Index all text files under a directory. */
	public static void indexFiles(String indexPath, String docsPath, boolean update, String stopPath, 
			String docType) throws Exception {
		Directory dir = FSDirectory.open(new File(indexPath));
		Analyzer analyzer;
		if ((new File(stopPath)).isFile()) {
			analyzer = new StandardAnalyzer(Version.LUCENE_44,
				new BufferedReader(new InputStreamReader(new FileInputStream(stopPath))));
		} else {
			analyzer = new StandardAnalyzer(Version.LUCENE_44);
		}
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzer);
		if (update) {
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		} else {
			iwc.setOpenMode(OpenMode.CREATE);
		}
		IndexWriter writer = new IndexWriter(dir, iwc);
		indexDocs(writer, new File(docsPath), docType);
		writer.close();
	}
	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory. */
	static void indexDocs(IndexWriter writer, File file, String docType) throws Exception {
		if (!file.canRead()) {
			return;
		}
		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					System.out.println("indexDocs: "+files[i]);
					indexDocs(writer, new File(file, files[i]), docType);
				}
			}
		} else {
			FileInputStream fis;
			fis = new FileInputStream(file);
			Document doc = getDoc(file, fis, docType);

			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				writer.addDocument(doc);
			} else {
				writer.updateDocument(new Term(FLDTYPE_PATH, file.getPath()), doc);
			}
			fis.close();
		}
	}
	static Document getDoc(File file, FileInputStream fis, String docType) throws Exception {
		Document doc = new Document();
		Field pathField = new StringField(FLDTYPE_PATH, file.getPath(), Field.Store.YES);
		doc.add(pathField);
		doc.add(new LongField(FLDTYPE_MODIFIED, file.lastModified(), Field.Store.NO));
		if (docType.equals("CLINSS")) {
			makeDocClinss(file, fis, doc);
		} else if (docType.equals("WIKI")){
			makeDocWiki(file, fis, doc);
		} else {
			makeDocDefault(file, fis, doc);
		}
		return doc;
	}
	static void makeDocDefault(File file, FileInputStream fis, Document doc) throws Exception {
		Field titleField = new StringField(FLDTYPE_TITLE, file.getName(), Field.Store.YES);
		doc.add(titleField);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		doc.add(new TextField(FLDTYPE_CONTENTS, br));
	}
	static void makeDocWiki(File file, FileInputStream fis, Document doc) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		br.mark(1000);
		Field titleField = new StringField(FLDTYPE_TITLE, br.readLine(), Field.Store.YES);
		doc.add(titleField);
		br.reset();
		doc.add(new TextField(FLDTYPE_CONTENTS, br));
	}
	static void makeDocClinss(File file, FileInputStream fis, Document doc) throws Exception {
		Clinss.getInstance().makeDoc(file, fis, doc);
		//Clinss.getInstance().makeTopicDoc(file, fis, doc);
	}
	public static void searchFiles(String indexPath) throws Exception {
		String field = FLDTYPE_CONTENTS;
//		int repeat = 0;
		boolean raw = false;
		int hitsPerPage = 10;
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		IndexSearcher searcher = new IndexSearcher(reader);
//		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
//		QueryParser parser = new QueryParser(Version.LUCENE_44, field, analyzer);
		while (true) {
			System.out.println("Enter query: ");
			String line = in.readLine();
			if (line == null || line.length() == -1) {
				break;
			}
			line = line.trim();
			if (line.length() == 0) {
				break;
			}
			//Query query = parser.parse(line);
			TermQuery qpat = new TermQuery(new Term(FLDTYPE_PATH, line));
			TermQuery qtit = new TermQuery(new Term(FLDTYPE_TITLE, line));
			TermQuery qcon = new TermQuery(new Term(FLDTYPE_CONTENTS, line));
			BooleanQuery bq = new BooleanQuery();
			bq.add(qpat, BooleanClause.Occur.SHOULD);
			bq.add(qtit, BooleanClause.Occur.SHOULD);
			bq.add(qcon, BooleanClause.Occur.SHOULD);
			Query query = bq;

			System.out.println("Searching for: " + query.toString(field));
			doPagingSearch(in, searcher, query, hitsPerPage, raw, true);
		}
		reader.close();
	}
	public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, int hitsPerPage, boolean raw, boolean interactive) throws Exception {
		// Collect enough docs to show 5 pages
		TopDocs results = searcher.search(query, 5 * hitsPerPage);
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = results.totalHits;
		System.out.println(numTotalHits + " total matching documents");
		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);
		while (true) {
			if (end > hits.length) {
				System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
				System.out.println("Collect more (y/n) ?");
				String line = in.readLine();
				if (line.length() == 0 || line.charAt(0) == 'n') {
					break;
				}
				hits = searcher.search(query, numTotalHits).scoreDocs;
			}
			end = Math.min(hits.length, start + hitsPerPage);
			for (int i = start; i < end; i++) {
				if (raw) {                              // output raw format
					System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
					continue;
				}
				Document doc = searcher.doc(hits[i].doc);
				String path = doc.get(FLDTYPE_PATH);
				if (path != null) {
					System.out.println((i+1) + ". " + path + " (score="+hits[i].score+")");
					String title = doc.get(FLDTYPE_TITLE);
					if (title != null) {
						System.out.println("   Title: " + doc.get(FLDTYPE_TITLE));
					}
				} else {
					System.out.println((i+1) + ". " + "No path for this document");
				}
			}
			if (!interactive || end == 0) {
				break;
			}
			if (numTotalHits >= end) {
				boolean quit = false;
				while (true) {
					System.out.print("Press ");
					if (start - hitsPerPage >= 0) {
						System.out.print("(p)revious page, ");  
					}
					if (start + hitsPerPage < numTotalHits) {
						System.out.print("(n)ext page, ");
					}
					System.out.println("(q)uit or enter number to jump to a page.");
					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0)=='q') {
						quit = true;
						break;
					}
					if (line.charAt(0) == 'p') {
						start = Math.max(0, start - hitsPerPage);
						break;
					} else if (line.charAt(0) == 'n') {
						if (start + hitsPerPage < numTotalHits) {
							start+=hitsPerPage;
						}
						break;
					} else {
						int page = Integer.parseInt(line);
						if ((page - 1) * hitsPerPage < numTotalHits) {
							start = (page - 1) * hitsPerPage;
							break;
						} else {
							System.out.println("No such page");
						}
					}
				}
				if (quit) break;
				end = Math.min(numTotalHits, start + hitsPerPage);
			}
		}
	}
}
