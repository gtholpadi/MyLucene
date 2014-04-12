package mllab_lucene;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class MySuggester {
	private AnalyzingSuggester suggr;
	int numSugg;

	public MySuggester(String indexPath, String field, int numSugg) throws Exception {
		Set<String> suggFields = new HashSet<String>();
		suggFields.add(field);
		initialize(indexPath, suggFields, numSugg);
	}
	public MySuggester(String indexPath, Set<String> suggFields, int numSugg) throws Exception {
		initialize(indexPath, suggFields, numSugg);
	}
	/** Initialize suggester with terms from several fields.
	 *  If suggFields is empty, all fields are used.
	 */
	private void initialize(String indexPath, Set<String> suggFields, int numSugg) throws Exception {
		this.numSugg = numSugg;
		// Get terms from index for given field
		IndexReader ireader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		TermFreqIteratorListWrapper tfit = new TermFreqIteratorListWrapper();
		List<AtomicReaderContext> readercs = ireader.leaves();
		for (AtomicReaderContext readerc : readercs) {
			AtomicReader reader = readerc.reader();
			Fields fields = reader.fields();
			for (String field : fields) {
				if (suggFields.size() > 0 && !suggFields.contains(field)) {
					continue;
				}
				Terms terms = fields.terms(field);
				TermsEnum termsEnum = terms.iterator(null);
				tfit.add(termsEnum);
			}
		}
		// Build suggester using terms
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44, new CharArraySet(Version.LUCENE_44, 0, true));
		suggr = new AnalyzingSuggester(analyzer);
		suggr.build(tfit);
		ireader.close();
	}
	/** Look up terms starting with 'query' in the index. */
	public List<LookupResult> lookup(String query) throws Exception {
		return suggr.lookup(query, false, numSugg);
	}
	/** A quick test. */
	public static void main(String[] args) throws Exception {
		MySuggester suggr = new MySuggester("./index", "contents", 5); // suggest single field
		//MySuggester suggr = new MySuggester("./index", new HashSet<String>(), 5); // suggest all fields
		List<LookupResult> results = suggr.lookup("s");
		for(LookupResult res : results) {
			System.out.println(res.key + " " +res.value);
		}
	}
}
