package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCImport extends JCTree {
    private String fqName;
    public JCImport(String type, int startPos, List<JCTree> children,
                    String fqName) {
        super(type, startPos, children);
        this.fqName = fqName;
    }
}
