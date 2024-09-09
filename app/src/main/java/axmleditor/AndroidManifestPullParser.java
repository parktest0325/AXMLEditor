package axmleditor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AndroidManifestPullParser {
    public static void main(String[] args) {
        try {
            FileInputStream fis = new FileInputStream("AndroidManifest.xml");
            AXMLParser parser = new AXMLParser(fis);
            parser.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}