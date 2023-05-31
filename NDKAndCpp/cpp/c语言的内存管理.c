
//在<stdlib.h> 中可以找到
//在内存中动态地分配 num 个长度为 size 的连续空间，并将每一个字节都初始化为 0。
// 所以它的结果是分配了 num*size 个字节长度的内存空间，并且每个字节的值都是0
// void *calloc(int num, int size);

//该函数释放 address 所指向的内存块,释放的是动态分配的内存空间
// void free(void *address);

//在堆区分配一块指定大小的内存空间，用来存放数据。这块内存空间在函数执行完成后不会被初始化，它们的值是未知的
// void *malloc(int num);

//void * 类型表示未确定类型的指针。C、C++ 规定 void * 类型可以通过类型转换强制转换为任何其它类型的指针

//该函数重新分配内存，把内存扩展到 newsize
// void *realloc(void *address, int newsize);  返回值:该函数返回一个指针 ，指向重新分配大小的内存。如果请求失败，则返回 NULL
//https://www.runoob.com/cprogramming/c-function-realloc.html
//通过realloc的方式确实能够改变原数组的尺寸。给用户的感受，就好像我的数组根据要求扩大或缩小了一样。然而，底层实现可以知道，我们要分情况讨论：
//就拿扩大来说，并不是在原数组的地址空间基础上，继续往后按照我们的意愿去扩展。因为，我们并不能保证拟扩展的地址空间上，
//是否有其他程序已经先行占用了？如果拟扩展地址空间上没有占用，那么后续扩展的内容调整后的内存空间和原来的内存空间，保持同一内存始址。
//否则，程序会在内存的堆区重新找一块空闲的地址空间，并返回新的内存始址。所以，realloc返回的指针很可能指向一个新的地址


#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main()
{
    char name[100];
    char *description;

    strcpy(name, "Zara Ali");

    /* 动态分配内存 */
    description = (char *)malloc( 200 * sizeof(char) );
    if( description == NULL )
    {
        fprintf(stderr, "Error - unable to allocate required memory\n");
    }
    else
    {
        strcpy( description, "Zara ali a DPS student in class 10th");
    }
    printf("Name = %s\n", name );
    printf("Description: %s\n", description );

    /* 使用 free() 函数释放内存 */
   free(description);
}
