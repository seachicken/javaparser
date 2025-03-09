package inga;

import inga.model.JCTree;

import java.nio.file.Path;

public interface Parser {
    JCTree parse(Path path, boolean withAnalyze, String classPath);
}
