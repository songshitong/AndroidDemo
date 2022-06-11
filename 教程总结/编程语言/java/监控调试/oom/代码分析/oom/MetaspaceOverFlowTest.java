package oom;

//import net.sf.cglib.proxy.Enhancer;
//import net.sf.cglib.proxy.MethodInterceptor;
//import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class MetaspaceOverFlowTest {

    /**
     * 通过CGLIB模拟向元空间写入数据
     */
    public static void main(String[] args) {
//        while (true) {
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                System.out.println(e.toString());
//            }
//
//            Enhancer enhancer = new Enhancer();
//
//            enhancer.setSuperclass(MetaspaceOverFlowTest.class);
//
//            // 设置为true、false结果会有何不同？
//            enhancer.setUseCache(false);
//
//            enhancer.setCallback(new MethodInterceptor() {
//                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
//                    return proxy.invokeSuper(obj, args);
//                }
//            });
//
//            enhancer.create();
//        }
    }
}
