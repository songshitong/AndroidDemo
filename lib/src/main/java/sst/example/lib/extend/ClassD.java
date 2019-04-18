package sst.example.lib.extend;

public class ClassD extends ClassA {
    @Override
    ClassC getClassB() {
        return new ClassC();
    }
}
