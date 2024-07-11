https://0xax.gitbooks.io/linux-insides/content/Interrupts/linux-interrupts-2.html


If you've read the previous parts, you can remember that the earliest place in the Linux kernel x86_64 architecture-specific source code 
which is related to the interrupt is located in the arch/x86/boot/pm.c source code file and 
represents the first setup of the Interrupt Descriptor Table. It occurs right before the transition 
into the protected mode in the go_to_protected_mode function by the call of the setup_idt:
```
void go_to_protected_mode(void)
{
    ...
    setup_idt();
    ...
}
```