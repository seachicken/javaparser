package inga;

import com.github.javaparser.JavaParser;
import com.github.javaparser.serialization.JavaParserJsonSerializer;

import javax.json.Json;
import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try {
            new JavaParser()
                    .parse(Paths.get(args[0]))
                    .getResult()
                    .ifPresent(ast -> new JavaParserJsonSerializer()
                            .serialize(ast, Json.createGenerator(System.out)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
