package sst.example.lib.mclass;

//在实现某个接口的时候，需要实现该接口所有的方法。这个时候default关键字就派上用场了。通过default关键字定义的方法，
// 集成该接口的方法不需要去实现该方法
//https://blog.csdn.net/qq_37909508/article/details/106483918
public class InterfaceDefaultMethod {

    interface Student{
        void doWork();
        void goHome();
    }

    //默认接口必须实现所有的方法
    class StudentA implements Student{

        @Override
        public void doWork() {

        }

        @Override
        public void goHome() {

        }
    }

    interface DefaultStudent extends Student{
        @Override
        default void doWork() {

        }

        default @Override void goHome(){}
    }

    //StudentB可以选择实现某个方法
    class StudentB implements DefaultStudent{

        @Override
        public void goHome() {
            //执行父接口的默认方法，不写这一句不执行
             DefaultStudent.super.goHome();
        }
    }
}
