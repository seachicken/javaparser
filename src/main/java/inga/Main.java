package inga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        var parser = new Parser();
        Scanner scanner = new Scanner(System.in);
        var mapper = new ObjectMapper();

        while (scanner.hasNextLine()) {
            var inputs = Arrays.stream(scanner.nextLine().split(" "))
                    .filter(input -> !input.isBlank())
                    .toList();
            if (inputs.isEmpty()) {
                continue;
            }

            var path = inputs.getFirst();
            var options = inputs.subList(1, inputs.size());
            var analyzeIndex = options.indexOf("--analyze");
            var classPath = analyzeIndex + 1 < options.size() ? options.get(analyzeIndex + 1) : "";
            var tree = parser.parse(
                    Path.of(path),
                    analyzeIndex != -1,
                    classPath
            );
            try {
                var json = mapper.writeValueAsString(tree);
                System.out.println(json);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
