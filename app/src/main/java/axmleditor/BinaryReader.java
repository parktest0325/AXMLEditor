package axmleditor;

import java.io.IOException;
import java.io.InputStream;

public class BinaryReader {
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
