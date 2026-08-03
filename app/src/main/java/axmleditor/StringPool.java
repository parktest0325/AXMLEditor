package axmleditor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class StringPool {
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
