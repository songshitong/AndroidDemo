
Address of stack memory associated with local variable returned
https://stackoverflow.com/questions/18041100/using-a-c-string-gives-a-warning-address-of-stack-memory-associated-with-local
代码示例：
```
char* completion () {
    char* matches[1];
    matches[0] = "add";
    return matches;
}
```
原因分析：
matches声明的内存在栈内存里，方法执行完后一般会内存释放，但是内存指针已经给外部了，此时再使用指针，里面的内容可能为空，可能已经变了
解决方式：
1 参数改为传入的：
```
void completion (char* matches) {
    matches[0] = "add";
}

char* matches[1];
completion(matches);
```
2 将matches声明为const的，但是后续就无法释放了
