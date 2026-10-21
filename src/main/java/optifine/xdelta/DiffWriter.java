package optifine.xdelta;

import java.io.IOException;

public interface DiffWriter {
    void addCopy(int i, int j) throws IOException;

    void addData(byte b) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
