// IMyBinder.aidl
package com.example.androiddemo;

// Declare any non-default types here with import statements

interface IMyBinder {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
               double aDouble, String aString);
   oneway void invokeMethodInMyService(String str);
}

// Requires setting the buildFeatures.aidl to true in the build file
//高版本androidstudio自动创建aidl需要在gradle开启  https://juejin.cn/post/7240998915942760507
// buildFeatures {
//        compose true
//        // Disable unused AGP features
//        buildConfig false
//        aidl true
//        renderScript false
//        resValues false
//        shaders false
//    }
