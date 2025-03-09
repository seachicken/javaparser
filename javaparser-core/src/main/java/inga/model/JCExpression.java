package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCExpression extends JCTree {
    private String name;

    public JCExpression(String type, int pos, int startPos, int endPos, List<JCTree> children,
                        String name) {
        super(type, pos, startPos, endPos, children);
        this.name = name;
    }
}
