

https://www.liaoxuefeng.com/wiki/1252599548343744/1266265125480448
AOP是Aspect Oriented Programming，即面向切面编程。
那什么是AOP？
我们先回顾一下OOP：Object Oriented Programming，OOP作为面向对象编程的模式，获得了巨大的成功，OOP的主要功能是数据封装、继承和多态。
而AOP是一种新的编程方式，它和OOP不同，OOP把系统看作多个对象的交互，AOP把系统分解为不同的关注点，或者称之为切面（Aspect）。
AOP本质上只是一种代理模式的实现方式

https://www.liaoxuefeng.com/wiki/1252599548343744/1310052352786466
在AOP编程中，我们经常会遇到下面的概念：
Aspect：切面，即一个横跨多个核心逻辑的功能，或者称之为系统关注点；
Joinpoint：连接点，即定义在应用程序流程的何处插入切面的执行；
Pointcut：切入点，即一组连接点的集合；
Advice：增强，指特定连接点上执行的动作；
Introduction：引介，指为一个已有的Java对象动态地增加新的接口；
Weaving：织入，指将切面整合到程序的执行流程中；
Interceptor：拦截器，是一种实现增强的方式；
Target Object：目标对象，即真正执行业务的核心逻辑对象；
AOP Proxy：AOP代理，是客户端持有的增强后的对象引用。


例如给业务方法执行前添加日志
```
@Aspect
@Component
public class LoggingAspect {
    // 在执行UserService的每个方法前执行:
    @Before("execution(public * com.itranswarp.learnjava.service.UserService.*(..))")
    public void doAccessCheck() {
        System.err.println("[Before] do access check...");
    }

    // 在执行MailService的每个方法前后执行:
    @Around("execution(public * com.itranswarp.learnjava.service.MailService.*(..))")
    public Object doLogging(ProceedingJoinPoint pjp) throws Throwable {
        System.err.println("[Around] start " + pjp.getSignature());
        Object retVal = pjp.proceed();
        System.err.println("[Around] done " + pjp.getSignature());
        return retVal;
    }
}
```