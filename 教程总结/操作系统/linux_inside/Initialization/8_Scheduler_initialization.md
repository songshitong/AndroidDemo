
https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-8.html


The main point of this part is scheduler initialization. But before we will start to learn initialization process of the scheduler, 
we need to do some stuff.

Scheduling  https://en.wikipedia.org/wiki/Scheduling_%28computing%29
In computing, scheduling is the action of assigning resources to perform tasks. The resources may be processors,
network links or expansion cards. The tasks may be threads, processes or data flows.

The scheduling activity is carried out by a process called scheduler. Schedulers are often designed so as 
to keep all computer resources busy (as in load balancing), allow multiple users to share system resources effectively, 
or to achieve a target quality-of-service.

The next step in the init/main.c is the setup_per_cpu_areas function. This function setups memory areas for the percpu variables, 
more about it you can read in the special part about the Per-CPU variables.
```
...
setup_per_cpu_areas
```

After percpu areas is up and running, the next step is the smp_prepare_boot_cpu function.
init/main.c
```
...
smp_prepare_boot_cpu();
```
This function does some preparations for symmetric multiprocessing. Since this function is architecture specific
arch/x86/include/asm/smp.h
```
static inline void smp_prepare_boot_cpu(void)
{
	smp_ops.smp_prepare_boot_cpu();
}
```
We may see here that it just calls the smp_prepare_boot_cpu callback of the smp_ops structure.
If we look at the definition of instance of this structure from the arch/x86/kernel/smp.c source code file,
we will see that the smp_prepare_boot_cpu expands to the call of the native_smp_prepare_boot_cpu function:
arch/x86/kernel/smp.c
```
struct smp_ops smp_ops = {
	.smp_prepare_boot_cpu	= native_smp_prepare_boot_cpu,
	.smp_prepare_cpus	= native_smp_prepare_cpus,
...	
EXPORT_SYMBOL_GPL(smp_ops);
``` 
todo EXPORT_SYMBOL_GPL是啥
arch/x86/kernel/smpboot.c
```
void __init native_smp_prepare_boot_cpu(void)
{
	int me = smp_processor_id();
	switch_to_new_gdt(me);
	/* already set me in cpu_online_mask in boot_cpu_init() */
	cpumask_set_cpu(me, cpu_callout_mask);
	cpu_set_state_online(me);
}
```
executes following things: first of all it gets the id of the current CPU (which is Bootstrap processor
and its id is zero for this moment) with the smp_processor_id function. I will not explain how the smp_processor_id works,
because we already saw it in the Kernel entry point part. 
After we've got processor id number we reload Global Descriptor Table for the given CPU with the switch_to_new_gdt function:
arch/x86/kernel/cpu/common.c
```
void switch_to_new_gdt(int cpu)
{
	/* Load the original GDT */
	load_direct_gdt(cpu);
	/* Reload the per-cpu base */
	load_percpu_segment(cpu);
}

void load_direct_gdt(int cpu)
{
	struct desc_ptr gdt_descr;
	gdt_descr.address = (long)get_cpu_gdt_rw(cpu);
	gdt_descr.size = GDT_SIZE - 1;
	load_gdt(&gdt_descr);
}
```
The gdt_descr variable represents pointer to the GDT descriptor here (we already saw definition of a desc_ptr structure in the Early interrupt 
and exception handling part). We get the address and the size of the GDT descriptor for the CPU with the given id. 
The GDT_SIZE is 256 or:
arch/x86/include/asm/segment.h
```
#define GDT_SIZE			(GDT_ENTRIES*8)
```

and the address of the descriptor we will get with the get_cpu_gdt_rw:
```
static inline struct desc_struct *get_cpu_gdt_rw(unsigned int cpu)
{
	return per_cpu(gdt_page, cpu).gdt;
}
```
The get_cpu_gdt_rw uses per_cpu macro for getting value of a gdt_page percpu variable for the given CPU number
(bootstrap processor with id - 0 in our case).

You may ask the following question: so, if we can access gdt_page percpu variable, where it was defined?
Actually we already saw it in this book. If you have read the first part of this chapter(https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-1.html), 
you can remember that we saw definition of the gdt_page in the arch/x86/kernel/head_64.S:
```
early_gdt_descr:
	.word	GDT_ENTRIES*8-1
early_gdt_descr_base:
	.quad	INIT_PER_CPU_VAR(gdt_page)
```
it locates after the __per_cpu_load symbol:
arch/x86/kernel/vmlinux.lds.S
```
#define INIT_PER_CPU(x) init_per_cpu__##x = x + __per_cpu_load
INIT_PER_CPU(gdt_page);
INIT_PER_CPU(irq_stack_union);
```
and filled gdt_page in the arch/x86/kernel/cpu/common.c:
```
DEFINE_PER_CPU_PAGE_ALIGNED(struct gdt_page, gdt_page) = { .gdt = {
#ifdef CONFIG_X86_64
	[GDT_ENTRY_KERNEL32_CS]		= GDT_ENTRY_INIT(0xc09b, 0, 0xfffff),
	[GDT_ENTRY_KERNEL_CS]		= GDT_ENTRY_INIT(0xa09b, 0, 0xfffff),
	[GDT_ENTRY_KERNEL_DS]		= GDT_ENTRY_INIT(0xc093, 0, 0xfffff),
	[GDT_ENTRY_DEFAULT_USER32_CS]	= GDT_ENTRY_INIT(0xc0fb, 0, 0xfffff),
	[GDT_ENTRY_DEFAULT_USER_DS]	= GDT_ENTRY_INIT(0xc0f3, 0, 0xfffff),
	[GDT_ENTRY_DEFAULT_USER_CS]	= GDT_ENTRY_INIT(0xa0fb, 0, 0xfffff),
#else
  ...
#endif
} };
EXPORT_PER_CPU_SYMBOL_GPL(gdt_page);
```
more about percpu variables you can read in the Per-CPU variables part. 
As we got address and size of the GDT descriptor we reload GDT with the load_gdt which just execute lgdt instruct and 
load percpu_segment with the following function:
arch/x86/kernel/cpu/common.c
```
void load_percpu_segment(int cpu)
{
#ifdef CONFIG_X86_32
	loadsegment(fs, __KERNEL_PERCPU);
#else
	__loadsegment_simple(gs, 0);
	wrmsrl(MSR_GS_BASE, (unsigned long)per_cpu(irq_stack_union.gs_base, cpu));
#endif
	load_stack_canary_segment();
}
```
The base address of the percpu area must contain gs register (or fs register for x86), so we are using loadsegment macro and pass gs.
In the next step we writes the base address if the IRQ stack and setup stack canary (this is only for x86_32).
canary https://en.wikipedia.org/wiki/Buffer_overflow_protection
Buffer overflow protection is any of various techniques used during software development to enhance the security 
of executable programs by detecting buffer overflows on stack-allocated variables, and preventing them
from causing program misbehavior or from becoming serious security vulnerabilities.

After we load new GDT, we fill cpu_callout_mask bitmap with the current cpu and set cpu state as online with 
the setting cpu_state percpu variable for the current processor - CPU_ONLINE:
arch/x86/kernel/smpboot.c
```
void __init native_smp_prepare_boot_cpu(void)
{
	int me = smp_processor_id();
	switch_to_new_gdt(me);
	/* already set me in cpu_online_mask in boot_cpu_init() */
	cpumask_set_cpu(me, cpu_callout_mask);
	cpu_set_state_online(me);
}
```
kernel/smpboot.c
```
void cpu_set_state_online(int cpu)
{
	(void)atomic_xchg(&per_cpu(cpu_hotplug_state, cpu), CPU_ONLINE);
}
```

So, what is cpu_callout_mask bitmap... As we initialized bootstrap processor (processor which is booted the first on x86) 
the other processors in a multiprocessor system are known as secondary processors. Linux kernel uses following two bitmasks:
arch/x86/include/asm/cpumask.h
```
extern cpumask_var_t cpu_callin_mask;
extern cpumask_var_t cpu_callout_mask;
```

After bootstrap processor initialized, it updates the cpu_callout_mask to indicate which secondary processor can be initialized next. 
All other or secondary processors can do some initialization stuff before and check the cpu_callout_mask on the boostrap processor bit.
Only after the bootstrap processor filled the cpu_callout_mask with this secondary processor, 
it will continue the rest of its initialization. After that the certain processor finish its initialization process,
the processor sets bit in the cpu_callin_mask. Once the bootstrap processor finds the bit in the cpu_callin_mask for the current secondary processor,
this processor repeats the same procedure for initialization of one of the remaining secondary processors. 
In a short words it works as i described, but we will see more details in the chapter about SMP.

That's all. We did all SMP boot preparation.



Build zonelists
In the next step we can see the call of the build_all_zonelists function. This function sets up the order of zones 
that allocations are preferred from. What are zones and what's order we will understand soon. 
For the start let's see how linux kernel considers physical memory. Physical memory is split into banks which are called - nodes.
If you has no hardware support for NUMA, you will see only one node:
```
$ cat /sys/devices/system/node/node0/numastat 
numa_hit 72452442
numa_miss 0
numa_foreign 0
interleave_hit 12925
local_node 72452442
other_node 0
```
todo  测试

Every node is presented by the struct pglist_data in the linux kernel. Each node is divided into a number of special blocks 
which are called - zones. Every zone is presented by the zone struct in the linux kernel and has one of the type:
1 ZONE_DMA - 0-16M;
2 ZONE_DMA32 - used for 32 bit devices that can only do DMA areas below 4G;
3 ZONE_NORMAL - all RAM from the 4GB on the x86_64;
4 ZONE_HIGHMEM - absent on the x86_64;
5 ZONE_MOVABLE - zone which contains movable pages.
which are presented by the zone_type enum. We can get information about zones with the:
```
$ cat /proc/zoneinfo
Node 0, zone      DMA
  pages free     3975
        min      3
        low      3
        ...
Node 0, zone    DMA32
  pages free     694163
        min      875
        low      1093
        ...
Node 0, zone   Normal
  pages free     2529995
        min      3146
        low      3932
        ...
```
todo 测试


As I wrote above all nodes are described with the pglist_data or pg_data_t structure in memory. 
This structure is defined in the include/linux/mmzone.h. 
```
typedef struct pglist_data {
	struct zone node_zones[MAX_NR_ZONES];
	struct zonelist node_zonelists[MAX_ZONELISTS];
	int nr_zones;
...
} pg_data_t;	


struct zone {
	unsigned long watermark[NR_WMARK];

	unsigned long nr_reserved_highatomic;

	long lowmem_reserve[MAX_NR_ZONES];
	...
	const char		*name;
	...
	struct free_area	free_area[MAX_ORDER];
	unsigned long		flags;
...
    bool			contiguous;
	ZONE_PADDING(_pad3_)
	/* Zone statistics */
	atomic_long_t		vm_stat[NR_VM_ZONE_STAT_ITEMS];
	atomic_long_t		vm_numa_stat[NR_VM_NUMA_STAT_ITEMS];
}	
```

The build_all_zonelists function from the mm/page_alloc.c constructs an ordered zonelist (of different zones DMA, 
DMA32, NORMAL, HIGH_MEMORY, MOVABLE) which specifies the zones/nodes to visit when a selected zone or node cannot 
satisfy the allocation request.
init/main.c
```
...
smp_prepare_boot_cpu();	/* arch-specific boot-cpu hooks */
build_all_zonelists(NULL);
```
mm/page_alloc.c
```
void __ref build_all_zonelists(pg_data_t *pgdat)
{
	if (system_state == SYSTEM_BOOTING) {
		build_all_zonelists_init();
	} else {
		__build_all_zonelists(pgdat);
	}
	vm_total_pages = nr_free_pagecache_pages();
	
	if (vm_total_pages < (pageblock_nr_pages * MIGRATE_TYPES))
		page_group_by_mobility_disabled = 1;
	else
		page_group_by_mobility_disabled = 0;

	pr_info("Built %i zonelists, m
	
	
	
	obility grouping %s.  Total pages: %ld\n",
		nr_online_nodes,
		page_group_by_mobility_disabled ? "off" : "on",
		vm_total_pages);
#ifdef CONFIG_NUMA
	pr_info("Policy zone: %s\n", zone_names[policy_zone]);
#endif
}
```

That's all. More about NUMA and multiprocessor systems will be in the special part.  todo 章节在哪



The rest of the stuff before scheduler initialization
Before we will start to dive into linux kernel scheduler initialization process we must do a couple of things. 
The first thing is the page_alloc_init
init/main.c
```
build_all_zonelists(NULL);
page_alloc_init();
```
mm/page_alloc.c
```
void __init page_alloc_init(void)
{
	int ret;

	ret = cpuhp_setup_state_nocalls(CPUHP_PAGE_ALLOC_DEAD,
					"mm/page_alloc:dead", NULL,
					page_alloc_cpu_dead);
	WARN_ON(ret < 0);
}
```
include/linux/cpuhotplug.h
```
static inline int cpuhp_setup_state_nocalls(enum cpuhp_state state,
					    const char *name,
					    int (*startup)(unsigned int cpu),
					    int (*teardown)(unsigned int cpu))
{
	return __cpuhp_setup_state(state, name, false, startup, teardown,
				   false);
}
```
kernel/cpu.c
```
int __cpuhp_setup_state(enum cpuhp_state state,
			const char *name, bool invoke,
			int (*startup)(unsigned int cpu),
			int (*teardown)(unsigned int cpu),
			bool multi_instance)
{
	int ret;

	cpus_read_lock();
	ret = __cpuhp_setup_state_cpuslocked(state, name, invoke, startup,
					     teardown, multi_instance);
	cpus_read_unlock();
	return ret;
}
EXPORT_SYMBOL(__cpuhp_setup_state);

int __cpuhp_setup_state_cpuslocked(enum cpuhp_state state,
				   const char *name, bool invoke,
				   int (*startup)(unsigned int cpu),
				   int (*teardown)(unsigned int cpu),
				   bool multi_instance)
{
	int cpu, ret = 0;
	bool dynstate;

	lockdep_assert_cpus_held();

	if (cpuhp_cb_check(state) || !name)
		return -EINVAL;

	mutex_lock(&cpuhp_state_mutex);

	ret = cpuhp_store_callbacks(state, name, startup, teardown,
				    multi_instance);

	dynstate = state == CPUHP_AP_ONLINE_DYN;
	if (ret > 0 && dynstate) {
		state = ret;
		ret = 0;
	}

	if (ret || !invoke || !startup)
		goto out;

	for_each_present_cpu(cpu) {
		struct cpuhp_cpu_state *st = per_cpu_ptr(&cpuhp_state, cpu);
		int cpustate = st->state;

		if (cpustate < state)
			continue;

		ret = cpuhp_issue_call(cpu, state, true, NULL);
		if (ret) {
			if (teardown)
				cpuhp_rollback_install(cpu, state, NULL);
			cpuhp_store_callbacks(state, NULL, NULL, NULL, false);
			goto out;
		}
	}
....
	return ret;
}


static int cpuhp_store_callbacks(enum cpuhp_state state, const char *name,
				 int (*startup)(unsigned int cpu),
				 int (*teardown)(unsigned int cpu),
				 bool multi_instance)
{
	/* (Un)Install the callbacks for further cpu hotplug operations */
	struct cpuhp_step *sp;
	int ret = 0;

	if (name && (state == CPUHP_AP_ONLINE_DYN ||
		     state == CPUHP_BP_PREPARE_DYN)) {
		ret = cpuhp_reserve_state(state);
		if (ret < 0)
			return ret;
		state = ret;
	}
	sp = cpuhp_get_step(state);
	if (name && sp->name)
		return -EBUSY;

	sp->startup.single = startup;
	sp->teardown.single = teardown;
	sp->name = name;
	sp->multi_instance = multi_instance;
	INIT_HLIST_HEAD(&sp->list);
	return ret;
}
```
It setups setup the startup and teardown callbacks (second and third parameters) for the CPUHP_PAGE_ALLOC_DEAD cpu hotplug state.
Of course the implementation of this function depends on the CONFIG_HOTPLUG_CPU kernel configuration option 
and if this option is set, such callbacks will be set for all cpu(s) in the system depends on their hotplug states. 
hotplug mechanism is a big theme and it will not be described in this book.

hotplug https://www.kernel.org/doc/Documentation/usb/hotplug.txt
In hotpluggable busses like USB (and Cardbus PCI), end-users plug devices
into the bus with power on.  In most cases, users expect the devices to become
immediately usable.  That means the system must do many things, including:
1 Find a driver that can handle the device.  That may involve
      loading a kernel module; newer drivers can use module-init-tools
      to publish their device (and class) support to user utilities.
2  Bind a driver to that device.  Bus frameworks do that using a
      device driver's probe() routine.
3 Tell other subsystems to configure the new device.  Print
      queues may need to be enabled, networks brought up, disk
      partitions mounted, and so on.  In some cases these will
      be driver-specific actions.

After this function we can see the kernel command line in the initialization output:
```
linux version 4.1.0-rc2+ (alex@localhost)(gcc version 4.9.2(Ubuntu 4.9.2-10ubuntu13)) #493 SMP Thu
Command line: root=/dev/sdb earlyprintk=ttys0,115200 loglevel=7 debug rdinit=/sbin/init root=/dev/ram
```
And a couple of functions such as parse_early_param and parse_args which handles linux kernel command line. 
You may remember that we already saw the call of the parse_early_param function in the sixth part of
the kernel initialization chapter, so why we call it again? Answer is simple: we call this function
in the architecture-specific code (x86_64 in our case), but not all architecture calls this function. 
And we need to call the second function parse_args to parse and handle non-early command line arguments.


In the next step we can see the call of the jump_label_init from the kernel/jump_label.c. and initializes jump label.
Jump label  https://lwn.net/Articles/412072/
The kernel is filled with tests whose results almost never change. A classic example is tracepoints,
which will be disabled on running systems with only very rare exceptions. There has long been interest 
in optimizing the tests done in such places; with 2.6.37, the "jump label" feature will make those tests go away entirely.

init/main.c
```
    parse_early_param();
	after_dashes = parse_args("Booting kernel",
				  static_command_line, __start___param,
				  __stop___param - __start___param,
				  -1, -1, NULL, &unknown_bootoption);
	if (!IS_ERR_OR_NULL(after_dashes))
		parse_args("Setting init args", after_dashes, NULL, 0, -1, -1,
			   NULL, set_init_arg);

	jump_label_init();
```
kernel/jump_label.c
```
void __init jump_label_init(void)
{
	struct jump_entry *iter_start = __start___jump_table;
	struct jump_entry *iter_stop = __stop___jump_table;
	struct static_key *key = NULL;
	struct jump_entry *iter;
    ...
	if (static_key_initialized)
		return;

	cpus_read_lock();
	jump_label_lock();
	jump_label_sort_entries(iter_start, iter_stop);

	for (iter = iter_start; iter < iter_stop; iter++) {
		struct static_key *iterk;

		/* rewrite NOPs */
		if (jump_label_type(iter) == JUMP_LABEL_NOP)
			arch_jump_label_transform_static(iter, JUMP_LABEL_NOP);

		iterk = jump_entry_key(iter);
		if (iterk == key)
			continue;

		key = iterk;
		static_key_set_entries(key, iter);
	}
	static_key_initialized = true;
	jump_label_unlock();
	cpus_read_unlock();
}
```

After this we can see the call of the setup_log_buf function which setups the printk log buffer. 
We already saw this function in the seventh part of the linux kernel initialization process chapter



PID hash initialization
The next is pidhash_init function. As you know each process has assigned a unique number which called
 process identification number or PID. Each process generated with fork or clone is automatically assigned 
 a new unique PID value by the kernel. 
The management of PIDs centered around the two special data structures: struct pid and struct upid.
include/linux/pid.h
```
struct upid {
	int nr;
	struct pid_namespace *ns;
};

struct pid
{
	atomic_t count;
	unsigned int level;
	/* lists of tasks that use this pid */
	struct hlist_head tasks[PIDTYPE_MAX];
	struct rcu_head rcu;
	struct upid numbers[1];
};
```
include/linux/pid_namespace.h
```
struct pid_namespace {
	struct kref kref;
	struct idr idr;
	struct rcu_head rcu;
	unsigned int pid_allocated;
	struct task_struct *child_reaper;
	struct kmem_cache *pid_cachep;
	unsigned int level;
...
	struct user_namespace *user_ns;
	struct ucounts *ucounts;
	struct work_struct proc_work;
	kgid_t pid_gid;
	int hide_pid;
	int reboot;	/* group exit code if this pidns was rebooted */
	struct ns_common ns;
} __randomize_layout;
```
include/linux/idr.h
```
struct idr {
	struct radix_tree_root	idr_rt;
	unsigned int		idr_base;
	unsigned int		idr_next;
};
```

First structure represents information about a PID in the kernel. The second structure represents the information 
that is visible in a specific namespace. All PID instances stored in the special hash table:  4.16中【 hlist_head *pid_hash】没有了
init/main.c
```
pid_idr_init();
```
kernel/pid.c
```
struct pid_namespace init_pid_ns = {
	.kref = KREF_INIT(2),
	.idr = IDR_INIT,
	.pid_allocated = PIDNS_ADDING,
	.level = 0,
	.child_reaper = &init_task,
	.user_ns = &init_user_ns,
	.ns.inum = PROC_PID_INIT_INO,
#ifdef CONFIG_PID_NS
	.ns.ops = &pidns_operations,
#endif
};


void __init pid_idr_init(void)
{
	/* Verify no one has done anything silly: */
	BUILD_BUG_ON(PID_MAX_LIMIT >= PIDNS_ADDING);

	/* bump default and minimum pid_max based on number of cpus */
	pid_max = min(pid_max_max, max_t(int, pid_max,
				PIDS_PER_CPU_DEFAULT * num_possible_cpus()));
	pid_max_min = max_t(int, pid_max_min,
				PIDS_PER_CPU_MIN * num_possible_cpus());
	pr_info("pid_max: default: %u minimum: %u\n", pid_max, pid_max_min);

	idr_init(&init_pid_ns.idr);

	init_pid_ns.pid_cachep = KMEM_CACHE(pid,
			SLAB_HWCACHE_ALIGN | SLAB_PANIC | SLAB_ACCOUNT);
}
```
include/linux/idr.h
```
#define IDR_INIT_BASE(base) {						\
	.idr_rt = RADIX_TREE_INIT(IDR_RT_MARKER),			\
	.idr_base = (base),						\
	.idr_next = 0,							\
}

#define IDR_INIT	IDR_INIT_BASE(0)


static inline void idr_init(struct idr *idr)
{
	idr_init_base(idr, 0);
}

static inline void idr_init_base(struct idr *idr, int base)
{
	INIT_RADIX_TREE(&idr->idr_rt, IDR_RT_MARKER);
	idr->idr_base = base;
	idr->idr_next = 0;
}
```
idr用来管理id  id register  例如根据p_id生成idr
```
/**
 * ida_get_new - allocate new ID
 * @ida:	idr handle
 * @p_id:	pointer to the allocated handle
 */
static inline int ida_get_new(struct ida *ida, int *p_id)
{
	return ida_get_new_above(ida, 0, p_id);
}
```



The rest of the stuff before scheduler initialization is the following functions: vfs_caches_init_early 
does early initialization of the virtual file system (more about it will be in the chapter which will describe virtual file system),
```
vfs_caches_init_early();
```
fs/dcache.c
```
void __init vfs_caches_init_early(void)
{
	int i;

	for (i = 0; i < ARRAY_SIZE(in_lookup_hashtable); i++)
		INIT_HLIST_BL_HEAD(&in_lookup_hashtable[i]);

	dcache_init_early();
	inode_init_early();
}
```
virtual file system  https://en.wikipedia.org/wiki/Virtual_file_system   todo 有更多章节吗
A virtual file system (VFS) or virtual filesystem switch is an abstract layer on top of a more concrete file system. 
The purpose of a VFS is to allow client applications to access different types of concrete file systems in a uniform way. 
A VFS can, for example, be used to access local and network storage devices transparently without the client application noticing the difference. 
It can be used to bridge the differences in Windows, classic Mac OS/macOS and Unix filesystems, 
so that applications can access files on local file systems of those types without having to know what type of file system they are accessing.

sort_main_extable sorts the kernel's built-in exception table entries which are between __start___ex_table and __stop___ex_table
kernel/extable.c
```
void __init sort_main_extable(void)
{
	if (main_extable_sort_needed && __stop___ex_table > __start___ex_table) {
		pr_notice("Sorting __ex_table...\n");
		sort_extable(__start___ex_table, __stop___ex_table);
	}
}
```
trap_init initializes trap handlers (more about last two function we will know in the separate chapter about interrupts).


The last step before the scheduler initialization is initialization of the memory manager with the mm_init function
 As we can see, the mm_init function initializes different parts of the linux kernel memory manager:
init/main.c
```
static void __init mm_init(void)
{
	page_ext_init_flatmem();
	mem_init();
	kmem_cache_init();
	pgtable_init();
	vmalloc_init();
	ioremap_huge_init();
	/* Should be run before the first non-init thread is created */
	init_espfix_bsp();
	/* Should be run after espfix64 is set up. */
	pti_init();
}
```
The first is page_ext_init_flatmem which depends on the CONFIG_SPARSEMEM kernel configuration option 
and initializes extended data per page handling.
The mem_init releases all bootmem, 
the kmem_cache_init initializes kernel cache, 
the percpu_init_late - replaces percpu chunks with those allocated by slub, 
the pgtable_init - initializes the page->ptl kernel cache, 
the vmalloc_init - initializes vmalloc. 
Please, NOTE that we will not dive into details about all of these functions and concepts, but we will see all of they
it in the Linux kernel memory manager chapter.




Scheduler initialization
you will not see the full explanation of the scheduler here, there will be special separate chapter about this. 
Here will be described first initial scheduler mechanisms which are initialized first of all.
init/main.c
```
sched_init();
```
kernel/sched/core.c 
```
void __init sched_init(void)
{
...
sched_clock_init();
wait_bit_init();
...
}
```
kernel/sched/clock.c
```
void sched_clock_init(void)
{
	sched_clock_running = 1;
}
```
kernel/sched/wait_bit.c
```
void __init wait_bit_init(void)
{
	int i;

	for (i = 0; i < WAIT_TABLE_SIZE; i++)
		init_waitqueue_head(bit_wait_table + i);
}
```
where bit_wait_table is defined as:
```
#define WAIT_TABLE_BITS 8
#define WAIT_TABLE_SIZE (1 << WAIT_TABLE_BITS)

static wait_queue_head_t bit_wait_table[WAIT_TABLE_SIZE] __cacheline_aligned;
```
The bit_wait_table is array of wait queues that will be used for wait/wake up of processes depends on the value of a designated bit.
The next step after initialization of waitqueues array is calculating size of memory to allocate for the root_task_group.
As we may see this size depends on two following kernel configuration options:
kernel/sched/core.c
```
...
#ifdef CONFIG_FAIR_GROUP_SCHED
	alloc_size += 2 * nr_cpu_ids * sizeof(void **);
#endif
#ifdef CONFIG_RT_GROUP_SCHED
	alloc_size += 2 * nr_cpu_ids * sizeof(void **);
#endif
```

Both of these options provide two different planning models. As we can read from the documentation,
https://www.kernel.org/doc/Documentation/scheduler/sched-design-CFS.txt
the current scheduler - CFS or Completely Fair Scheduler use a simple concept. It models process scheduling 
as if the system has an ideal multitasking processor where each process would receive 1/n processor time,
where n is the number of the runnable processes. The scheduler uses the special set of rules. 
These rules determine when and how to select a new process to run and they are called scheduling policy.

The Completely Fair Scheduler supports following normal or in other words non-real-time scheduling policies:
SCHED_NORMAL;
SCHED_BATCH;
SCHED_IDLE.

The SCHED_NORMAL is used for the most normal applications, the amount of cpu each process consumes is mostly determined 
by the nice value, 
the SCHED_BATCH used for the 100% non-interactive tasks 
and the SCHED_IDLE runs tasks only when the processor has no task to run besides this task.

NICE https://en.wikipedia.org/wiki/Nice_%28Unix%29
nice is a program found on Unix and Unix-like operating systems such as Linux. It directly maps to a kernel call of the same name. 
nice is used to invoke a utility or shell script with a particular CPU priority, thus giving the process more 
or less CPU time than other processes. 


The real-time policies are also supported for the time-critical applications: SCHED_FIFO and SCHED_RR. 
If you've read something about the Linux kernel scheduler, you can know that it is modular.
That means it supports different algorithms to schedule different types of processes. 
Usually this modularity is called scheduler classes. These modules encapsulate scheduling policy details 
and are handled by the scheduler core without knowing too much about them.

Now let's get back to the our code and look on the two configuration options: CONFIG_FAIR_GROUP_SCHED and CONFIG_RT_GROUP_SCHED.
The smallest unit that the scheduler works with is an individual task or thread. However, a process is 
not the only type of entity that the scheduler can operate with. Both of these options provide support for group scheduling. 
The first option provides support for group scheduling with the completely fair scheduler policies and the second 
with the real-time policies respectively.

In simple words, group scheduling is a feature that allows us to schedule a set of tasks as if they were a single task. 
For example, if you create a group with two tasks on the group, then this group is just like one normal task, 
from the kernel perspective. After a group is scheduled, the scheduler will pick a task from this group 
and it will be scheduled inside the group. So, such mechanism allows us to build hierarchies and manage their resources. 
Although a minimal unit of scheduling is a process, the Linux kernel scheduler does not use task_struct structure under the hood.
There is special sched_entity structure that is used by the Linux kernel scheduler as scheduling unit.
hierarchies /ˈhaɪərɑːkiz/  //层次结构


So, the current goal is to calculate a space to allocate for a sched_entity(ies) of the root task group and we do it two times with:
kernel/sched/core.c
```
void __init sched_init(void)
{
..
#ifdef CONFIG_FAIR_GROUP_SCHED
	alloc_size += 2 * nr_cpu_ids * sizeof(void **);
#endif
#ifdef CONFIG_RT_GROUP_SCHED
	alloc_size += 2 * nr_cpu_ids * sizeof(void **);
#endif
	if (alloc_size) {
		ptr = (unsigned long)kzalloc(alloc_size, GFP_NOWAIT);
..		
}
```
The first is for case when scheduling of task groups is enabled with completely fair scheduler and 
the second is for the same purpose by in a case of real-time scheduler. So here we calculate size which is 
equal to size of a pointer multiplied on amount of CPUs in the system and multiplied to 2. 
We need to multiply this on 2 as we will need to allocate a space for two things:
1 scheduler entity structure;
2 runqueue.

After we have calculated size, we allocate a space with the kzalloc function and set pointers of sched_entity and runquques there:
kernel/sched/core.c
```
#ifdef CONFIG_FAIR_GROUP_SCHED
		root_task_group.se = (struct sched_entity **)ptr;
		ptr += nr_cpu_ids * sizeof(void **);

		root_task_group.cfs_rq = (struct cfs_rq **)ptr;
		ptr += nr_cpu_ids * sizeof(void **);

#endif /* CONFIG_FAIR_GROUP_SCHED */
#ifdef CONFIG_RT_GROUP_SCHED
		root_task_group.rt_se = (struct sched_rt_entity **)ptr;
		ptr += nr_cpu_ids * sizeof(void **);

		root_task_group.rt_rq = (struct rt_rq **)ptr;
		ptr += nr_cpu_ids * sizeof(void **);

#endif /* CONFIG_RT_GROUP_SCHED */
```
As I already mentioned, the Linux group scheduling mechanism allows to specify a hierarchy. 
The root of such hierarchies is the root_runqueuetask_group task group structure. This structure contains many fields, 
but we are interested in se, rt_se, cfs_rq and rt_rq for this moment:
The first two are instances of sched_entity structure.
it used by the scheduler as a unit of scheduling.
kernel/sched/sched.h
```
struct task_group {
...
struct sched_entity **se;
	/* runqueue "owned" by this group on each cpu */
struct cfs_rq **cfs_rq;
...	
struct sched_rt_entity **rt_se;
struct rt_rq **rt_rq;
...	
}
```
include/linux/sched.h
```
struct sched_entity {
	/* For load-balancing: */
	struct load_weight		load;
	unsigned long			runnable_weight;
	struct rb_node			run_node;
	struct list_head		group_node;
	unsigned int			on_rq;

	u64				exec_start;
	u64				sum_exec_runtime;
	u64				vruntime;
	u64				prev_sum_exec_runtime;

	u64				nr_migrations;

	struct sched_statistics		statistics;

#ifdef CONFIG_FAIR_GROUP_SCHED
	int				depth;
	struct sched_entity		*parent;
	/* rq on which this entity is (to be) queued: */
	struct cfs_rq			*cfs_rq;
	/* rq "owned" by this entity/group: */
	struct cfs_rq			*my_q;
#endif
...
};
```
The cfs_rq and rt_rq present run queues. A run queue is a special per-cpu structure that is used by the Linux kernel schedu
ler to store active threads or in other words set of threads which potentially will be picked up by the scheduler to run.

The space is allocated and the next step is to initialize a bandwidth of CPU for real-time and deadline tasks:
kernel/sched/core.c
```
..
	init_rt_bandwidth(&def_rt_bandwidth, global_rt_period(), global_rt_runtime());
	init_dl_bandwidth(&def_dl_bandwidth, global_rt_period(), global_rt_runtime());
..	
```
All groups have to be able to rely on the amount of CPU time. The two following structures: def_rt_bandwidth and def_dl_bandwidth 
represent default values of bandwidths for real-time and deadline tasks. We will not look at definition of these structures 
as it is not so important for now, but we are interested in two following values:
sched_rt_period_us;
sched_rt_runtime_us.
The first represents a period and the second represents quantum that is allocated for real-time tasks during sched_rt_period_us. 
You may see global values of these parameters in the:  todo 测试
```
$ cat /proc/sys/kernel/sched_rt_period_us 
1000000

$ cat /proc/sys/kernel/sched_rt_runtime_us 
950000
```
The values related to a group can be configured in <cgroup>/cpu.rt_period_us and <cgroup>/cpu.rt_runtime_us. 
Due no one filesystem is not mounted yet, the def_rt_bandwidth and the def_dl_bandwidth will be initialzed 
with default values which will be retuned by the global_rt_period and global_rt_runtime functions.


That's all with the bandwiths of real-time and deadline tasks and in the next step, depends on enable of SMP, 
we make initialization of the root domain:
```
#ifdef CONFIG_SMP
	init_defrootdomain();
#endif
```
https://en.wikipedia.org/wiki/Symmetric_multiprocessing
Symmetric multiprocessing or shared-memory multiprocessing(SMP) involves a multiprocessor computer hardware 
and software architecture where two or more identical processors are connected to a single, shared main memory,
have full access to all input and output devices, and are controlled by a single operating system instance 
that treats all processors equally, reserving none for special purposes

The real-time scheduler requires global resources to make scheduling decision. But unfortunately scalability bottlenecks 
appear as the number of CPUs increase. The concept of root domains was introduced for improving scalability and
avoid such bottlenecks. Instead of bypassing over all run queues, the scheduler gets information about a CPU where/from 
to push/pull a real-time task from the root_domain structure. This structure is 
just keeps track of CPUs that can be used to push or pull a process.
bottlenecks  /ˈbɒtlnɛks/ (尤指工商业发展的)瓶颈，阻碍，障碍；瓶颈路段(常引起交通阻塞)

After root domain initialization, we make initialization of the bandwidth for the real-time tasks of the root task group as 
we did the same above with the same default values.:
```
#ifdef CONFIG_RT_GROUP_SCHED
	init_rt_bandwidth(&root_task_group.rt_bandwidth,
			global_rt_period(), global_rt_runtime());
#endif /* CONFIG_RT_GROUP_SCHED */
```

In the next step, depends on the CONFIG_CGROUP_SCHED kernel configuration option we allocate slab cache for task_group(s)
and initialize the siblings and children lists of the root task group. As we can read from the documentation, 
the CONFIG_CGROUP_SCHED is:  
https://github.com/torvalds/linux/blob/v2.6.26/init/Kconfig    4.16的注释变了
```
This option allows you to create arbitrary task groups using the "cgroup" pseudo
filesystem and control the cpu bandwidth allocated to each such task group.
```

As we finished with the lists initialization, we can see the call of the autogroup_init function:
```
#ifdef CONFIG_CGROUP_SCHED
	task_group_cache = KMEM_CACHE(task_group, 0);

	list_add(&root_task_group.list, &task_groups);
	INIT_LIST_HEAD(&root_task_group.children);
	INIT_LIST_HEAD(&root_task_group.siblings);
	autogroup_init(&init_task);
#endif /* CONFIG_CGROUP_SCHED */
```

which initializes automatic process group scheduling. The autogroup feature is about automatic creation and 
population of a new task group during creation of a new session via setsid call.
https://linux.die.net/man/2/setsid
setsid() creates a new session if the calling process is not a process group leader. The calling process is the leader of the new session, 
the process group leader of the new process group, and has no controlling terminal.

After this we are going through the all possible CPUs (you can remember that possible CPUs are stored in 
the cpu_possible_mask bitmap that can ever be available in the system) and initialize a runqueue for each possible cpu:
```
for_each_possible_cpu(i) {
		struct rq *rq;
		...
}
```
kernel/sched/sched.h
```
struct rq {
  unsigned int nr_running;
  ..
  unsigned int nr_numa_running;
  ..
  struct rt_rq rt;
  struct dl_rq dl;
..	
}
```

As I already mentioned this above, a run queue is a fundamental data structure in a scheduling process.
The scheduler uses it to determine who will be runned next. As you may see, this structure has many different fields and
we will not cover all of them here, but we will look on them when they will be directly used.


After initialization of per-cpu run queues with default values, we need to setup load weight of the first task in the system:
kernel/sched/core.c
```
set_load_weight(&init_task, false);
```
First of all let's try to understand what is it load weight of a process. If you will look at the definition 
of the sched_entity structure, you will see that it starts from the load field:
include/linux/sched.h
```
struct sched_entity {
	/* For load-balancing: */
	struct load_weight		load;
	...
```
represented by the load_weight structure which just contains two fields that represent actual load weight of a scheduler entity 
and its invariant value:
include/linux/sched.h
```
struct load_weight {
	unsigned long			weight;
	u32				inv_weight;
};
```
You already may know that each process in the system has priority. The higher priority allows to get more time to run.
A load weight of a process is a relation between priority of this process and timeslices of this process. 
Each process has three following fields related to priority:
include/linux/sched.h
```
struct task_struct {
...
    int                prio;
    int                static_prio;
    int                normal_prio;
...
}
```
The first one is dynamic priority which can't be changed during lifetime of a process based on its static priority 
  and interactivity of the process. 
The static_prio contains initial priority most likely well-known to you nice value. This value does not changed by the kernel 
  if a user will not change it. 
The last one is normal_priority based on the value of the static_prio too, but also it depends on 
  the scheduling policy of a process.

So the main goal of the set_load_weight function is to initialze load_weight fields for the init task:
kernel/sched/core.c
```
static void set_load_weight(struct task_struct *p, bool update_load)
{
	int prio = p->static_prio - MAX_RT_PRIO;
	struct load_weight *load = &p->se.load;

	if (idle_policy(p->policy)) {
		load->weight = scale_load(WEIGHT_IDLEPRIO);
		load->inv_weight = WMULT_IDLEPRIO;
		return;
	}

	if (update_load && p->sched_class == &fair_sched_class) {
		reweight_task(p, prio);
	} else {
		load->weight = scale_load(sched_prio_to_weight[prio]);
		load->inv_weight = sched_prio_to_wmult[prio];
	}
}
```
As you may see we calculate initial prio from the initial value of the static_prio of the init task and 
use it as index of sched_prio_to_weight and sched_prio_to_wmult arrays to set weight and inv_weight values. 
These two arrays contain a load weight depends on priority value. In a case of when a process is idle process,
we set minimal load weight.

For this moment we came to the end of initialization process of the Linux kernel scheduler. The last steps are: 
to make current process (it will be the first init process) idle that will be runned when a cpu has no other process to run. 
Calculating next time period of the next calculation of CPU load and initialization of the fair class:
kernel/sched/core.c
```
..
init_sched_fair_class();
..
```
kernel/sched/fair.c
```
__init void init_sched_fair_class(void)
{
#ifdef CONFIG_SMP
	open_softirq(SCHED_SOFTIRQ, run_rebalance_domains);
...
}
```
Here we register a soft irq that will call the run_rebalance_domains handler. After the SCHED_SOFTIRQ will be triggered, 
the run_rebalance will be called to rebalance a run queue on the current CPU.

The last two steps of the sched_init function is to initialization of scheduler statistics and setting scheeduler_running variable:
kernel/sched/core.c
```
scheduler_running = 1;
```

That's all. Linux kernel scheduler is initialized. Of course, we have skipped many different details and explanations here,
because we need to know and understand how different concepts (like process and process groups, runqueue, rcu, etc.) works 
in the linux kernel , but we took a short look on the scheduler initialization process.