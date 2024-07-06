package inga;

import com.sun.source.util.JavacTask;
import inga.model.*;

import javax.lang.model.element.Modifier;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {
    private String className = "";

    public JCTree parse(Path path) {
        className = "";
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
            var objects = fileManager.getJavaFileObjects(path.toFile());
            var task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, objects);
            var tree = new JCTree();
            for (var unit : task.parse()) {
                tree.setChildren(
                        List.of(parse(
                                (com.sun.tools.javac.tree.JCTree) unit,
                                (com.sun.tools.javac.tree.JCTree.JCCompilationUnit) unit
                        )));
            }
            return tree;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private JCTree parse(com.sun.tools.javac.tree.JCTree tree,
                         com.sun.tools.javac.tree.JCTree.JCCompilationUnit root) {
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCPackageDecl packageDecl) {
            return new JCPackageDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    packageDecl.getPackageName().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCImport jcImport) {
            return new JCImport(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    jcImport.getQualifiedIdentifier().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl classDecl) {
            className = classDecl.name.toString();
            return new JCClassDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    classDecl.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass newClass) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    newClass.clazz instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply typeApply
                            ? typeApply.clazz.toString()
                            : newClass.clazz.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl methodDecl) {
            return new JCMethodDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    methodDecl.name.toString().equals("<init>") ? className : methodDecl.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl variableDecl) {
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    variableDecl.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply typeApply) {
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    typeApply.clazz.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    fieldAccess.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree primitiveTypeTree) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    primitiveTypeTree.typetag.name()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCLiteral literal) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    literal.value == null ? "" : literal.value.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCIdent ident) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    ident.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCArrayTypeTree arrayType) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    arrayType.getType().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAnnotation annotation) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    annotation.annotationType.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCModifiers modifiers) {
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    modifiers.getFlags().stream().map(Modifier::name).collect(Collectors.joining(","))
            );
        } else {
            return new JCTree(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList()
            );
        }
    }

    private List<com.sun.tools.javac.tree.JCTree> getChildren(com.sun.tools.javac.tree.JCTree tree) {
        var results = new ArrayList<com.sun.tools.javac.tree.JCTree>();
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCCompilationUnit unit) {
            results.addAll(unit.defs);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl classDecl) {
            results.add(classDecl.mods);
            if (classDecl.extending != null) {
                results.add(classDecl.extending);
            }
            results.addAll(classDecl.defs);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass newClass) {
            results.addAll(newClass.args);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewArray newArray) {
            if (newArray.elems != null) {
                results.addAll(newArray.elems);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl methodDecl) {
            results.add(methodDecl.mods);
            results.addAll(methodDecl.params);
            if (methodDecl.body != null) {
                results.add(methodDecl.body);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCLambda lambda) {
            results.addAll(lambda.params);
            if (lambda.body != null) {
                results.add(lambda.body);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl variableDecl) {
            results.add(variableDecl.mods);
            if (variableDecl.vartype != null) {
                results.add(variableDecl.vartype);
            }
            if (variableDecl.init != null) {
                results.add(variableDecl.init);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCIf jcIf) {
            if (jcIf.thenpart != null) {
                results.add(jcIf.thenpart);
            }
            if (jcIf.elsepart != null) {
                results.add(jcIf.elsepart);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAssign jcAssign) {
            if (jcAssign.lhs != null) {
                results.add(jcAssign.lhs);
            }
            if (jcAssign.rhs != null) {
                results.add(jcAssign.rhs);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCReturn jcReturn) {
            if (jcReturn.expr != null) {
                results.add(jcReturn.expr);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCTry jcTry) {
            results.add(jcTry.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCWhileLoop loop) {
            results.add(loop.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCDoWhileLoop loop) {
            results.add(loop.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCBlock block) {
            results.addAll(block.stats);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCExpressionStatement expressionStatement) {
            results.add(expressionStatement.expr);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodInvocation methodInvocation) {
            results.add(methodInvocation.meth);
            results.addAll(methodInvocation.args);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess) {
            results.add(fieldAccess.selected);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCModifiers modifiers) {
            results.addAll(modifiers.annotations);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAnnotation annotation) {
            results.addAll(annotation.args);
        }
        return results;
    }
}
