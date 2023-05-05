package fixtures.java;

import fixtures.a.ClassA;

public class Class {
    private ClassA classA;

    public void method() {
        classA.method();
        new ClassA().method();
        privateMethod();
    }

    private void privateMethod() {
        int variable = 0;
        if (true) {
            variable = classA.method();
        }
    }
}