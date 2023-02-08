

https://blog.quarkslab.com/smali-the-parseltongue-language.html

Android applications run inside the Dalvik Virtual machine, and that binary needs to read DEX (Dalvik EXecutable) 
format in order to execute the application.

 Smali, the intermediate representation (IR) for DEX files.     将dex反编译得到

The syntax for Smali is loosely based on Jasmin's/dedexer's syntax
https://jasmin.sourceforge.net/guide.html

dalvik字节码
https://source.android.com/docs/core/runtime/dalvik-bytecode


语法
const/4 vA, #+B	
A: destination register (4 bits)  B: signed int (4 bits)
Move the given literal value (sign-extended to 32 bits) into the specified register.


读取
sstaticop vAA, field@BBBB
60: sget
61: sget-wide
...
67: sput
...
6a: sput-boolean
...
B: static field reference index (16 bits)	
Perform the identified object static field operation with the identified static field, loading or storing into the value register.
