package info.kgeorgiy.ja.garipov.walk;

import java.io.IOException;
import java.io.InputStream;

public interface Hasher {
    long hash(InputStream inputStream) throws IOException;
}
