package inga;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import inga.model.*;

import javax.lang.model.element.Modifier;
import javax.tools.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JavaParser implements Parser {
    private String className = "";

    public JCTree parse(Path path, boolean withAnalyze, String classPath) {
        className = "";
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            var objects = fileManager.getJavaFileObjects(path.toFile());
            DiagnosticListener<JavaFileObject> silence = diagnostic -> {};
            List<String> options = null;
            if (withAnalyze) {
                options = Arrays.asList(
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
                        Collections.singletonList(parse(
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
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCPackageDecl) {
            var packageDecl = (com.sun.tools.javac.tree.JCTree.JCPackageDecl) tree;
            return new JCPackageDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    packageDecl.getPackageName().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCImport) {
            var jcImport = (com.sun.tools.javac.tree.JCTree.JCImport) tree;
            return new JCImport(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    jcImport.getQualifiedIdentifier().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl) {
            var classDecl = (com.sun.tools.javac.tree.JCTree.JCClassDecl) tree;
            className = classDecl.name.toString();
            return new JCClassDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    classDecl.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass) {
            var newClass = (com.sun.tools.javac.tree.JCTree.JCNewClass) tree;
            String name;
            if (newClass.clazz instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply) {
                var typeApply = (com.sun.tools.javac.tree.JCTree.JCTypeApply) newClass.clazz;
                name = typeApply.clazz.toString();
            } else {
                name = newClass.clazz.toString();
            }
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    name,
                    getFqName(newClass)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl) {
            var methodDecl = (com.sun.tools.javac.tree.JCTree.JCMethodDecl) tree;
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    normarizeMethodName(methodDecl.name.toString(), className),
                    getFqName(methodDecl)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMemberReference) {
            var memberReference = (com.sun.tools.javac.tree.JCTree.JCMemberReference) tree;
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    normarizeMethodName(memberReference.name.toString(), className),
                    getFqName(memberReference)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl) {
            var variableDecl = (com.sun.tools.javac.tree.JCTree.JCVariableDecl) tree;
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    variableDecl.name.toString(),
                    getFqClassName(variableDecl.type)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply) {
            var typeApply = (com.sun.tools.javac.tree.JCTree.JCTypeApply) tree;
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    typeApply.clazz.toString(),
                    ""
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess) {
            var fieldAccess = (com.sun.tools.javac.tree.JCTree.JCFieldAccess) tree;
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    fieldAccess.name.toString(),
                    getFqName(fieldAccess)
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree) {
            var primitiveTypeTree = (com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    primitiveTypeTree.typetag.name()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCLiteral) {
            var literal = (com.sun.tools.javac.tree.JCTree.JCLiteral) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    literal.value == null ? "" : literal.value.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCIdent) {
            var ident = (com.sun.tools.javac.tree.JCTree.JCIdent) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    ident.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCArrayTypeTree) {
            var arrayType = (com.sun.tools.javac.tree.JCTree.JCArrayTypeTree) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    arrayType.getType().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAnnotation) {
            var annotation = (com.sun.tools.javac.tree.JCTree.JCAnnotation) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    annotation.annotationType.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCModifiers) {
            var modifiers = (com.sun.tools.javac.tree.JCTree.JCModifiers) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    modifiers.getFlags().stream().map(Modifier::name).collect(Collectors.joining(","))
            );
        } else {
            return new JCTree(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList())
            );
        }
    }

    private List<com.sun.tools.javac.tree.JCTree> getChildren(com.sun.tools.javac.tree.JCTree tree) {
        var results = new ArrayList<com.sun.tools.javac.tree.JCTree>();
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCCompilationUnit) {
            var unit = (com.sun.tools.javac.tree.JCTree.JCCompilationUnit) tree;
            results.addAll(unit.defs);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl) {
            var classDecl = (com.sun.tools.javac.tree.JCTree.JCClassDecl) tree;
            results.add(classDecl.mods);
            if (classDecl.extending != null) {
                results.add(classDecl.extending);
            }
            results.addAll(classDecl.defs);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass) {
            var newClass = (com.sun.tools.javac.tree.JCTree.JCNewClass) tree;
            results.addAll(newClass.args);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewArray) {
            var newArray = (com.sun.tools.javac.tree.JCTree.JCNewArray) tree;
            if (newArray.elems != null) {
                results.addAll(newArray.elems);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl) {
            var methodDecl = (com.sun.tools.javac.tree.JCTree.JCMethodDecl) tree;
            results.add(methodDecl.mods);
            results.addAll(methodDecl.params);
            if (methodDecl.body != null) {
                results.add(methodDecl.body);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCLambda) {
            var lambda = (com.sun.tools.javac.tree.JCTree.JCLambda) tree;
            results.addAll(lambda.params);
            if (lambda.body != null) {
                results.add(lambda.body);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl) {
            var variableDecl = (com.sun.tools.javac.tree.JCTree.JCVariableDecl) tree;
            results.add(variableDecl.mods);
            if (variableDecl.vartype != null) {
                results.add(variableDecl.vartype);
            }
            if (variableDecl.init != null) {
                results.add(variableDecl.init);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCIf) {
            var jcIf = (com.sun.tools.javac.tree.JCTree.JCIf) tree;
            if (jcIf.thenpart != null) {
                results.add(jcIf.thenpart);
            }
            if (jcIf.elsepart != null) {
                results.add(jcIf.elsepart);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAssign) {
            var jcAssign = (com.sun.tools.javac.tree.JCTree.JCAssign) tree;
            if (jcAssign.lhs != null) {
                results.add(jcAssign.lhs);
            }
            if (jcAssign.rhs != null) {
                results.add(jcAssign.rhs);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCReturn) {
            var jcReturn = (com.sun.tools.javac.tree.JCTree.JCReturn) tree;
            if (jcReturn.expr != null) {
                results.add(jcReturn.expr);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCTry) {
            var jcTry = (com.sun.tools.javac.tree.JCTree.JCTry) tree;
            results.addAll(jcTry.resources);
            results.add(jcTry.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCWhileLoop) {
            var loop = (com.sun.tools.javac.tree.JCTree.JCWhileLoop) tree;
            results.add(loop.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCDoWhileLoop) {
            var loop = (com.sun.tools.javac.tree.JCTree.JCDoWhileLoop) tree;
            results.add(loop.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCBlock) {
            var block = (com.sun.tools.javac.tree.JCTree.JCBlock) tree;
            results.addAll(block.stats);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCExpressionStatement) {
            var expressionStatement = (com.sun.tools.javac.tree.JCTree.JCExpressionStatement) tree;
            results.add(expressionStatement.expr);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodInvocation) {
            var methodInvocation = (com.sun.tools.javac.tree.JCTree.JCMethodInvocation) tree;
            results.add(methodInvocation.meth);
            results.addAll(methodInvocation.args);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess) {
            var fieldAccess = (com.sun.tools.javac.tree.JCTree.JCFieldAccess) tree;
            results.add(fieldAccess.selected);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCModifiers) {
            var modifiers = (com.sun.tools.javac.tree.JCTree.JCModifiers) tree;
            results.addAll(modifiers.annotations);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAnnotation) {
            var annotation = (com.sun.tools.javac.tree.JCTree.JCAnnotation) tree;
            results.addAll(annotation.args);
        }
        return results;
    }

    private String getFqName(com.sun.tools.javac.tree.JCTree tree) {
        if (tree.type == null) {
            return "";
        }

        String result = "";
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass) {
            var newClass = (com.sun.tools.javac.tree.JCTree.JCNewClass) tree;
            result = getFqClassName(newClass.type) + "." + newClass.type.tsym.name
                    + (newClass.args.isEmpty() ? "" : "-")
                    + newClass.args.stream().map(a -> getFqClassName(a.type)).collect(Collectors.joining("-"));
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess) {
            var fieldAccess = (com.sun.tools.javac.tree.JCTree.JCFieldAccess) tree;
            result = getFqClassName(fieldAccess.selected.type) + "." + fieldAccess.name.toString()
                    + (fieldAccess.type.getParameterTypes().isEmpty() ? "" : "-")
                    + fieldAccess.type.getParameterTypes().stream().map(this::getFqClassName).collect(Collectors.joining("-"));
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMemberReference) {
            var memberReference = (com.sun.tools.javac.tree.JCTree.JCMemberReference) tree;
            result = getFqClassName(memberReference.expr.type) + "." + normarizeMethodName(memberReference.name.toString(), ((com.sun.tools.javac.tree.JCTree.JCIdent) memberReference.expr).name.toString())
                    + (memberReference.mode == MemberReferenceTree.ReferenceMode.INVOKE
                    ? (memberReference.type.allparams().isEmpty() ? "" : "-") + memberReference.type.allparams().stream().map(this::getFqClassName).collect(Collectors.joining("-"))
                    : (memberReference.expr.type.allparams().isEmpty() ? "" : "-") + memberReference.expr.type.allparams().stream().map(this::getFqClassName).collect(Collectors.joining("-")));
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl) {
            var method = (com.sun.tools.javac.tree.JCTree.JCMethodDecl) tree;
            result = method.sym.owner.flatName() + "." + normarizeMethodName(method.name.toString(), className)
                    + (method.type.getParameterTypes().isEmpty() ? "" : "-")
                    + method.type.getParameterTypes().stream().map(this::getFqClassName).collect(Collectors.joining("-"));
        }
        return result;
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

    private String normarizeMethodName(String methodName, String className) {
        return methodName.equals("<init>") ? className : methodName;
    }
}
