package mllab_lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class MyUtil {
	public static Analyzer getAnalyzer(File stopPath) throws Exception {
		Analyzer analyzer = null;
		if (stopPath.isFile()) {
			analyzer = new StandardAnalyzer(Version.LUCENE_44,
				new BufferedReader(new InputStreamReader(new FileInputStream(stopPath))));
		} else {
			analyzer = new StandardAnalyzer(Version.LUCENE_44);
		}
		return analyzer;
	}
}
