package inga;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import inga.model.*;

import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticListener;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {
    private String className = "";

    public JCTree parse(Path path, boolean withAnalyze, String classPath) {
        className = "";
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
            var objects = fileManager.getJavaFileObjects(path.toFile());
            var silence = (DiagnosticListener) diagnostic -> {};
            List<String> options = null;
            if (withAnalyze) {
                options = List.of(
                        "-classpath", classPath,
                        "-proc:none"
                );
            }
            var task = (JavacTask) compiler.getTask(null, fileManager, silence, options, null, objects);
            var tree = new JCTree();
            for (var unit : task.parse()) {
                if (withAnalyze) {
                    task.analyze();
                }
                tree.setChildren(
                        List.of(parse(
                                (com.sun.tools.javac.tree.JCTree) unit,
                                (com.sun.tools.javac.tree.JCTree.JCCompilationUnit) unit
                        )));
            }
            return tree;
        } catch (IllegalStateException e) {
            e.printStackTrace(System.err);
            return null;
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
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    newClass.clazz instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply typeApply
                            ? typeApply.clazz.toString()
                            : newClass.clazz.toString(),
                    getFqName(newClass)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl methodDecl) {
            return new JCMethodDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    normarizeMethodName(methodDecl.name.toString(), className)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl variableDecl) {
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    variableDecl.name.toString(),
                    getFqClassName(variableDecl.type)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply typeApply) {
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    typeApply.clazz.toString(),
                    ""
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess) {
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).toList(),
                    fieldAccess.name.toString(),
                    getFqName(fieldAccess)
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
            results.addAll(jcTry.resources);
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

    private String normarizeMethodName (String methodName, String className) {
        return methodName.equals("<init>") ? className : methodName;
    }

    private String getFqName(com.sun.tools.javac.tree.JCTree tree) {
        if (tree.type == null) {
            return "";
        }

        return switch (tree) {
            case com.sun.tools.javac.tree.JCTree.JCNewClass newClass -> getFqClassName(newClass.type)
                    + "."
                    + newClass.type.tsym.name
                    + (newClass.args.isEmpty() ? "" : "-")
                    + newClass.args.stream().map(a -> getFqClassName(a.type)).collect(Collectors.joining("-"));
            case com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess ->
                    getFqClassName(fieldAccess.selected.type) + "." + fieldAccess.name.toString()
                            + (fieldAccess.type.getParameterTypes().isEmpty() ? "" : "-")
                            + fieldAccess.type.getParameterTypes().stream().map(this::getFqClassName).collect(Collectors.joining("-"));
            default -> "";
        };
    }

    private String getFqClassName(Type type) {
        if (type == null) {
            return "";
        }

        if (type.isPrimitive()) {
            return type.getTag().name();
        } else if (type.getTag() == TypeTag.ARRAY) {
            return type.toString();
        } else if (type instanceof Type.CapturedType) {
            return type.getUpperBound().tsym.flatName().toString();
        } else {
            return type.tsym.flatName().toString();
        }
    }
}
