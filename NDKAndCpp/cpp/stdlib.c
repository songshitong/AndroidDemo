
//stdlib

#include <stdio.h>
#include <stdlib.h>

void functionA ()
{
    printf("这是函数A\n");
}

int main ()
{
    /* 注册终止函数 */
    //当程序正常终止时，调用指定的函数 func。您可以在任何地方注册你的终止函数，但它会在程序终止的时候被调用。
    //在android上不生效  https://developer.android.com/ndk/guides/pgo?hl=zh-cn
    atexit(functionA );

    printf("启动主程序...\n");

    printf("退出主程序...\n");

    return(0);
}