package mllab_lucene;
import java.io.File;
import java.io.PrintWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
public class ClinssProcess {
	public static void processDir(String srcdir, String dstdir) throws Exception {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		for(String file : (new File(srcdir)).list()) {
			// Extract relevant text from xml
			Document xdoc = db.parse(new File(srcdir+"/"+file));
			xdoc.getDocumentElement().normalize();
			String title = xdoc.getElementsByTagName("title").item(0).getTextContent();
			String contents = xdoc.getElementsByTagName("content").item(0).getTextContent();
			// Write text to file in dest directory
			PrintWriter dstfile = new PrintWriter(dstdir+"/"+file);
			dstfile.println(title+"\n"+contents);
			dstfile.close();
		}
	}
	public static void main(String[] args) throws Exception {
		String usage = "java mllab_lucene.ClinssProcess COMMAND [ARGS]";
		if (args.length == 0) {
			System.out.println("Usage: " + usage);
			System.exit(1);
		}
		String cmd = args[0];
		int i = 1;
		if ("procdir".equals(cmd)) {
			String srcdir = args[i++];
			String dstdir = args[i++];
			processDir(srcdir, dstdir);
		}
	}
}
