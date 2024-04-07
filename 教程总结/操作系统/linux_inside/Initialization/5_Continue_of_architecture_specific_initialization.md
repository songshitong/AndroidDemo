



https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-5.html


In the previous part, we stopped at the initialization of an architecture-specific stuff from the setup_arch function
and now we will continue with it.
As we reserved memory for the initrd, next step is the olpc_ofw_detect which detects One Laptop Per Child support. 
We will not consider platform related stuff in this book and will skip functions related with it. So let's go ahead.
https://wiki.laptop.org/go/OFW_FAQ  

The next step is the early_trap_init function. This function initializes debug (#DB - raised when the TF flag of rflags is set) 
and int3 (#BP) interrupts gate. If you don't know anything about interrupts, you can read about it in the Early interrupt and exception handling. 
In x86 architecture INT, INTO and INT3 are special instructions which allow a task to explicitly call an interrupt handler.
The INT3 instruction calls the breakpoint (#BP) handler. You may remember, we already saw it in the part about interrupts:
and exceptions:
```
----------------------------------------------------------------------------------------------
|Vector|Mnemonic|Description         |Type |Error Code|Source                   |
----------------------------------------------------------------------------------------------
|3     | #BP    |Breakpoint          |Trap |NO        |INT 3                    |
----------------------------------------------------------------------------------------------
```


Debug interrupt #DB is the primary method of invoking debuggers. early_trap_init defined in the arch/x86/kernel/traps.c.
This functions sets #DB and #BP handlers and reloads IDT:
arch/x86/kernel/setup.c   4.16方法变了
```
void __init setup_arch(char **cmdline_p)
{
...
    olpc_ofw_detect();
	idt_setup_early_traps();
...	
}
```
arch/x86/kernel/idt.c
```
#define DEFAULT_STACK	0

#define G(_vector, _addr, _ist, _type, _dpl, _segment)	\
	{						\
		.vector		= _vector,		\
		.bits.ist	= _ist,			\
		.bits.type	= _type,		\
		.bits.dpl	= _dpl,			\
		.bits.p		= 1,			\
		.addr		= _addr,		\
		.segment	= _segment,		\
	}

/* Interrupt gate */
#define INTG(_vector, _addr)				\
	G(_vector, _addr, DEFAULT_STACK, GATE_INTERRUPT, DPL0, __KERNEL_CS)

/* System interrupt gate */
#define SYSG(_vector, _addr)				\
	G(_vector, _addr, DEFAULT_STACK, GATE_INTERRUPT, DPL3, __KERNEL_CS)
	
/*
 * Early traps running on the DEFAULT_STACK because the other interrupt
 * stacks work only after cpu_init().
 */
static const __initconst struct idt_data early_idts[] = {
	INTG(X86_TRAP_DB,		debug),
	SYSG(X86_TRAP_BP,		int3),
#ifdef CONFIG_X86_32
	INTG(X86_TRAP_PF,		page_fault),
#endif
};


void __init idt_setup_early_traps(void)
{
	idt_setup_from_table(idt_table, early_idts, ARRAY_SIZE(early_idts),
			     true);
	load_idt(&idt_descr);
}

static void
idt_setup_from_table(gate_desc *idt, const struct idt_data *t, int size, bool sys)
{
	gate_desc desc;

	for (; size > 0; t++, size--) {
	   //将t的信息写入desc
		idt_init_desc(&desc, t);
		write_idt_entry(idt, t->vector, &desc);
		if (sys)
			set_bit(t->vector, system_vectors);
	}
}
```
todo Interrupt Stack Table 的新版设置在哪里

Task state segment https://en.wikipedia.org/wiki/Task_state_segment
The task state segment (TSS) is a structure on x86-based computers which holds information about a task. 
It is used by the operating system kernel for task management. Specifically, the following information is stored in the TSS:
1 Processor register state
2 I/O port permissions
3 Inner-level stack pointers
4 Previous TSS link

kernel-stacks
https://www.kernel.org/doc/Documentation/x86/kernel-stacks


As #DB and #BP gates written to the idt_descr, we reload IDT table with load_idt which just call ldtr instruction. 
Now let's look on interrupt handlers and will try to understand how they works. Of course,
I can't cover all interrupt handlers in this book and I do not see the point in this. 
It is very interesting to delve in the Linux kernel source code, so we will see how debug handler implemented in this part,
and understand how other interrupt handlers are implemented will be your task.



#DB handler
As you can read above, we passed address of the #DB handler as &debug lxr.
free-electrons.com is a great resource for searching identifiers in the Linux kernel source code, but unfortunately 
you will not find debug handler with it. All of you can find, it is debug definition in the arch/x86/include/asm/traps.h:
http://lxr.free-electrons.com/ident    新地址应该是https://elixir.bootlin.com/linux/latest/source
```
asmlinkage void debug(void);
```  
todo  asmlinkage

We can see asmlinkage attribute which tells to us that debug is function written with assembly. Yeah, 
again and again assembly :). Implementation of the #DB handler as other handlers is in this arch/x86/entry/entry_64.S 
and defined with the idtentry assembly macro:
arch/x86/entry/entry_64.S
```
idtentry debug			do_debug		has_error_code=0	paranoid=1 shift_ist=DEBUG_STACK
```
idtentry is a macro which defines an interrupt/exception entry point. As you can see it takes five arguments:
1 name of the interrupt entry point;
2 name of the interrupt handler;
3 has interrupt error code or not;
4 paranoid - if this parameter = 1, switch to special stack (read above);
5 shift_ist - stack to switch during interrupt.


Now let's look on idtentry macro implementation. This macro defined in the same assembly file and defines
debug function with the ENTRY macro. 
For the start idtentry macro checks that given parameters are correct in case if need to switch to the special stack.
In the next step it checks that give interrupt returns error code. If interrupt does not return error code
   (in our case #DB does not return error code), it calls INTR_FRAME or XCPT_FRAME if interrupt has error code. 
   Both of these macros XCPT_FRAME and INTR_FRAME do nothing and need only for the building initial frame state for interrupts. 
   They uses CFI directives and used for debugging. More info you can find in the CFI directives.
As comment from the arch/x86/kernel/entry_64.S says: CFI macros are used to generate dwarf2 unwind information for better backtraces. 
They don't change any code. so we will ignore them.

CFI-directives  https://sourceware.org/binutils/docs/as/CFI-directives.html

```
.macro idtentry sym do_sym has_error_code:req paranoid=0 shift_ist=-1
ENTRY(\sym)
	UNWIND_HINT_IRET_REGS offset=\has_error_code*8

	/* Sanity check */
	.if \shift_ist != -1 && \paranoid == 0
	.error "using shift_ist requires paranoid=1"
	.endif

	ASM_CLAC
...
	.endif
END(\sym)
.endm
```

You can remember from the previous part about early interrupts/exceptions handling that after interrupt occurs, 
current stack will have following format:
```

    +-----------------------+
    |                       |
+40 |         SS            |
+32 |         RSP           |
+24 |        RFLAGS         |
+16 |         CS            |
+8  |         RIP           |
 0  |       Error Code      | <---- rsp
    |                       |
    +-----------------------+
```
The next two macro from the idtentry implementation are:
```
ASM_CLAC
PARAVIRT_ADJUST_EXCEPTION_FRAME
```

First ASM_CLAC macro depends on CONFIG_X86_SMAP configuration option and need for security reason, more about it you can read here. 
The second PARAVIRT_ADJUST_EXCEPTION_FRAME macro is for handling handle Xen-type-exceptions (this chapter about kernel initialization and we will not consider virtualization stuff here)
CONFIG_X86_SMAP
https://lwn.net/Articles/517475/


The next piece of code checks if interrupt has error code or not and pushes $-1 which is 0xffffffffffffffff on x86_64 on the stack if not:
```
.if \has_error_code == 0
	pushq	$-1				/* ORIG_RAX: no syscall to restart */
	.endif
```

After interrupt handler finished its work, calls paranoid_exit which restores stack, switch on userspace if interrupt came from there and calls iret. 
That's all. Of course it is not all :), but we will see more deeply in the separate chapter about interrupts.
```
/* these procedures expect "no swapgs" flag in ebx */
	.if \paranoid
	jmp	paranoid_exit
	.else
	jmp	error_exit
	.endif
```



 the next function is early_cpu_init. This function  collects information about CPU and its vendor.
 arch/x86/kernel/setup.c
 ```
 idt_setup_early_traps();
	early_cpu_init();
...	
 ```
arch/x86/kernel/cpu/common.c
```
void __init early_cpu_init(void)
{
	const struct cpu_dev *const *cdev;
	int count = 0;

...

	for (cdev = __x86_cpu_dev_start; cdev < __x86_cpu_dev_end; cdev++) {
		const struct cpu_dev *cpudev = *cdev;

		if (count >= X86_VENDOR_NUM)
			break;
		cpu_devs[count] = cpudev;
		count++;
...
	}
	early_identify_cpu(&boot_cpu_data);
}

//获取cpu信息
static void __init early_identify_cpu(struct cpuinfo_x86 *c)
{
#ifdef CONFIG_X86_64
	c->x86_clflush_size = 64;
	c->x86_phys_bits = 36;
	c->x86_virt_bits = 48;
#else
	c->x86_clflush_size = 32;
	c->x86_phys_bits = 32;
	c->x86_virt_bits = 32;
#endif
	c->x86_cache_alignment = c->x86_clflush_size;

	memset(&c->x86_capability, 0, sizeof c->x86_capability);
	c->extended_cpuid_level = 0;

	/* cyrix could have cpuid enabled via c_identify()*/
	if (have_cpuid_p()) {
		cpu_detect(c);
		get_cpu_vendor(c);
		get_cpu_cap(c);
		setup_force_cpu_cap(X86_FEATURE_CPUID);

		if (this_cpu->c_early_init)
			this_cpu->c_early_init(c);

		c->cpu_index = 0;
		filter_cpuid_features(c, false);

		if (this_cpu->c_bsp_init)
			this_cpu->c_bsp_init(c);
	} else {
		identify_cpu_without_cpuid(c);
		setup_clear_cpu_cap(X86_FEATURE_CPUID);
	}

.....
}
```


Early ioremap initialization
The next step is initialization of early ioremap. In general there are two ways to communicate with devices:
I/O Ports;
Device memory.
arch/x86/kernel/setup.c
```
...
early_cpu_init();
early_ioremap_init();
...
```