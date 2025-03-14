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
        Parser parser = findParserFromRuntimeJavaVersion();
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

    private static Parser findParserFromRuntimeJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        String[] splitVersion = javaVersion.split("[.-]");
        int version = Integer.parseInt(splitVersion[0]);
        if (version == 1) {
            try {
                File toolsJar = new File(System.getProperty("java.home") + "/../lib/tools.jar");
                if (toolsJar.exists()) {
                    final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(ClassLoader.getSystemClassLoader(), toolsJar.toURI().toURL());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to add tools.jar to classpath", e);
            }
        }
        String parserName = "inga.JavaParser";
        try {
////            if (version >= 21) {
////                parserName = "inga.Java21Parser";
////            } else if (version >= 9) {
//            if (version >= 17) {
//                parserName = "inga.Java17Parser";
//            } else if (version >= 9) {
////            if (version >= 9) {
//                parserName = "inga.Java11Parser";
//            } else {
//            }
            System.out.println("version: " + version);

//            System.err.println(String.format("%s class is not found. javaVersion: %s", parserName, javaVersion));

//            Class.forName("inga.JavaToolsLoader")
//                    .getMethod("load")
//                    .invoke(null);
//            Thread.currentThread().getContextClassLoader().loadClass("com.sun.tools.javac.tree.JCTree");
//            try (URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader()) {
//                if (classLoader == null) {
//                    return null;
//                }
//                Stream.of(classLoader.getURLs())
//                        .map(URL::getPath)
//                        .collect(Collectors.toList());
//            } catch (NoClassDefFoundError | IOException e) {
//                e.printStackTrace(System.err);
//            }
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