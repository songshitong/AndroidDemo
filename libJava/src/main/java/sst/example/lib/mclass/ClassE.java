package sst.example.lib.mclass;

public class ClassE extends ClassD {
    @Override
    ClassC getClassB() {
        //java继承返回父类的类型，不在往上查找
        return super.getClassB();
    }
}
