
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
```