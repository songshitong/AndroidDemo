

fragment必须有一个空的构造器,否则报错could not find Fragment constructor
如果你想要的使用非默认的构造函数，需要自己实现一个FragmentFactory去初始化，然后强烈推荐使用setArguments和getArguments存取参数
https://www.jianshu.com/p/8df58655bfe3
线上异常的调用栈  fragment重建了后会调用默认构造器
androidx.fragment.app.FragmentManager.restoreSaveStateInternal(FragmentManager.java:2496)
androidx.fragment.app.FragmentStateManager.<init>(FragmentStateManager.java:85)
...
androidx.fragment.app.Fragment.instantiate(Fragment.java:663)
```
 public static Fragment instantiate(@NonNull Context context, @NonNull String fname,
            @Nullable Bundle args) {
        try {
            Class<? extends Fragment> clazz = FragmentFactory.loadFragmentClass(
                    context.getClassLoader(), fname);
            Fragment f = clazz.getConstructor().newInstance();  //调用空的构造器
            if (args != null) {
                args.setClassLoader(f.getClass().getClassLoader());
                f.setArguments(args); //设置参数
            }
            return f;
        } catch (java.lang.InstantiationException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (NoSuchMethodException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": could not find Fragment constructor", e);
        } catch (InvocationTargetException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": calling Fragment constructor caused an exception", e);
        }
    }
```