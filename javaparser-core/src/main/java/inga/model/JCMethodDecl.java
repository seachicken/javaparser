package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCMethodDecl extends JCTree {
    private String name;

    public JCMethodDecl(String type, int pos, int startPos, int endPos, List<JCTree> children,
                        String name) {
        super(type, pos, startPos, endPos, children);
        this.name = name;
    }
}
