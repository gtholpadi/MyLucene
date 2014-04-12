
package mllab_lucene;
import java.io.File;
import java.io.FileFilter;

public class SingleFileFilter implements FileFilter {
	String filename;
	public SingleFileFilter(String fname) {
		filename = fname;
	}
	@Override
	public boolean accept(File f) {
		if (f.getName().equals(filename)) {
			return true;
		}
		return false;
	}
}
