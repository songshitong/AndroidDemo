
In the following posts, we will cover interrupts and exceptions handling in the linux kernel.


What is an Interrupt?
We have already heard of the word interrupt in several parts of this book. We even saw a couple of examples of interrupt handlers. 
In the current chapter we will start from the theory, i.e.
1 What are interrupts ?
2 What are interrupt handlers?


We will then continue to dig deeper into the details of interrupts and how the Linux kernel handles them.

The first question that arises in our mind when we come across word interrupt is What is an interrupt? 
An interrupt is an event raised by software or hardware when it needs the CPU's attention. For example,
we press a button on the keyboard and what do we expect next? What should the operating system and computer do after this? 
To simplify matters, assume that each peripheral device has an interrupt line to the CPU. 
A device can use it to signal an interrupt to the CPU. However, interrupts are not signaled directly to the CPU. 
In the old machines there was a PIC which is a chip responsible for sequentially processing multiple interrupt requests 
from multiple devices. In the new machines there is an Advanced Programmable Interrupt Controller commonly known as - APIC. 

https://en.wikipedia.org/wiki/Advanced_Programmable_Interrupt_Controller
In computing, Intel's Advanced Programmable Interrupt Controller (APIC) is a family of programmable interrupt controllers.

An APIC consists of two separate devices:
1 Local APIC
2 I/O APIC

The first - Local APIC is located on each CPU core. The local APIC is responsible for handling the CPU-specific interrupt configuration.
The local APIC is usually used to manage interrupts from the APIC-timer, thermal sensor and any other such locally
connected I/O devices.

thermal /ˈθɜːrml/  热的；热量的；保暖的；温暖的；防寒的

The second - I/O APIC provides multi-processor interrupt management. It is used to distribute external interrupts among the CPU cores.
More about the local and I/O APICs will be covered later in this chapter. 
As you can understand, interrupts can occur at any time. When an interrupt occurs, the operating system must handle it immediately.
But what does it mean to handle an interrupt? When an interrupt occurs, the operating system must ensure the following steps:
1 The kernel must pause execution of the current process; (preempt current task);
2 The kernel must search for the handler of the interrupt and transfer control (execute interrupt handler);
3 After the interrupt handler completes execution, the interrupted process can resume execution.

Of course there are numerous intricacies involved in this procedure of handling interrupts. But the above 3 steps form the basic skeleton of the procedure.

Addresses of each of the interrupt handlers are maintained in a special location referred to as the - Interrupt Descriptor Table or IDT.
The processor uses a unique number for recognizing the type of interruption or exception. This number is called - vector number. 
A vector number is an index in the IDT. There is a limited amount of the vector numbers and it can be from 0 to 255.
You can note the following range-check upon the vector number within the Linux kernel source-code:
arch/x86/kernel/idt.c
```
BUG_ON((unsigned)n > 0xFF);
```
You can find this check within the Linux kernel source code related to interrupt setup (e.g. 
The set_intr_gate in arch/x86/kernel/idt.c). 
The first 32 vector numbers from 0 to 31 are reserved by the processor and used for the processing of architecture-defined exceptions and interrupts. 
You can find the table with the description of these vector numbers in the second part of the Linux kernel initialization process - 
Early interrupt and exception handling. 
Vector numbers from 32 to 255 are designated as user-defined interrupts and are not reserved by the processor. 
These interrupts are generally assigned to external I/O devices to enable those devices to send interrupts to the processor.


Now let's talk about the types of interrupts. Broadly speaking, we can split interrupts into 2 major classes:
1 External or hardware generated interrupts 
2 Software-generated interrupts 

The first - external interrupts are received through the Local APIC or pins on the processor which are connected to the Local APIC. 
The second - software-generated interrupts are caused by an exceptional condition in the processor itself (
   sometimes using special architecture-specific instructions). A common example of an exceptional condition is division by zero. 
  Another example is exiting a program with the syscall instruction.

As mentioned earlier, an interrupt can occur at any time for a reason which the code and CPU have no control over.
On the other hand, exceptions are synchronous with program execution and can be classified into 3 categories:
1 Faults
2 Traps
3 Aborts

A fault is an exception reported before the execution of a "faulty" instruction (which can then be corrected). 
  If correct, it allows the interrupted program to resume.
Next a trap is an exception, which is reported immediately following the execution of the trap instruction. 
  Traps also allow the interrupted program to be continued just as a fault does.
Finally, an abort is an exception that does not always report the exact instruction which caused the exception and 
  does not allow the interrupted program to be resumed.

Also, we already know from the previous part that interrupts can be classified as maskable and non-maskable. 
Maskable interrupts are interrupts which can be blocked with the two following instructions for x86_64 - sti and cli.
We can find them in the Linux kernel source code:
arch/x86/include/asm/irqflags.h
```
static inline void native_irq_disable(void)
{
	asm volatile("cli": : :"memory");
}

static inline void native_irq_enable(void)
{
	asm volatile("sti": : :"memory");
}
```
These two instructions modify the IF flag bit within the interrupt register. The sti instruction sets the IF flag 
and the cli instruction clears this flag. Non-maskable interrupts are always reported.
Usually any failure in the hardware is mapped to such non-maskable interrupts.


If multiple exceptions or interrupts occur at the same time, the processor handles them in order of their predefined priorities. 
We can determine the priorities from the highest to the lowest in the following table:
1->10级
```
+----------------------------------------------------------------+
|              |                                                 |
|   Priority   | Description                                     |
|              |                                                 |
+--------------+-------------------------------------------------+
|              | Hardware Reset and Machine Checks               |
|     1        | - RESET                                         |
|              | - Machine Check                                 |
+--------------+-------------------------------------------------+
|              | Trap on Task Switch                             |
|     2        | - T flag in TSS is set                          |
|              |                                                 |
+--------------+-------------------------------------------------+
|              | External Hardware Interventions                 |
|              | - FLUSH                                         |
|     3        | - STOPCLK                                       |
|              | - SMI                                           |
|              | - INIT                                          |
+--------------+-------------------------------------------------+
|              | Traps on the Previous Instruction               |
|     4        | - Breakpoints                                   |
|              | - Debug Trap Exceptions                         |
+--------------+-------------------------------------------------+
|     5        | Nonmaskable Interrupts                          |
+--------------+-------------------------------------------------+
|     6        | Maskable Hardware Interrupts                    |
+--------------+-------------------------------------------------+
|     7        | Code Breakpoint Fault                           |
+--------------+-------------------------------------------------+
|     8        | Faults from Fetching Next Instruction           |
|              | Code-Segment Limit Violation                    |
|              | Code Page Fault                                 |
+--------------+-------------------------------------------------+
|              | Faults from Decoding the Next Instruction       |
|              | Instruction length > 15 bytes                   |
|     9        | Invalid Opcode                                  |
|              | Coprocessor Not Available                       |
|              |                                                 |
+--------------+-------------------------------------------------+
|     10       | Faults on Executing an Instruction              |
|              | Overflow                                        |
|              | Bound error                                     |
|              | Invalid TSS                                     |
|              | Segment Not Present                             |
|              | Stack fault                                     |
|              | General Protection                              |
|              | Data Page Fault                                 |
|              | Alignment Check                                 |
|              | x87 FPU Floating-point exception                |
|              | SIMD floating-point exception                   |
|              | Virtualization exception                        |
+--------------+-------------------------------------------------+
```

Now that we know a little about the various types of interrupts and exceptions, it is time to move on to a more practical part.
We start with the description of the Interrupt Descriptor Table. As mentioned earlier, the IDT stores entry points of the interrupts and exceptions handlers.
The IDT is similar in structure to the Global Descriptor Table which we saw in the second part of the Kernel booting process. 
But of course it has some differences. Instead of descriptors, the IDT entries are called gates. 
It can contain one of the following gates:
1 Interrupt gates
2 Task gates
3 Trap gates.


In the x86 architecture. Only long mode interrupt gates and trap gates can be referenced in the x86_64. 
Like the Global Descriptor Table, the Interrupt Descriptor table is an array of 8-byte gates on x86 and 
an array of 16-byte gates on x86_64. We can remember from the second part of the Kernel booting process, 
that Global Descriptor Table must contain NULL descriptor as its first element. Unlike the Global Descriptor Table, 
the Interrupt Descriptor Table may contain a gate; it is not mandatory. 
For example, you may remember that we have loaded the Interrupt Descriptor table with the NULL gates only in the earlier part
while transitioning into protected mode:
arch/x86/boot/pm.c
```
/*
 * Set up the IDT
 */
static void setup_idt(void)
{
    static const struct gdt_ptr null_idt = {0, 0};
    asm volatile("lidtl %0" : : "m" (null_idt));
}
```

The Interrupt Descriptor table can be located anywhere in the linear address space and the base address of it must be aligned on an 8-byte boundary 
on x86 or 16-byte boundary on x86_64. The base address of the IDT is stored in the special register - IDTR.
There are two instructions on x86-compatible processors to modify the IDTR register:
1  LIDT
2  SIDT


The first instruction LIDT is used to load the base-address of the IDT i.e., the specified operand into the IDTR. 
The second instruction SIDT is used to read and store the contents of the IDTR into the specified operand.
The IDTR register is 48-bits on the x86 and contains the following information:
```
+-----------------------------------+----------------------+
|                                   |                      |
|     Base address of the IDT       |   Limit of the IDT   |
|                                   |                      |
+-----------------------------------+----------------------+
47                                16 15                    0
```

Looking at the implementation of setup_idt, we have prepared a null_idt and loaded it to the IDTR register with the lidt instruction.
Note that null_idt has gdt_ptr type which is defined as:
arch/x86/boot/pm.c
```
struct gdt_ptr {
	u16 len;
	u32 ptr;
} __attribute__((packed));
```

Here we can see the definition of the structure with the two fields of 2-bytes and 4-bytes each (a total of 48-bits) as we can see in the diagram.
Now let's look at the IDT entries structure. The IDT entries structure is an array of the 16-byte entries 
which are called gates in the x86_64. They have the following structure:
```
127                                                                             96
+-------------------------------------------------------------------------------+
|                                                                               |
|                                Reserved                                       |
|                                                                               |
+--------------------------------------------------------------------------------
95                                                                              64
+-------------------------------------------------------------------------------+
|                                                                               |
|                               Offset 63..32                                   |
|                                                                               |
+-------------------------------------------------------------------------------+
63                               48 47      46  44   42    39             34    32
+-------------------------------------------------------------------------------+
|                                  |       |  D  |   |     |      |   |   |     |
|       Offset 31..16              |   P   |  P  | 0 |Type |0 0 0 | 0 | 0 | IST |
|                                  |       |  L  |   |     |      |   |   |     |
 -------------------------------------------------------------------------------+
31                                   16 15                                      0
+-------------------------------------------------------------------------------+
|                                      |                                        |
|          Segment Selector            |                 Offset 15..0           |
|                                      |                                        |
+-------------------------------------------------------------------------------+
```

To form an index into the IDT, the processor scales the exception or interrupt vector by sixteen. 
The processor handles the occurrence of exceptions and interrupts just like it handles calls of a procedure 
when it sees the call instruction. A processor uses a unique number or vector number of the interrupt or 
the exception as the index to find the necessary Interrupt Descriptor Table entry. 
Now let's take a closer look at an IDT entry.

As we can see, IDT entry on the diagram consists of the following fields:
1  0-15 bits - offset from the segment selector which is used by the processor as the base address of the entry point of the interrupt handler;
2  16-31 bits - base address of the segment select which contains the entry point of the interrupt handler;
3  IST - a new special mechanism in the x86_64, which is described below;
      DPL - Descriptor Privilege Level;
      P - Segment Present flag;
4  48-63 bits - the second part of the handler base address;
5  64-95 bits - the third part of the base address of the handler;
6  96-127 bits - and the last bits are reserved by the CPU.
And the last Type field describes the type of the IDT entry. There are three different kinds of handlers for interrupts:
1 Interrupt gate
2 Trap gate
3 Task gate


The IST or Interrupt Stack Table is a new mechanism in the x86_64. It is used as an alternative to the legacy stack-switch mechanism.
Previously the x86 architecture provided a mechanism to automatically switch stack frames in response to an interrupt. 
The IST is a modified version of the x86 Stack switching mode. This mechanism unconditionally switches stacks when it is enabled 
and can be enabled for any interrupt in the IDT entry related with the certain interrupt (we will soon see it). 
From this we can understand that IST is not necessary for all interrupts. Some interrupts can continue to use the legacy stack switching mode. 
The IST mechanism provides up to seven IST pointers in the Task State Segment or TSS which is the special structure which 
contains information about a process. The TSS is used for stack switching during the execution of an interrupt or exception handler in the Linux kernel.
Each pointer is referenced by an interrupt gate from the IDT.
Task state segment
https://en.wikipedia.org/wiki/Task_state_segment
The task state segment (TSS) is a structure on x86-based computers which holds information about a task. 
It is used by the operating system kernel for task management. Specifically, the following information is stored in the TSS:
1 Processor register state
2 I/O port permissions
3 Inner-level stack pointers
4 Previous TSS link
All this information should be stored at specific locations within the TSS as specified in the IA-32 manuals.

The Interrupt Descriptor Table represented by the array of the gate_desc structures:
```
extern gate_desc idt_table[];
```
where gate_struct is defined as:
arch/x86/include/asm/desc_defs.h
```
struct idt_bits {
	u16		ist	: 3,
			zero	: 5,
			type	: 5,
			dpl	: 2,
			p	: 1;
} __attribute__((packed));

struct gate_struct {
	u16		offset_low;
	u16		segment;
	struct idt_bits	bits;
	u16		offset_middle;
#ifdef CONFIG_X86_64
	u32		offset_high;
	u32		reserved;
#endif
} __attribute__((packed));

typedef struct gate_struct gate_desc;
```
Each active thread has a large stack in the Linux kernel for the x86_64 architecture. 
The stack size is defined as THREAD_SIZE and is equal to:
```
arch/arc/include/uapi/asm/page.h
/* PAGE_SHIFT determines the page size */
#if defined(CONFIG_ARC_PAGE_SIZE_16K)
#define PAGE_SHIFT 14
#elif defined(CONFIG_ARC_PAGE_SIZE_4K)
#define PAGE_SHIFT 12
#else
/*
 * Default 8k
 * done this way (instead of under CONFIG_ARC_PAGE_SIZE_8K) because adhoc
 * user code (busybox appletlib.h) expects PAGE_SHIFT to be defined w/o
 * using the correct uClibc header and in their build our autoconf.h is
 * not available
 */
#define PAGE_SHIFT 13
#endif

#define PAGE_SIZE	_BITUL(PAGE_SHIFT)	/* Default 8K */

arch/x86/include/asm/page_64_types.h
#ifdef CONFIG_KASAN
#define KASAN_STACK_ORDER 1
#else
#define KASAN_STACK_ORDER 0
#endif

#define THREAD_SIZE_ORDER	(2 + KASAN_STACK_ORDER)
#define THREAD_SIZE  (PAGE_SIZE << THREAD_SIZE_ORDER)
```
The PAGE_SIZE is 4096-bytes and the THREAD_SIZE_ORDER depends on the KASAN_STACK_ORDER. 
As we can see, the KASAN_STACK depends on the CONFIG_KASAN kernel configuration parameter
lib/Kconfig.kasan
```
config KASAN
	bool "KASan: runtime memory debugger"
	depends on SLUB || (SLAB && !DEBUG_SLAB)
	select CONSTRUCTORS
	select STACKDEPOT
	help
	  Enables kernel address sanitizer - runtime memory debugger,
	  designed to find out-of-bounds accesses and use-after-free bugs.
	  ...
```
KASan is a runtime memory debugger. Thus, the THREAD_SIZE will be 16384 bytes if CONFIG_KASAN is disabled or 
32768 if this kernel configuration option is enabled.
These stacks contain useful data as long as a thread is alive or in a zombie state. While the thread is in user-space, 
the kernel stack is empty except for the thread_info structure (details about this structure are available in the fourth part of the Linux kernel initialization process) at the bottom of the stack.
The active or zombie threads aren't the only threads with their own stack. There also exist specialized stacks that
are associated with each available CPU. These stacks are active when the kernel is executing on that CPU.
When the user-space is executing on the CPU, these stacks do not contain any useful information.
Each CPU has a few special per-cpu stacks as well. 

The first is the interrupt stack used for the external hardware interrupts. Its size is determined as follows:
arch/x86/include/asm/page_64_types.h
```
#define IRQ_STACK_ORDER (2 + KASAN_STACK_ORDER)
#define IRQ_STACK_SIZE (PAGE_SIZE << IRQ_STACK_ORDER)
```
Or 16384 bytes. The per-cpu interrupt stack is represented by the irq_stack struct and the fixed_percpu_data struct
in the Linux kernel for x86_64:
arch/x86/include/asm/processor.h
```
/*
 * per-CPU IRQ handling stacks
 */
struct irq_stack {
	u32                     stack[THREAD_SIZE/sizeof(u32)];
} __aligned(THREAD_SIZE);
```
fixed_percpu_data在4.16变为irq_stack_union
```
#ifdef CONFIG_X86_64
struct fixed_percpu_data {
    /*
     * GCC hardcodes the stack canary as %gs:40.  Since the
     * irq_stack is the object at %gs:0, we reserve the bottom
     * 48 bytes of the irq stack for the canary.
     */
    char        gs_base[40];
    unsigned long    stack_canary;
};
...
#endif


union irq_stack_union {
	char irq_stack[IRQ_STACK_SIZE];
	struct {
		char gs_base[40];
		unsigned long stack_canary;
	};
};
```

The irq_stack struct contains a 16 kilobytes array.
Also, you can see that the fixed_percpu_data contains two fields:
1  gs_base - The gs register always points to the bottom of the fixed_percpu_data. On the x86_64, 
   the gs register is shared by per-cpu area and stack canary (more about per-cpu variables you can read in the special part).
  All per-cpu symbols are zero-based and the gs points to the base of the per-cpu area.
  You already know that segmented memory model is abolished in the long mode, but we can set the base address for the two segment registers - 
  fs and gs with the Model specific registers and these registers can be still be used as address registers. 
  If you remember the first part of the Linux kernel initialization process, you can remember that we have set the gs register:
```
 movl    $MSR_GS_BASE,%ecx
    movl    initial_gs(%rip),%eax
    movl    initial_gs+4(%rip),%edx
    wrmsr
```
where initial_gs points to the fixed_percpu_data:
```
SYM_DATA(initial_gs,    .quad INIT_PER_CPU_VAR(fixed_percpu_data))
```
arch/x86/kernel/head_64.S     4.16的汇编
```
	/* Both SMP bootup and ACPI suspend change these variables */
	__REFDATA
	.balign	8
	GLOBAL(initial_code)
	.quad	x86_64_start_kernel
	GLOBAL(initial_gs)
	.quad	INIT_PER_CPU_VAR(irq_stack_union)
	GLOBAL(initial_stack)
	/*
	 * The SIZEOF_PTREGS gap is a convention which helps the in-kernel
	 * unwinder reliably detect the end of the stack.
	 */
	.quad  init_thread_union + THREAD_SIZE - SIZEOF_PTREGS
	__FINITDATA

	__INIT
```

segmented memory model  https://en.wikipedia.org/wiki/Memory_segmentation
Memory segmentation is an operating system memory management technique of dividing a computer's primary memory into segments or sections. 
In a computer system using segmentation, a reference to a memory location includes a value that identifies a segment and an offset (memory location) within that segment. 
Segments or sections are also used in object files of compiled programs when they are linked together into a program image and when the image is loaded into memory.

Model specific registers  https://en.wikipedia.org/wiki/Model-specific_register
A model-specific register (MSR) is any of various control registers in the x86 system architecture used for debugging, 
program execution tracing, computer performance monitoring, and toggling certain CPU features.

2
stack_canary - Stack canary for the interrupt stack is a stack protector to verify that the stack hasn't been overwritten.
Note that gs_base is a 40 bytes array. GCC requires that stack canary will be on the fixed offset from the base of the gs 
 and its value must be 40 for the x86_64 and 20 for the x86.
Stack canary  https://en.wikipedia.org/wiki/Stack_buffer_overflow#Stack_canaries
Stack canaries, named for their analogy to a canary in a coal mine, are used to detect a stack buffer overflow before execution of malicious code can occur. 
This method works by placing a small integer, the value of which is randomly chosen at program start, 
in memory just before the stack return pointer. Most buffer overflows overwrite memory from lower to higher memory addresses,
so in order to overwrite the return pointer (and thus take control of the process) the canary value must also be overwritten. 


The fixed_percpu_data is the first datum in the percpu area, we can see it in the System.map:  //todo
```
0000000000000000 D __per_cpu_start
0000000000000000 D fixed_percpu_data
00000000000001e0 A kexec_control_code_size
0000000000001000 D cpu_debug_store
0000000000002000 D irq_stack_backing_store
0000000000006000 D cpu_tss_rw
0000000000009000 D gdt_page
000000000000a000 d exception_stacks
...
```
We can see its definition in the code:
arch/x86/include/asm/processor.h
```
DECLARE_PER_CPU_FIRST(union irq_stack_union, irq_stack_union) __visible;
```

Now, it's time to look at the initialization of the fixed_percpu_data. Besides the fixed_percpu_data definition,
we can see the definition of the following per-cpu variables in the
arch/x86/include/asm/processor.h
```
DECLARE_INIT_PER_CPU(irq_stack_union);

DECLARE_PER_CPU(char *, irq_stack_ptr);
DECLARE_PER_CPU(unsigned int, irq_count);
...
DECLARE_PER_CPU_ALIGNED(struct stack_canary, stack_canary);
..
DECLARE_PER_CPU(struct irq_stack *, hardirq_stack);
DECLARE_PER_CPU(struct irq_stack *, softirq_stack);
```
The first and third are the stack pointers for hardware and software interrupts. It is obvious from the name of the variables, 
that these point to the top of stacks. 
The second - irq_count is used to check if a CPU is already on an interrupt stack or not.
Initialization of the hardirq_stack is located in the irq_init_percpu_irqstack function
arch/x86/kernel/irq_32.c
```
void irq_ctx_init(int cpu)
{
	struct irq_stack *irqstk;

	if (per_cpu(hardirq_stack, cpu))
		return;

	irqstk = page_address(alloc_pages_node(cpu_to_node(cpu),
					       THREADINFO_GFP,
					       THREAD_SIZE_ORDER));
	per_cpu(hardirq_stack, cpu) = irqstk;

	irqstk = page_address(alloc_pages_node(cpu_to_node(cpu),
					       THREADINFO_GFP,
					       THREAD_SIZE_ORDER));
	per_cpu(softirq_stack, cpu) = irqstk;

	printk(KERN_DEBUG "CPU %u irqstacks, hard=%p soft=%p\n",
	       cpu, per_cpu(hardirq_stack, cpu),  per_cpu(softirq_stack, cpu));
}
```
Here we go over all the CPUs one-by-one and setup the hardirq_stack
and as we already know the gs register points to the bottom of the interrupt stack.
```
  movl    $MSR_GS_BASE,%ecx
    movl    initial_gs(%rip),%eax
    movl    initial_gs+4(%rip),%edx
    wrmsr

    GLOBAL(initial_gs)
    .quad    INIT_PER_CPU_VAR(irq_stack_union)
```
Here we can see the wrmsr instruction which loads the data from edx:eax into the Model specific register pointed by the ecx register. 
In our case the model specific register is MSR_GS_BASE which contains the base address of the memory segment pointed by the gs register. 
edx:eax points to the address of the initial_gs which is the base address of our irq_stack_union.

We already know that x86_64 has a feature called Interrupt Stack Table or IST and this feature provides the ability to 
switch to a new stack for events non-maskable interrupt, double fault etc. There can be up to seven IST entries per-cpu. 
Some of them are:
1 DOUBLEFAULT_STACK
2 NMI_STACK
3 DEBUG_STACK
4 MCE_STACK
or
arch/x86/include/asm/page_64_types.h
```
#define DOUBLEFAULT_STACK 1
#define NMI_STACK 2
#define DEBUG_STACK 3
#define MCE_STACK 4
#define N_EXCEPTION_STACKS 4  /* hw limit: 7 */
```

All interrupt-gate descriptors which switch to a new stack with the IST are initialized with the set_intr_gate_ist function.
For example
arch/x86/kernel/idt.c
```
static const __initconst struct idt_data ist_idts[] = {
	ISTG(X86_TRAP_DB,	debug,		DEBUG_STACK),
	ISTG(X86_TRAP_NMI,	nmi,		NMI_STACK),
	ISTG(X86_TRAP_DF,	double_fault,	DOUBLEFAULT_STACK),
#ifdef CONFIG_X86_MCE
	ISTG(X86_TRAP_MC,	&machine_check,	MCE_STACK),
#endif
};
```
where &nmi and &double_fault are addresses of the entries to the given interrupt handlers:
arch/x86/include/asm/traps.h
```
asmlinkage void debug(void);
asmlinkage void nmi(void);
...
#ifdef CONFIG_X86_64
asmlinkage void double_fault(void);
#endif
```
defined in the arch/x86/entry/entry_64.S  
```
idtentry double_fault			do_double_fault			has_error_code=1 paranoid=2
..
ENTRY(nmi)
...
END(nmi)
```

When an interrupt or an exception occurs, the new ss selector is forced to NULL and the ss selector’s rpl field is set to the new cpl. 
The old ss, rsp, register flags, cs, rip are pushed onto the new stack. In 64-bit mode, the size of interrupt stack-frame pushes is fixed at 8-bytes,
so that we will get the following stack:
```
+---------------+
|               |
|      SS       | 40
|      RSP      | 32
|     RFLAGS    | 24
|      CS       | 16
|      RIP      | 8
|   Error code  | 0
|               |
+---------------+
```

If the IST field in the interrupt gate is not 0, we read the IST pointer into rsp. 
If the interrupt vector number has an error code associated with it, we then push the error code onto the stack. 
If the interrupt vector number has no error code, we go ahead and push the dummy error code on to the stack.
We need to do this to ensure stack consistency. 
Next, we load the segment-selector field from the gate descriptor into the CS register and must verify that the 
target code-segment is a 64-bit mode code segment by the checking bit 21 i.e. the L bit in the Global Descriptor Table.
Finally, we load the offset field from the gate descriptor into rip which will be the entry-point of the interrupt handler.
After this the interrupt handler begins to execute and when the interrupt handler finishes its execution,
it must return control to the interrupted process with the iret instruction. 
The iret instruction unconditionally pops the stack pointer (ss:rsp) to restore the stack of the interrupted process 
  and does not depend on the cpl change.

That's all.

Conclusion
It is the end of the first part of Interrupts and Interrupt Handling in the Linux kernel.
We covered some theory and the first steps of initialization of stuff related to interrupts and exceptions.
In the next part we will continue to dive into the 
more practical aspects of interrupts and interrupt handling