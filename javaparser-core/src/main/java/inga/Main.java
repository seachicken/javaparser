package inga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import inga.model.JCTree;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        Parser parser = findParserByRuntimeJava();
        Scanner scanner = new Scanner(System.in);
        ObjectMapper mapper = new ObjectMapper();

        while (scanner.hasNextLine()) {
            List<String> inputs = Arrays.stream(scanner.nextLine().split(" "))
                    .filter(input -> !input.isEmpty())
                    .collect(Collectors.toList());
            if (inputs.isEmpty()) {
                continue;
            }

            String path = inputs.stream().findFirst().get();
            List<String> options = inputs.subList(1, inputs.size());
            int analyzeIndex = options.indexOf("--analyze");
            String classPath = analyzeIndex + 1 < options.size() ? options.get(analyzeIndex + 1) : "";
            JCTree tree = parser.parse(
                    Paths.get(path),
                    analyzeIndex != -1,
                    classPath
            );
            try {
                String json = mapper.writeValueAsString(tree);
                System.out.println(json);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static Parser findParserByRuntimeJava() {
        String javaVersion = System.getProperty("java.version");
        String[] splitVersion = javaVersion.split("[.-]");
        int version = Integer.parseInt(splitVersion[0]);
        // for Java 8, refer to the tools.jar in the runtime
        System.err.println("Java version: " + version);
        if (version == 1) {
            try {
                File toolsJar = new File(System.getProperty("java.home") + "/../lib/tools.jar");
                if (toolsJar.exists()) {
                    final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(ClassLoader.getSystemClassLoader(), toolsJar.toURI().toURL());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to add tools.jar to classpath", e);
            }
        }
        String parserName = "inga.JavaParser";
        try {
            return (Parser) Class
                    .forName(parserName)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ClassNotFoundException |
                 NoSuchMethodException |
                 InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException e) {
            throw new IllegalStateException(String.format("%s class is not found. javaVersion: %s", parserName, javaVersion), e);
        }
    }
}