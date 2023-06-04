package fixtures.java;

import fixtures.a.ClassA;

public class Class {
    private ClassA classA;

    public void method() {
        classA.method();
    }

    public void methodOverload(int i) {
        int v = 0;
    }

    public void methodOverload(long l) {
        int v = 0;
    }

    public void methodOverload(String s) {
        int v = 0;
    }
}