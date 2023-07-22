package info.kgeorgiy.ja.garipov.walk;

import java.io.IOException;
import java.io.InputStream;

public class PJWHasher implements Hasher {
    final int BUFFER_SIZE = 1024;
    byte[] buffer = new byte[BUFFER_SIZE];

    @Override
    public long hash(InputStream input) throws IOException {
        long hash = 0;
        try (InputStream inputStream = input) {
            long high;
            int readBytesCount;
            while ((readBytesCount = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < readBytesCount; i++) {
                    hash = (hash << 8) + (buffer[i] & 0xff);
                    if ((high = hash & 0xff00_0000_0000_0000L) != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
        }
        return hash;
    }
}
