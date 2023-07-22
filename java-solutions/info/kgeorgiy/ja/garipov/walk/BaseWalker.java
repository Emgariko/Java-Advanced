package info.kgeorgiy.ja.garipov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;

public class BaseWalker {
    private final int recursionDepth;

    public void walk(String[] args) {
        if (args == null || args.length != 2 || (args[0] == null || args[1] == null)) {
            System.err.println("Wrong arguments");
            return;
        }

        Path inputFilePath;
        try {
            inputFilePath = Path.of(args[0]);
        } catch (InvalidPathException exc) {
            System.err.println("Invalid input path: " + exc.getMessage());
            return;
        }

        Path outputFilePath;
        try {
            outputFilePath = Path.of(args[1]);
        } catch (InvalidPathException exc) {
            System.err.println("Invalid output path: " + exc.getMessage());
            return;
        }

        if (outputFilePath.getParent() != null) {
            try {
                Files.createDirectories(outputFilePath.getParent());
            } catch (FileAlreadyExistsException ignored) {
                // do nothing
            } catch (IOException exc) {
                System.err.println("Output error creating the file directory: " + exc.getMessage());
                return;
            }
        }

        try (BufferedReader input = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)) {
            try (BufferedWriter output = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
                HashingFileVisitor fileVisitor = new HashingFileVisitor(new PJWHasher(), output);
                String fileName;
                while (true) {
                    try {
                        fileName = input.readLine();
                        if (fileName == null) {
                            break;
                        }
                    } catch (IOException exc) {
                        System.err.println("Invalid input content: " + exc.getMessage());
                        return;
                    }

                    Path curFilePath;
                    try {
                        curFilePath = Path.of(fileName);
                    } catch (InvalidPathException exc) {
                        System.err.println("Invalid path in input: " + exc.getMessage());
                        try {
                            fileVisitor.write(0, fileName);
                        } catch (IOException exc1) {
                            System.err.println("Output error writing in file");
                            return;
                        }
                        continue;
                    }


                    Files.walkFileTree(curFilePath, Collections.singleton(FileVisitOption.FOLLOW_LINKS), recursionDepth, fileVisitor);
                }
            } catch (IOException exc) {
                System.err.println("Output error opening or creating the file: " + exc.getMessage());
            }
        } catch (IOException exc) {
            System.err.println("Input error opening the file: " + exc.getMessage());
        }
    }

    public BaseWalker(int recursionDepth) {
        this.recursionDepth = recursionDepth;
    }
}
