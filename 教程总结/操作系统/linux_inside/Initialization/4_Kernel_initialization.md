

Kernel entry point

The start_kernel is the entry of the generic and architecture independent kernel code, although we will return to
the arch/ folder many times. If you look inside of the start_kernel function, you will see that this function is very big.
For this moment it contains about 86 calls of functions. Yes, it's very big and of course this part will not
cover all the processes that occur in this function. In the current part we will only start to do it. 

The main purpose of the start_kernel to finish kernel initialization process and launch the first init process. 
Before the first process will be started, the start_kernel must do many things such as: to enable lock validator, 
to initialize processor id, to enable early cgroups subsystem, to setup per-cpu areas, to initialize different caches in vfs, 
to initialize memory manager, rcu, vmalloc, scheduler, IRQs, ACPI and many many more. 
Only after these steps will we see the launch of the first init process in the last part of this chapter. 
So much kernel code awaits us, let's start.

lock validator
https://www.kernel.org/doc/Documentation/locking/lockdep-design.txt



A little about function attributes
the start_kernel function is defined  with the __init attribute and as you already may know from other parts,
all functions which are defined with this attribute are necessary during kernel initialization.
include/linux/init.h
```
#define __init      __section(.init.text) __cold notrace
```

After the initialization process have finished, the kernel will release these sections with a call to the free_initmem function. 
Note also that __init is defined with two attributes: __cold and notrace. The purpose of the first cold attribute 
is to mark that the function is rarely used and the compiler must optimize this function for size. 
The second notrace is defined as:
```
#define notrace __attribute__((no_instrument_function))
```
where no_instrument_function says to the compiler not to generate profiling function calls.

In the definition of the start_kernel function, you can also see the __visible attribute which expands to the:
```
#define __visible __attribute__((externally_visible))
```
where externally_visible tells to the compiler that something uses this function or variable, 
to prevent marking this function/variable as unusable.
todo 找到这个macro



First steps in the start_kernel
init/main.c
```
asmlinkage __visible void __init start_kernel(void)
{
	char *command_line;
	char *after_dashes;
	...
}	
```
The first represents a pointer to the kernel command line and the second will contain the result of the parse_args function 
which parses an input string with parameters in the form name=value, looking for specific keywords and invoking the right handlers.
We will not go into the details related with these two variables at this time, but will see it in the next parts.

In the next step we can see a call to the set_task_stack_end_magic function. This function takes address of the init_task 
and sets STACK_END_MAGIC (0x57AC6E9D) as canary for it. 
 a canary to the init process stack to prevent stack overflow.
init_task represents the initial task structure:
init/main.c
```
set_task_stack_end_magic(&init_task);
```
kernel/fork.c
```
void set_task_stack_end_magic(struct task_struct *tsk)
{
	unsigned long *stackend;

	stackend = end_of_stack(tsk);
	*stackend = STACK_END_MAGIC;	/* for overflow detection */
}
```

init/init_task.c
```
struct task_struct init_task
#ifdef CONFIG_ARCH_TASK_STRUCT_ON_STACK
	__init_task_data
#endif
= {
#ifdef CONFIG_THREAD_INFO_IN_TASK
	.thread_info	= INIT_THREAD_INFO(init_task),
	.stack_refcount	= ATOMIC_INIT(1),
#endif
	.state		= 0,
	.stack		= init_stack,
    .usage		= ATOMIC_INIT(2),
	.flags		= PF_KTHREAD,
	.prio		= MAX_PRIO - 20,
	.static_prio	= MAX_PRIO - 20,
	.normal_prio	= MAX_PRIO - 20,
	.policy		= SCHED_NORMAL,
	.cpus_allowed	= CPU_MASK_ALL,	
	...
	.tasks		= LIST_HEAD_INIT(init_task.tasks),
...	
```
arch/x86/include/asm/thread_info.h
```
#define INIT_THREAD_INFO(tsk)			\
{						\
	.flags		= 0,			\
}
```

include/linux/sched.h
```
struct task_struct {
#ifdef CONFIG_THREAD_INFO_IN_TASK
	/*
	 * For reasons of header soup (see current_thread_info()), this
	 * must be the first element of task_struct.
	 */
	struct thread_info		thread_info;
#endif
...
    void				*stack;
....
```
where task_struct stores all the information about a process. I will not explain this structure in this book because it's very big.
At this moment task_struct contains more than 100 fields! Although you will not see the explanation of the task_struct in this book, 
we will use it very often since it is the fundamental structure which describes the process in the Linux kernel.
I will describe the meaning of the fields of this structure as we meet them in practice.

it just fills the init_task with the values for the first process. For example it sets:
1 init process state to zero or runnable. A runnable process is one which is waiting only for a CPU to run on;
2 init process flags - PF_KTHREAD which means - kernel thread;
3 a list of runnable task;
4 process address space;
5 init process stack to the &init_thread_info 


 thread_info defined as:
https://github.com/torvalds/linux/blob/v3.16/arch/x86/include/asm/thread_info.h  不同版本的thread info定义不同
```
struct thread_info {
	struct task_struct	*task;		/* main task structure */
	struct exec_domain	*exec_domain;	/* execution domain */
	__u32			flags;		/* low level flags */
	__u32			status;		/* thread synchronous flags */
	__u32			cpu;		/* current CPU */
	int			saved_preempt_count;
	mm_segment_t		addr_limit;
	struct restart_block    restart_block;
	void __user		*sysenter_return;
	unsigned int		sig_on_uaccess_error:1;
	unsigned int		uaccess_err:1;	/* uaccess failed */
};
```
and occupies 52 bytes. The thread_info structure contains architecture-specific information on the thread. 
We know that on x86_64 the stack grows down and thread_union.thread_info is stored at the bottom of the stack in our case. 
So the process stack is 16 kilobytes and thread_info is at the bottom. 
The remaining thread size will be 16 kilobytes - 62 bytes = 16332 bytes. 

Schematically it can be represented as follows:
```
+-----------------------+
|                       |
|                       |
|        stack          |
|                       |
|_______________________|
|          |            |
|          |            |
|          |            |
|__________↓____________|             +--------------------+
|                       |             |                    |
|      thread_info      |<----------->|     task_struct    |
|                       |             |                    |
+-----------------------+             +--------------------+
```
https://www.quora.com/In-Linux-kernel-why-thread_info-structure-and-the-kernel-stack-of-a-process-binds-in-union-construct


set_task_stack_end_magic gets the end of the stack for the given task_struct with the end_of_stack function. 
Earlier (and now for all architectures besides x86_64) stack was located in the thread_info structure.
So the end of a process stack depends on the CONFIG_STACK_GROWSUP configuration option. As we learn in x86_64 architecture, 
the stack grows down. So the end of the process stack will be:
include/linux/sched/task_stack.h
```
static inline unsigned long *end_of_stack(struct task_struct *p)
{
#ifdef CONFIG_STACK_GROWSUP
	return (unsigned long *)((unsigned long)task_thread_info(p) + THREAD_SIZE) - 1;
#else
	return (unsigned long *)(task_thread_info(p) + 1);
#endif
}
```
where task_thread_info just returns the stack 
```
#ifdef CONFIG_THREAD_INFO_IN_TASK
static inline struct thread_info *task_thread_info(struct task_struct *task)
{
	return &task->thread_info;
}
#elif !defined(__HAVE_THREAD_FUNCTIONS)
# define task_thread_info(task)	((struct thread_info *)(task)->stack)
#endif
```

From the Linux kernel v4.9-rc1 release, thread_info structure may contains only flags and stack pointer resides 
in task_struct structure which represents a thread in the Linux kernel. This depends on CONFIG_THREAD_INFO_IN_TASK 
kernel configuration option which is enabled by default for x86_64.
https://github.com/torvalds/linux/blob/v4.16/init/Kconfig   todo kconfig是啥
```
config THREAD_INFO_IN_TASK
	bool
	help
	  Select this to move thread_info off the stack into task_struct.  To
	  make this work, an arch will need to remove all thread_info fields
	  except flags and fix any runtime bugs.

	  One subtle change that will be needed is to use try_get_task_stack()
	  and put_task_stack() in save_thread_stack_tsk() and get_wchan().
```
arch/x86/Kconfig
```
config X86
	def_bool y
	...
	select THREAD_INFO_IN_TASK
	...
```
As we got the end of the init process stack, we write STACK_END_MAGIC there. After canary is set, we can check it like this:
```
if (*end_of_stack(task) != STACK_END_MAGIC) {
        //
        // handle stack overflow here
    //
}
```

init/main.c
```
asmlinkage __visible void __init start_kernel(void)
{
	char *command_line;
	char *after_dashes;

	set_task_stack_end_magic(&init_task);
	smp_setup_processor_id();
	...
}

void __init __weak smp_setup_processor_id(void)
{
}	
```
The next function after the set_task_stack_end_magic is smp_setup_processor_id. This function has an empty body for x86_64
as it not implemented for all architectures, but some such as s390 and arm64


The next function in start_kernel is debug_objects_early_init. Implementation of this function is almost the same as lockdep_init,
but fills hashes for object debugging. As I wrote above, we will not see the explanation of this and other functions 
which are for debugging purposes in this chapter.


boot_init_stack_canary
arch/x86/include/asm/stackprotector.h
```
static __always_inline void boot_init_stack_canary(void)
{
	u64 canary;
	u64 tsc;

#ifdef CONFIG_X86_64
	BUILD_BUG_ON(offsetof(union irq_stack_union, stack_canary) != 40);
#endif
	/*
	 * We both use the random pool and the current TSC as a source
	 * of randomness. The TSC only matters for very early init,
	 * there it already has some randomness on most systems. Later
	 * on during the bootup the random pool has true entropy too.
	 */
	get_random_bytes(&canary, sizeof(canary));
	tsc = rdtsc();
	canary += tsc + (tsc << 32UL);
	canary &= CANARY_MASK;

	current->stack_canary = canary;
#ifdef CONFIG_X86_64
	this_cpu_write(irq_stack_union.stack_canary, canary);
#else
	this_cpu_write(stack_canary.canary, canary);
#endif
}
```
the boot_init_stack_canary function which fills task_struct->canary with the canary value for the -fstack-protector gcc feature. 
This function depends on the CONFIG_CC_STACKPROTECTOR configuration option and if this option is disabled, 
boot_init_stack_canary does nothing, otherwise it generates random numbers based on random pool and the TSC
got a random number, fill the stack_canary field of task_struct with it,write this value to the top of the IRQ stack

TSC
https://en.wikipedia.org/wiki/Time_Stamp_Counter
The Time Stamp Counter (TSC) is a 64-bit register present on all x86 processors since the Pentium.
It counts the number of CPU cycles since its reset. The instruction RDTSC returns the TSC in EDX:EAX. 
In x86-64 mode, RDTSC also clears the upper 32 bits of RAX and RDX



kernel/cpu.c
```
/*
 * Activate the first processor.
 */
void __init boot_cpu_init(void)
{
	int cpu = smp_processor_id();

	/* Mark the boot cpu "present", "online" etc for SMP and UP case */
	set_cpu_online(cpu, true);
	set_cpu_active(cpu, true);
	set_cpu_present(cpu, true);
	set_cpu_possible(cpu, true);

#ifdef CONFIG_SMP
	__boot_cpu_id = cpu;
#endif
}
```
This function initializes various CPU masks for the bootstrap processor. 
First of all it gets the bootstrap processor id
For now it is just zero. If the CONFIG_DEBUG_PREEMPT configuration option is disabled, 
smp_processor_id just expands to the call of raw_smp_processor_id which expands to the:
include/linux/smp.h
```
# define smp_processor_id() raw_smp_processor_id()
#endif
```
arch/x86/include/asm/smp.h
```
#define raw_smp_processor_id() (this_cpu_read(cpu_number))
```
this_cpu_read as many other function like this (this_cpu_write, this_cpu_add and etc...)  and presents this_cpu operation. 
These operations provide a way of optimizing access to the per-cpu variables which are associated with the current processor. 
In our case it is this_cpu_read:
include/linux/percpu-defs.h
```
#define this_cpu_read(pcp)		__pcpu_size_call_return(this_cpu_read_, pcp)
```
Remember that we have passed cpu_number as pcp to the this_cpu_read from the raw_smp_processor_id. 
Now let's look at the __pcpu_size_call_return implementation:
```
#define __pcpu_size_call_return(stem, variable)				\
({									\
	typeof(variable) pscr_ret__;					\
	__verify_pcpu_ptr(&(variable));					\
	switch(sizeof(variable)) {					\
	case 1: pscr_ret__ = stem##1(variable); break;			\
	case 2: pscr_ret__ = stem##2(variable); break;			\
	case 4: pscr_ret__ = stem##4(variable); break;			\
	case 8: pscr_ret__ = stem##8(variable); break;			\
	default:							\
		__bad_size_call_parameter(); break;			\
	}								\
	pscr_ret__;							\
})
```
First of all we can see the definition of the pscr_ret__ variable with the int type. 
Why int? Ok, variable is common_cpu and it was declared as per-cpu int variable:
```
#define DECLARE_PER_CPU_READ_MOSTLY(type, name)			\
	DECLARE_PER_CPU_SECTION(type, name, "..read_mostly")
```
In the next step we call __verify_pcpu_ptr with the address of cpu_number. __veryf_pcpu_ptr used to verify 
that the given parameter is a per-cpu pointer.
```
#define __verify_pcpu_ptr(ptr)						\
do {									\
	const void __percpu *__vpp_verify = (typeof((ptr) + 0))NULL;	\
	(void)__vpp_verify;						\
} while (0)

```

After that we set pscr_ret__ value which depends on the size of the variable. Our common_cpu variable is int,
so it 4 bytes in size. It means that we will get this_cpu_read_4(common_cpu) in pscr_ret__. 
//todo this_cpu_read_4怎么来的
In the end of the __pcpu_size_call_return we just call it. this_cpu_read_4 is a macro:
arch/x86/include/asm/percpu.h
```
#define this_cpu_read_4(pcp)		percpu_from_op("mov", pcp)
```
which calls percpu_from_op and pass mov instruction and per-cpu variable there. percpu_from_op will expand to the inline assembly call:
```
#define percpu_from_op(op, var)				\
({							\
	typeof(var) pfo_ret__;				\
	switch (sizeof(var)) {				\
	case 1:						\
		asm(op "b "__percpu_arg(1)",%0"		\
		    : "=q" (pfo_ret__)			\
		    : "m" (var));			\
		break;					\
	case 2:						\
		asm(op "w "__percpu_arg(1)",%0"		\
		    : "=r" (pfo_ret__)			\
		    : "m" (var));			\
		break;					\
	case 4:						\
		asm(op "l "__percpu_arg(1)",%0"		\
		    : "=r" (pfo_ret__)			\
		    : "m" (var));			\
		break;					\
	case 8:						\
		asm(op "q "__percpu_arg(1)",%0"		\
		    : "=r" (pfo_ret__)			\
		    : "m" (var));			\
		break;					\
	default: __bad_percpu_size();			\
	}						\
	pfo_ret__;					\
})
```
todo 理解汇编

As we didn't setup per-cpu area, we have only one - for the current running CPU, 
we will get zero as a result of the smp_processor_id.

As we got the current processor id, boot_cpu_init sets the given CPU online, active, present and possible with the:
kernel/cpu.c
```
void __init boot_cpu_init(void)
{
	int cpu = smp_processor_id();

	set_cpu_online(cpu, true);
	set_cpu_active(cpu, true);
	set_cpu_present(cpu, true);
	set_cpu_possible(cpu, true);
...
}
```
All of these functions use the concept - cpumask. 
cpu_possible is a set of CPU ID's which can be plugged in at any time during the life of that system boot. 
cpu_present represents which CPUs are currently plugged in. 
cpu_online represents subset of the cpu_present and indicates CPUs which are available for scheduling.
These masks depend on the CONFIG_HOTPLUG_CPU configuration option and if this option is disabled 
  possible == present and active == online. 
Implementation of the all of these functions are very similar. Every function checks the second parameter. 
  If it is true, it calls cpumask_set_cpu or cpumask_clear_cpu otherwise.
For example let's look at set_cpu_possible. As we passed true as the second parameter, the:
include/linux/cpumask.h
```
static inline void
set_cpu_possible(unsigned int cpu, bool possible)
{
	if (possible)
		cpumask_set_cpu(cpu, &__cpu_possible_mask);
	else
		cpumask_clear_cpu(cpu, &__cpu_possible_mask);
}

static inline void cpumask_set_cpu(unsigned int cpu, struct cpumask *dstp)
{
	set_bit(cpumask_check(cpu), cpumask_bits(dstp));
}

#define cpumask_bits(maskp) ((maskp)->bits)

typedef struct cpumask { DECLARE_BITMAP(bits, NR_CPUS); } cpumask_t;
```
CPU masks provide a bitmap suitable for representing the set of CPU's in a system, one bit position per CPU number.
CPU mask presented by the cpumask structure
DECLARE_BITMAP todo 找这个宏

更多关于cpumask 和 set_cpu_*
https://0xax.gitbooks.io/linux-insides/content/Concepts/linux-cpu-2.html
https://www.kernel.org/doc/Documentation/cpu-hotplug.txt

As we activated the bootstrap processor, it's time to go to the next function in the start_kernel. 
Now it is page_address_init, but this function does nothing in our case, because it executes only when all 
RAM can't be mapped directly.


Print linux banner
The next call is pr_notice:
init/main.c
```
...
pr_notice("%s", linux_banner);
...
```
as you can see it just expands to the printk call. At this moment we use pr_notice to print the Linux banner
include/linux/printk.h
```
#define pr_notice(fmt, ...) \
	printk(KERN_NOTICE pr_fmt(fmt), ##__VA_ARGS__)
```

which is just the kernel version with some additional parameters:
```
Linux version 4.0.0-rc6+ (alex@localhost) (gcc version 4.9.1 (Ubuntu 4.9.1-16ubuntu6) ) #319 SMP
```



Architecture-dependent parts of initialization
The next step is architecture-specific initialization. The Linux kernel does it with the call of the setup_arch function. 
This is a very big function like start_kernel and we do not have time to consider all of its implementation in this part.
Here we'll only start to do it and continue in the next part. As it is architecture-specific,
we need to go again to the arch/ directory. 

This function starts from the reserving memory block for the kernel _text and _data which starts from the _text symbol 
 and ends before __bss_stop. We are using memblock for the reserving of memory block:
init/main.c
```
...
setup_arch(&command_line);
...
```
arch/x86/kernel/setup.c
```
void __init setup_arch(char **cmdline_p)
{
	memblock_reserve(__pa_symbol(_text),
			 (unsigned long)__bss_stop - (unsigned long)_text);
...			 
```
You can read about memblock https://0xax.gitbooks.io/linux-insides/content/MM/linux-mm-1.html

memblock_reserve function takes two parameters:
1 base physical address of a memory block;
2 size of a memory block.

We can get the base physical address of the _text symbol with the __pa_symbol macro:
arch/x86/include/asm/page.h
```
#define __pa_symbol(x) \
	__phys_addr_symbol(__phys_reloc_hide((unsigned long)(x)))
```
arch/x86/include/asm/page_64.h
```
#define __phys_reloc_hide(x)	(x)
#define __phys_addr_symbol(x) \
	((unsigned long)(x) - __START_KERNEL_map + phys_base)
```
First of all it calls __phys_reloc_hide macro on the given parameter. The __phys_reloc_hide macro does nothing for x86_64 and
just returns the given parameter. 
Implementation of the __phys_addr_symbol macro is easy. It just subtracts the symbol address from the 
  base address of the kernel text mapping base virtual address (you can remember that it is __START_KERNEL_map) 
  and adds phys_base which is the base address of _text

After we got the physical address of the _text symbol, memblock_reserve can reserve a memory block from 
the _text to the __bss_stop - _text.




Initial ramdisk
https://en.wikipedia.org/wiki/Initial_ramdisk
In Linux systems, initrd (initial ramdisk) is a scheme for loading a temporary root file system into memory,
to be used as part of the Linux startup process. initrd and initramfs (from INITial RAM File System)
refer to two different methods of achieving this. Both are commonly used to make preparations before
the real root file system can be mounted.

Reserve memory for initrd
In the next step after we reserved place for the kernel text and data is reserving place for the initrd. 
We will not see details about initrd in this post, you just may know that it is temporary root file system stored in memory 
and used by the kernel during its startup. The early_reserve_initrd function does all work. 
First of all this function gets the base address of the ram disk, its size and the end address with:
After we got ramdisk's size, base address and end address, we check that bootloader provided ramdisk
and reserve memory block with the calculated addresses for the initial ramdisk in the end

arch/x86/kernel/setup.c
```
void __init setup_arch(char **cmdline_p)
{
	memblock_reserve(__pa_symbol(_text),
			 (unsigned long)__bss_stop - (unsigned long)_text);

	early_reserve_initrd();
	....
}

static void __init early_reserve_initrd(void)
{
	/* Assume only end is not page aligned */
	u64 ramdisk_image = get_ramdisk_image();
	u64 ramdisk_size  = get_ramdisk_size();
	u64 ramdisk_end   = PAGE_ALIGN(ramdisk_image + ramdisk_size);

	if (!boot_params.hdr.type_of_loader ||
	    !ramdisk_image || !ramdisk_size)
		return;		/* No initrd provided by bootloader */

	memblock_reserve(ramdisk_image, ramdisk_end - ramdisk_image);
}	
```
ramdisk_image,ramdisk_size,ramdisk_end,All of these parameters are taken from boot_params.
The kernel setup header contains a couple of fields which describes ramdisk, for example:
```
Field name:    ramdisk_image
Type:        write (obligatory)
Offset/size:    0x218/4
Protocol:    2.00+

  The 32-bit linear address of the initial ramdisk or ramfs.  Leave at
  zero if there is no initial ramdisk/ramfs.
```
So we can get all the information that interests us from boot_params. For example let's look at get_ramdisk_image:
```
static u64 __init get_ramdisk_image(void)
{
	u64 ramdisk_image = boot_params.hdr.ramdisk_image;
	ramdisk_image |= (u64)boot_params.ext_ramdisk_image << 32;
	return ramdisk_image;
}
```
Here we get the address of the ramdisk from the boot_params and shift left it on 32. We need to do it because 
as you can read in the Documentation/x86/zero-page.txt:
https://github.com/0xAX/linux/blob/0a07b238e5f488b459b6113a62e06b6aab017f71/Documentation/x86/zero-page.txt
```
0C0/004	ALL	ext_ramdisk_image ramdisk_image high 32bits
```
So after shifting it on 32, we're getting a 64-bit address in ramdisk_image and we return it. 
get_ramdisk_size works on the same principle as get_ramdisk_image, but it used ext_ramdisk_size instead of ext_ramdisk_image.
