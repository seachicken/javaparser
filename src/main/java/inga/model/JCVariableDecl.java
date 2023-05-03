package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCVariableDecl extends JCTree {
    private String name;

    public JCVariableDecl(String type,int startPos, int endPos, List<JCTree> children,
                          String name) {
        super(type, startPos, endPos, children);
        this.name = name;
    }
}
