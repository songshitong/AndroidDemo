


https://stackoverflow.com/questions/2092098/why-do-most-fields-class-members-in-android-tutorial-start-with-m
https://source.android.com/source/code-style.html#follow-field-naming-conventions
变量名前面加m   一般认为m代表member variables
Non-public, non-static field names start with m.
Static field names start with s.
Other fields start with a lower case letter.
Public static final fields (constants) are ALL_CAPS_WITH_UNDERSCORES
```
public class MyClass {
    public static final int SOME_CONSTANT = 42;
    public int publicField;
    private static MyClass sSingleton;
    int mPackagePrivate;
    private int mPrivate;
    protected int mProtected;
}
```