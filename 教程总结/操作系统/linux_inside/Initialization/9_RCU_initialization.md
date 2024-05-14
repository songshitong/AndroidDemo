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
kernel/rcu/tree.h
```
struct rcu_state {
	struct rcu_node node[NUM_RCU_NODES];	/* Hierarchy. */
	...
}
```
array of structures. As we can read in the comment of above definition:
```
/*
 * RCU global state, including node hierarchy.  This hierarchy is
 * represented in "heap" form in a dense array.  The root (first level)
 * of the hierarchy is in ->node[0] (referenced by ->level[0]), the second
 * level in ->node[1] through ->node[m] (->node[1] referenced by ->level[1]),
 * and the third level in ->node[m+1] and following (->node[m+1] referenced
 * by ->level[2]).  The number of levels is determined by the number of
 * CPUs and by CONFIG_RCU_FANOUT.  Small systems will have a "hierarchy"
 * consisting of a single rcu_node.
 */
```
The rcu_node structure  contains information about current grace period, is grace period completed or not,
CPUs or groups that need to switch in order for current grace period to proceed, etc.
kernel/rcu/tree.h
```
struct rcu_node {
raw_spinlock_t __private lock;
unsigned long gpnum;
..
}
```
Every rcu_node contains a lock for a couple of CPUs. These rcu_node structures are embedded into a linear array in the
rcu_state structure and represented as a tree with the root as the first element and covers all CPUs. 
As you can see the number of the rcu nodes determined by the NUM_RCU_NODES which depends on number of available CPUs:
include/linux/rcu_node_tree.h
```
#ifdef CONFIG_RCU_FANOUT_LEAF
#define RCU_FANOUT_LEAF CONFIG_RCU_FANOUT_LEAF
#else /* #ifdef CONFIG_RCU_FANOUT_LEAF */
#define RCU_FANOUT_LEAF 16
#endif /* #else #ifdef CONFIG_RCU_FANOUT_LEAF */


#elif NR_CPUS <= RCU_FANOUT_4
#  define RCU_NUM_LVLS	      4
#  define NUM_RCU_LVL_0	      1
#  define NUM_RCU_LVL_1	      DIV_ROUND_UP(NR_CPUS, RCU_FANOUT_3)
#  define NUM_RCU_LVL_2	      DIV_ROUND_UP(NR_CPUS, RCU_FANOUT_2)
#  define NUM_RCU_LVL_3	      DIV_ROUND_UP(NR_CPUS, RCU_FANOUT_1)
#  define NUM_RCU_NODES	      (NUM_RCU_LVL_0 + NUM_RCU_LVL_1 + NUM_RCU_LVL_2 + NUM_RCU_LVL_3)
...
```
where levels values depend on the CONFIG_RCU_FANOUT_LEAF configuration option. For example for the simplest case,
one rcu_node will cover two CPU on machine with the eight CPUs:
```
+-----------------------------------------------------------------+
|  rcu_state                                                      |
|                 +----------------------+                        |
|                 |         root         |                        |
|                 |       rcu_node       |                        |
|                 +----------------------+                        |
|                    |                |                           |
|               +----v-----+       +--v-------+                   |
|               |          |       |          |                   |
|               | rcu_node |       | rcu_node |                   |
|               |          |       |          |                   |
|         +------------------+     +----------------+             |
|         |                  |        |             |             |
|         |                  |        |             |             |
|    +----v-----+    +-------v--+   +-v--------+  +-v--------+    |
|    |          |    |          |   |          |  |          |    |
|    | rcu_node |    | rcu_node |   | rcu_node |  | rcu_node |    |
|    |          |    |          |   |          |  |          |    |
|    +----------+    +----------+   +----------+  +----------+    |
|         |                 |             |               |       |
|         |                 |             |               |       |
|         |                 |             |               |       |
|         |                 |             |               |       |
+---------|-----------------|-------------|---------------|-------+
          |                 |             |               |
+---------v-----------------v-------------v---------------v--------+
|                 |                |               |               |
|     CPU1        |      CPU3      |      CPU5     |     CPU7      |
|                 |                |               |               |
|     CPU2        |      CPU4      |      CPU6     |     CPU8      |
|                 |                |               |               |
+------------------------------------------------------------------+
```
So, in the rcu_init_geometry function we just need to calculate the total number of rcu_node structures.
We start to do it with the calculation of the jiffies till to the first and next fqs which is force-quiescent-state
(read above about it):
jiffies /ˈdʒɪfiz/  瞬间；一会儿
```
	d = RCU_JIFFIES_TILL_FORCE_QS + nr_cpu_ids / RCU_JIFFIES_FQS_DIV;
	if (jiffies_till_first_fqs == ULONG_MAX)
		jiffies_till_first_fqs = d;
	if (jiffies_till_next_fqs == ULONG_MAX)
		jiffies_till_next_fqs = d;
```
where:
kernel/rcu/tree.h
```
#define RCU_JIFFIES_TILL_FORCE_QS (1 + (HZ > 250) + (HZ > 500))
#define RCU_JIFFIES_FQS_DIV	256
```
https://en.wikipedia.org/wiki/Jiffy_%28time%29
Jiffy can be an informal term for any unspecified short period, as in "I will be back in a jiffy". From this, 
it has acquired a number of more precise applications as the name of multiple units of measurement, 
each used to express or measure very brief durations of time.
ULONG_MAX
https://www.rowleydownload.co.uk/avr/documentation/index.htm?http://www.rowleydownload.co.uk/avr/documentation/ULONG_MAX.htm

As we calculated these jiffies, we check that previous defined jiffies_till_first_fqs and jiffies_till_next_fqs variables 
are equal to the ULONG_MAX (their default values) and set they equal to the calculated value. 
As we did not touch these variables before, they are equal to the ULONG_MAX:
```
static ulong jiffies_till_first_fqs = ULONG_MAX;
static ulong jiffies_till_next_fqs = ULONG_MAX;
```

In the next step of the rcu_init_geometry, we check that rcu_fanout_leaf didn't change (it has the same value as CONFIG_RCU_FANOUT_LEAF in compile-time)
and equal to the value of the CONFIG_RCU_FANOUT_LEAF configuration option, we just return:
```
if (rcu_fanout_leaf == RCU_FANOUT_LEAF &&
	    nr_cpu_ids == NR_CPUS)
		return;
```

After this we need to compute the number of nodes that an rcu_node tree can handle with the given number of levels:
```
rcu_capacity[0] = rcu_fanout_leaf;
	for (i = 1; i < RCU_NUM_LVLS; i++)
		rcu_capacity[i] = rcu_capacity[i - 1] * RCU_FANOUT;
```
And in the last step we calculate the number of rcu_nodes at each level of the tree in the loop.


As we calculated geometry of the rcu_node tree, we need to go back to the rcu_init function and next step 
we need to initialize two rcu_state structures with the rcu_init_one function:
```
rcu_init_one(&rcu_bh_state);
rcu_init_one(&rcu_sched_state);
```


The rcu_init_one function takes two arguments:
1 Global RCU state;
2 Per-CPU data for RCU.
kernel/rcu/tree.h
```
extern struct rcu_state rcu_sched_state;
extern struct rcu_state rcu_bh_state;
```
About this states you can read here. As I wrote above we need to initialize rcu_state structures and rcu_init_one function will help us with it.
https://lwn.net/Articles/264090/
After the rcu_state initialization, we can see the call of the __rcu_init_preempt which depends on the CONFIG_PREEMPT_RCU 
kernel configuration option. It does the same as previous functions - initialization of the rcu_preempt_state structure 
with the rcu_init_one function which has rcu_state type.

After this, in the rcu_init, we can see the call of the function:
kernel/rcu/tree.c
```
open_softirq(RCU_SOFTIRQ, rcu_process_callbacks);
```
This function registers a handler of the pending interrupt. Pending interrupt or softirq supposes that 
part of actions can be delayed for later execution when the system is less loaded. Pending interrupts is represented 
by the following structure:
include/linux/interrupt.h
```
struct softirq_action
{
	void	(*action)(struct softirq_action *);
};
```
contains only one field - handler of an interrupt.
You can check about softirqs in the your system with the:  todo 测试
```
$ cat /proc/softirqs
                    CPU0       CPU1       CPU2       CPU3       CPU4       CPU5       CPU6       CPU7
          HI:          2          0          0          1          0          2          0          0
       TIMER:     137779     108110     139573     107647     107408     114972      99653      98665
      NET_TX:       1127          0          4          0          1          1          0          0
      NET_RX:        334        221     132939       3076        451        361        292        303
       BLOCK:       5253       5596          8        779       2016      37442         28       2855
BLOCK_IOPOLL:          0          0          0          0          0          0          0          0
     TASKLET:         66          0       2916        113          0         24      26708          0
       SCHED:     102350      75950      91705      75356      75323      82627      69279      69914
     HRTIMER:        510        302        368        260        219        255        248        246
         RCU:      81290      68062      82979      69015      68390      69385      63304      63473
```
The open_softirq function takes two parameters:
1 index of the interrupt;
2 interrupt handler.
and adds interrupt handler to the array of the pending interrupts:
kernel/softirq.c
```
void open_softirq(int nr, void (*action)(struct softirq_action *))
{
	softirq_vec[nr].action = action;
}
```
In our case the interrupt handler is - rcu_process_callbacks  and does the RCU core processing for the current CPU.
kernel/rcu/tree.c
```
static __latent_entropy void rcu_process_callbacks(struct softirq_action *unused)
{
	struct rcu_state *rsp;
	if (cpu_is_offline(smp_processor_id()))
		return;
	trace_rcu_utilization(TPS("Start RCU core"));
	for_each_rcu_flavor(rsp)
		__rcu_process_callbacks(rsp);
	trace_rcu_utilization(TPS("End RCU core"));
}
```

kernel/rcu/tree.c
After we registered softirq interrupt for the RCU, we can see the following code:
```
void __init rcu_init(void)
{
..
pm_notifier(rcu_pm_notify, 0);
 for_each_online_cpu(cpu) {
     rcutree_prepare_cpu(cpu);
     rcu_cpu_starting(cpu);
     rcutree_online_cpu(cpu);
 }
}
```
Here we can see registration of the cpu notifier which needs in systems which supports CPU hotplug and
https://www.kernel.org/doc/Documentation/cpu-hotplug.txt
we will not dive into details about this theme. 

That's all.We saw initialization process of the RCU subsystem. As I wrote above, more about the RCU will be in 
the separate chapter about synchronization primitives.


Rest of the initialization process
Ok, we already passed the main theme of this part which is RCU initialization, but it is not the end of the linux kernel initialization process. 
In the last paragraph of this theme we will see a couple of functions which work in the initialization time,
but we will not dive into deep details around this function for different reasons. 
Some reasons not to dive into details are following:
1 They are not very important for the generic kernel initialization process and depend on the different kernel configuration;
2 They have the character of debugging and not important for now;
3 We will see many of this stuff in the separate parts/chapters.


After we initialized RCU, the next step which you can see in the init/main.c is the - trace_init function.
As you can understand from its name, this function initialize tracing subsystem. You can read more about linux kernel trace system- here.
https://elinux.org/Kernel_Trace_Systems
https://en.wikipedia.org/wiki/Tracing_%28software%29
Tracing in software engineering refers to the process of capturing and recording information about the execution of a software program.
This information is typically used by programmers for debugging purposes, and additionally, depending on the type 
and detail of information contained in a trace log, by experienced system administrators or technical-support personnel 
and by software monitoring tools to diagnose common problems with software.
init/main.c
```
	ftrace_init();
	/* trace_printk can be enabled here */
	early_trace_init();
	...
	trace_init();
```

After the trace_init, we can see the call of the radix_tree_init. If you are familiar with the different data structures, 
you can understand from the name of this function that it initializes kernel implementation of the Radix tree. 
This function is defined in the lib/radix-tree.c and you can read more about it in the part about Radix tree.
https://en.wikipedia.org/wiki/Radix_tree
```
radix_tree_init();
```


In the next step we can see the functions which are related to the interrupts handling subsystem, they are:
early_irq_init
init_IRQ
softirq_init
We will see explanation about this functions and their implementation in the special part about interrupts and exceptions handling.
After this many different functions (like init_timers, hrtimers_init, time_init, etc.) which are 
related to different timing and timers stuff. We will see more about these function in the chapter about timers.


The next couple of functions are related with the perf events - perf_event-init (there will be separate chapter about perf), 
initialization of the profiling with the profile_init.
```
perf_event_init();
```
perf events:https://perf.wiki.kernel.org/index.php/Main_Page
the Linux perf command, also called perf_events. perf is powerful: it can instrument CPU performance counters, 
tracepoints, kprobes, and uprobes (dynamic tracing). It is capable of lightweight profiling. 
It is also included in the Linux kernel, under tools/perf, and is frequently updated and enhanced.

After this we enable irq with the call of the:
```
local_irq_enable();
```
which expands to the sti instruction and making post initialization of the SLAB with the call of the 
kmem_cache_init_late function (As I wrote above we will know about the SLAB in the Linux memory management chapter).
https://0xax.gitbooks.io/linux-insides/content/MM/index.html


After the post initialization of the SLAB, next point is initialization of the console with the console_init function 
from the drivers/tty/tty_io.c.
```
console_init();
```

After the console initialization, we can see the lockdep_info function which prints information about the Lock dependency validator.
```
lockdep_info();
```
After this, we can see the initialization of the dynamic allocation of the debug objects with the debug_objects_mem_init, 
kernel memory leak detector initialization with the kmemleak_init, 
percpu pageset setup with the setup_per_cpu_pageset,
setup of the NUMA policy with the numa_policy_init,
setting time for the scheduler with the sched_clock_init,
pidmap initialization with the call of the pidmap_init function for the initial PID namespace, 
cache creation with the anon_vma_init for the private virtual memory areas and
early initialization of the ACPI with the acpi_early_init.
```
sched_clock_postinit();
...
debug_objects_mem_init();
kmemleak_init();
setup_per_cpu_pageset();
numa_policy_init();
..
pid_idr_init();
anon_vma_init();
...
acpi_early_init();
```