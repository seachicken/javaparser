package inga;

import inga.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
        var tree = parser.parse(readFile("java/Class.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");

        JCPackageDecl jcPackage = findChild(file, "PACKAGE");
        assertThat(jcPackage.getPackageName()).isEqualTo("fixtures.java");

        JCClassDecl jcClass = findChild(file, "CLASS");
        assertThat(jcClass.getName()).isEqualTo("Class");

        JCMethodDecl method = findChild(jcClass, "METHOD");
        assertThat(method.getName()).isEqualTo("method");
    }

    @Test
    void removeDiamondOperatorFromNewClass() {
        var tree = parser.parse(readFile("java/ClassDiamondOperator.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");
        JCMethodDecl method = findChild(jcClass, "METHOD");
        JCVariableDecl newClass = findChild(findChild(findChild(method, "BLOCK"), "VARIABLE"), "NEW_CLASS");

        assertThat(newClass.getName()).isEqualTo("ArrayList");
    }

    @Test
    void parseOverloadMethods() {
        var tree = parser.parse(readFile("java/Class.java"), false, "");
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

    @Test
    void parseConstructor() {
        var tree = parser.parse(readFile("java/ClassConstructor.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");
        JCMethodDecl constructor = findChild(jcClass, "METHOD");

        assertThat(constructor.getName()).isEqualTo("Class");
    }

    @Nested
    class Spring {
        @Test
        void parseGetMethod() {
            var tree = parser.parse(readFile("spring/RestController.java"), false, "");
            JCTree file = findChild(tree, "COMPILATION_UNIT");
            JCClassDecl jcClass = findChild(file, "CLASS");

            List<JCExpression> annotations = findChildren(findChild(jcClass, "MODIFIERS"), "ANNOTATION");
            assertThat(annotations)
                    .extracting(JCExpression::getName)
                    .containsExactly("RestController", "RequestMapping");
            assertThat(annotations.get(0).getChildren())
                    .isEmpty();
            assertThat(annotations.get(1).getChildren())
                    .extracting(JCTree::getType)
                    .containsExactly("ASSIGNMENT");
        }
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