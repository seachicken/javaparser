package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCImport extends JCTree {
    private String fqName;
    public JCImport(String type, int pos, int startPos, int endPos, List<JCTree> children,
                    String fqName) {
        super(type, pos, startPos, endPos, children);
        this.fqName = fqName;
    }
}
