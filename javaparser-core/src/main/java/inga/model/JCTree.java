package inga.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JCTree {
    private String type;
    private int pos;
    private int startPos;
    private int endPos;
    private List<JCTree> children;
}
