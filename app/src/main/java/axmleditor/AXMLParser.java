package axmleditor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

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

// class BinaryReader {
//     private InputStream inputStream;
//     private int position;

//     public BinaryReader(InputStream inputStream) {
//         this.inputStream = inputStream;
//         this.position = 0;
//     }

//     public int readInt() throws IOException {
//         int result = 0;
//         for (int i = 0; i < 4; i++) {
//             result |= (inputStream.read() & 0xFF) << (i * 8);
//         }
//         position += 4;
//         return result;
//     }

//     public void skip(int bytes) throws IOException {
//         inputStream.skip(bytes);
//         position += bytes;
//     }

//     public int getPosition() {
//         return position;
//     }

//     public short readShort() throws IOException {
//         return (short) ((inputStream.read() & 0xFF) | ((inputStream.read() & 0xFF) << 8));
//     }
    
//     public byte[] readBytes(int length) throws IOException {
//         byte[] buffer = new byte[length];
//         inputStream.read(buffer);
//         position += length;
//         return buffer;
//     }
// }

// class StringPool {
//     private List<String> strings;

//     private StringPool(List<String> strings) {
//         this.strings = strings;
//     }

//     public static StringPool read(BinaryReader reader) throws IOException {
//         int chunkSize = reader.readInt();
//         int stringCount = reader.readInt();
//         int styleCount = reader.readInt();
//         int flags = reader.readInt();
//         int stringsStart = reader.readInt();
//         int stylesStart = reader.readInt();

//         List<Integer> stringOffsets = new ArrayList<>();
//         for (int i = 0; i < stringCount; i++) {
//             stringOffsets.add(reader.readInt());
//         }

//         List<String> strings = new ArrayList<>();
//         for (int offset : stringOffsets) {
//             reader.skip(offset - reader.getPosition());
//             int length = reader.readShort();
//             byte[] stringBytes = reader.readBytes(length * 2);
//             strings.add(new String(stringBytes, StandardCharsets.UTF_16LE));
//         }
    
//         return new StringPool(strings);
//     }

//     public String getString(int index) {
//         return (index >= 0 && index < strings.size()) ? strings.get(index) : null;
//     }
// }


class StringPool {
    private static final int UTF8_FLAG = 0x00000100;

    private boolean m_isUTF8;
    private int[] m_stringOffsets;
    private int[] m_styleOffsets;
    private byte[] m_strings;
    private int[] m_styles;

    public static StringPool read(BinaryReader reader, int chunkSize) throws IOException {
        int stringCount = reader.readInt();
        int styleOffsetCount = reader.readInt();
        int flags = reader.readInt();
        int stringsOffset = reader.readInt();
        int stylesOffset = reader.readInt();

        StringPool pool = new StringPool();
        pool.m_isUTF8 = (flags & UTF8_FLAG) != 0;
        pool.m_stringOffsets = reader.readIntArray(stringCount);
        
        if (styleOffsetCount != 0) {
            pool.m_styleOffsets = reader.readIntArray(styleOffsetCount);
        }

        {
            int size = ((stylesOffset == 0) ? chunkSize : stylesOffset) - stringsOffset;
            if ((size % 4) != 0) {
                throw new IOException("String data size is not multiple of 4 (" + size + ").");
            }
            pool.m_strings = reader.readByteArray(size);
        }

        if (stylesOffset != 0) {
            int size = (chunkSize - stylesOffset);
            if ((size % 4) != 0) {
                throw new IOException("Style data size is not multiple of 4 (" + size + ").");
            }
            pool.m_styles = reader.readIntArray(size / 4);
        }

        return pool;
    }

    public String getString(int index) {
        if (index < 0 || index >= m_stringOffsets.length) {
            return null;
        }

        int offset = m_stringOffsets[index];
        int length;

        if (m_isUTF8) {
            length = m_strings[offset];
            if ((length & 0x80) != 0) {
                length = ((length & 0x7F) << 8) | m_strings[offset + 1];
            }
        } else {
            length = getShort(m_strings, offset) * 2;
            offset += 2;
        }

        return decodeString(offset, length);
    }

    private String decodeString(int offset, int length) {
        try {
            return m_isUTF8
                ? new String(m_strings, offset, length, "UTF-8")
                : new String(m_strings, offset, length, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private static int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff;
    }
}


class BinaryReader {
    private InputStream inputStream;
    private int position;

    public BinaryReader(InputStream inputStream) {
        this.inputStream = inputStream;
        this.position = 0;
    }

    public int readInt() throws IOException {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (inputStream.read() & 0xFF) << (i * 8);
        }
        position += 4;
        return result;
    }

    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = readInt();
        }
        return array;
    }

    public byte[] readByteArray(int length) throws IOException {
        byte[] array = new byte[length];
        int bytesRead = inputStream.read(array);
        if (bytesRead != length) {
            throw new IOException("Expected to read " + length + " bytes, but read " + bytesRead);
        }
        position += length;
        return array;
    }

    public int getPosition() {
        return position;
    }

    public void skip(int bytes) throws IOException {
        long skipped = inputStream.skip(bytes);
        if (skipped != bytes) {
            throw new IOException("Expected to skip " + bytes + " bytes, but skipped " + skipped);
        }
        position += bytes;
    }
}