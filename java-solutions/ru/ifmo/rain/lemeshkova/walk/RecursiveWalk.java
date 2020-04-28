package ru.ifmo.rain.lemeshkova.walk;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

public class RecursiveWalk {
    final static String errorHash = "00000000";

    public static void main(String[] args) {
        try {

            if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
                throw new ExitException("Invalid usage or arguments.%nUsage format: \"java RecursiveWalk <input file> <output file>\"");
            }

            Path inputFilePath;
            try {
                inputFilePath = Paths.get(args[0]);
            } catch (InvalidPathException ex) {
                throw new ExitException("Invalid input path " + args[0] + " : " + ex.getMessage());
            }
            if (!Files.isReadable(inputFilePath)) {
                throw new ExitException("Cannot read input file");
            }

            Path outputFilePath;
            try {
                outputFilePath = Paths.get(args[1]);
            } catch (InvalidPathException ex) {
                throw new ExitException("Invalid input path " + args[0] + " : " + ex.getMessage());
            }

            if (!Files.exists(outputFilePath)) {
                try {
                    if (outputFilePath.getParent() == null) {
                        throw new IOException("Incorrect parent directory");
                    }
                    Files.createDirectories(outputFilePath.getParent());
                } catch (IOException e) {
                    throw new ExitException("Cannot create directories for output file " + outputFilePath + " " + e.getMessage());
                }
                try {
                    Files.createFile(outputFilePath);
                } catch (IOException e) {
                    throw new ExitException("Cannot create file " + outputFilePath + " " + e.getMessage());
                }
            }

            try (BufferedReader reader = Files.newBufferedReader(inputFilePath)) {
                try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            Path inputPath = Paths.get(line);
                            if (!Files.exists(inputPath)) {
                                writeResult(writer, inputPath.toString(), errorHash);
                            } else {
                                try (Stream<Path> walk = Files.walk(inputPath)) {
                                    walk.filter(Files::isRegularFile).forEach(x -> {
                                        try {
                                            writeResult(writer, x.toString(), getHashFile(x));
                                        } catch (IOException e) {
                                            System.out.println("Cannot write hash result of file " + x.toString() + ": " + e.getMessage());
                                        }
                                    });
                                } catch (IOException e) {
                                    System.out.println("Cannot read files in directory " + inputPath.toString() + ": " + e.getMessage());
                                }
                            }
                        } catch (InvalidPathException ex) {
                            writeResult(writer, line, errorHash);
                        }
                    }
                } catch (NoSuchFileException ex) {
                    System.out.println("No such output file " + outputFilePath);
                } catch (IOException ex) {
                    System.out.println("Exception while writing result in file " + outputFilePath);
                    ex.printStackTrace();
                }
            } catch (NoSuchFileException ex) {
                System.out.println("No such input file " + outputFilePath);
            } catch (IOException ex) {
                System.out.println("Exception while writing result in file " + outputFilePath);
                ex.printStackTrace();
            }
        } catch (ExitException ex) {
            System.err.println(ex.getMessage());
        }
    }

    static String getHashFile(Path inputPath) {
        if (Files.isReadable(inputPath)) {
            try (InputStream bufferedInput = new BufferedInputStream(Files.newInputStream(inputPath))) {
                byte[] buffer = new byte[2 << 10];
                int h = 0x811c9dc5;
                int bufferReadSize;
                while ((bufferReadSize = bufferedInput.read(buffer)) >= 0) {
                    for (int i = 0; i < bufferReadSize; i++) {
                        h = (h * 0x01000193) ^ (buffer[i] & 0xff);
                    }
                }
                return String.format("%08x", h);
            } catch (IOException e) {
                return errorHash;
            }
        } else {
            return errorHash;
        }

    }

    static void writeResult(BufferedWriter writer, String path, String result) throws IOException {
        writer.write(result + " " + path + System.lineSeparator());
    }
}