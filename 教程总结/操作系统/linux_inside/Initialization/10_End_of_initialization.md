
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

In our case the resource is the RLIMIT_NPROC which is the maximum number of processes that user can own 
and RLIMIT_SIGPENDING - the maximum number of pending signals. We can see it in the:  todo 测试
```
cat /proc/self/limits
Limit                     Soft Limit           Hard Limit           Units     
...
Max processes             63815                63815                processes 
Max pending signals       63815                63815                signals 
```



Initialization of the caches
The next function after the fork_init is the proc_caches_init. 
init/main.c
```
...
fork_init();
proc_caches_init();
```
kernel/fork.c
```
void __init proc_caches_init(void)
{
	sighand_cachep = kmem_cache_create("sighand_cache",
			sizeof(struct sighand_struct), 0,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC|SLAB_TYPESAFE_BY_RCU|
			SLAB_ACCOUNT, sighand_ctor);
	signal_cachep = kmem_cache_create("signal_cache",
			sizeof(struct signal_struct), 0,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC|SLAB_ACCOUNT,
			NULL);
	files_cachep = kmem_cache_create("files_cache",
			sizeof(struct files_struct), 0,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC|SLAB_ACCOUNT,
			NULL);
	fs_cachep = kmem_cache_create("fs_cache",
			sizeof(struct fs_struct), 0,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC|SLAB_ACCOUNT,
			NULL);
	
	mm_cachep = kmem_cache_create_usercopy("mm_struct",
			sizeof(struct mm_struct), ARCH_MIN_MMSTRUCT_ALIGN,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC|SLAB_ACCOUNT,
			offsetof(struct mm_struct, saved_auxv),
			sizeof_field(struct mm_struct, saved_auxv),
			NULL);
	vm_area_cachep = KMEM_CACHE(vm_area_struct, SLAB_PANIC|SLAB_ACCOUNT);
	mmap_init();
	nsproxy_cache_init();
}
```
This function allocates caches for the memory descriptors (or mm_struct structure). 
At the beginning of the proc_caches_init we can see allocation of the different SLAB caches with 
the call of the kmem_cache_create:
sighand_cachep - manage information about installed signal handlers;
signal_cachep - manage information about process signal descriptor;
files_cachep - manage information about opened files;
fs_cachep - manage filesystem information

https://en.wikipedia.org/wiki/Slab_allocation
Slab allocation is a memory management mechanism intended for the efficient memory allocation of objects. 
In comparison with earlier mechanisms, it reduces fragmentation caused by allocations and deallocations. 
This technique is used for retaining allocated memory containing a data object of a certain type for 
reuse upon subsequent allocations of objects of the same type. It is analogous to an object pool, but only applies to memory,
not other resources.


After this we allocate SLAB cache for the mm_struct structures:
mm_cachep = kmem_cache_create_usercopy("mm_struct"...)
After this we allocate SLAB cache for the important vm_area_struct which used by the kernel to manage virtual memory space:
vm_area_cachep = KMEM_CACHE(vm_area_struct, SLAB_PANIC|SLAB_ACCOUNT);
Note, that we use KMEM_CACHE macro here instead of the kmem_cache_create. This macro is just expands to the kmem_cache_create call:
include/linux/slab.h
```
#define KMEM_CACHE(__struct, __flags)					\
		kmem_cache_create(#__struct, sizeof(struct __struct),	\
			__alignof__(struct __struct), (__flags), NULL)
```
The KMEM_CACHE has one difference from kmem_cache_create. Take a look on __alignof__ operator. 
The KMEM_CACHE macro aligns SLAB to the size of the given structure, but kmem_cache_create uses given value to align space.

After this we can see the call of the mmap_init and nsproxy_cache_init functions.
The first function initializes virtual memory area SLAB and the second function initializes SLAB for namespaces.



The next function after the proc_caches_init is buffer_init
init/main.c
```
...
proc_caches_init();
buffer_init();
```
fs/buffer.c
```
void __init buffer_init(void)
{
	unsigned long nrpages;
	int ret;

	bh_cachep = kmem_cache_create("buffer_head",
			sizeof(struct buffer_head), 0,
				(SLAB_RECLAIM_ACCOUNT|SLAB_PANIC|
				SLAB_MEM_SPREAD),
				NULL);

	/*
	 * Limit the bh occupancy to 10% of ZONE_NORMAL
	 */
	nrpages = (nr_free_buffer_pages() * 10) / 100;
	max_buffer_heads = nrpages * (PAGE_SIZE / sizeof(struct buffer_head));
	ret = cpuhp_setup_state_nocalls(CPUHP_FS_BUFF_DEAD, "fs/buffer:dead",
					NULL, buffer_exit_cpu_dead);
	WARN_ON(ret < 0);
}
```
This function is allocate cache for the buffer_head.
include/linux/buffer_head.h
```
struct buffer_head {
	unsigned long b_state;		/* buffer state bitmap (see above) */
	struct buffer_head *b_this_page;/* circular list of page's buffers */
	struct page *b_page;		/* the page this bh is mapped to */

	sector_t b_blocknr;		/* start block number */
	size_t b_size;			/* size of mapping */
	char *b_data;			/* pointer to data within the page */

	struct block_device *b_bdev;
	bh_end_io_t *b_end_io;		/* I/O completion */
 	void *b_private;		/* reserved for b_end_io */
	struct list_head b_assoc_buffers; /* associated with another mapping */
	struct address_space *b_assoc_map;	/* mapping this buffer is associated with */
	atomic_t b_count;		/* users using this buffer_head */
};
```
The buffer_head is a special structure which  used for managing buffers. 
In the start of the buffer_init function we allocate cache for the struct buffer_head structures with 
the call of the kmem_cache_create function as we did in the previous functions. 
And calculate the maximum size of the buffers in memory,which will be equal to the 10% of the ZONE_NORMAL 
   (all RAM from the 4GB on the x86_64).
```
nrpages = (nr_free_buffer_pages() * 10) / 100;
max_buffer_heads = nrpages * (PAGE_SIZE / sizeof(struct buffer_head));
```



The next function after the buffer_init is - vfs_caches_init. 
init/main.c
```
buffer_init();
...
vfs_caches_init();
```
fs/dcache.c
```
void __init vfs_caches_init(void)
{
	names_cachep = kmem_cache_create_usercopy("names_cache", PATH_MAX, 0,
			SLAB_HWCACHE_ALIGN|SLAB_PANIC, 0, PATH_MAX, NULL);
	dcache_init();
	inode_init();
	files_init();
	files_maxfiles_init();
	mnt_init();
	bdev_cache_init();
	chrdev_init();
}
```
This function allocates SLAB caches and hashtable for different VFS caches.
We already saw the vfs_caches_init_early function in the eighth part of the linux kernel initialization process 
which initialized caches for dcache (or directory-cache) and inode cache. The vfs_caches_init function makes 
post-early initialization of the dcache and inode caches, private data cache, hash tables for the mount points, etc.
More details about VFS will be described in the separate part. 


After this we can see signals_init function. This function is defined  allocates a cache 
for the sigqueue structures which represents queue of the real time signals. 
```
init/main.c
signals_init();

kernel/signal.c
void __init signals_init(void)
{
	/* If this check fails, the __ARCH_SI_PREAMBLE_SIZE value is wrong! */
	BUILD_BUG_ON(__ARCH_SI_PREAMBLE_SIZE
		!= offsetof(struct siginfo, _sifields._pad));
	BUILD_BUG_ON(sizeof(struct siginfo) != SI_MAX_SIZE);

	sigqueue_cachep = KMEM_CACHE(sigqueue, SLAB_PANIC);
}
```

The next function is page_writeback_init. This function initializes the ratio for the dirty pages. 
Every low-level page entry contains the dirty bit which indicates whether a page has been written to after been loaded into memory.
```
init/main.c
pagecache_init()

mm/filemap.c
void __init pagecache_init(void)
{
	int i;
	for (i = 0; i < PAGE_WAIT_TABLE_SIZE; i++)
		init_waitqueue_head(&page_wait_table[i]);

	page_writeback_init();
}
```
mm/page-writeback.c
```
void __init page_writeback_init(void)
{
	BUG_ON(wb_domain_init(&global_wb_domain, GFP_KERNEL));

	cpuhp_setup_state(CPUHP_AP_ONLINE_DYN, "mm/writeback:online",
			  page_writeback_cpu_online, NULL);
	cpuhp_setup_state(CPUHP_MM_WRITEBACK_DEAD, "mm/writeback:dead", NULL,
			  page_writeback_cpu_online);
}
```



Creation of the root for the procfs
After all of this preparations we need to create the root for the proc filesystem. We will do it with the call of the 
proc_root_init function from the fs/proc/root.c. 
```
init/main.c
proc_root_init();

fs/proc/root.c
void __init proc_root_init(void)
{
	int err;
	proc_init_inodecache();
	set_proc_pid_nlink();
	err = register_filesystem(&proc_fs_type);
	if (err)
		return;

	proc_self_init();
	proc_thread_self_init();
	proc_symlink("mounts", NULL, "self/mounts");

	proc_net_init();

#ifdef CONFIG_SYSVIPC
	proc_mkdir("sysvipc", NULL);
#endif
	proc_mkdir("fs", NULL);
	proc_mkdir("driver", NULL);
	proc_create_mount_point("fs/nfsd"); /* somewhere for the nfsd filesystem to be mounted */
#if defined(CONFIG_SUN_OPENPROMFS) || defined(CONFIG_SUN_OPENPROMFS_MODULE)
	/* just give it a mountpoint */
	proc_create_mount_point("openprom");
#endif
	proc_tty_init();
	proc_mkdir("bus", NULL);
	proc_sys_init();
}
```

https://en.wikipedia.org/wiki/Procfs
The proc filesystem (procfs) is a special filesystem in Unix-like operating systems that presents information about processes 
and other system information in a hierarchical file-like structure, providing a more convenient and 
standardized method for dynamically accessing process data held in the kernel than traditional tracing methods 
or direct access to kernel memory. 


At the start of the proc_root_init function we allocate
the cache for the inodes and register a new filesystem in the system with the
err = register_filesystem(&proc_fs_type);
As I wrote above we will not dive into details about VFS and different filesystems in this chapter,
but will see it in the chapter about the VFS. 

After we've registered a new filesystem in our system, we call the proc_self_init function from the fs/proc/self.c
fs/proc/self.c
```
void __init proc_self_init(void)
{
	proc_alloc_inum(&self_inum);
}
```
fs/proc/generic.c
```
/*
 * Return an inode number between PROC_DYNAMIC_FIRST and
 * 0xffffffff, or zero on failure.
 */
int proc_alloc_inum(unsigned int *inum)
{
	int i;

	i = ida_simple_get(&proc_inum_ida, 0, UINT_MAX - PROC_DYNAMIC_FIRST + 1,
			   GFP_KERNEL);
	if (i < 0)
		return i;

	*inum = PROC_DYNAMIC_FIRST + (unsigned int)i;
	return 0;
}
```

and this function allocates inode number for the self (/proc/self directory refers to the process accessing the /proc filesystem). 
The next step after the proc_self_init is proc_setup_thread_self which setups the /proc/thread-self directory 
which contains information about current thread.
fs/proc/thread_self.c
```
int proc_setup_thread_self(struct super_block *s)
{
	struct inode *root_inode = d_inode(s->s_root);
	struct pid_namespace *ns = s->s_fs_info;
	struct dentry *thread_self;

	inode_lock(root_inode);
	thread_self = d_alloc_name(s->s_root, "thread-self");
	if (thread_self) {
		struct inode *inode = new_inode_pseudo(s);
		if (inode) {
			inode->i_ino = thread_self_inum;
			inode->i_mtime = inode->i_atime = inode->i_ctime = current_time(inode);
			inode->i_mode = S_IFLNK | S_IRWXUGO;
			inode->i_uid = GLOBAL_ROOT_UID;
			inode->i_gid = GLOBAL_ROOT_GID;
			inode->i_op = &proc_thread_self_inode_operations;
			d_add(thread_self, inode);
		} else {
			dput(thread_self);
			thread_self = ERR_PTR(-ENOMEM);
		}
	} else {
		thread_self = ERR_PTR(-ENOMEM);
	}
	....
	return 0;
}

```

After this we create /proc/self/mounts symlink which will contains mount points with the call of the
proc_symlink("mounts", NULL, "self/mounts");
and a couple of directories depends on the different configuration options


In the end of the proc_root_init we call the proc_sys_init function which creates /proc/sys directory 
and initializes the Sysctl.
fs/proc/proc_sysctl.c
```
int __init proc_sys_init(void)
{
	struct proc_dir_entry *proc_sys_root;
	proc_sys_root = proc_mkdir("sys", NULL);
	proc_sys_root->proc_iops = &proc_sys_dir_operations;
	proc_sys_root->proc_fops = &proc_sys_dir_file_operations;
	proc_sys_root->nlink = 0;
	return sysctl_init();
}
```

https://en.wikipedia.org/wiki/Sysctl
sysctl is a software mechanism in some Unix-like operating systems that reads and modifies the attributes 
of the system kernel such as its version number, maximum limits, and security settings.



It is the end of start_kernel function. I did not describe all functions which are called in the start_kernel. 
I skipped them, because they are not important for the generic kernel initialization stuff and 
depend on only different kernel configurations. They are taskstats_init_early which exports per-task statistic to the user-space, 
delayacct_init - initializes per-task delay accounting,
key_init and security_init initialize different security stuff, 
check_bugs - fix some architecture-dependent bugs, 
ftrace_init function executes initialization of the ftrace,
cgroup_init makes initialization of the rest of the cgroup subsystem,etc. 
Many of these parts and subsystems will be described in the other chapters.



That's all. Finally we have passed through the long-long start_kernel function. But it is not the end of the linux kernel initialization process.
We haven't run the first process yet. In the end of the start_kernel we can see the last call of the - rest_init function. 
Let's go ahead.
init/main.c
```
/* Do the rest non-__init'ed, we're now alive */
	rest_init();
```
First steps after the start_kernel
init/main.c
```
static noinline void __ref rest_init(void)
{
	struct task_struct *tsk;
	int pid;

	rcu_scheduler_starting();
	pid = kernel_thread(kernel_init, NULL, CLONE_FS);
	rcu_read_lock();
	tsk = find_task_by_pid_ns(pid, &init_pid_ns);
	set_cpus_allowed_ptr(tsk, cpumask_of(smp_processor_id()));
	rcu_read_unlock();

	numa_default_policy();
	pid = kernel_thread(kthreadd, NULL, CLONE_FS | CLONE_FILES);
	rcu_read_lock();
	kthreadd_task = find_task_by_pid_ns(pid, &init_pid_ns);
	rcu_read_unlock();
	
	system_state = SYSTEM_SCHEDULING;

	complete(&kthreadd_done);
	schedule_preempt_disabled();
	cpu_startup_entry(CPUHP_ONLINE);
}

```
In the beginning of the rest_init we can see call of the two following functions:
```
rcu_scheduler_starting();
smpboot_thread_init(); //4.16中没有smpboot_thread_init了
```
kernel/rcu/tree.c
```
void rcu_scheduler_starting(void)
{
	WARN_ON(num_online_cpus() != 1);
	WARN_ON(nr_context_switches() > 0);
	rcu_test_sync_prims();
	rcu_scheduler_active = RCU_SCHEDULER_INIT;
	rcu_test_sync_prims();
}
```
The first rcu_scheduler_starting makes RCU scheduler active and the second smpboot_thread_init registers the 
smpboot_thread_notifier CPU notifier (more about it you can read in the CPU hotplug documentation.

https://en.wikipedia.org/wiki/Read-copy-update
In computer science, read-copy-update (RCU) is a synchronization mechanism that avoids the use of lock primitives 
while multiple threads concurrently read and update elements that are linked through pointers and that
belong to shared data structures



the kernel_thread function  creates new kernel thread.As we can see the kernel_thread function takes three arguments:
1 Function which will be executed in a new thread;
2 Parameter for the kernel_init function;
3 Flags.
kernel/fork.c
```
/*
 * Create a kernel thread.
 */
pid_t kernel_thread(int (*fn)(void *), void *arg, unsigned long flags)
{
	return _do_fork(flags|CLONE_VM|CLONE_UNTRACED, (unsigned long)fn,
		(unsigned long)arg, NULL, NULL, 0);
}
```
We will not dive into details about kernel_thread implementation (we will see it in the chapter which describe scheduler,
just need to say that kernel_thread invokes clone).
关于clone:
http://www.tutorialspoint.com/unix_system_calls/clone.htm

Now we only need to know that we create new kernel thread with kernel_thread function, parent and 
child of the thread will use shared information about filesystem and it will start to execute kernel_init function. 
A kernel thread differs from a user thread that it runs in kernel mode. So with these two kernel_thread calls we create 
two new kernel threads with the PID = 1 for init process and PID = 2 for kthreadd.

We already know what is init process. Let's look on the kthreadd. It is a special kernel thread which manages 
  and helps different parts of the kernel to create another kernel thread. We can see it in the output of the ps util:
//todo 测试
```
$ ps -ef | grep kthreadd
root         2     0  0 Jan11 ?        00:00:00 [kthreadd]
```

Let's postpone kernel_init and kthreadd for now and go ahead in the rest_init. In the next step after we have created two new kernel threads 
we can see the following code:
```
rcu_read_lock();
tsk = find_task_by_pid_ns(pid, &init_pid_ns);
set_cpus_allowed_ptr(tsk, cpumask_of(smp_processor_id()));
rcu_read_unlock();
```
The first rcu_read_lock function marks the beginning of an RCU read-side critical section and the rcu_read_unlock
marks the end of an RCU read-side critical section. We call these functions because we need to protect the find_task_by_pid_ns. 
The find_task_by_pid_ns returns pointer to the task_struct by the given pid. So, here we are getting the pointer 
to the task_struct for PID = 2 (we got it after kthreadd creation with the kernel_thread). 

In the next step we call complete function
```
complete(&kthreadd_done);
```
and pass address of the kthreadd_done. The kthreadd_done defined as
```
static __initdata DECLARE_COMPLETION(kthreadd_done);
```
where DECLARE_COMPLETION macro defined as:
include/linux/completion.h
```
#define DECLARE_COMPLETION(work) \
	struct completion work = COMPLETION_INITIALIZER(work)
```
and expands to the definition of the completion structure. This structure is defined  presents completions concept.
Completions is a code synchronization mechanism which provides race-free solution for the threads that 
must wait for some process to have reached a point or a specific state. 
Using completions consists of three parts: 
1 The first is definition of the complete structure and we did it with the DECLARE_COMPLETION. 
2 The second is call of the wait_for_completion. After the call of this function, a thread which called it 
  will not continue to execute and will wait while other thread did not call complete function. 
 Note that we call wait_for_completion with the kthreadd_done in the beginning of the kernel_init_freeable:
kernel_init->kernel_init_freeable-> wait_for_completion
```
wait_for_completion(&kthreadd_done);
```
3 And the last step is to call complete function as we saw it above. 


After this the kernel_init_freeable function will not be executed while kthreadd thread will not be set. 
After the kthreadd was set, we can see three following functions in the rest_init:
```
init_idle_bootup_task(current); //4.16没有这个
 schedule_preempt_disabled();
 cpu_startup_entry(CPUHP_ONLINE);
```


The second function schedule_preempt_disabled disables preempt in idle tasks. 
And the third function cpu_startup_entry is defined in the kernel/sched/idle.c and calls cpu_idle_loop from the kernel/sched/idle.c.
kernel/sched/idle.c
```
void cpu_startup_entry(enum cpuhp_state state)
{
#ifdef CONFIG_X86
	boot_init_stack_canary();
#endif
	arch_cpu_idle_prepare();
	cpuhp_online_idle(state);
	while (1)
		do_idle();
}


static void do_idle(void)
{
	int cpu = smp_processor_id();
	__current_set_polling();
	tick_nohz_idle_enter();

	while (!need_resched()) {
		check_pgt_cache();
		rmb();

		if (cpu_is_offline(cpu)) {
			cpuhp_report_idle_dead();
			arch_cpu_idle_dead();
		}

		local_irq_disable();
		arch_cpu_idle_enter();

		if (cpu_idle_force_poll || tick_check_broadcast_expired())
			cpu_idle_poll();
		else
			cpuidle_idle_call();
		arch_cpu_idle_exit();
	}

	preempt_set_need_resched();
	tick_nohz_idle_exit();
	__current_clr_polling();

	smp_mb__after_atomic();

	sched_ttwu_pending();
	schedule_idle();

	if (unlikely(klp_patch_pending(current)))
		klp_update_patch_state(current);
}
```

The cpu_idle_loop function works as process with PID = 0 and works in the background. 
Main purpose of the cpu_idle_loop is to consume the idle CPU cycles. When there is no process to run, 
this process starts to work. We have one process with idle scheduling class (we just set the current task to the idle 
with the call of the init_idle_bootup_task function), so the idle thread does not do useful work but just checks 
if there is an active task to switch to
```
...
while (1) {
       while (!need_resched()) {
       ...
       ...
       ...
       }
...
}
```
init/main.c
```
static int __ref kernel_init(void *unused)
{
	int ret;

	kernel_init_freeable();
	/* need to finish all async __init code before freeing the memory */
	async_synchronize_full();
	ftrace_free_init_mem();
	jump_label_invalidate_initmem();
	free_initmem();
	mark_readonly();
	system_state = SYSTEM_RUNNING;
	numa_default_policy();

	rcu_end_inkernel_boot();

	if (ramdisk_execute_command) {
		ret = run_init_process(ramdisk_execute_command);
		if (!ret)
			return 0;
		pr_err("Failed to execute %s (error %d)\n",
		       ramdisk_execute_command, ret);
	}

	if (execute_command) {
		ret = run_init_process(execute_command);
		if (!ret)
			return 0;
		panic("Requested init %s failed (error %d).",
		      execute_command, ret);
	}
	if (!try_to_run_init_process("/sbin/init") ||
	    !try_to_run_init_process("/etc/init") ||
	    !try_to_run_init_process("/bin/init") ||
	    !try_to_run_init_process("/bin/sh"))
		return 0;

	panic("No working init found.  Try passing init= option to kernel. "
	      "See Linux Documentation/admin-guide/init.rst for guidance.");
}
```
kernel_init_freeable
```
static noinline void __init kernel_init_freeable(void)
{
	wait_for_completion(&kthreadd_done);

	gfp_allowed_mask = __GFP_BITS_MASK;

	set_mems_allowed(node_states[N_MEMORY]);

	cad_pid = task_pid(current);

	smp_prepare_cpus(setup_max_cpus);

	workqueue_init();

	init_mm_internals();

	do_pre_smp_initcalls();
	lockup_detector_init();

	smp_init();
	sched_init_smp();

	page_alloc_init_late();

	do_basic_setup();
	if (sys_open((const char __user *) "/dev/console", O_RDWR, 0) < 0)
		pr_err("Warning: unable to open an initial console.\n");

	(void) sys_dup(0);
	(void) sys_dup(0);
	if (!ramdisk_execute_command)
		ramdisk_execute_command = "/init";

	if (sys_access((const char __user *) ramdisk_execute_command, 0) != 0) {
		ramdisk_execute_command = NULL;
		prepare_namespace();
	}
	integrity_load_keys();
	load_default_modules();
}
```

More about it will be in the chapter about scheduler. 
So for this moment the start_kernel calls the rest_init function which spawns an init (kernel_init function) process 
and become idle process itself. Now is time to look on the kernel_init.
Execution of the kernel_init function starts from the call of the kernel_init_freeable function.
The kernel_init_freeable function first of all waits for the completion of the kthreadd setup. I already wrote about it above:
wait_for_completion(&kthreadd_done);
After this we set gfp_allowed_mask to __GFP_BITS_MASK which means that system is already running,
set allowed cpus/mems to all CPUs and NUMA nodes with the set_mems_allowed function,
cpus/mems
https://www.kernel.org/doc/Documentation/cgroups/cpusets.txt
NUMA
https://en.wikipedia.org/wiki/Non-uniform_memory_access

allow init process to run on any CPU with the set_cpus_allowed_ptr,
set pid for the cad or Ctrl-Alt-Delete,
do preparation for booting of the other CPUs with the call of the smp_prepare_cpus,
call early initcalls with the do_pre_smp_initcalls,
initialize SMP with the smp_init and initialize lockup_detector with the call of the lockup_detector_init and
lockup_detector_init
initialize scheduler with the sched_init_smp.
https://www.kernel.org/doc/Documentation/lockup-watchdogs.txt
The Linux kernel can act as a watchdog to detect both soft and hard lockups.
A 'softlockup' is defined as a bug that causes the kernel to loop in
kernel mode for more than 20 seconds , without giving other tasks a chance to run.
A 'hardlockup' is defined as a bug that causes the CPU to loop in
kernel mode for more than 10 seconds , without letting other interrupts have a chance to run.


After this we can see the call of the following functions - do_basic_setup. Before we will call the do_basic_setup function,
our kernel already initialized for this moment. As comment says:
```
Now we can finally start doing some real work..
```

init/main.c
```
static void __init do_basic_setup(void)
{
	cpuset_init_smp();
	shmem_init();
	driver_init();
	init_irq_proc();
	do_ctors();
	usermodehelper_enable();
	do_initcalls();
}
```
The do_basic_setup will reinitialize cpuset to the active CPUs, initialize the khelper - which is a kernel thread 
which used for making calls out to userspace from within the kernel, initialize tmpfs, initialize drivers subsystem,
enable the user-mode helper workqueue and make post-early call of the initcalls. 

We can see opening of the dev/console and dup twice file descriptors from 0 to 2 after the do_basic_setup
```
if (sys_open((const char __user *) "/dev/console", O_RDWR, 0) < 0)
		pr_err("Warning: unable to open an initial console.\n");

	(void) sys_dup(0);
	(void) sys_dup(0);
```
We are using two system calls here sys_open and sys_dup. In the next chapters we will see explanation and 
implementation of the different system calls. After we opened initial console, we check that rdinit= option 
was passed to the kernel command line or set default path of the ramdisk:
```
	if (!ramdisk_execute_command)
		ramdisk_execute_command = "/init";
```

Check user's permissions for the ramdisk and call the prepare_namespace function from the init/do_mounts.c 
which checks and mounts the initrd:
```
if (sys_access((const char __user *) ramdisk_execute_command, 0) != 0) {
		ramdisk_execute_command = NULL;
		prepare_namespace();
	}
```
https://en.wikipedia.org/wiki/Initial_ramdisk
In Linux systems, initrd (initial ramdisk) is a scheme for loading a temporary root file system into memory,
to be used as part of the Linux startup process. 


This is the end of the kernel_init_freeable function and we need return to the kernel_init. 
The next step after the kernel_init_freeable finished its execution is the async_synchronize_full. 
This function waits until all asynchronous function calls have been done and after it we will call the free_initmem 
which will release all memory occupied by the initialization stuff which located between __init_begin and __init_end. 
After this we protect .rodata with the mark_rodata_ro and update state of the system from the SYSTEM_BOOTING to the
```
system_state = SYSTEM_RUNNING;
```
And tries to run the init process:
```
if (ramdisk_execute_command) {
		ret = run_init_process(ramdisk_execute_command);
		if (!ret)
			return 0;
		pr_err("Failed to execute %s (error %d)\n",
		       ramdisk_execute_command, ret);
	}
```
First of all it checks the ramdisk_execute_command which we set in the kernel_init_freeable function 
and it will be equal to the value of the rdinit= kernel command line parameters or /init by default. 
The run_init_process function fills the first element of the argv_init array which represents arguments of the init program 
and call do_execve function:
init/main.c
```
static const char *argv_init[MAX_INIT_ARGS+2] = { "init", NULL, };
static int run_init_process(const char *init_filename)
{
	argv_init[0] = init_filename;
	return do_execve(getname_kernel(init_filename),
		(const char __user *const __user *)argv_init,
		(const char __user *const __user *)envp_init);
}
```

The do_execve function is defined runs program with the given file name and arguments.
fs/exec.c
```
int do_execve(struct filename *filename,
	const char __user *const __user *__argv,
	const char __user *const __user *__envp)
{
	struct user_arg_ptr argv = { .ptr.native = __argv };
	struct user_arg_ptr envp = { .ptr.native = __envp };
	return do_execveat_common(AT_FDCWD, filename, argv, envp, 0);
}
```

If we did not pass rdinit= option to the kernel command line, kernel starts to check the execute_command which
is equal to value of the init= kernel command line parameter:
```
if (execute_command) {
		ret = run_init_process(execute_command);
		if (!ret)
			return 0;
		panic("Requested init %s failed (error %d).",
		      execute_command, ret);
	}
```

If we did not pass init= kernel command line parameter either, kernel tries to run one of the following executable files:
```
if (!try_to_run_init_process("/sbin/init") ||
    !try_to_run_init_process("/etc/init") ||
    !try_to_run_init_process("/bin/init") ||
    !try_to_run_init_process("/bin/sh"))
    return 0;
```
Otherwise we finish with panic:
```
panic("No working init found.  Try passing init= option to kernel. "
      "See Linux Documentation/init.txt for guidance.");
```
That's all! Linux kernel initialization process is finished!

