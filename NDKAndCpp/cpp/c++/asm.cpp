#include <stdio.h>

//https://c-cpp.com/c/language/asm
//内联汇编（常由 asm 关键词引入）给予在 C 程序中嵌入汇编语言源代码的能力。
//不同于 C++ 中，内联汇编在 C 中被当作扩展。它是条件性支持及由实现定义的，意思是它可以不存在，而且即使实现有所提供，它也并不拥有固定的含义
//
//语法
//asm ( 字符串字面量 ) ;
//解释
//这种内联汇编语法为 C++ 标准所接受，而在 C++ 中称为汇编声明。 字符串字面量 通常是以汇编语言编写的短程序，每当执行这条声明时对其执行。
//不同的 C 编译器拥有差异巨大的汇编声明规则，和与周围的 C 代码之间交互的不同约定。
//汇编声明可出现于块（函数体或其他复合语句）内，而且同所有其他声明，此声明亦可出现于块外。
//
//注解
//MSVC 在 ARM 与 x64 处理器上不支持内联汇编，而且在 x86 处理器上仅支持由 __asm 引入的形式。
//以 GCC 或 Clang 用 ISO C 模式编译（例如以选项 -std=c11 ）时，必须用 __asm__ 代替 asm
extern int func(void);
// func 的定义以汇编语言书写
__asm__(".globl func\n\t"
        ".type func, @function\n\t"
        "func:\n\t"
        ".cfi_startproc\n\t"
        "movl $7, %eax\n\t"
        "ret\n\t"
        ".cfi_endproc");

int main(void)
{
    int n = func();
    // gcc 的扩展内联汇编
    __asm__ ("leal (%0,%0,4),%0"
    : "=r" (n)
    : "0" (n));
    printf("7*5 = %d\n", n);
    fflush(stdout); // 冲刷是有意的

    // C++ 中的标准内联汇编
    __asm__ ("movq $60, %rax\n\t" // Linux 上的“退出”的系统调用序号
             "movq $2,  %rdi\n\t" // 此程序返回 2
             "syscall");
}