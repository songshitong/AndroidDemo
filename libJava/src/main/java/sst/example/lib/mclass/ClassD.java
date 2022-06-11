package sst.example.lib.mclass;

public class ClassD extends ClassA {
    @Override
    ClassC getClassB() {
        return new ClassC();
    }
}
