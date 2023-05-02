package inga;

import inga.model.JCClassDecl;
import inga.model.JCMethodDecl;
import inga.model.JCTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {
    private Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Parser();
    }

    @Test
    void parseClass() {
        var tree = parser.parse(readFile("java/Class.java"));
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");

        JCMethodDecl method = findChild(jcClass, "METHOD");
        assertThat(method.getName()).isEqualTo("method");
        assertThat(method.getFqName()).isEqualTo("ClassA.method");
    }

    private <T extends JCTree> T findChild(JCTree tree, String type) {
        return (T) tree.getChildren().stream().filter(e -> e.getType().equals(type)).findFirst().get();
    }

    private Path readFile(String path) {
        return Path.of(getClass().getClassLoader().getResource("fixtures/" + path).getFile());
    }
}