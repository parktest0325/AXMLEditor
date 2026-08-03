package axmleditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AXMLParser {
    private static final int CHUNK_TYPE_XML = 0x00080003;
    private static final int CHUNK_TYPE_STRING_POOL = 0x001C0001;
    private static final int CHUNK_TYPE_RESOURCE_MAP = 0x00080180;
    private static final int CHUNK_TYPE_START_NAMESPACE = 0x00100100;
    private static final int CHUNK_TYPE_END_NAMESPACE = 0x00100101;
    private static final int CHUNK_TYPE_START_TAG = 0x00100102;
    private static final int CHUNK_TYPE_END_TAG = 0x00100103;
    private static final int CHUNK_TYPE_TEXT = 0x00100104;

    private BinaryReader reader;
    private StringPool stringPool;
    private List<Integer> resourceIds;

    public AXMLParser(InputStream inputStream) {
        this.reader = new BinaryReader(inputStream);
        this.resourceIds = new ArrayList<>();
    }

    public void parse() throws IOException {
        int fileType = reader.readInt();
        if (fileType != CHUNK_TYPE_XML) {
            throw new IOException("Invalid AXML file");
        }
    
        int fileSize = reader.readInt();
        System.out.println("File size: " + fileSize);
        
        while (reader.getPosition() < fileSize) {
            int chunkType = reader.readInt();
            int chunkSize = reader.readInt();
            System.out.println("Chunk type: 0x" + Integer.toHexString(chunkType) + ", size: " + chunkSize);
    
            switch (chunkType) {
                case CHUNK_TYPE_STRING_POOL:
                    stringPool = StringPool.read(reader, chunkSize);
                    break;
                case CHUNK_TYPE_RESOURCE_MAP:
                    readResourceIds(chunkSize);
                    break;
                case CHUNK_TYPE_START_NAMESPACE:
                    readStartNamespace();
                    break;
                case CHUNK_TYPE_END_NAMESPACE:
                    readEndNamespace();
                    break;
                case CHUNK_TYPE_START_TAG:
                    readStartTag();
                    break;
                case CHUNK_TYPE_END_TAG:
                    readEndTag();
                    break;
                case CHUNK_TYPE_TEXT:
                    readText();
                    break;
                default:
                    reader.skip(chunkSize - 8); // Skip unknown chunks
            }
        }
    }

    private void readResourceIds(int chunkSize) throws IOException {
        int idCount = (chunkSize - 8) / 4; // 8 bytes for chunk header
        for (int i = 0; i < idCount; i++) {
            resourceIds.add(reader.readInt());
        }
    }

    private void readStartNamespace() throws IOException {
        int lineNumber = reader.readInt();
        reader.skip(4); // Skip 0xFFFFFFFF
        int prefix = reader.readInt();
        int uri = reader.readInt();
        System.out.println("Start Namespace: " + stringPool.getString(prefix) + " = " + stringPool.getString(uri));
    }

    private void readEndNamespace() throws IOException {
        int lineNumber = reader.readInt();
        reader.skip(4); // Skip 0xFFFFFFFF
        int prefix = reader.readInt();
        int uri = reader.readInt();
        System.out.println("End Namespace: " + stringPool.getString(prefix));
    }

    private void readStartTag() throws IOException {
        int lineNumber = reader.readInt();
        reader.skip(4); // Skip 0xFFFFFFFF
        int namespaceUri = reader.readInt();
        int name = reader.readInt();
        reader.skip(4); // Skip flags
        int attributeCount = reader.readInt() & 0xFFFF;
        reader.skip(4); // Skip class attribute

        System.out.println("Start Tag: " + stringPool.getString(name));

        for (int i = 0; i < attributeCount; i++) {
            int attrNamespaceUri = reader.readInt();
            int attrName = reader.readInt();
            int attrValueString = reader.readInt();
            int attrType = reader.readInt();
            int attrData = reader.readInt();

            String attrValue = (attrValueString != -1) ? stringPool.getString(attrValueString) : String.valueOf(attrData);
            System.out.println("  Attribute: " + stringPool.getString(attrName) + " = " + attrValue);
        }
    }

    private void readEndTag() throws IOException {
        int lineNumber = reader.readInt();
        reader.skip(4); // Skip 0xFFFFFFFF
        int namespaceUri = reader.readInt();
        int name = reader.readInt();
        System.out.println("End Tag: " + stringPool.getString(name));
    }

    private void readText() throws IOException {
        int lineNumber = reader.readInt();
        reader.skip(4); // Skip 0xFFFFFFFF
        int name = reader.readInt();
        reader.skip(8); // Skip additional fields
        System.out.println("Text: " + stringPool.getString(name));
    }

}