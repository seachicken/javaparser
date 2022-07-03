package inga;

import com.github.javaparser.JavaParser;
import com.github.javaparser.serialization.JavaParserJsonSerializer;

import javax.json.Json;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        JavaParser javaParser = new JavaParser();
        JavaParserJsonSerializer jsonSerializer = new JavaParserJsonSerializer();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String path = scanner.nextLine();
            try (var writer = new StringWriter();
                 var jsonGenerator = Json.createGenerator(writer)) {
                javaParser.parse(Paths.get(path))
                        .getResult()
                        .ifPresent(ast -> {
                            jsonSerializer.serialize(ast, jsonGenerator);
                            System.out.println(writer);
                        });
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
