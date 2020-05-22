package ru.ifmo.rain.lemeshkova.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;


/**
 * Code generating implementation of the {@code JarImpler} interfaces.
 * Capable of generating {@code .java} and {@code .jar} files for classes,
 * implementing the provided {@code Class} token using {@code Reflection API}.
 *
 * @author Natalia Lemeshkova
 * @see Impler
 * @see JarImpler
 * @see java.lang.reflect
 */
public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Show message about valid usage of console call.
     * Invoked in {@link #main(String[])} if invalid arguments passed.
     */
    private static void showUsage() {
        System.out.println("Invalid usage." + System.lineSeparator() +
                "Use : Implementor -jar (for jar mode) <full class name> \n Or : Implementor -jar(for jar mode) <full class name> <relative path>");
    }

    /**
     * Main method. A command line utility for {@code Implementor}.
     * Supports three modes
     * <ol>
     *     <li><b>java</b>: {@code <className> <outputPath>}.
     *     Creates a {@code .java} file by passing the arguments to {@link #implement(Class, Path)}.</li>
     *     <li><b>java</b>: {@code <className>}.
     *     Creates a {@code .java} file by passing the arguments to {@link #implement(Class, Path)} with empty path.</li>
     *     <li><b>jar</b>: {@code -jar <className> <outputPath>}.
     *     Creates a {@code .jar} file by passing the arguments to {@link #implementJar(Class, Path)}.</li>
     * </ol>
     * If any arguments are invalid or an error occurs, execution is stopped
     * and a message describing the issue is displayed.
     *
     * @param args list of command line arguments
     */
    public static void main(String[] args) {
        try {
            JarImplementor implementor = new JarImplementor();
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                return;
            }
            Class<?> clazz = Class.forName(args[0]);
            String path = "";
            if (args.length == 2) path = args[1];
            implementor.implement(clazz, Paths.get(path));
        } catch (ClassNotFoundException e) {
            System.out.println("Invalid class " + e.getMessage());
            e.printStackTrace();
        } catch (ImplerException e) {
            System.out.println("Exception while implementing " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidPathException e) {
            System.out.println("Invalid path" + e.getMessage());
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            showUsage();
        }
    }

    /**
     * Extension for generated {@code .class} files.
     */
    private static final String CLASS = ".class";

    /**
     * Produces {@code .jar} file implementing class or interface specified by provided {@code token}.
     * The generated {@code .jar} file location is specified by {@code jarFile}.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Invalid argument(s)");
        }
        createDirectories(jarFile);
        Path temp;
        try {
            temp = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Cannot create temporary directory: ", e);
        }
//        Path temp = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        implement(token, temp);
        compile(token, temp);
        buildJar(token, jarFile, temp);
        deleteRecursively(temp);
    }

    /**
     * Compiles the {@code token} implementation {@code .java} file.
     * Stores the resulting {@code .class} file at {@code temp}.
     *
     * @param token type token, the implementation of which is stored at {@code temp}
     * @param temp  working directory containing the source of {@code token} implementation
     * @throws ImplerException if an error occurs during compilation
     * @see JavaCompiler
     */
    private void compile(Class<?> token, Path temp) throws ImplerException {
        Path filePath = Paths.get("");
        for (String s : token.getPackage().getName().split("\\.")) {
            filePath = filePath.resolve(s);
        }
        filePath = filePath.resolve(token.getSimpleName() + "Impl");
        Path classPath = null;
        try {
            classPath = temp.toAbsolutePath().resolve(Paths.get(token.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException ignored) {
        }
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) throw new ImplerException("Cannot run java compiler");
        if (javaCompiler.run(null, null, null,
                "-cp", classPath + File.pathSeparator + System.getProperty("java.class.path"), Path.of(temp.toAbsolutePath().toString(), filePath + ".java").toString())
                != 0)
            throw new ImplerException("Failed while running compiling class");
    }

    /**
     * Builds a {@code .jar} file containing compiled implementation of {@code token}.
     *
     * @param token   type token, the implementation of which is stored at {@code temp}
     * @param jarFile resulting {@code .jar} file destination
     * @param temp    directory containing the compiled {@code .class} files
     * @throws ImplerException if en error occurs when working with {@code .jar} file
     */
    private void buildJar(Class<?> token, Path jarFile, Path temp) throws ImplerException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String localName = getModifiedPackageName(token, '/') + "/" + token.getSimpleName() + IMPL_SUFFIX + CLASS;
            out.putNextEntry(new ZipEntry(localName));
            Files.copy(temp.resolve(localName), out);
        } catch (IOException e) {
            throw new ImplerException("Error when working with jar file", e);
        }
    }

    /**
     * Deletes the specified directory and all its contents.
     *
     * @param path the location of the directory to be deleted
     * @throws ImplerException if an error occurs during directory deletion
     */
    private static void deleteRecursively(Path path) throws ImplerException {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    file.toFile().deleteOnExit();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    dir.toFile().deleteOnExit();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ImplerException("Error while cleaning temporary directory " + path.toAbsolutePath(), e);
        }
    }
}
