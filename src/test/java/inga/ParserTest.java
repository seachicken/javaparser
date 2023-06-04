package inga;

import inga.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

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

        JCPackageDecl jcPackage = findChild(file, "PACKAGE");
        assertThat(jcPackage.getPackageName()).isEqualTo("fixtures.java");

        JCClassDecl jcClass = findChild(file, "CLASS");
        assertThat(jcClass.getName()).isEqualTo("Class");

        JCMethodDecl method = findChild(jcClass, "METHOD");
        assertThat(method.getName()).isEqualTo("method");
    }

    @Test
    void parseOverloadMethods() {
        var tree = parser.parse(readFile("java/Class.java"));
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");
        List<JCMethodDecl> methods = findChildren(jcClass, "METHOD");
        List<JCMethodDecl> overloadMethods = methods
                .stream()
                .filter(d -> d.getName().equals("methodOverload")).toList();

        JCExpression intType = findChild(findChild(overloadMethods.get(0), "VARIABLE"), "PRIMITIVE_TYPE");
        assertThat(intType.getName()).isEqualTo("INT");

        JCExpression longType = findChild(findChild(overloadMethods.get(1), "VARIABLE"), "PRIMITIVE_TYPE");
        assertThat(longType.getName()).isEqualTo("LONG");

        JCExpression stringType = findChild(findChild(overloadMethods.get(2), "VARIABLE"), "IDENTIFIER");
        assertThat(stringType.getName()).isEqualTo("String");
    }

    private <T extends JCTree> T findChild(JCTree tree, String type) {
        return (T) tree.getChildren().stream().filter(e -> e.getType().equals(type)).findFirst().get();
    }

    private <T extends JCTree> List<T> findChildren(JCTree tree, String type) {
        return (List<T>) tree.getChildren().stream().filter(e -> e.getType().equals(type)).toList();
    }

    private Path readFile(String path) {
        return Path.of(getClass().getClassLoader().getResource("fixtures/" + path).getFile());
    }
}