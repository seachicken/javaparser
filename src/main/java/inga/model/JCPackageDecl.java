package inga.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JCPackageDecl extends JCTree {
    private String packageName;

    public JCPackageDecl(String type, int startPos, List<JCTree> children,
                         String packageName) {
        super(type, startPos, children);
        this.packageName = packageName;
    }
}
