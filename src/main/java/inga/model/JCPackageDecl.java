package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCPackageDecl extends JCTree {
    private String packageName;

    public JCPackageDecl(String type, int startPos, int endPos, List<JCTree> children,
                         String packageName) {
        super(type, startPos, endPos, children);
        this.packageName = packageName;
    }
}
