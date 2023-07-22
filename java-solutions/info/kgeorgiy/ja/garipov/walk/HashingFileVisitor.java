package info.kgeorgiy.ja.garipov.walk;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashingFileVisitor extends SimpleFileVisitor<Path> {
    private final Hasher hasher;
    private final Writer output;
    public HashingFileVisitor(Hasher hasher, Writer output) {
        this.hasher = hasher;
        this.output = output;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        long hash = 0;
        try(InputStream inputStream = Files.newInputStream(file)) {
            hash = hasher.hash(inputStream);
        } catch (IOException exception) {
            hash = 0;
        } finally {
            write(hash, file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        write(0L, file);
        return FileVisitResult.CONTINUE;
    }

    public void write(long hash, Path file) throws IOException {
        output.write(String.format("%016x %s%n", hash, file.toString()));
    }

    public void write(long hash, String fileName) throws IOException {
        output.write(String.format("%016x %s%n", hash, fileName));
    }
}
