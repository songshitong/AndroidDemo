
https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-1.html




In the last part of the previous chapter we stopped at the jmp instruction
```
jmp    *%rax
```
At this moment the rax register contains address of the Linux kernel entry point which was obtained
as a result of the call of the decompress_kernel function
our last instruction in the kernel setup code is a jump on the kernel entry point. We already know where 
the entry point of the Linux kernel is defined, so we are able to start to learn what Linux kernel does after the start.



First steps in the kernel