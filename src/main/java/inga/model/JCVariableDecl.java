package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCVariableDecl extends JCTree {
    private String name;

    public JCVariableDecl(String type,int startPos,  List<JCTree> children,
                          String name) {
        super(type, startPos, children);
        this.name = name;
    }
}
