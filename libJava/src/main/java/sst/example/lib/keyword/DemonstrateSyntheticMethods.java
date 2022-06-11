package sst.example.lib.keyword;

import java.util.Calendar;

//合成方法Synthetic  一般由编译器生成，编译期调用   可以通过反射调用
//编译后的类DemonstrateSyntheticMethods$1
// $FF: synthetic class
//class DemonstrateSyntheticMethods$1 {
//}
public final class DemonstrateSyntheticMethods {
    public DemonstrateSyntheticMethods() {
    }

    public static void main(String[] arguments) {
        DemonstrateSyntheticMethods.NestedClass nested = new DemonstrateSyntheticMethods.NestedClass();
        System.out.println("String: " + nested.highlyConfidential);
    }

    private static final class NestedClass {
        private String highlyConfidential;
        private int highlyConfidentialInt;
        private Calendar highlyConfidentialCalendar;
        private boolean highlyConfidentialBoolean;

        private NestedClass() {
            this.highlyConfidential = "Don't tell anyone about me";
            this.highlyConfidentialInt = 42;
            this.highlyConfidentialCalendar = Calendar.getInstance();
            this.highlyConfidentialBoolean = true;
        }
    }
}