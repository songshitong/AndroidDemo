
https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-10.html
This part will be the last part of the Kernel initialization process chapter, so let's finish it.


After the call of the acpi_early_init function from the init/main.c, we can see the following code:
init/main.c
```
static void __init mm_init(void)
{
	...
	/* Should be run before the first non-init thread is created */
	init_espfix_bsp();
	...
}
```
include/asm-generic/pgtable.h
```
#ifndef CONFIG_X86_ESPFIX64
static inline void init_espfix_bsp(void) { }
#endif
```


Here we can see the call of the init_espfix_bsp function which depends on the CONFIG_X86_ESPFIX64 kernel configuration option. 
As we can understand from the function name, it does something with the stack. This function is defined 
in the arch/x86/kernel/espfix_64.c and prevents leaking of 31:16 bits of the esp register during returning to 16-bit stack. 
First of all we install espfix page upper directory into the kernel page directory in the init_espfix_bs:
arch/x86/kernel/espfix_64.c
```
void __init init_espfix_bsp(void)
{
	pgd_t *pgd;
	p4d_t *p4d;

	/* Install the espfix pud into the kernel page directory */
	pgd = &init_top_pgt[pgd_index(ESPFIX_BASE_ADDR)];
	p4d = p4d_alloc(&init_mm, pgd, ESPFIX_BASE_ADDR);
	p4d_populate(&init_mm, p4d, espfix_pud_page);

	/* Randomize the locations */
	init_espfix_random();

	/* The rest is the same as for any other processor */
	init_espfix_ap(0);
}
```
Where ESPFIX_BASE_ADDR is:
```
#define P4D_SHIFT	39
#define ESPFIX_PGD_ENTRY	_AC(-2, UL)
#define ESPFIX_BASE_ADDR	(ESPFIX_PGD_ENTRY << P4D_SHIFT)
```
Also we can find it in the Documentation/x86/x86_64/mm:
https://github.com/torvalds/linux/blob/16f73eb02d7e1765ccab3d2018e0bd98eb93d973/Documentation/x86/x86_64/mm.txt
```
Virtual memory map with 4 level page tables:
...
... unused hole ...
ffffff0000000000 - ffffff7fffffffff (=39 bits) %esp fixup stacks
...
```

After we've filled page global directory with the espfix pud, the next step is call of the init_espfix_random 
and init_espfix_ap functions.
The first function returns random locations for the espfix page and the second enables the espfix for the current CPU.
arch/x86/kernel/espfix_64.c
```
static void init_espfix_random(void)
{
	unsigned long rand;
	if (!arch_get_random_long(&rand)) {
		/* The constant is an arbitrary large prime */
		rand = rdtsc();
		rand *= 0xc345c6b72fd16123UL;
	}

	slot_random = rand % ESPFIX_STACKS_PER_PAGE;
	page_random = (rand / ESPFIX_STACKS_PER_PAGE)
		& (ESPFIX_PAGE_SPACE - 1);
}

void init_espfix_ap(int cpu)
{
...
done:
	per_cpu(espfix_stack, cpu) = addr;
	per_cpu(espfix_waddr, cpu) = (unsigned long)stack_page
				      + (addr & ~PAGE_MASK);
}
```

After the init_espfix_bsp finished the work, we can see the call of the thread_info_cache_init function 
and allocates cache for the thread_info if THREAD_SIZE is less than PAGE_SIZE
thread_info_cache_init在v4.16变为thread_stack_cache_init
init/main.c
```
...
thread_stack_cache_init();
```
kernel/fork.c
```
void thread_stack_cache_init(void)
{
	thread_stack_cache = kmem_cache_create_usercopy("thread_stack",
					THREAD_SIZE, THREAD_SIZE, 0, 0,
					THREAD_SIZE, NULL);
}
```
As we already know the PAGE_SIZE is (_AC(1,UL) << PAGE_SHIFT) or 4096 bytes and THREAD_SIZE is (PAGE_SIZE << THREAD_SIZE_ORDER) 
or 16384 bytes for the x86_64.
arch/x86/include/asm/page_64_types.h   //不同架构的线程大小不同
```
#ifdef CONFIG_KASAN
#define KASAN_STACK_ORDER 1
#else
#define KASAN_STACK_ORDER 0
#endif

#define THREAD_SIZE_ORDER	(2 + KASAN_STACK_ORDER)
#define THREAD_SIZE  (PAGE_SIZE << THREAD_SIZE_ORDER)
```


The next function after the thread_info_cache_init is the cred_init . 
This function just allocates cache for the credentials (like uid, gid, etc.):
init/main.c
```
...
thread_stack_cache_init();
cred_init();
...
```
kernel/cred.c
```
void __init cred_init(void)
{
	cred_jar = kmem_cache_create("cred_jar", sizeof(struct cred), 0,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC|SLAB_ACCOUNT, NULL);
}
```
more about credentials you can read in the Documentation/security/credentials.txt.
https://github.com/torvalds/linux/blob/16f73eb02d7e1765ccab3d2018e0bd98eb93d973/Documentation/security/credentials.txt
上面链接失效了
https://www.kernel.org/doc/html/v4.18/security/credentials.html



Next step is the fork_init function . The fork_init function allocates cache for the task_struct. 
Let's look on the implementation of the fork_init. First of all we can see definitions of the ARCH_MIN_TASKALIGN macro 
and creation of a slab where task_structs will be allocated:
init/main.c
```
...
cred_init();
fork_init();
...
```
kernel/fork.c
```
void __init fork_init(void)
{
	int i;
#ifndef CONFIG_ARCH_TASK_STRUCT_ALLOCATOR
#ifndef ARCH_MIN_TASKALIGN
#define ARCH_MIN_TASKALIGN	0
#endif
	int align = max_t(int, L1_CACHE_BYTES, ARCH_MIN_TASKALIGN);
	unsigned long useroffset, usersize;

	/* create a slab on which task_structs can be allocated */
	task_struct_whitelist(&useroffset, &usersize);
	task_struct_cachep = kmem_cache_create_usercopy("task_struct",
			arch_task_struct_size, align,
			SLAB_PANIC|SLAB_ACCOUNT,
			useroffset, usersize, NULL);
#endif

	/* do the arch specific task caches init */
	arch_task_cache_init();

	set_max_threads(MAX_THREADS);

	init_task.signal->rlim[RLIMIT_NPROC].rlim_cur = max_threads/2;
	init_task.signal->rlim[RLIMIT_NPROC].rlim_max = max_threads/2;
	init_task.signal->rlim[RLIMIT_SIGPENDING] =
		init_task.signal->rlim[RLIMIT_NPROC];

	for (i = 0; i < UCOUNT_COUNTS; i++) {
		init_user_ns.ucount_max[i] = max_threads/2;
	}

#ifdef CONFIG_VMAP_STACK
	cpuhp_setup_state(CPUHP_BP_PREPARE_DYN, "fork:vm_stack_cache",
			  NULL, free_vm_stack_cache);
#endif

	lockdep_init_task(&init_task);
}
```
arch/Kconfig   todo kconfig是什么文件
```
# Select if arch has its private alloc_task_struct() function
config ARCH_TASK_STRUCT_ALLOCATOR
	bool
```
As we can see this code depends on the CONFIG_ARCH_TASK_STRUCT_ACLLOCATOR kernel configuration option. 
This configuration option shows the presence of the alloc_task_struct for the given architecture. 
As x86_64 has no alloc_task_struct function, this code will not work and even will not be compiled on the x86_64.
presence /ˈprezns/   在场；出现；存在；出席；(尤指执行任务的)部队；仪态；(派遣的)一个队；感觉在附近的人（或鬼魂）记忆


Allocating cache for init task
After this we can see the call of the arch_task_cache_init function in the fork_init:
arch/sh/kernel/process.c
```
void arch_task_cache_init(void)
{
	if (!xstate_size)
		return;

	task_xstate_cachep = kmem_cache_create("task_xstate", xstate_size,
					       __alignof__(union thread_xstate),
					       SLAB_PANIC, NULL);
}
```
The arch_task_cache_init does initialization of the architecture-specific caches. //没找到x86目录下面
In our case it is x86_64, so as we can see, the arch_task_cache_init allocates cache for the task_xstate 
which represents FPU state and sets up offsets and sizes of all extended states in xsave area with 
the call of the setup_xstate_comp function.
https://en.wikipedia.org/wiki/Floating-point_unit
A floating-point unit (FPU, colloquially a math coprocessor) is a part of a computer system specially 
designed to carry out operations on floating-point numbers.

https://www.felixcloutier.com/x86/xsaves
Save Processor Extended States Supervisor

setup_xstate_comp 在early_cpu_init->early_identify_cpu->fpu__init_system->fpu__init_system_xstate->
fpu__init_cpu_xstate()->setup_xstate_comp
arch/x86/kernel/fpu/xstate.c
```
static void __init setup_xstate_comp(void)
{
  ...
  xstate_comp_offsets[i] = xstate_comp_offsets[i-1]
					+ xstate_comp_sizes[i-1];
  ...					
}
```

After the arch_task_cache_init we calculate default maximum number of threads with the:
set_max_threads(MAX_THREADS);

where default maximum number of threads is:
kernel/fork.c
```
#define MAX_THREADS FUTEX_TID_MASK

include/uapi/linux/futex.h
#define FUTEX_TID_MASK		0x3fffffff
```


In the end of the fork_init function we initialize signal handler:
```
init_task.signal->rlim[RLIMIT_NPROC].rlim_cur = max_threads/2;
init_task.signal->rlim[RLIMIT_NPROC].rlim_max = max_threads/2;
init_task.signal->rlim[RLIMIT_SIGPENDING] =
        init_task.signal->rlim[RLIMIT_NPROC];
```
As we know the init_task is an instance of the task_struct structure, so it contains signal field which represents signal handler.
It has following type struct signal_struct. On the first two lines we can see setting of the current 
and maximum limit of the resource limits. Every process has an associated set of resource limits. 
These limits specify amount of resources which current process can use. Here rlim is resource control limit and presented by the:
include/uapi/linux/resource.h
```
struct rlimit {
	__kernel_ulong_t	rlim_cur;
	__kernel_ulong_t	rlim_max;
};
```
