package info.kgeorgiy.ja.garipov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Allows to implement class extending/implementing given class/interface.
 * There are two abilities to generate implementation:
 * <ul>
 *     <li>generate {@code .java} file</li>
 *     <li>generate compiled {@code .class} file in {@code .jar} archive</li>
 * </ul>
 * @author Garipov Emil (emil2001garipov@gmail.com)
 */
public class Implementor implements JarImpler {
    /**
     * Left parenthesis for code generation.
     */
    private static final String LEFT_BRACKET = "(";
    /**
     * Right parenthesis for code generation.
     */
    private static final String RIGHT_BRACKET = ")";
    /**
     * Left brace for code generation.
     */
    private static final String LEFT_CODE_BRACKET = "{";
    /**
     * Right brace for code generation.
     */
    private static final String RIGHT_CODE_BRACKET = "}";
    /**
     * Implementation file name suffix for class name generation.
     */
    private static final String IMPL = "Impl";
    /**
     * Implementation file extension for class generation.
     */
    private static final String JAVA = "java";
    /**
     * Java Language Keyword {@code super} for code generation.
     */
    private static final String SUPER = "super";
    /**
     * Semicolon for code generation.
     */
    private static final String SEMICOLON = ";";
    /**
     * Java Language Keyword {@code throws} for code generation.
     */
    private static final String THROWS = "throws";
    /**
     * Comma for code generation.
     */
    private static final String COMMA = ",";
    /**
     * Dot for code generation.
     */
    private static final char DOT = '.';
    /**
     * Space for code generation.
     */
    private static final String SPACE = " ";
    /**
     * Java Language Keyword {@code public} for code generation.
     */
    private static final String PUBLIC = "public";
    /**
     * Java Language Keyword {@code protected} for code generation.
     */
    private static final String PROTECTED = "protected";
    /**
     * Java Language Keyword {@code return} for code generation.
     */
    private static final String RETURN = "return";
    /**
     * {@code false} string for code generation.
     */
    private static final String FALSE = "false";
    /**
     * {@code 0} char for code generation.
     */
    private static final String ZERO = "0";
    /**
     * {@code null} literal for code generation.
     */
    private static final String NULL = "null";
    /**
     * Java Language Keyword {@code class} for code generation.
     */
    private static final String CLASS = "class";
    /**
     * Java Language Keyword {@code implements} for code generation.
     */
    private static final String IMPLEMENTS = "implements";
    /**
     * Java Language Keyword {@code extends} for code generation.
     */
    private static final String EXTENDS = "extends";
    /**
     * Java Language Keyword {@code package} for code generation.
     */
    private static final String PACKAGE = "package";
    /**
     * Parameter name prefix for code generation.
     */
    private static final String PARAMETER_LETTER = "p";
    /**
     * Jar-archive Manifest version for .jar-archive creating.
     */
    private static final String MANIFEST_VERSION = "1.0";

    /**
     * {@link FileVisitor} removing content of visited directories.
     */
    private static final FileVisitor<Path> deleteVisitor = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Implementor Default constructor
     */
    public Implementor() {

    }

    /**
     * Creates a file against the root to generate implementation there.
     * Creates a file, where implementation will be created, in directory corresponding to the token package against the root.
     * @param token type token of implementing class.
     * @param root root path from which directories are resolved.
     * @return BufferedWriter which writes in created file.
     * @throws ImplerException if error occurs creating the file.
     */
    private BufferedWriter createFile(Class<?> token, Path root) throws ImplerException {
        Path implementedClassFile = root.resolve(Paths.get(
                token.getPackageName().replace(DOT, File.separatorChar), token.getSimpleName() + IMPL + DOT + JAVA));
        BufferedWriter output;
        try {
            Files.createDirectories(implementedClassFile.getParent());
            output = Files.newBufferedWriter(implementedClassFile);
        } catch (IOException exception) {
            throw new ImplerException("I/O error occurs creating the file :" + exception.getMessage());
        }
        return output;
    }

    /**
     * Initializes given class's constructors in the implementation file.
     * @param output if writing in the file isn't available.
     * @param token type token of implementing class.
     * @throws IOException if writing in the file isn't available.
     * @throws ImplerException if implementation can't be generated because the class has no available constructors.
     */
    private void initConstructors(BufferedWriter output, Class<?> token) throws IOException, ImplerException {
        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors()).filter(
                constructor -> !Modifier.isPrivate(constructor.getModifiers())).collect(Collectors.toList());
        if (token.getDeclaredConstructors().length != 0 && constructors.size() == 0) {
            throw new ImplerException("Extended class has no accessible constructor");
        }
        for (Constructor<?> constructor : constructors) {
            initModifiers(output, constructor.getModifiers());
            print(output, token.getSimpleName() + IMPL + LEFT_BRACKET);
            initParams(output, constructor.getParameterTypes());
            initThrows(output, constructor.getExceptionTypes());
            print(output, SUPER + LEFT_BRACKET);
            for (int i = 0; i < constructor.getParameterCount(); i++) {
                print(output, PARAMETER_LETTER + i);
                if (i + 1 < constructor.getParameterCount()) {
                    print(output, COMMA + SPACE);
                }
            }
            print(output, RIGHT_BRACKET + SEMICOLON);
            output.newLine();
            print(output, RIGHT_CODE_BRACKET);
            output.newLine();
            output.newLine();
        }
    }

    /**
     * Initializes given exceptions thrown by method or constructor in the implementation file.
     * @param output output file where implementation will be generated.
     * @param exceptionTypes array of exceptions thrown by method or constructor.
     * @throws IOException if writing in the file isn't available.
     */
    private void initThrows(BufferedWriter output, Class<?>[] exceptionTypes) throws IOException {
        if (exceptionTypes.length != 0) {
            print(output, SPACE + THROWS + SPACE);
            for (int j = 0; j < exceptionTypes.length; j++) {
                print(output, exceptionTypes[j].getCanonicalName());
                if (j + 1 < exceptionTypes.length) {
                    print(output, COMMA + SPACE);
                }
            }
        }
        print(output, SPACE + LEFT_CODE_BRACKET);
        output.newLine();
    }

    /**
     * Initializes constructor or method parameters in the implementation file.
     * @param output output file where implementation will be generated.
     * @param params constructor or method parameters.
     * @throws IOException if writing in the file isn't available.
     */
    private void initParams(BufferedWriter output, Class<?>[] params) throws IOException {
        for (int j = 0; j < params.length; j++) {
            print(output, params[j].getCanonicalName() + " " + PARAMETER_LETTER + j);
            if (j + 1 < params.length) {
                print(output, COMMA + SPACE);
            }
        }
        print(output, RIGHT_BRACKET);
    }

    /**
     * Initializes class, interface or method modifiers, given by {@code modifiers} value in the implementation file.
     *
     * @param output output file where implementation will be generated.
     * @param modifiers modifiers, which value is Java language modifiers.
     * @throws IOException if writing in the file isn't available.
     */
    private void initModifiers(BufferedWriter output, int modifiers) throws IOException {
        if (Modifier.isPublic(modifiers)) {
            print(output, PUBLIC + SPACE);
        } else if (Modifier.isProtected(modifiers)) {
            print(output, PROTECTED + SPACE);
        }
    }

    /**
     * Initializes method signature for a given class's method in the implementation file.
     * @param output output file where implementation will be generated.
     * @param method method token to generate body implementation for.
     * @throws IOException if writing in the file isn't available.
     */
    private void initMethodSignature(BufferedWriter output, Method method) throws IOException {
        initModifiers(output, method.getModifiers());
        Class<?> returnType = method.getReturnType();
        print(output, returnType.getCanonicalName() + SPACE + method.getName() + LEFT_BRACKET);
        initParams(output, method.getParameterTypes());
        initThrows(output, method.getExceptionTypes());
    }

    /**
     * Initializes method body of a given class's method in the implementation file.
     * @param output output file where implementation will be generated.
     * @param method method token to generate body implementation for.
     * @throws IOException if writing in the file isn't available.
     */
    private void initMethodBody(BufferedWriter output, Method method) throws IOException {
        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(void.class) && !returnType.equals(Void.class)) {
            print(output, RETURN + SPACE);
            if (returnType.isPrimitive()) {
                if (returnType.equals(boolean.class)) {
                    print(output, FALSE);
                } else {
                    print(output, ZERO);
                }
            } else {
                print(output, NULL);
            }
            print(output, SEMICOLON);
        }
        output.newLine();
    }

    /**
     * Wrapper class for class's {@link Method}.
     * This class overrides equals and hashCode methods in the following way: two wrapped methods are equals if and only
     * if their names and parameters are equals.
     */
    private static class MethodWrapper{
        /**
         * Inner method.
         */
        private final Method method;

        /**
         * Default Wrapper constructor
         * @param method wrapped method
         */
        public MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Returns inner method.
         * @return inner {@code Method}.
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Checks if this wrappedMethod is equals to given {@code Object}.
         * Overrided {@link Object#equals(Object)} method:
         *
         * @param o the object with which the equality is checked
         * @return true if and only if their names and parameters are equals, and false otherwise.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodWrapper that = (MethodWrapper) o;
            return method.getName().equals(that.getMethod().getName()) &&
                    Arrays.equals(method.getParameterTypes(), that.getMethod().getParameterTypes());
        }

        /**
         * Calculates this wrapped method hashCode via polynomial hash.
         * Overrided {@link Object#hashCode()} method. Hash value calculating based on this wrapped method name and
         * parameters.
         * @return hash code of this wrapped method.
         */
        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + method.getName().hashCode();
            result = result * 37 + Arrays.hashCode(method.getParameterTypes());
            return result;
        }
    }

    /**
     * Initializes methods of a given class in the implementation file.
     * @param output file where implementation will be generated.
     * @param token class type token to create an implementation for.
     * @throws IOException if writing in the file isn't available.
     */
    private void initMethods(BufferedWriter output, Class<?> token) throws IOException {
        // get abstract methods
        HashSet<MethodWrapper> implementedMethods = new HashSet<>();
        Class<?> parent = token;
        while (parent != null) {
            List<MethodWrapper> methods =
            Arrays.stream(parent.getDeclaredMethods()).map(MethodWrapper::new).collect(Collectors.toList());
            for (MethodWrapper methodWrapper : methods) {
                Method method = methodWrapper.getMethod();
                int modifiers = method.getModifiers();
                if (Modifier.isPrivate(modifiers)) {
                    continue;
                }
                if (Modifier.isAbstract(modifiers)) {
                    if (!implementedMethods.contains(methodWrapper)) {
                        implementedMethods.add(methodWrapper);
                        initMethodSignature(output, methodWrapper.getMethod());
                        initMethodBody(output, methodWrapper.getMethod());
                        print(output, RIGHT_CODE_BRACKET);
                        output.newLine();
                    }
                } else {
                    implementedMethods.add(methodWrapper);
                }
            }
            parent = parent.getSuperclass();
        }

        // get methods from Interfaces
        for (int i = 0; i < Objects.requireNonNull(token).getMethods().length; i++) {
            Method method = token.getMethods()[i];
            if (Modifier.isAbstract(method.getModifiers()) && !implementedMethods.contains(new MethodWrapper(method))) {
                initMethodSignature(output, method);
                initMethodBody(output, method);

                print(output, RIGHT_CODE_BRACKET);
                output.newLine();
                output.newLine();
            }
        }
    }

    /**
     * Initializes the signature of a given class in the implementation file.
     * @param output file where implementation will be generated.
     * @param token class type token to create an implementation for.
     * @throws IOException if writing in the file isn't available.
     * @throws ImplerException if implementation can't be generated because the given class is final or private.
     */
    private void initSignature(BufferedWriter output, Class<?> token) throws IOException, ImplerException {
        int modifiers = token.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Can't extends/implements private class/interface");
        }
        if (Modifier.isFinal(modifiers)) {
            throw new ImplerException("Can't extends final class");
        }
        if (Modifier.isPublic(modifiers)) {
            print(output, PUBLIC + SPACE);
        }
        print(output, CLASS + SPACE + token.getSimpleName() + IMPL + SPACE);
        if (token.isInterface()) {
           print(output, IMPLEMENTS + SPACE);
        } else {
            print(output, EXTENDS + SPACE);
        }
        print(output, token.getCanonicalName() + SPACE + LEFT_CODE_BRACKET);
        output.newLine();
    }

    /**
     * Initializes the package of a given class in the implementation file.
     * @param output file where implementation will be generated.
     * @param token class type token to create an implementation for.
     * @throws IOException if writing in the file isn't available.
     */
    private void initPackage(BufferedWriter output, Class<?> token) throws IOException {
        String packageName = token.getPackageName();
        if (!packageName.equals("")) {
            print(output, PACKAGE + SPACE + packageName + SEMICOLON);
            output.newLine();
            output.newLine();
        }
    }

    /**
     * Generates implementation for given class.
     * Generates implementation for given class to the directory corresponding to the class package against the root.
     * @param token class type token to create an implementation for.
     * @param root root directory.
     * @throws ImplerException if implementation can't be generated for any reason.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        try (BufferedWriter output = createFile(token, root)) {
            if (token == Enum.class) {
                throw new ImplerException("Can't extends/implements an enum");
            }
            if (token.isArray()) {
                throw new ImplerException("Can't extends/implements an array");
            }
            if (token.isPrimitive()) {
                throw new ImplerException("Can't extends/implements a primitive");
            }
            initPackage(output, token);
            initSignature(output, token);
            if (!token.isInterface()) { // sure that it's a class
                initConstructors(output, token);
            }
            initMethods(output, token);
            print(output, RIGHT_CODE_BRACKET);
            output.newLine();
        } catch (IOException exception) {
            throw  new ImplerException("I/O error occurs :" + exception.getMessage());
        }
    }

    /**
     * Recursively removes content of given directories represented by {@code dir}
     * @param dir path to the directory that should be removed.
     * @throws IOException if I/O error occurs recursively visiting the directory.
     */
    public void removeDir(Path dir) throws IOException {
        Files.walkFileTree(dir, deleteVisitor);
    }

    /**
     * Resolves path of given class implementation file against the root.
     * @param root root directory path from which the paths are resolved.
     * @param clazz class type token.
     * @param suffix extension suffix, might be: {@code .java} or {@code .class}
     * @return path of {@code clazz} implementation file resolved against the {@code root}.
     */
    // resolves path of .java implementation file specified by clazz against the root
    public static Path implPath(final Path root, final Class<?> clazz, String suffix) {
        // return root.resolve(getImplName(clazz).replace(".", File.separator) + ".java").toAbsolutePath();
        return root.resolve(Paths.get(
                clazz.getPackageName().replace(DOT, File.separatorChar),
                clazz.getSimpleName() + IMPL + DOT + suffix));
    }

    /**
     * Creates .jar-archive in the specified by {@code jarFile} directory contains compiled given class implementation
     * located in the directory represented by {@code dir}.
     * @param token class which compiled implementation will be putted into .jar-archive.
     * @param dir path representing the directory where given {@code token} class implementation is located.
     * @param jarFile output .jar-archive directory.
     * @throws ImplerException if creating/writing to jar file isn't available.
     */
    private void createJar(Class<?> token, Path dir, Path jarFile) throws ImplerException {
        Manifest man = new Manifest();
        man.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarFile.toFile()), man)) {
            String implFilePathZipEntryName = token.getPackageName().replace('.', '/') +
                    '/' + token.getSimpleName() + IMPL + DOT + CLASS;
            output.putNextEntry(new ZipEntry(implFilePathZipEntryName));
            Files.copy(implPath(dir, token, CLASS), output);
        } catch (IOException exception) {
            throw new ImplerException("Can't create/write to jar file:" + exception.getMessage());
        }
    }

    // compiles the .java file located in root and specified by token

    /**
     * Compiles the {@code token} .java implementation file located in the {@code root} directory.
     * {@code token } parameter specifies the packages of the .java implementation file which is already generated.
     * @param token class which already generated implementation will be compiled.
     * @param root the directory from which implementation directories are resolved.
     * @throws ImplerException if the implementation can't be generated.
     */
    private static void compileFile(Class<?> token, final Path root) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath;
        try {
            classpath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("URL cannot be converted");
        }
        String filePath = implPath(root, token, JAVA).toString();
        final String[] args ={"-cp", classpath, filePath};
        compiler.run(null, null, null, args);
    }

    /**
     * Creates a .jar-archive contains compiled implementation of the given {@code token} class.
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if implementation can't be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path targetDir = jarFile.toAbsolutePath().getParent();
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory(targetDir, "temp-dir");
            implement(token, tempDir);
            compileFile(token, tempDir);
            createJar(token, tempDir, jarFile);
        } catch (IOException exception) {
            throw  new ImplerException("I/O error occurs :" + exception.getMessage());
        } finally {
            try {
                removeDir(tempDir);
            } catch (IOException exception) {
                System.err.println("Can't remove tempDir(" + tempDir + "):" + exception.getMessage());
            }
        }
    }

    /**
     * Implementor launching with extra options method.
     * Allows to choose which way will be executed:
     * Generate .java given class implementation file
     * or
     * Generate .jar archive with the specified name contains given class compiled implementation .class file
     * @param args executing options.
     */
    public static void main(String[] args){
        boolean jar = false;
        String className;
        String jarPath = null;
        String rootDirectory = null;
        if (args[0].equals("-jar")) {
            jar = true;
            className = args[1];
            jarPath = args[2];
        } else {
            className = args[0];
            rootDirectory = ".";
        }
        try {
            if (!jar) {
                new Implementor().implement(Class.forName(className), Path.of(rootDirectory));
            } else {
                new Implementor().implementJar(Class.forName(className), Path.of(jarPath));
            }
        } catch (ImplerException exception) {
            System.err.println("Can't generate implementation: " + exception.getMessage());
        } catch (ClassNotFoundException expection) {
            System.err.println("Class not found: " + expection.getMessage());
        }
    }

    /**
     * Prints given string to the {@link BufferedWriter} output.
     * @param output represents file to write.
     * @param s value to write.
     * @throws IOException is writing isn't available.
     */
    private static void print(BufferedWriter output, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 127) {
                output.write(c);
            } else {
                output.write(String.format("\\u%04x", (int) c));
            }
        }
    }
}
