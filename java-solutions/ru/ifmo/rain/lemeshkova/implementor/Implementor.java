
package ru.ifmo.rain.lemeshkova.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Code generating implementation of the {@code Impler} interface.
 * Capable of generating {@code .java} files for classes,
 * implementing the provided {@code Class} token using {@code Reflection API}.
 *
 * @author Natalia Lemeshkova
 * @see Impler
 * @see JarImpler
 * @see java.lang.reflect
 */
public class Implementor implements Impler {
    /**
     * System defined line separator for generated {@code .java} files.
     */
    private final String DOUBLE_SEP = System.lineSeparator() + System.lineSeparator();

    /**
     * Suffix, defining the name of the resulting class.
     */
    protected final String IMPL_SUFFIX = "Impl";

    /**
     * Extension for generated {@code .java} files.
     */
    private static final String JAVA = ".java";

    /**
     * Produces code implementing the class or interface specified by provided {@code token}.
     * The generated {@code .java} file location is specified by {@code root}.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        if (token == null || root == null) throw new ImplerException("Invalid arguments");
        checkImplementable(token);
        Path fullPackagePath = getFullPath(root, token);
        createDirectories(fullPackagePath);
        createFile(fullPackagePath);
        try (BufferedWriter writer = Files.newBufferedWriter(fullPackagePath, StandardCharsets.UTF_8)) {
            writer.write(escape(createClassImplementation(token)));
        } catch (IOException e) {
            throw new ImplerException("Cannot write class in file", e);
        }
    }

    /**
     * Check if the provided {@code token} can be implemented or throw {@link ImplerException} it impossible.
     * Cases to throw {@link ImplerException} as if token is one of such types:
     * <ul>
     *     <li>primitive type</li>
     *     <li>array type</li>
     *     <li>final</li>
     *     <li>private</li>
     *     <li>Enum</li>
     * </ul>
     *
     * @param token the type of token
     * @throws ImplerException if provided token cannot be implemented
     */
    private void checkImplementable(Class<?> token) throws ImplerException {
        if (token.isPrimitive()
                || token.isArray()
                || token == Enum.class
                || Modifier.isFinal(token.getModifiers())
                || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Cannot Implement " + token.toString());
        }
    }

    /**
     * Generates full {@code token} implementation class source code.
     * Generates a compile error free implementation of {@code token},
     * ready to be exported to a {@code .java} file.
     *
     * @param token the type token to be implemented
     * @return a {@code StringBuilder} representation of {@code token} implementation
     * @throws ImplerException if the implementation cannot be generated,
     *                         due to absence of non-private constructors of {@code token}
     * @see #formatHeader(Class)
     * @see #createBody(Class, StringBuilder)
     */
    private StringBuilder createClassImplementation(Class<?> token) throws ImplerException {
        StringBuilder code = formatHeader(token);
        createBody(token, code);
        code.append(DOUBLE_SEP).append("}");
        return code;
    }

    /**
     * Generate body of class implementation for token and add this code to {@code StringBuilder }.
     * Body consists of constructor(if token doesn't represents interface) and all abstract unrealized methods.
     *
     * @param token the type token to be implemented
     * @param code  previous code to wich the body code is added
     * @throws ImplerException if the implementation cannot be generated,
     *                         due to absence of non-private constructors of {@code token}
     */
    private void createBody(Class<?> token, StringBuilder code) throws ImplerException {
        if (!token.isInterface()) code.append(createConstructor(token));
        List<MethodImpl> methods = getAbstractNotPrivateMethods(token);
        code.append(methods.stream().map(MethodImpl::toString).collect(Collectors.joining(DOUBLE_SEP)));
    }

    /**
     * Generates a {@code token} implementation constructor.
     * If the {@code token} is an {@code interface}, returns an empty {@code String}.
     * Otherwise, generates a constructor based on an arbitrary non-private constructor of {@code token}.
     * The generated constructor immediately calls {@code super(...)}.
     *
     * @param token the type token
     * @return a {@code String} representation of an arbitrary {@code token} implementation constructor,
     * or an empty {@code String}, if the constructor is not required
     * @throws ImplerException if a constructor is required, but no non-private constructors of {@code token} are found
     * @see #formatConstructor (Constructor)
     */
    private String createConstructor(Class<?> token) throws ImplerException {
        return Arrays.stream(token.getDeclaredConstructors()).filter(x -> !Modifier.isPrivate(x.getModifiers())).
                findAny().map(this::formatConstructor).orElseThrow(() -> new ImplerException("No implementable constructors"));
    }

    /**
     * Generate the header of implementation consisting of package and class declaration.
     * Class declaration consists of name and implemented(or extended) name of token class.
     *
     * @param token the type token
     * @return a {@code StringBuilder} consists of package and class declaration
     */
    private StringBuilder formatHeader(Class<?> token) {
        StringBuilder code = formatPackage(token);
        String action = (token.isInterface()) ? "implements" : "extends";
        code.append(String.format("public class %s%s %s %s{" + DOUBLE_SEP, token.getSimpleName(), IMPL_SUFFIX, action, token.getCanonicalName()));
        return code;
    }

    /**
     * Returns the package declaration for specified {@code token}.
     *
     * @param token the type token
     * @return a {@code StringBuilder} representing the package declaration of provided {@code token},
     * or an empty {@code StringBuilder} if the package is default.
     */
    private StringBuilder formatPackage(Class<?> token) {
        return new StringBuilder(token.getPackage().toString() + ";" + DOUBLE_SEP);
    }

    /**
     * Generates a {@code String} representation of the provided {@code constructor}.
     * Uses {@link Constructor#getModifiers()}, class name, {@link Constructor#getParameterTypes()},
     * {@link Constructor#getExceptionTypes()}, and body to generate the constructor.
     * The default body immediately calls {@code super(...)}.
     *
     * @param constructor the constructor
     * @return a {@code String} representation of the provided {@code constructor}
     * @see #formatMethod(String, String, String, String, String, String)
     * @see #formatArguments(int)
     * @see #formatExceptions(Class[])
     */
    private String formatConstructor(Constructor<?> constructor) {
        return formatMethod(formatAccessModifier(constructor.getModifiers()), "",
                constructor.getDeclaringClass().getSimpleName() + IMPL_SUFFIX,
                formatParameters(constructor.getParameterTypes()), formatExceptions(constructor.getExceptionTypes()),
                "super(" + formatArguments(constructor.getParameterTypes().length) + ");") + DOUBLE_SEP;
    }

    /**
     * Returns a {@code List} of {@link MethodImpl} of {@code token}. Scans the {@code token}
     * and its superclasses for available {@code abstract} methods in it's interfaces and parents classes until reach not abstract class.
     * Uses a {@code MethodImpl} objects and {@code HashMap} to avoid duplicate methods.
     *
     * @param token the type token
     * @return a {@code List} of available {@code abstract} methods
     * @see MethodImpl
     */
    private List<MethodImpl> getAbstractNotPrivateMethods(Class<?> token) {
        Set<MethodImpl> abstractMethods = new HashSet<>();
        Set<MethodImpl> finalMethods = new HashSet<>();
        while (token != null) {
            if (!Modifier.isAbstract(token.getModifiers())) break;
            Stream.concat(
                    Arrays.stream(token.getInterfaces()).flatMap((Class<?> interface_) -> Arrays.stream(interface_.getMethods())),
                    Arrays.stream(token.getDeclaredMethods()).filter((x) -> Modifier.isAbstract(x.getModifiers())
                            && !Modifier.isPrivate(x.getModifiers()))).map(MethodImpl::new).collect(Collectors.toCollection(()->abstractMethods));
            Arrays.stream(token.getDeclaredMethods()).filter((x) -> Modifier.isFinal(x.getModifiers())
                    && !Modifier.isPrivate(x.getModifiers())).map(MethodImpl::new).collect(Collectors.toCollection(()->finalMethods));
            token = token.getSuperclass();
        }
        abstractMethods.removeAll(finalMethods);
        return abstractMethods.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Generates a {@code String} representation of a function, using its modifiers,
     * parameters, body, and additional qualifiers.
     *
     * @param accessModifier an access modifier represented in string format
     * @param returnType     string representation of return type
     * @param methodName     string representation of methodName
     * @param parameters     string representation of parameters(declaration consists of type parameters and names of paramenters
     * @param exceptions     string representation of exceptions, that throws function(might be an empty string)
     * @param body           string representation body function(might be an empty string)
     * @return {@code string} format of method
     * @see #formatExceptions(Class[])
     * @see #formatAccessModifier(int)
     * @see #formatArguments(int)
     * @see #formatMethodBody(Class)
     */
    private String formatMethod(String accessModifier, String returnType, String methodName, String parameters, String exceptions, String body) {
        return String.format("%s %s %s(%s) %s {%s}", accessModifier, returnType, methodName, parameters, exceptions, body);
    }

    /**
     * Returns the string represented access modifier based on provided {@code modifiers}.
     *
     * @param modifiers set of modifiers
     * @return a modifier in string format, representing the access modifier of {@code mod} alone
     */
    private String formatAccessModifier(int modifiers) {
        if (Modifier.isPrivate(modifiers)) return "private";
        if (Modifier.isProtected(modifiers)) return "protected";
        if (Modifier.isPublic(modifiers)) return "public";
        return "";
    }

    /**
     * Return the string representation of parameters with type names and autogenerated names of variables
     *
     * @param params arguments to represent
     * @return string with type names of parameters, divided by comma
     */
    private String formatParameters(Class<?>[] params) {
        AtomicInteger counter = new AtomicInteger(0);
        return Arrays.stream(params).map(x -> x.getCanonicalName() + " a" + counter.getAndIncrement()).collect(Collectors.joining(", "));
    }

    /**
     * Return string representation of all exception in class in request compilable format
     *
     * @param exceptions an array of {@code Class} objects, describing the exception to implement
     * @return string, started with "throws " and enumeration of all exceptions name if {@code exceptions} not empty
     * else return empty string
     */
    private String formatExceptions(Class<?>[] exceptions) {
        if (exceptions.length == 0) return "";
        return "throws " + Arrays.stream(exceptions).map(Class::getCanonicalName).collect(Collectors.joining(", "));
    }

    /**
     * Generates enumeration of names for parameters size of length.
     * Using for calling superclass constructor and passing arguments names.
     *
     * @param length size of producing names for enumeration of parameters
     * @return {@code string} in format {@code "a0, a1, a2, ... , an} where n is length-1;
     * @see #formatConstructor(Constructor)
     */
    private String formatArguments(int length) {
        return IntStream.range(0, length).mapToObj(x -> "a" + x).collect(Collectors.joining(", "));
    }

    /**
     * Return string representation of body for method.
     *
     * @param token class representing return type
     * @return {@code String} consists of "return " default value of return type parameter
     */
    private String formatMethodBody(Class<?> token) {
        if (token.equals(Void.TYPE)) return "";
        String defaultValue = getDefaultValue(token);
        return String.format("return %s;", defaultValue);
    }

    private String getDefaultValue(Class<?> token) {
        if (!token.isPrimitive()) {
            return "null";
        }
        if (token.equals(Boolean.TYPE)) {
            return "false";
        }
        return "0";
    }

    /**
     * Return full path to {@code .java} file considering the {@code root} and {@code token} package.
     *
     * @param root  base directory
     * @param token type token to create implementation for
     * @return the full path to {@code token} implementation file
     */
    protected Path getFullPath(Path root, Class<?> token) {
        return root.resolve(Path.of(getModifiedPackageName(token, File.separatorChar), token.getSimpleName() + IMPL_SUFFIX + JAVA));
    }

    /**
     * Return modified string representation of class package with replaced {@code .}  to separator
     *
     * @param token     class which package will be modified
     * @param separator character to replace {@code .} in package name
     * @return string, representing modified package name with replaced {@code .}
     */
    protected String getModifiedPackageName(Class<?> token, char separator) {
        return token.getPackageName().replace('.', separator);
    }

    /**
     * Encodes the provided {@code StringBuilder}, escaping all unicode characters in {@code \\u} notation.
     *
     * @param s the {@code StringBuilder} to be encoded
     * @return the encoded {@code String}
     */
    private String escape(StringBuilder s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            sb.append(c < 128 ? String.valueOf(c) : String.format("\\u%04x", (int) c));
        }
        return sb.toString();
    }

    /**
     * Create all directories in which current path will be located
     *
     * @param fullPath path which parents directories need to be created
     * @throws ImplerException throws if any error while creating directories have occurred
     */
    protected void createDirectories(Path fullPath) throws ImplerException {
        try {
            Path parent = fullPath.getParent();
            if (parent != null){
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ImplerException("Cannot create directories for file '" + fullPath + "'", e);
        }
    }

    /**
     * Creates a temporary directory at the specified location.
     *
     * @param root the location for the temporary directory
     * @return {@code Path} object locating of created temporary directory
     * @throws ImplerException if an error occurs while creating the temporary directory,
     *                         or an invalid path was provided
     */
    public static Path createTempDirectory(Path root) throws ImplerException {
        try {
           return Files.createTempDirectory(root.toAbsolutePath().getParent(), "jar-implementor");
//            return Files.createTempDirectory(root.toAbsolutePath(), "jar-implementor");
        } catch (IOException e) {
            throw new ImplerException("Could not create temporary directory");
        }
    }

    /**
     * Create file with specified name and location or do nothing if such file already exists
     *
     * @param fullPath path representing name and location of creating file
     * @throws ImplerException if any error occurred while creating file
     */
    private void createFile(Path fullPath) throws ImplerException {
        if (!Files.exists(fullPath)) {
            try {
                Files.createFile(fullPath.toAbsolutePath());
            } catch (IOException ex) {
                throw new ImplerException("Cannot create file '" + fullPath.getParent() + "'", ex);
            }
        }
    }

    /**
     * Class wrapping {@code Method} and containing necessary fields and properties for implementing class and methods to compiling.
     * Contains attributes of return type, parameters classes, accessModifier and name of method,
     * provides {@link #equals(Object)} and {@link #hashCode()} implementations based on class fields, ignoring access modifier.
     */
    private class MethodImpl {
        /**
         * Return type of {@code Method} object.
         */
        Class<?> returnType;

        /**
         * Parameters types of {@code Method} object.
         */
        Class<?>[] parameters;

        /**
         * String representation of access modifier of {@code Method} object.
         */
        String accessModifier;

        /**
         * Name of {@code Method} object.
         */
        String name;

        /**
         * Constructor creates a new {@code MethodImpl} instance, getting all attributes for {@code MethodImpl} from {@code Method}.
         *
         * @param method the method to be wrapper
         */
        public MethodImpl(Method method) {
            returnType = method.getReturnType();
            parameters = method.getParameterTypes();
            name = method.getName();
            accessModifier = formatAccessModifier(method.getModifiers());
        }

        /**
         * Comparing function for this {@code MethodImpl} and the specified object. Returns
         * true if the objects are the same. Two {@code MethodImpl} are the same if
         * their have th same name, return type and parameters types.
         *
         * @param obj object with which to compare
         * @return {@code true} if this object has the same name, return type and parameters types as the {@code obj}
         * argument, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (!(obj instanceof MethodImpl)) return false;
            MethodImpl method = (MethodImpl) obj;
            return (method.name.equals(name) && method.returnType.equals(returnType) && Arrays.equals(method.parameters, parameters));
        }

        /**
         * Represents method in implementing compiling format
         *
         * @return string code of method which can be compiled
         * @see #formatMethod(String, String, String, String, String, String)
         */
        @Override
        public String toString() {
            return formatMethod(accessModifier, returnType.getCanonicalName(), name, formatParameters(this.parameters), "", formatMethodBody(returnType));
        }

        /**
         * Returns a hashcode for {@code MethodImpl}. The hashcode is computed
         * using the hashcodes for fields name and parameter types.
         *
         * @return hash code value for this object.
         * @see Objects#hashCode(Object)
         */
        @Override
        public int hashCode() {
            return Objects.hash(name.hashCode(), returnType.hashCode(), List.of(parameters).hashCode());
        }
    }

}