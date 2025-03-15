package inga;

import inga.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class Java11ParserTest {
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaParser();
    }

    @Test
    void parseClass() {
        JCTree tree = parser.parse(readFile("java/Class.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");

        JCPackageDecl jcPackage = findChild(file, "PACKAGE");
        assertThat(jcPackage.getPackageName()).isEqualTo("fixtures.java");

        JCClassDecl jcClass = findChild(file, "CLASS");
        assertThat(jcClass.getName()).isEqualTo("Class");

        JCVariableDecl method = findChild(jcClass, "METHOD");
        assertThat(method.getName()).isEqualTo("method");
    }

    @Test
    void removeDiamondOperatorFromNewClass() {
        JCTree tree = parser.parse(readFile("java/ClassDiamondOperator.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");
        JCVariableDecl method = findChild(jcClass, "METHOD");
        JCVariableDecl newClass = findChild(findChild(findChild(method, "BLOCK"), "VARIABLE"), "NEW_CLASS");

        assertThat(newClass.getName()).isEqualTo("ArrayList");
    }

    @Test
    void parseOverloadMethods() {
        JCTree tree = parser.parse(readFile("java/Class.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");
        List<JCVariableDecl> methods = findChildren(jcClass, "METHOD");
        List<JCVariableDecl> overloadMethods = methods
                .stream()
                .filter(d -> d.getName().equals("methodOverload")).collect(Collectors.toList());

        JCExpression intType = findChild(findChild(overloadMethods.get(0), "VARIABLE"), "PRIMITIVE_TYPE");
        assertThat(intType.getName()).isEqualTo("INT");

        JCExpression longType = findChild(findChild(overloadMethods.get(1), "VARIABLE"), "PRIMITIVE_TYPE");
        assertThat(longType.getName()).isEqualTo("LONG");

        JCExpression stringType = findChild(findChild(overloadMethods.get(2), "VARIABLE"), "IDENTIFIER");
        assertThat(stringType.getName()).isEqualTo("String");
    }

    @Test
    void parseConstructor() {
        JCTree tree = parser.parse(readFile("java/ClassConstructor.java"), false, "");
        JCTree file = findChild(tree, "COMPILATION_UNIT");
        JCClassDecl jcClass = findChild(file, "CLASS");
        JCVariableDecl constructor = findChild(jcClass, "METHOD");

        assertThat(constructor.getName()).isEqualTo("Class");
    }

    @Nested
    class Spring {
        @Test
        void parseGetMethod() {
            JCTree tree = parser.parse(readFile("spring/RestController.java"), false, "");
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
        return (List<T>) tree.getChildren().stream().filter(e -> e.getType().equals(type)).collect(Collectors.toList());
    }

    private Path readFile(String path) {
        return Paths.get(getClass().getClassLoader().getResource("fixtures/" + path).getFile());
    }
}