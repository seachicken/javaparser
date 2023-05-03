package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCClassDecl extends JCTree {
    private String name;

    public JCClassDecl(String type, int startPos, int endPos, List<JCTree> children,
                       String name) {
        super(type, startPos, endPos, children);
        this.name = name;
    }
}
