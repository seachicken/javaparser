package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCMethodDecl extends JCTree {
    private String name;

    public JCMethodDecl(String type, int startPos, List<JCTree> children,
                        String name) {
        super(type, startPos, children);
        this.name = name;
    }
}
