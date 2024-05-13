https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-9.html

In this part we will continue to dive to the linux kernel initialization process and the main purpose of this part 
will be to learn about initialization of the RCU.
https://en.wikipedia.org/wiki/Read-copy-update
In computer science, read-copy-update (RCU) is a synchronization mechanism that avoids the use of lock primitives 
while multiple threads concurrently read and update elements that are linked through pointers and 
that belong to shared data structures (e.g., linked lists, trees, hash tables).


We can see that the next step in the init/main.c after the sched_init is the call of the preempt_disable. 

```
	sched_init();
	/*
	 * Disable preemption - early bootup scheduling is extremely
	 * fragile until we cpu_idle() for the first time.
	 */
	preempt_disable()
```
preempt 抢占；抢先；先发制人；取代；占先；先占
There are two macros: preempt_disable preempt_enable for preemption disabling and enabling.
First of all let's try to understand what is preempt in the context of an operating system kernel. 
In simple words, preemption is ability of the operating system kernel to preempt current task to run task with higher priority. 
Here we need to disable preemption because we will have only one init process for the early boot time 
and we don't need to stop it before we call cpu_idle function. 
The preempt_disable macro  depends on the CONFIG_PREEMPT_COUNT kernel configuration option. This macro is implemented as:
include/linux/preempt.h
```
#ifdef CONFIG_PREEMPT_COUNT

#define preempt_disable() \
do { \
	preempt_count_inc(); \
	barrier(); \
} while (0)

...
#else /* !CONFIG_PREEMPT_COUNT */
#define preempt_disable()			barrier()
```

Let's look on it. First of all we can see one difference between these macro implementations. 
The preempt_disable with CONFIG_PREEMPT_COUNT set contains the call of the preempt_count_inc. 
There is special percpu variable which stores the number of held locks and preempt_disable calls:
include/linux/preempt.h
```
#define preempt_count_inc() preempt_count_add(1)
```
v4.16的实现变了
kernel/sched/core.c
```
void preempt_count_add(int val)
{
...
	__preempt_count_add(val);
...
	preempt_latency_start(val);
}
```
arch/x86/include/asm/preempt.h
```
static __always_inline void __preempt_count_add(int val)
{
	raw_cpu_add_4(__preempt_count, val);
}
```
arch/x86/include/asm/percpu.h
```
#define raw_cpu_add_4(pcp, val)		percpu_add_op((pcp), val)
```
where preempt_count_add calls the raw_cpu_add_4 macro which adds 1 to the given percpu variable (__preempt_count) in our case 
(more about precpu variables you can read in the part about Per-CPU variables).
Ok, we increased __preempt_count and the next step we can see the call of the barrier macro in the both macros. 
The barrier macro inserts an optimization barrier. In the processors with x86_64 architecture independent memory access operations 
can be performed in any order. 
That's why we need the opportunity to point compiler and processor on compliance of order. This mechanism is memory barrier.
compliance /kəmˈplaɪəns/  遵从；服从；顺从
Let's consider a simple example:
```
preempt_disable();
foo();
preempt_enable();
```
Compiler can rearrange it as:
```
preempt_disable();
preempt_enable();
foo();
```
In this case non-preemptible function foo can be preempted. As we put barrier macro in the preempt_disable and preempt_enable macros,
it prevents the compiler from swapping preempt_count_inc with other statements. More about barriers you can read here and here.
https://www.kernel.org/doc/Documentation/memory-barriers.txt
https://en.wikipedia.org/wiki/Memory_barrier



In the next step we can see following statement:
init/main.c
```
preempt_disable();
	if (WARN(!irqs_disabled(),
		 "Interrupts were enabled *very* early, fixing it\n"))
		local_irq_disable();
```
which check IRQs state, and disabling (with cli instruction for x86_64) if they are enabled.
https://en.wikipedia.org/wiki/Interrupt_request
In a computer, an interrupt request (or IRQ) is a hardware signal sent to the processor that temporarily 
   stops a running program and allows a special program, an interrupt handler, to run instead.
That's all. Preemption is disabled and we can go ahead.


Initialization of the integer ID management
In the next step we can see the call of the idr_init_cache function which defined in the lib/idr.c.
https://github.com/torvalds/linux/blob/16f73eb02d7e1765ccab3d2018e0bd98eb93d973/lib/idr.c
The idr library is used in a various places in the linux kernel to manage assigning integer IDs to objects and 
looking up objects by id.
v4.16没有idr_init_cache函数了,地址中也没有，不看了




RCU initialization
The next step is RCU initialization with the rcu_init function and it's implementation depends on two kernel configuration options:
CONFIG_TINY_RCU
CONFIG_TREE_RCU
In the first case rcu_init will be in the kernel/rcu/tiny.c and in the second case it will be defined in the kernel/rcu/tree.c.
We will see the implementation of the tree rcu, but first of all about the RCU in general.

RCU or read-copy update is a scalable high-performance synchronization mechanism implemented in the Linux kernel. 
On the early stage the linux kernel provided support and environment for the concurrently running applications, 
but all execution was serialized in the kernel using a single global lock. In our days linux kernel has no single global lock,
but provides different mechanisms including lock-free data structures, percpu data structures and other. 
One of these mechanisms is - the read-copy update.
lock-free data structures https://en.wikipedia.org/wiki/Concurrent_data_structure
The RCU technique is designed for rarely-modified data structures. The idea of the RCU is simple. 
For example we have a rarely-modified data structure. If somebody wants to change this data structure, 
we make a copy of this data structure and make all changes in the copy. In the same time all other users of the data structure 
use old version of it. 
Next, we need to choose safe moment when original version of the data structure will have no users and
update it with the modified copy.

Of course this description of the RCU is very simplified. To understand some details about RCU, first of all 
we need to learn some terminology. Data readers in the RCU executed in the critical section.
terminology /ˌtɜːmɪˈnɒlədʒi/ (某学科的)术语；专门用语；有特别含义的用语
critical /ˈkrɪtɪkl/  关键的；批评的；严重的；批判性的；挑剔的；不稳定的；至关紧要的；极重要的；有判断力的；可能有危险的
Every time when data reader get to the critical section, it calls the rcu_read_lock, and rcu_read_unlock on exit 
from the critical section. If the thread is not in the critical section, it will be in state which called - quiescent state.
quiescent /kwiˈesnt/  静态的；静止的；静止状态的；沉寂的
The moment when every thread is in the quiescent state called - grace period. If a thread wants to remove an element 
from the data structure, this occurs in two steps.
grace 优美；宽限期；优雅；风度；高雅；恩宠；文雅；大人，阁下，夫人；体面；（饭前的）谢恩祈祷
First step is removal - atomically removes element from the data structure, but does not release the physical memory. 
   After this thread-writer announces and waits until it is finished. From this moment, the removed element is available 
   to the thread-readers. 
After the grace period finished, the second step of the element removal will be started, it just removes the element 
   from the physical memory.


There a couple of implementations of the RCU. Old RCU called classic, the new implementation called tree RCU. 
As you may already understand, the CONFIG_TREE_RCU kernel configuration option enables tree RCU. 
Another is the tiny RCU which depends on CONFIG_TINY_RCU and CONFIG_SMP=n. We will see more details
about the RCU in general in the separate chapter about synchronization primitives, 
but now let's look on the rcu_init implementation from the kernel/rcu/tree.c:
```
void __init rcu_init(void)
{
	int cpu;
	rcu_early_boot_tests();
	rcu_bootup_announce();
	rcu_init_geometry();
	rcu_init_one(&rcu_bh_state);
	rcu_init_one(&rcu_sched_state);
	if (dump_tree)
		rcu_dump_rcu_node_tree(&rcu_sched_state);
	__rcu_init_preempt();
	open_softirq(RCU_SOFTIRQ, rcu_process_callbacks);

	pm_notifier(rcu_pm_notify, 0);
	for_each_online_cpu(cpu) {
		rcutree_prepare_cpu(cpu);
		rcu_cpu_starting(cpu);
		rcutree_online_cpu(cpu);
	}
}
```
In the beginning of the rcu_init function we define cpu variable and call rcu_bootup_announce. 
The rcu_bootup_announce function is pretty simple:
kernel/rcu/tree_plugin.h
```
static void __init rcu_bootup_announce(void)
{


	pr_info("Preemptible hierarchical RCU implementation.\n");
	rcu_bootup_announce_oddness();
}

static void __init rcu_bootup_announce_oddness(void)
{
	if (IS_ENABLED(CONFIG_RCU_TRACE))
		pr_info("\tRCU event tracing is enabled.\n");
	if ((IS_ENABLED(CONFIG_64BIT) && RCU_FANOUT != 64) ||
	    (!IS_ENABLED(CONFIG_64BIT) && RCU_FANOUT != 32))
		pr_info("\tCONFIG_RCU_FANOUT set to non-default value of %d\n",
		       RCU_FANOUT);
	if (rcu_fanout_exact)
		pr_info("\tHierarchical RCU autobalancing is disabled.\n");
	if (IS_ENABLED(CONFIG_RCU_FAST_NO_HZ))
		pr_info("\tRCU dyntick-idle grace-period acceleration is enabled.\n");
	....
}
```
It just prints information about the RCU with the pr_info function and rcu_bootup_announce_oddness which uses pr_info too, 
for printing different information about the current RCU configuration which depends on different kernel configuration options 
like CONFIG_RCU_TRACE, CONFIG_PROVE_RCU, CONFIG_RCU_FANOUT_EXACT, etc.

In the next step, we can see the call of the rcu_init_geometry function. This function is defined in the same source code file 
and computes the node tree geometry depends on the amount of CPUs. Actually RCU provides scalability 
with extremely low internal RCU lock contention. What if a data structure will be read from the different CPUs? 
RCU API provides the rcu_state structure which presents RCU global state including node hierarchy.
```
static void __init rcu_init_geometry(void)
{
	ulong d;
	int i;
	int rcu_capacity[RCU_NUM_LVLS];
	d = RCU_JIFFIES_TILL_FORCE_QS + nr_cpu_ids / RCU_JIFFIES_FQS_DIV;
	if (jiffies_till_first_fqs == ULONG_MAX)
		jiffies_till_first_fqs = d;
	if (jiffies_till_next_fqs == ULONG_MAX)
		jiffies_till_next_fqs = d;

	if (rcu_fanout_leaf == RCU_FANOUT_LEAF &&
	    nr_cpu_ids == NR_CPUS)
		return;
	pr_info("RCU: Adjusting geometry for rcu_fanout_leaf=%d, nr_cpu_ids=%u\n",
		rcu_fanout_leaf, nr_cpu_ids);
	if (rcu_fanout_leaf < 2 ||
	    rcu_fanout_leaf > sizeof(unsigned long) * 8) {
		rcu_fanout_leaf = RCU_FANOUT_LEAF;
		WARN_ON(1);
		return;
	}
	rcu_capacity[0] = rcu_fanout_leaf;
	for (i = 1; i < RCU_NUM_LVLS; i++)
		rcu_capacity[i] = rcu_capacity[i - 1] * RCU_FANOUT;

	if (nr_cpu_ids > rcu_capacity[RCU_NUM_LVLS - 1]) {
		rcu_fanout_leaf = RCU_FANOUT_LEAF;
		WARN_ON(1);
		return;
	}

	for (i = 0; nr_cpu_ids > rcu_capacity[i]; i++) {
	}
	rcu_num_lvls = i + 1;

	for (i = 0; i < rcu_num_lvls; i++) {
		int cap = rcu_capacity[(rcu_num_lvls - 1) - i];
		num_rcu_lvl[i] = DIV_ROUND_UP(nr_cpu_ids, cap);
	}

	rcu_num_nodes = 0;
	for (i = 0; i < rcu_num_lvls; i++)
		rcu_num_nodes += num_rcu_lvl[i];
}
```
Hierarchy is presented by the:
```
struct rcu_node node[NUM_RCU_NODES];
```