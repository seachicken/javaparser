package inga;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.TreeVisitor;
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

import static java.util.Collections.emptyList;

public class JavaParser implements Parser {
    private String className = "";

    public JCTree parse(Path path, boolean withAnalyze, String classPath) {
        className = "";
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> objects = fileManager.getJavaFileObjects(path.toFile());
            DiagnosticListener silence = diagnostic -> {};
            List<String> options = null;
            if (withAnalyze) {
                options = Arrays.asList(
                        "-classpath", classPath,
                        "-proc:none"
                );
            }
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, silence, options, null, objects);
            JCTree tree = new JCTree();
            for (CompilationUnitTree unit : task.parse()) {
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
        if (tree instanceof JCPackageDecl2) {
            return new JCPackageDecl(
                    "PACKAGE",
                    tree.getPreferredPosition(),
                    -1,
                    -1,
                    emptyList(),
                    ((JCPackageDecl2) tree).getPackageName()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCImport) {
            com.sun.tools.javac.tree.JCTree.JCImport jcImport = (com.sun.tools.javac.tree.JCTree.JCImport) tree;
            return new JCImport(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    jcImport.getQualifiedIdentifier().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl) {
            com.sun.tools.javac.tree.JCTree.JCClassDecl classDecl = (com.sun.tools.javac.tree.JCTree.JCClassDecl) tree;
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
            com.sun.tools.javac.tree.JCTree.JCNewClass newClass = (com.sun.tools.javac.tree.JCTree.JCNewClass) tree;
            String name;
            if (newClass.clazz instanceof com.sun.tools.javac.tree.JCTree.JCTypeApply) {
                com.sun.tools.javac.tree.JCTree.JCTypeApply typeApply = (com.sun.tools.javac.tree.JCTree.JCTypeApply) newClass.clazz;
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
            com.sun.tools.javac.tree.JCTree.JCMethodDecl methodDecl = (com.sun.tools.javac.tree.JCTree.JCMethodDecl) tree;
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
            com.sun.tools.javac.tree.JCTree.JCMemberReference memberReference = (com.sun.tools.javac.tree.JCTree.JCMemberReference) tree;
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
            com.sun.tools.javac.tree.JCTree.JCVariableDecl variableDecl = (com.sun.tools.javac.tree.JCTree.JCVariableDecl) tree;
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
            com.sun.tools.javac.tree.JCTree.JCTypeApply typeApply = (com.sun.tools.javac.tree.JCTree.JCTypeApply) tree;
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
            com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess = (com.sun.tools.javac.tree.JCTree.JCFieldAccess) tree;
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
            com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree primitiveTypeTree = (com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    primitiveTypeTree.typetag.name()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCLiteral) {
            com.sun.tools.javac.tree.JCTree.JCLiteral literal = (com.sun.tools.javac.tree.JCTree.JCLiteral) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    literal.value == null ? "" : literal.value.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCIdent) {
            com.sun.tools.javac.tree.JCTree.JCIdent ident = (com.sun.tools.javac.tree.JCTree.JCIdent) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    ident.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCArrayTypeTree) {
            com.sun.tools.javac.tree.JCTree.JCArrayTypeTree arrayType = (com.sun.tools.javac.tree.JCTree.JCArrayTypeTree) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    arrayType.getType().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAnnotation) {
            com.sun.tools.javac.tree.JCTree.JCAnnotation annotation = (com.sun.tools.javac.tree.JCTree.JCAnnotation) tree;
            return new JCExpression(
                    tree.getKind().name(),
                    tree.getPreferredPosition(),
                    tree.getStartPosition(),
                    tree.getEndPosition(root.endPositions),
                    getChildren(tree).stream().map(n -> parse(n, root)).collect(Collectors.toList()),
                    annotation.annotationType.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCModifiers) {
            com.sun.tools.javac.tree.JCTree.JCModifiers modifiers = (com.sun.tools.javac.tree.JCTree.JCModifiers) tree;
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
        ArrayList<com.sun.tools.javac.tree.JCTree> results = new ArrayList<>();
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCCompilationUnit) {
            com.sun.tools.javac.tree.JCTree.JCCompilationUnit unit = (com.sun.tools.javac.tree.JCTree.JCCompilationUnit) tree;
            results.add(new JCPackageDecl2(unit.getPackageName(), unit.getPackageName().toString()));
            results.addAll(unit.defs);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl) {
            com.sun.tools.javac.tree.JCTree.JCClassDecl classDecl = (com.sun.tools.javac.tree.JCTree.JCClassDecl) tree;
            results.add(classDecl.mods);
            if (classDecl.extending != null) {
                results.add(classDecl.extending);
            }
            results.addAll(classDecl.defs);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass) {
            com.sun.tools.javac.tree.JCTree.JCNewClass newClass = (com.sun.tools.javac.tree.JCTree.JCNewClass) tree;
            results.addAll(newClass.args);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewArray) {
            com.sun.tools.javac.tree.JCTree.JCNewArray newArray = (com.sun.tools.javac.tree.JCTree.JCNewArray) tree;
            if (newArray.elems != null) {
                results.addAll(newArray.elems);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl) {
            com.sun.tools.javac.tree.JCTree.JCMethodDecl methodDecl = (com.sun.tools.javac.tree.JCTree.JCMethodDecl) tree;
            results.add(methodDecl.mods);
            results.addAll(methodDecl.params);
            if (methodDecl.body != null) {
                results.add(methodDecl.body);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCLambda) {
            com.sun.tools.javac.tree.JCTree.JCLambda lambda = (com.sun.tools.javac.tree.JCTree.JCLambda) tree;
            results.addAll(lambda.params);
            if (lambda.body != null) {
                results.add(lambda.body);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl) {
            com.sun.tools.javac.tree.JCTree.JCVariableDecl variableDecl = (com.sun.tools.javac.tree.JCTree.JCVariableDecl) tree;
            results.add(variableDecl.mods);
            if (variableDecl.vartype != null) {
                results.add(variableDecl.vartype);
            }
            if (variableDecl.init != null) {
                results.add(variableDecl.init);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCIf) {
            com.sun.tools.javac.tree.JCTree.JCIf jcIf = (com.sun.tools.javac.tree.JCTree.JCIf) tree;
            if (jcIf.thenpart != null) {
                results.add(jcIf.thenpart);
            }
            if (jcIf.elsepart != null) {
                results.add(jcIf.elsepart);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAssign) {
            com.sun.tools.javac.tree.JCTree.JCAssign jcAssign = (com.sun.tools.javac.tree.JCTree.JCAssign) tree;
            if (jcAssign.lhs != null) {
                results.add(jcAssign.lhs);
            }
            if (jcAssign.rhs != null) {
                results.add(jcAssign.rhs);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCReturn) {
            com.sun.tools.javac.tree.JCTree.JCReturn jcReturn = (com.sun.tools.javac.tree.JCTree.JCReturn) tree;
            if (jcReturn.expr != null) {
                results.add(jcReturn.expr);
            }
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCTry) {
            com.sun.tools.javac.tree.JCTree.JCTry jcTry = (com.sun.tools.javac.tree.JCTree.JCTry) tree;
            results.addAll(jcTry.resources);
            results.add(jcTry.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCWhileLoop) {
            com.sun.tools.javac.tree.JCTree.JCWhileLoop loop = (com.sun.tools.javac.tree.JCTree.JCWhileLoop) tree;
            results.add(loop.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCDoWhileLoop) {
            com.sun.tools.javac.tree.JCTree.JCDoWhileLoop loop = (com.sun.tools.javac.tree.JCTree.JCDoWhileLoop) tree;
            results.add(loop.body);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCBlock) {
            com.sun.tools.javac.tree.JCTree.JCBlock block = (com.sun.tools.javac.tree.JCTree.JCBlock) tree;
            results.addAll(block.stats);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCExpressionStatement) {
            com.sun.tools.javac.tree.JCTree.JCExpressionStatement expressionStatement = (com.sun.tools.javac.tree.JCTree.JCExpressionStatement) tree;
            results.add(expressionStatement.expr);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodInvocation) {
            com.sun.tools.javac.tree.JCTree.JCMethodInvocation methodInvocation = (com.sun.tools.javac.tree.JCTree.JCMethodInvocation) tree;
            results.add(methodInvocation.meth);
            results.addAll(methodInvocation.args);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess) {
            com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess = (com.sun.tools.javac.tree.JCTree.JCFieldAccess) tree;
            results.add(fieldAccess.selected);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCModifiers) {
            com.sun.tools.javac.tree.JCTree.JCModifiers modifiers = (com.sun.tools.javac.tree.JCTree.JCModifiers) tree;
            results.addAll(modifiers.annotations);
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCAnnotation) {
            com.sun.tools.javac.tree.JCTree.JCAnnotation annotation = (com.sun.tools.javac.tree.JCTree.JCAnnotation) tree;
            results.addAll(annotation.args);
        }
        return results;
    }

    private String getFqName(com.sun.tools.javac.tree.JCTree tree) {
        if (tree.type == null) {
            return "";
        }

        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCNewClass) {
            com.sun.tools.javac.tree.JCTree.JCNewClass newClass = (com.sun.tools.javac.tree.JCTree.JCNewClass) tree;
            return getFqClassName(newClass.type) + "." + newClass.type.tsym.name
                    + (newClass.args.isEmpty() ? "" : "-")
                    + newClass.args.stream().map(a -> getFqClassName(a.type)).collect(Collectors.joining("-"));
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCFieldAccess) {
            com.sun.tools.javac.tree.JCTree.JCFieldAccess fieldAccess = (com.sun.tools.javac.tree.JCTree.JCFieldAccess) tree;
            return getFqClassName(fieldAccess.selected.type) + "." + fieldAccess.name.toString()
                    + (fieldAccess.type.getParameterTypes().isEmpty() ? "" : "-")
                    + fieldAccess.type.getParameterTypes().stream().map(this::getFqClassName).collect(Collectors.joining("-"));
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMemberReference) {
            com.sun.tools.javac.tree.JCTree.JCMemberReference memberReference = (com.sun.tools.javac.tree.JCTree.JCMemberReference) tree;
            if (memberReference.expr.type == null) {
                return "";
            }

            return getFqClassName(memberReference.expr.type) + "." + normarizeMethodName(memberReference.name.toString(), getClassName(memberReference.expr.type))
                    + (memberReference.mode == MemberReferenceTree.ReferenceMode.INVOKE
                    ? (memberReference.type.allparams().isEmpty() ? "" : "-") + memberReference.type.allparams().stream().map(this::getFqClassName).collect(Collectors.joining("-"))
                    : (memberReference.expr.type.allparams().isEmpty() ? "" : "-") + memberReference.expr.type.allparams().stream().map(this::getFqClassName).collect(Collectors.joining("-")));
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl) {
            com.sun.tools.javac.tree.JCTree.JCMethodDecl method = (com.sun.tools.javac.tree.JCTree.JCMethodDecl) tree;
            return method.sym.owner.flatName() + "." + normarizeMethodName(method.name.toString(), className)
                    + (method.type.getParameterTypes().isEmpty() ? "" : "-")
                    + method.type.getParameterTypes().stream().map(this::getFqClassName).collect(Collectors.joining("-"));
        }

        return "";
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

    private String getClassName(Type type) {
        if (type.getTag() == TypeTag.ARRAY) {
            return type.toString();
        } else {
            return type.tsym.name.toString();
        }
    }

    private String normarizeMethodName(String methodName, String className) {
        return methodName.equals("<init>") ? className : methodName;
    }

    private static class JCPackageDecl2 extends com.sun.tools.javac.tree.JCTree.JCExpression {
        private final String packageName;

        public JCPackageDecl2(JCExpression exp, String packageName) {
            super();
            setType(exp.type);
            setPos(exp.pos);
            this.packageName = packageName;
        }

        public String getPackageName() {
            return packageName;
        }

        @Override
        public Tag getTag() {
            return null;
        }

        @Override
        public void accept(Visitor visitor) {
        }

        @Override
        public Kind getKind() {
            return null;
        }

        @Override
        public <R, D> R accept(TreeVisitor<R, D> treeVisitor, D d) {
            return null;
        }
    }
}
