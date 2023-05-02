package inga;

import com.sun.source.util.JavacTask;
import inga.model.*;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    public JCTree parse(Path path) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
            var objects = fileManager.getJavaFileObjects(path.toFile());
            var task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, objects);
            var tree = new JCTree();
            for (var unit : task.parse()) {
                tree.setChildren(List.of(parse((com.sun.tools.javac.tree.JCTree) unit)));
            }
            return tree;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private JCTree parse(com.sun.tools.javac.tree.JCTree tree) {
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCImport jcImport) {
            return new JCImport(
                    tree.getKind().name(),
                    tree.getStartPosition(),
                    getChildren(tree).stream().map(this::parse).toList(),
                    jcImport.getQualifiedIdentifier().toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCVariableDecl variableDecl) {
            return new JCVariableDecl(
                    tree.getKind().name(),
                    tree.getStartPosition(),
                    getChildren(tree).stream().map(this::parse).toList(),
                    variableDecl.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCMethodDecl methodDecl) {
            return new JCMethodDecl(
                    tree.getKind().name(),
                    tree.getStartPosition(),
                    getChildren(tree).stream().map(this::parse).toList(),
                    methodDecl.name.toString()
            );
        } else if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl classDecl) {
            return new JCClassDecl(
                    tree.getKind().name(),
                    tree.getStartPosition(),
                    getChildren(tree).stream().map(this::parse).toList(),
                    classDecl.name.toString()
            );
        } else {
            return new JCTree(
                    tree.getKind().name(),
                    tree.getStartPosition(),
                    getChildren(tree).stream().map(this::parse).toList()
            );
        }
    }

    private List<com.sun.tools.javac.tree.JCTree> getChildren(com.sun.tools.javac.tree.JCTree tree) {
        var results = new ArrayList<com.sun.tools.javac.tree.JCTree>();
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCCompilationUnit unit) {
            results.addAll(unit.defs);
        }
        if (tree instanceof com.sun.tools.javac.tree.JCTree.JCClassDecl classDecl) {
            results.addAll(classDecl.defs);
        }
        return results;
    }
}
