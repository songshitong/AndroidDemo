

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