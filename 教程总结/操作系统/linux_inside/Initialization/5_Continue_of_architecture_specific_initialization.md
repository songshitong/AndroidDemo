



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
arch/x86/mm/ioremap.c
```
void __init early_ioremap_init(void)
{
	pmd_t *pmd;

#ifdef CONFIG_X86_64
	BUILD_BUG_ON((fix_to_virt(0) + PAGE_SIZE) & ((1 << PMD_SHIFT) - 1));
....

	early_ioremap_setup();

	pmd = early_ioremap_pmd(fix_to_virt(FIX_BTMAP_BEGIN));
	memset(bm_pte, 0, sizeof(bm_pte));
	pmd_populate_kernel(&init_mm, pmd, bm_pte);
   ...
}
```
When a physical address is accessed by the CPU, it may refer to a portion of physical RAM which can be
mapped on memory of the I/O device. So ioremap used to map device memory into kernel address space.

As i wrote above next function is the early_ioremap_init which re-maps I/O memory to kernel address space so it can access it. 
We need to initialize early ioremap for early initialization code which needs to temporarily map I/O or memory regions 
before the normal mapping functions like ioremap are available. Implementation of this function is in the arch/x86/mm/ioremap.c. 
At the start of the early_ioremap_init we can see definition of the pmd pointer with pmd_t type (which presents 
page middle directory entry typedef struct { pmdval_t pmd; } pmd_t; where pmdval_t is unsigned long) and 
make a check that fixmap aligned in a correct way

early_ioremap_setup
https://github.com/torvalds/linux/blob/v4.16/mm/early_ioremap.c
```
void __init early_ioremap_setup(void)
{
	int i;

	for (i = 0; i < FIX_BTMAPS_SLOTS; i++)
		if (WARN_ON(prev_map[i]))
			break;

   //FIX_BTMAPS_SLOTS 8
	for (i = 0; i < FIX_BTMAPS_SLOTS; i++)
		slot_virt[i] = __fix_to_virt(FIX_BTMAP_BEGIN - NR_FIX_BTMAPS*i);
}
```
fixmap - is fixed virtual address mappings which extends from FIXADDR_START to FIXADDR_TOP. 
Fixed virtual addresses are needed for subsystems that need to know the virtual address at compile time. 
After the check early_ioremap_init makes a call of the early_ioremap_setup function . 
early_ioremap_setup fills slot_virt array of the unsigned long with virtual addresses with 512 temporary boot-time fix-mappings

After this we get page middle directory entry for the FIX_BTMAP_BEGIN and put to the pmd variable,
fills bm_pte with zeros which is boot time page tables and call pmd_populate_kernel function for 
setting given page table entry in the given page middle directory

That's all for this. If you feeling puzzled, don't worry. There is special part about ioremap and fixmaps 
in the Linux Kernel Memory Management. Part 2 chapter.




Obtaining major and minor numbers for the root device
After early ioremap was initialized, you can see the following code:
arch/x86/kernel/setup.c
```
...
	early_ioremap_init();
...
	ROOT_DEV = old_decode_dev(boot_params.hdr.root_dev);
	...
```
This code obtains major and minor numbers for the root device where initrd will be mounted later in the do_mount_root function.
Major number of the device identifies a driver associated with the device. Minor number referred on the device controlled by driver.
Note that old_decode_dev takes one parameter from the boot_params_structure. As we can read from the x86 linux kernel boot protocol:
```
Field name:    root_dev
Type:        modify (optional)
Offset/size:    0x1fc/2
Protocol:    ALL

  The default root device device number.  The use of this field is
  deprecated, use the "root=" option on the command line instead.
```
Now let's try to understand what old_decode_dev does. Actually it just calls MKDEV inside which generates
dev_t from the give major and minor numbers. It's implementation is pretty simple:
include/linux/kdev_t.h
```
#define MINORBITS	20
#define MKDEV(ma,mi)	(((ma) << MINORBITS) | (mi))

static inline dev_t old_decode_dev(u16 val)
{
	return MKDEV((val >> 8) & 255, val & 255);
}
```
where dev_t is a kernel data type to present major/minor number pair. But what's the strange old_ prefix? 
For historical reasons, there are two ways of managing the major and minor numbers of a device.
In the first way major and minor numbers occupied 2 bytes. You can see it in the previous code: 8 bit for major number 
and 8 bit for minor number. But there is a problem: only 256 major numbers and 256 minor numbers are possible. 
So 16-bit integer was replaced by 32-bit integer where 12 bits reserved for major number and 20 bits for minor. 
You can see this in the new_decode_dev implementation:
```
static inline dev_t new_decode_dev(u32 dev)
{
	unsigned major = (dev & 0xfff00) >> 8;
	unsigned minor = (dev & 0xff) | ((dev >> 12) & 0xfff00);
	return MKDEV(major, minor);
}
```
After calculation we will get 0xfff or 12 bits for major if it is 0xffffffff and 0xfffff or 20 bits for minor. 
So in the end of execution of the old_decode_dev we will get major and minor numbers for the root device in ROOT_DEV.



Memory map setup
The next point is the setup of the memory map with the call of the setup_memory_map function. But before this 
we setup different parameters as information about a screen (current row and column, video page and etc...), 
Extended display identification data, video mode, bootloader_type and etc...:
arch/x86/kernel/setup.c
```
...
screen_info = boot_params.screen_info;
edid_info = boot_params.edid_info;
saved_video_mode = boot_params.hdr.vid_mode;
bootloader_type = boot_params.hdr.type_of_loader;
if ((bootloader_type >> 4) == 0xe) {
    bootloader_type &= 0xf;
    bootloader_type |= (boot_params.hdr.ext_loader_type+0x10) << 4;
}
bootloader_version  = bootloader_type & 0xf;
bootloader_version |= boot_params.hdr.ext_loader_ver << 4;
...
```
All of these parameters we got during boot time and stored in the boot_params structure. After this we 
need to setup the end of the I/O memory. As you know one of the main purposes of the kernel is resource management.
And one of the resource is memory. As we already know there are two ways to communicate with devices are I/O ports and device memory. 
All information about registered resources are available through:
1 /proc/ioports - provides a list of currently registered port regions used for input or output communication with a device;
2 /proc/iomem - provides current map of the system's memory for each physical device.

At the moment we are interested in /proc/iomem:    todo 测试
```
cat /proc/iomem
00000000-00000fff : reserved
00001000-0009d7ff : System RAM
0009d800-0009ffff : reserved
000a0000-000bffff : PCI Bus 0000:00
000c0000-000cffff : Video ROM
000d0000-000d3fff : PCI Bus 0000:00
000d4000-000d7fff : PCI Bus 0000:00
000d8000-000dbfff : PCI Bus 0000:00
000dc000-000dffff : PCI Bus 0000:00
000e0000-000fffff : reserved
000e0000-000e3fff : PCI Bus 0000:00
000e4000-000e7fff : PCI Bus 0000:00
000f0000-000fffff : System ROM
```
As you can see range of addresses are shown in hexadecimal notation with its owner. Linux kernel provides 
API for managing any resources in a general way. Global resources (for example PICs or I/O ports) can be divided into subsets
 relating to any hardware bus slot. The main structure resource:
include/linux/ioport.h
```
/*
 * Resources are tree-like, allowing
 * nesting etc..
 */
struct resource {
	resource_size_t start;
	resource_size_t end;
	const char *name;
	unsigned long flags;
	unsigned long desc;
	struct resource *parent, *sibling, *child;
};

//flags定义
#define IORESOURCE_BITS		0x000000ff	/* Bus-specific bits */

#define IORESOURCE_TYPE_BITS	0x00001f00	/* Resource type */
#define IORESOURCE_IO		0x00000100	/* PCI/ISA I/O ports */
#define IORESOURCE_MEM		0x00000200
#define IORESOURCE_REG		0x00000300	/* Register offsets */
#define IORESOURCE_IRQ		0x00000400
#define IORESOURCE_DMA		0x00000800
#define IORESOURCE_BUS		0x00001000
...
```
presents abstraction for a tree-like subset of system resources. This structure provides range of addresses 
from start to end (resource_size_t is phys_addr_t or u64 for x86_64) which a resource covers, name of a resource 
(you see these names in the /proc/iomem output) and flags of a resource . 
The last are three pointers to the resource structure. These pointers enable a tree-like structure:
```
+-------------+      +-------------+
|             |      |             |
|    parent   |------|    sibling  |
|             |      |             |
+-------------+      +-------------+
       |
       |
+-------------+
|             |
|    child    | 
|             |
+-------------+
```
Every subset of resources has root range resources. For iomem it is iomem_resource which defined as:
https://github.com/torvalds/linux/blob/v4.16/kernel/resource.c
```
struct resource iomem_resource = {
	.name	= "PCI mem",
	.start	= 0,
	.end	= -1,
	.flags	= IORESOURCE_MEM,
};
EXPORT_SYMBOL(iomem_resource);
```
iomem_resource defines root addresses range for io memory with PCI mem name and IORESOURCE_MEM (0x00000200) as flags.
As i wrote above our current point is setup the end address of the iomem. We will do it with:
arch/x86/kernel/setup.c
```
...
iomem_resource.end = (1ULL << boot_cpu_data.x86_phys_bits) - 1;
```
Here we shift 1 on boot_cpu_data.x86_phys_bits. boot_cpu_data is cpuinfo_x86 structure which we filled during execution
of the early_cpu_init. As you can understand from the name of the x86_phys_bits field, it presents
maximum bits amount of the maximum physical address in the system. Note also that iomem_resource is passed 
to the EXPORT_SYMBOL macro. This macro exports the given symbol (iomem_resource in our case) for 
dynamic linking or in other words it makes a symbol accessible to dynamically loaded modules.

After we set the end address of the root iomem resource address range, as I wrote above the next step
will be setup of the memory map. It will be produced with the call of the setup_ memory_map function:
arch/x86/kernel/e820.c
```
void __init e820__memory_setup(void)
{
	char *who;

	/* This is a firmware interface ABI - make sure we don't break it: */
	BUILD_BUG_ON(sizeof(struct boot_e820_entry) != 20);

	who = x86_init.resources.memory_setup();

	memcpy(e820_table_kexec, e820_table, sizeof(*e820_table_kexec));
	memcpy(e820_table_firmware, e820_table, sizeof(*e820_table_firmware));

	pr_info("e820: BIOS-provided physical RAM map:\n");
	e820__print_table(who);
}
```
First of all we call look here the call of the x86_init.resources.memory_setup. x86_init is a x86_init_ops structure 
which presents platform specific setup functions as resources initialization, pci initialization and etc...
arch/x86/kernel/x86_init.c
```
struct x86_init_ops x86_init __initdata = {

	.resources = {
		.probe_roms		= probe_roms,
		.reserve_resources	= reserve_standard_io_resources,
		.memory_setup		= e820__memory_setup_default,
	},
...	
```
/arch/x86/kernel/e820.c
```
char *__init e820__memory_setup_default(void)
{
	char *who = "BIOS-e820";
	if (append_e820_table(boot_params.e820_table, boot_params.e820_entries) < 0) {
		u64 mem_size;
		if (boot_params.alt_mem_k < boot_params.screen_info.ext_mem_k) {
			mem_size = boot_params.screen_info.ext_mem_k;
			who = "BIOS-88";
		} else {
			mem_size = boot_params.alt_mem_k;
			who = "BIOS-e801";
		}

		e820_table->nr_entries = 0;
		e820__range_add(0, LOWMEMSIZE(), E820_TYPE_RAM);
		e820__range_add(HIGH_MEMORY, mem_size << 10, E820_TYPE_RAM);
	}

	/* We just appended a lot of ranges, sanitize the table: */
	e820__update_table(e820_table);

	return who;
}

```
As we can see here memory_setup field is default_machine_specific_memory_setup where we get the number
of the e820 entries which we collected in the boot time, sanitize the BIOS e820 map and fill e820map structure 
with the memory regions. As all regions are collected, print of all regions with printk. 
You can find this print if you execute dmesg command and you can see something like this:  todo
```
[    0.000000] e820: BIOS-provided physical RAM map:
[    0.000000] BIOS-e820: [mem 0x0000000000000000-0x000000000009d7ff] usable
[    0.000000] BIOS-e820: [mem 0x000000000009d800-0x000000000009ffff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000000e0000-0x00000000000fffff] reserved
[    0.000000] BIOS-e820: [mem 0x0000000000100000-0x00000000be825fff] usable
[    0.000000] BIOS-e820: [mem 0x00000000be826000-0x00000000be82cfff] ACPI NVS
[    0.000000] BIOS-e820: [mem 0x00000000be82d000-0x00000000bf744fff] usable
[    0.000000] BIOS-e820: [mem 0x00000000bf745000-0x00000000bfff4fff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000bfff5000-0x00000000dc041fff] usable
[    0.000000] BIOS-e820: [mem 0x00000000dc042000-0x00000000dc0d2fff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000dc0d3000-0x00000000dc138fff] usable
[    0.000000] BIOS-e820: [mem 0x00000000dc139000-0x00000000dc27dfff] ACPI NVS
[    0.000000] BIOS-e820: [mem 0x00000000dc27e000-0x00000000deffefff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000defff000-0x00000000deffffff] usable
...
```



Copying of the BIOS Enhanced Disk Device information
The next two steps is parsing of the setup_data with parse_setup_data function and copying BIOS EDD to the safe place.
arch/x86/kernel/setup.c
```
parse_setup_data();
copy_edd();


static void __init parse_setup_data(void)
{
	struct setup_data *data;
	u64 pa_data, pa_next;

	pa_data = boot_params.hdr.setup_data;
	while (pa_data) {
		u32 data_len, data_type;
		data = early_memremap(pa_data, sizeof(*data));
		data_len = data->len + sizeof(struct setup_data);
		data_type = data->type;
		pa_next = data->next;
		early_memunmap(data, sizeof(*data));

		switch (data_type) {
		case SETUP_E820_EXT:
			e820__memory_setup_extended(pa_data, data_len);
			break;
		....
		case SETUP_EFI:
			parse_efi_setup(pa_data, data_len);
			break;
		default:
			break;
		}
		pa_data = pa_next;
	}
}

static inline void __init copy_edd(void)
{
     memcpy(edd.mbr_signature, boot_params.edd_mbr_sig_buffer,
	    sizeof(edd.mbr_signature));
     memcpy(edd.edd_info, boot_params.eddbuf, sizeof(edd.edd_info));
     edd.mbr_signature_nr = boot_params.edd_mbr_sig_buf_entries;
     edd.edd_info_nr = boot_params.eddbuf_entries;
}
```

setup_data is a field from the kernel boot header and as we can read from the x86 boot protocol:
https://www.kernel.org/doc/html/v5.6/x86/boot.html
```
Field name:	setup_data
Type:	write (special)
Offset/size:	0x250/8
Protocol:	2.09+
The 64-bit physical pointer to NULL terminated single linked list of struct setup_data. 
This is used to define a more extensible boot parameters passing mechanism
```
It used for storing setup information for different types as device tree blob, EFI setup data and etc... 
In the second step we copy BIOS EDD information from the boot_params structure that we collected in the arch/x86/boot/edd.c to the edd structure




Memory descriptor initialization
The next step is initialization of the memory descriptor of the init process. As you already can know 
every process has its own address space. This address space presented with special data structure which called memory descriptor.
Directly in the linux kernel source code memory descriptor presented with mm_struct structure.
mm_struct contains many different fields related with the process address space as start/end address of the kernel code/data,
start/end of the brk, number of memory areas, list of memory areas and etc...
include/linux/mm_types.h
```
struct mm_struct {
	struct vm_area_struct *mmap;		/* list of VMAs */
	struct rb_root mm_rb;
    ..
    unsigned long task_size;
    ..
    unsigned long start_code, end_code, start_data, end_data;
	unsigned long start_brk, brk, start_stack;
	...
}	
```

 As every process has its own memory descriptor, task_struct structure contains it in the mm and active_mm field.
And our first init process has it too. You can remember that we saw the part of initialization of the init task_struct 
with INIT_TASK macro in the previous part:
 init/init_task.c
```
struct task_struct init_task
#ifdef CONFIG_ARCH_TASK_STRUCT_ON_STACK
	__init_task_data
#endif
= {
...
.mm		= NULL,
.active_mm	= &init_mm,  //mm和active_mm 类型都为mm_struct
...
}
```

mm points to the process address space and active_mm points to the active address space if process has
no address space such as kernel threads (more about it you can read in the documentation).
https://www.kernel.org/doc/Documentation/vm/active_mm.txt
Now we fill memory descriptor of the initial process:
arch/x86/kernel/setup.c
```
...
init_mm.start_code = (unsigned long) _text;
init_mm.end_code = (unsigned long) _etext;
init_mm.end_data = (unsigned long) _edata;
init_mm.brk = _brk_end;
```
with the kernel's text, data and brk. init_mm is the memory descriptor of the initial process and defined as:
https://github.com/torvalds/linux/blob/v4.16/mm/init-mm.c
```
struct mm_struct init_mm = {
	.mm_rb		= RB_ROOT,
	.pgd		= swapper_pg_dir,
	.mm_users	= ATOMIC_INIT(2),
	.mm_count	= ATOMIC_INIT(1),
	.mmap_sem	= __RWSEM_INITIALIZER(init_mm.mmap_sem),
	.page_table_lock =  __SPIN_LOCK_UNLOCKED(init_mm.page_table_lock),
	.mmlist		= LIST_HEAD_INIT(init_mm.mmlist),
	.user_ns	= &init_user_ns,
	INIT_MM_CONTEXT(init_mm)
};
```
where mm_rb is a red-black tree of the virtual memory areas,
pgd is a pointer to the page global directory, 
mm_users is address space users,
mm_count is primary usage counter 
mmap_sem is memory area semaphore. 
After we setup memory descriptor of the initial process, next step is initialization of the Intel Memory Protection Extensions
with mpx_mm_init. The next step is initialization of the code/data/bss resources with:
arch/x86/kernel/setup.c
```
	code_resource.start = __pa_symbol(_text);
	code_resource.end = __pa_symbol(_etext)-1;
	data_resource.start = __pa_symbol(_etext);
	data_resource.end = __pa_symbol(_edata)-1;
	bss_resource.start = __pa_symbol(__bss_start);
	bss_resource.end = __pa_symbol(__bss_stop)-1;
```
We already know a little about resource structure (read above). Here we fills code/data/bss resources 
 with their physical addresses. You can see it in the /proc/iomem:  todo
```
00100000-be825fff : System RAM
  01000000-015bb392 : Kernel code
  015bb393-01930c3f : Kernel data
  01a11000-01ac3fff : Kernel bss
```
arch/x86/kernel/setup.c
```
static struct resource data_resource = {
	.name	= "Kernel data",
	.start	= 0,
	.end	= 0,
	.flags	= IORESOURCE_BUSY | IORESOURCE_SYSTEM_RAM
};

static struct resource code_resource = {
	.name	= "Kernel code",
	.start	= 0,
	.end	= 0,
	.flags	= IORESOURCE_BUSY | IORESOURCE_SYSTEM_RAM
};

static struct resource bss_resource = {
	.name	= "Kernel bss",
	.start	= 0,
	.end	= 0,
	.flags	= IORESOURCE_BUSY | IORESOURCE_SYSTEM_RAM
};
```

The last step which we will cover in this part will be NX configuration. NX-bit or no execute bit is 63-bit 
in the page directory entry which controls the ability to execute code from all physical pages mapped by the table entry.
This bit can only be used/set when the no-execute page-protection mechanism is enabled by the setting EFER.NXE to 1.
In the x86_configure_nx function we check that CPU has support of NX-bit and it does not disabled. 
After the check we fill __supported_pte_mask depend on it:
arch/x86/kernel/setup.c
```
...
x86_configure_nx();
```
arch/x86/mm/setup_nx.c
```
void x86_configure_nx(void)
{
	if (boot_cpu_has(X86_FEATURE_NX) && !disable_nx)
		__supported_pte_mask |= _PAGE_NX;
	else
		__supported_pte_mask &= ~_PAGE_NX;
}
```