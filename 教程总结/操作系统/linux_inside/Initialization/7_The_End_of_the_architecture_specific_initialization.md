
https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-7.html
In the next step, as we set limit of the memblock to the all mapped pages, we can see the call 
of the setup_log_buf function 
arch/x86/kernel/setup.c
```
...
memblock_set_current_limit(get_max_mapped());
...
/* Allocate bigger log buffer */
	setup_log_buf(1);
```
kernel/printk/printk.c
```
#define __LOG_BUF_LEN (1 << CONFIG_LOG_BUF_SHIFT)
static char __log_buf[__LOG_BUF_LEN] __aligned(LOG_ALIGN);
static char *log_buf = __log_buf;

void __init setup_log_buf(int early)
{
	unsigned long flags;
	char *new_log_buf;
	int free;

	if (log_buf != __log_buf)
		return;

	if (!early && !new_log_buf_len)
		log_buf_add_cpu();

	if (!new_log_buf_len)
		return;

	if (early) {
		new_log_buf =
			memblock_virt_alloc(new_log_buf_len, LOG_ALIGN);
	} else {
		new_log_buf = memblock_virt_alloc_nopanic(new_log_buf_len,
							  LOG_ALIGN);
	}

	if (unlikely(!new_log_buf)) {
		pr_err("log_buf_len: %ld bytes not available\n",
			new_log_buf_len);
		return;
	}

	logbuf_lock_irqsave(flags);
	log_buf_len = new_log_buf_len;
	log_buf = new_log_buf;
	new_log_buf_len = 0;
	free = __LOG_BUF_LEN - log_next_idx;
	memcpy(log_buf, __log_buf, __LOG_BUF_LEN);
	logbuf_unlock_irqrestore(flags);

	pr_info("log_buf_len: %d bytes\n", log_buf_len);
	pr_info("early log buf free: %d(%d%%)\n",
		free, (free * 100) / __LOG_BUF_LEN);
}
```
The setup_log_buf function setups kernel cyclic buffer and its length depends on the CONFIG_LOG_BUF_SHIFT configuration option.
As we can read from the documentation of the CONFIG_LOG_BUF_SHIFT it can be between 12 and 21. In the insides, 
buffer defined as array of chars

It starts with check that current buffer is empty (It must be empty, because we just setup it) and another check that it is early setup. 
If setup of the kernel log buffer is not early, we call the log_buf_add_cpu function which increase size of the buffer for every CPU

We will not research log_buf_add_cpu function, because as you can see in the setup_arch, we call setup_log_buf as: setup_log_buf(1)
where 1 means that it is early setup.

In the next step we check new_log_buf_len variable which is updated length of the kernel log buffer and 
  allocate new space for the buffer with the memblock_virt_alloc function for it, or just return.



As kernel log buffer is ready, the next function is reserve_initrd. You can remember that we already
called the early_reserve_initrd function in the fourth part of the Kernel initialization.
Now, as we reconstructed direct memory mapping in the init_mem_mapping function, we need to move initrd into directly mapped memory.
The reserve_initrd function starts from the definition of the base address and end address of the initrd 
and check that initrd is provided by a bootloader. All the same as what we saw in the early_reserve_initrd. 
But instead of the reserving place in the memblock area with the call of the memblock_reserve function, 
we get the mapped size of the direct memory area and check that the size of the initrd is not greater than this area with:
arch/x86/kernel/setup.c
```
reserve_initrd();

static void __init reserve_initrd(void)
{
	/* Assume only end is not page aligned */
	u64 ramdisk_image = get_ramdisk_image();
	u64 ramdisk_size  = get_ramdisk_size();
	u64 ramdisk_end   = PAGE_ALIGN(ramdisk_image + ramdisk_size);
	u64 mapped_size;

	if (!boot_params.hdr.type_of_loader ||
	    !ramdisk_image || !ramdisk_size)
		return;		/* No initrd provided by bootloader */

	initrd_start = 0;

	mapped_size = memblock_mem_size(max_pfn_mapped);
	if (ramdisk_size >= (mapped_size>>1))
		panic("initrd too large to handle, "
		       "disabling initrd (%lld needed, %lld available)\n",
		       ramdisk_size, mapped_size>>1);

	printk(KERN_INFO "RAMDISK: [mem %#010llx-%#010llx]\n", ramdisk_image,
			ramdisk_end - 1);

	if (pfn_range_is_mapped(PFN_DOWN(ramdisk_image),
				PFN_DOWN(ramdisk_end))) {
		/* All are mapped, easy case */
		initrd_start = ramdisk_image + PAGE_OFFSET;
		initrd_end = initrd_start + ramdisk_size;
		return;
	}

	relocate_initrd();

	memblock_free(ramdisk_image, ramdisk_end - ramdisk_image);
}
```
You can see here that we call memblock_mem_size function and pass the max_pfn_mapped to it, where max_pfn_mapped 
   contains the highest direct mapped page frame number.
If you do not remember what is page frame number, explanation is simple: First 12 bits of the virtual address represent offset 
  in the physical page or page frame. If we right-shift out 12 bits of the virtual address,
  we'll discard offset part and will get Page Frame Number.  todo 计算
mm/memblock.c
```
phys_addr_t __init memblock_mem_size(unsigned long limit_pfn)
{
	unsigned long pages = 0;
	struct memblock_region *r;
	unsigned long start_pfn, end_pfn;

	for_each_memblock(memory, r) {
		start_pfn = memblock_region_memory_base_pfn(r);
		end_pfn = memblock_region_memory_end_pfn(r);
		start_pfn = min_t(unsigned long, start_pfn, limit_pfn);
		end_pfn = min_t(unsigned long, end_pfn, limit_pfn);
		pages += end_pfn - start_pfn;
	}
	return PFN_PHYS(pages);
}
```
In the memblock_mem_size we go through the all memblock mem (not reserved) regions and calculates size 
of the mapped pages and return it to the mapped_size variable (see code above).

As we got amount of the direct mapped memory, we check that size of the initrd is not greater than mapped pages. 
If it is greater we just call panic which halts the system and prints famous Kernel panic message. 
In the next step we print information about the initrd size. We can see the result of this in the dmesg output:
We can see the result of this in the dmesg output:  todo
```
[0.000000] RAMDISK: [mem 0x36d20000-0x37687fff]
```
panic
https://en.wikipedia.org/wiki/Kernel_panic
A kernel panic (sometimes abbreviated as KP) is a safety measure taken by an operating system's kernel
upon detecting an internal fatal error in which either it is unable to safely recover or continuing 
to run the system would have a higher risk of major data loss. The term is largely specific to Unix and Unix-like systems. 
The equivalent on Microsoft Windows operating systems is a stop error, often called a "blue screen of death".
panic  
恐慌；惊恐；惶恐不安；人心惶惶的局面

initrd to the direct mapping area with the relocate_initrd function. In the start of the relocate_initrd 
function we try to find a free area with the memblock_find_in_range function:
arch/x86/kernel/setup.c
```
static void __init relocate_initrd(void)
{
	/* Assume only end is not page aligned */
	u64 ramdisk_image = get_ramdisk_image();
	u64 ramdisk_size  = get_ramdisk_size();
	u64 area_size     = PAGE_ALIGN(ramdisk_size);

	/* We need to move the initrd down into directly mapped mem */
	relocated_ramdisk = memblock_find_in_range(0, PFN_PHYS(max_pfn_mapped),
						   area_size, PAGE_SIZE);

	if (!relocated_ramdisk)
		panic("Cannot find place for new RAMDISK of size %lld\n",
		      ramdisk_size);

	/* Note: this includes all the mem currently occupied by
	   the initrd, we rely on that fact to keep the data intact. */
	memblock_reserve(relocated_ramdisk, area_size);
	initrd_start = relocated_ramdisk + PAGE_OFFSET;
	initrd_end   = initrd_start + ramdisk_size;
	printk(KERN_INFO "Allocated new RAMDISK: [mem %#010llx-%#010llx]\n",
	       relocated_ramdisk, relocated_ramdisk + ramdisk_size - 1);

	copy_from_early_mem((void *)initrd_start, ramdisk_image, ramdisk_size);

	printk(KERN_INFO "Move RAMDISK from [mem %#010llx-%#010llx] to"
		" [mem %#010llx-%#010llx]\n",
		ramdisk_image, ramdisk_image + ramdisk_size - 1,
		relocated_ramdisk, relocated_ramdisk + ramdisk_size - 1);
}
```
The memblock_find_in_range function tries to find a free area in a given range, in our case from 0 to 
the maximum mapped physical address and size must equal to the aligned size of the initrd. 
If we didn't find a area with the given size, we call panic again. If all is good, we start to relocated 
RAM disk to the down of the directly mapped memory in the next step.


In the end of the reserve_initrd function, we free memblock memory which occupied by the ramdisk with the call of the:
```
memblock_free(ramdisk_image, ramdisk_end - ramdisk_image);
```

After we relocated initrd ramdisk image, the next function is vsmp_init from the arch/x86/kernel/vsmp_64.c. 
This function initializes support of the ScaleMP vSMP. As I already wrote in the previous parts, 
this chapter will not cover non-related x86_64 initialization parts (for example as the current or ACPI, etc.).
So we will skip implementation of this for now and will back to it in the part which cover techniques of parallel computing.



The next function is io_delay_init . This function allows to override 
default I/O delay 0x80 port. We already saw I/O delay in the Last preparation before transition into protected mode
arch/x86/kernel/io_delay.c
```
void __init io_delay_init(void)
{
	if (!io_delay_override)
		dmi_check_system(io_delay_0xed_port_dmi_table);
}
```
This function check io_delay_override variable and overrides I/O delay port if io_delay_override is set. 
We can set io_delay_override variably by passing io_delay option to the kernel command line. 
As we can read from the Documentation/kernel-parameters.txt, io_delay option is:
https://github.com/torvalds/linux/blob/master/Documentation/admin-guide/kernel-parameters.txt
```
io_delay=	[X86,EARLY] I/O delay method
		0x80
			Standard port 0x80 based delay
		0xed
			Alternate port 0xed based delay (needed on some systems)
		udelay
			Simple two microseconds delay
		none
			No delay
```
We can see io_delay command line parameter setup with the early_param macro in the arch/x86/kernel/io_delay.c
```
early_param("io_delay", io_delay_param); //最后一行
```
More about early_param you can read in the previous part,So the io_delay_param function which setups io_delay_override
variable will be called in the do_early_param function(https://github.com/torvalds/linux/blob/16f73eb02d7e1765ccab3d2018e0bd98eb93d973/init/main.c). 
io_delay_param function gets the argument of the io_delay kernel command line parameter and sets io_delay_type depends on it:
```
static int __init io_delay_param(char *s)
{
	if (!s)
		return -EINVAL;

	if (!strcmp(s, "0x80"))
		io_delay_type = CONFIG_IO_DELAY_TYPE_0X80;
	else if (!strcmp(s, "0xed"))
		io_delay_type = CONFIG_IO_DELAY_TYPE_0XED;
	else if (!strcmp(s, "udelay"))
		io_delay_type = CONFIG_IO_DELAY_TYPE_UDELAY;
	else if (!strcmp(s, "none"))
		io_delay_type = CONFIG_IO_DELAY_TYPE_NONE;
	else
		return -EINVAL;

	io_delay_override = 1;
	return 0;
}
```

The next functions are acpi_boot_table_init, early_acpi_boot_init and initmem_init after the io_delay_init, 
but as I wrote above we will not cover ACPI related stuff in this Linux Kernel initialization process chapter.
ACPI
https://en.wikipedia.org/wiki/ACPI
Advanced Configuration and Power Interface (ACPI) is an open standard that operating systems can use to 
discover and configure computer hardware components, to perform power management (e.g. putting unused hardware components to sleep), 
auto configuration (e.g. Plug and Play and hot swapping), and status monitoring.



Direct memory access
https://en.wikipedia.org/wiki/Direct_memory_access
Direct memory access (DMA) is a feature of computer systems that allows certain hardware subsystems 
to access main system memory independently of the central processing unit (CPU)

Allocate area for DMA
In the next step we need to allocate area for the Direct memory access with the dma_contiguous_reserve function
 DMA is a special mode when devices communicate with memory without CPU. 
Note that we pass one parameter - max_pfn_mapped << PAGE_SHIFT, to the dma_contiguous_reserve function 
and as you can understand from this expression, this is limit of the reserved memory.
arch/x86/kernel/setup.c
```
dma_contiguous_reserve(max_pfn_mapped << PAGE_SHIFT);
```
drivers/base/dma-contiguous.c
```
void __init dma_contiguous_reserve(phys_addr_t limit)
{
	phys_addr_t selected_size = 0;
	phys_addr_t selected_base = 0;
	phys_addr_t selected_limit = limit;
	bool fixed = false;

	pr_debug("%s(limit %08lx)\n", __func__, (unsigned long)limit);

	if (size_cmdline != -1) {
		selected_size = size_cmdline;
		selected_base = base_cmdline;
		selected_limit = min_not_zero(limit_cmdline, limit);
		if (base_cmdline + size_cmdline == limit_cmdline)
			fixed = true;
	} else {
#ifdef CONFIG_CMA_SIZE_SEL_MBYTES
		selected_size = size_bytes;
#elif defined(CONFIG_CMA_SIZE_SEL_PERCENTAGE)
		selected_size = cma_early_percent_memory();
#elif defined(CONFIG_CMA_SIZE_SEL_MIN)
		selected_size = min(size_bytes, cma_early_percent_memory());
#elif defined(CONFIG_CMA_SIZE_SEL_MAX)
		selected_size = max(size_bytes, cma_early_percent_memory());
#endif
	}

	if (selected_size && !dma_contiguous_default_area) {
		pr_debug("%s: reserving %ld MiB for global area\n", __func__,
			 (unsigned long)selected_size / SZ_1M);

		dma_contiguous_reserve_area(selected_size, selected_base,
					    selected_limit,
					    &dma_contiguous_default_area,
					    fixed);
	}
}
```
selected_size represents size in bytes of the reserved area,
selected_base is base address of the reserved area,
selected_limit is end address of the reserved area ,
the last fixed parameter shows where to place reserved area.
If fixed is 1 we just reserve area with the memblock_reserve, if it is 0 we allocate space with the kmemleak_alloc. 

In the next step we check size_cmdline variable and if it is not equal to -1 we fill all variables which 
you can see above with the values from the cma kernel command line parameter

You can find in this source code file definition of the early parameter:
```
early_param("cma", early_cma);
```
where cma is:
https://www.kernel.org/doc/html/v4.14/admin-guide/kernel-parameters.html
```
 cma=nn[MG]@[start[MG][-end[MG]]]
                        [ARM,X86,KNL]
                        Sets the size of kernel global memory area for
                        contiguous memory allocations and optionally the
                        placement constraint by the physical address range of
                        memory allocations. A value of 0 disables CMA
                        altogether. For more information, see
                        include/linux/dma-contiguous.h
```
If we will not pass cma option to the kernel command line, size_cmdline will be equal to -1. In this way 
we need to calculate size of the reserved area which depends on the following kernel configuration options:
1 CONFIG_CMA_SIZE_SEL_MBYTES - size in megabytes, default global CMA area, which is equal to CMA_SIZE_MBYTES * SZ_1M or CONFIG_CMA_SIZE_MBYTES * 1M;
2 CONFIG_CMA_SIZE_SEL_PERCENTAGE - percentage of total memory;
3 CONFIG_CMA_SIZE_SEL_MIN - use lower value;
4 CONFIG_CMA_SIZE_SEL_MAX - use higher value.

As we calculated the size of the reserved area, we reserve area with the call of the dma_contiguous_reserve_area function which first of all calls:
```
int __init dma_contiguous_reserve_area(phys_addr_t size, phys_addr_t base,
				       phys_addr_t limit, struct cma **res_cma,
				       bool fixed)
{
	int ret;

	ret = cma_declare_contiguous(base, size, limit, 0, 0, fixed,
					"reserved", res_cma);
	if (ret)
		return ret;

	/* Architecture specific contiguous memory fixup. */
	dma_contiguous_early_fixup(cma_get_base(*res_cma),
				cma_get_size(*res_cma));
	return 0;
}
```
The cma_declare_contiguous reserves contiguous area from the given base address with given size. 

After we reserved area for the DMA, next function is the memblock_find_dma_reserve. As you can understand from its name, 
this function counts the reserved pages in the DMA area. This part will not cover all details of the CMA and DMA, 
because they are big. We will see much more details in the special part in the Linux Kernel Memory management which covers contiguous memory allocators and areas.
arch/x86/kernel/setup.c
```
dma_contiguous_reserve(max_pfn_mapped << PAGE_SHIFT);
...
memblock_find_dma_reserve();
```





Initialization of the sparse memory
The next step is the call of the function - x86_init.paging.pagetable_init. If you try to find this 
function in the linux kernel source code, in the end of your search, you will see the following macro:
arch/x86/kernel/setup.c
```
...
x86_init.paging.pagetable_init();
```
arch/x86/include/asm/pgtable_types.h
```
#define native_pagetable_init        paging_init
```

The paging_init function initializes sparse memory and zone sizes. First of all what's zones and what is it Sparsemem.
The Sparsemem is a special foundation in the linux kernel memory manager which used to split memory area 
into different memory banks in the NUMA systems.
arch/x86/mm/init_64.c
```
void __init paging_init(void)
{
	sparse_memory_present_with_active_regions(MAX_NUMNODES);
	sparse_init();

	node_clear_state(0, N_MEMORY);
	if (N_MEMORY != N_NORMAL_MEMORY)
		node_clear_state(0, N_NORMAL_MEMORY);

	zone_sizes_init();
}
```
numa
https://en.wikipedia.org/wiki/Non-uniform_memory_access
Non-uniform memory access (NUMA) is a computer memory design used in multiprocessing,
where the memory access time depends on the memory location relative to the processor. Under NUMA,
a processor can access its own local memory faster than non-local memory 
(memory local to another processor or memory shared between processors). The benefits of NUMA are limited to particular workloads,
notably on servers where the data is often associated strongly with certain tasks or users


As you can see there is call of the sparse_memory_present_with_active_regions function which records
a memory area for every NUMA node to the array of the mem_section structure which contains a pointer 
to the structure of the array of struct page.
mm/page_alloc.c
```
void __init sparse_memory_present_with_active_regions(int nid)
{
	unsigned long start_pfn, end_pfn;
	int i, this_nid;

	for_each_mem_pfn_range(i, nid, &start_pfn, &end_pfn, &this_nid)
		memory_present(this_nid, start_pfn, end_pfn);
}
```
mm/sparse.c
```
void __init memory_present(int nid, unsigned long start, unsigned long end)
{
	unsigned long pfn;

#ifdef CONFIG_SPARSEMEM_EXTREME
	if (unlikely(!mem_section)) {
		unsigned long size, align;

		size = sizeof(struct mem_section*) * NR_SECTION_ROOTS;
		align = 1 << (INTERNODE_CACHE_SHIFT);
		mem_section = memblock_virt_alloc(size, align);
	}
#endif

	start &= PAGE_SECTION_MASK;
	mminit_validate_memmodel_limits(&start, &end);
	for (pfn = start; pfn < end; pfn += PAGES_PER_SECTION) {
		unsigned long section = pfn_to_section_nr(pfn);
		struct mem_section *ms;

		sparse_index_init(section, nid);
		set_section_nid(section, nid);

		ms = __nr_to_section(section);
		if (!ms->section_mem_map) {
			ms->section_mem_map = sparse_encode_early_nid(nid) |
							SECTION_IS_ONLINE;
			section_mark_present(ms);
		}
	}
}
```
include/linux/mmzone.h
```
struct mem_section {
	unsigned long section_mem_map;

	/* See declaration of similar field in struct zone */
	unsigned long *pageblock_flags;
#ifdef CONFIG_PAGE_EXTENSION
	struct page_ext *page_ext;
	unsigned long pad;
#endif
};
```

The next sparse_init function allocates non-linear mem_section and mem_map.
mm/sparse.c
```
/*
 * Allocate the accumulated non-linear sections, allocate a mem_map
 * for each and record the physical to section mapping.
 */
void __init sparse_init(void)
{
 ...
 #ifdef CONFIG_SPARSEMEM_ALLOC_MEM_MAP_TOGETHER
	size2 = sizeof(struct page *) * NR_MEM_SECTIONS;
	map_map = memblock_virt_alloc(size2, 0);
	if (!map_map)
		panic("can not allocate map_map\n");
	alloc_usemap_and_memmap(sparse_early_mem_maps_alloc_node,
							(void *)map_map);
#endif
 ...
 for_each_present_section_nr(0, pnum) {
		usemap = usemap_map[pnum];
		if (!usemap)
			continue;

#ifdef CONFIG_SPARSEMEM_ALLOC_MEM_MAP_TOGETHER
		map = map_map[pnum];
#else
		map = sparse_early_mem_map_alloc(pnum);
#endif
		if (!map)
			continue;

		sparse_init_one_section(__nr_to_section(pnum), pnum, map,
								usemap);
	}
	...
}
```

In the next step we clear state of the movable memory nodes and initialize sizes of zones. 
Every NUMA node is divided into a number of pieces which are called - zones. So, zone_sizes_init function 
from the arch/x86/mm/init.c initializes size of zones.

Again, this part and next parts do not cover this theme in full details. There will be special part about NUMA.




vsyscall mapping
The next step after SparseMem initialization is setting of the trampoline_cr4_features which must contain 
content of the cr4 Control register. First of all we need to check that current CPU has support of the cr4 register and
if it has, we save its content to the mmu_cr4_features which is storage for cr4 in the real mode:
arch/x86/kernel/setup.c
```
mmu_cr4_features = __read_cr4() & ~X86_CR4_PCIDE;
```
Control_register
https://en.wikipedia.org/wiki/Control_register
A control register is a processor register that changes or controls the general behavior of a CPU or other digital device. 
Common tasks performed by control registers include interrupt control, switching the addressing mode, 
paging control, and coprocessor control.


The next function which you can see is map_vsyscal
arch/x86/kernel/setup.c
```
...
map_vsyscall();
```
arch/x86/entry/vsyscall/vsyscall_64.c
```
void __init map_vsyscall(void)
{
	extern char __vsyscall_page;
	unsigned long physaddr_vsyscall = __pa_symbol(&__vsyscall_page);

	if (vsyscall_mode != NONE) {
		__set_fixmap(VSYSCALL_PAGE, physaddr_vsyscall,
			     PAGE_KERNEL_VVAR);
		set_vsyscall_pgtable_user_bits(swapper_pg_dir);
	}

	BUILD_BUG_ON((unsigned long)__fix_to_virt(VSYSCALL_PAGE) !=
		     (unsigned long)VSYSCALL_ADDR);
}
```
This function maps memory space for vsyscalls and depends on CONFIG_X86_VSYSCALL_EMULATION kernel configuration option. 
Actually vsyscall is a special segment which provides fast access to the certain system calls like getcpu, etc.
vsyscalls
https://lwn.net/Articles/446528/

In the beginning of the map_vsyscall we can see definition of two variables. The first is extern variable __vsyscall_page. 
As a extern variable, it defined somewhere in other source code file. todo 语法

And in the end of the map_vsyscall we check that virtual address of the VSYSCALL_PAGE (which is first index in the fixed_addresses) 
is not greater than VSYSCALL_ADDR which is -10UL << 20 or ffffffffff600000 with the BUILD_BUG_ON macro

The __vsyscall_page symbol points to the aligned calls of the vsyscalls as gettimeofday, etc.:
arch/x86/entry/vsyscall/vsyscall_emu_64.S
```
__PAGE_ALIGNED_DATA
	.globl __vsyscall_page
	.balign PAGE_SIZE, 0xcc
	.type __vsyscall_page, @object
__vsyscall_page:

	mov $__NR_gettimeofday, %rax
	syscall
	ret

	.balign 1024, 0xcc
	mov $__NR_time, %rax
	syscall
	ret

	.balign 1024, 0xcc
	mov $__NR_getcpu, %rax
	syscall
	ret
	.balign 4096, 0xcc
	.size __vsyscall_page, 4096
```
The second variable is physaddr_vsyscall which just stores physical address of the __vsyscall_page symbol.
In the next step we check the vsyscall_mode variable, and if it is not equal to NONE, it is EMULATE by default:
```
static enum { EMULATE, NONE } vsyscall_mode =
#ifdef CONFIG_LEGACY_VSYSCALL_NONE
	NONE;
#else
	EMULATE;
#endif
```

And after this check we can see the call of the __set_fixmap function which calls native_set_fixmap with the same parameters:
arch/x86/include/asm/fixmap.h
```
#ifndef CONFIG_PARAVIRT
static inline void __set_fixmap(enum fixed_addresses idx,
				phys_addr_t phys, pgprot_t flags)
{
	native_set_fixmap(idx, phys, flags);
}
#endif
```
arch/x86/mm/pgtable.c
```
void __native_set_fixmap(enum fixed_addresses idx, pte_t pte)
{
	unsigned long address = __fix_to_virt(idx);

	if (idx >= __end_of_fixed_addresses) {
		BUG();
		return;
	}
	set_pte_vaddr(address, pte);
	fixmaps_set++;
}

void native_set_fixmap(enum fixed_addresses idx, phys_addr_t phys,
		       pgprot_t flags)
{
	__native_set_fixmap(idx, pfn_pte(phys >> PAGE_SHIFT, flags));
}
```
Here we can see that native_set_fixmap makes value of Page Table Entry from the given physical address
(physical address of the __vsyscall_page symbol in our case) and calls internal function - __native_set_fixmap. 
Internal function gets the virtual address of the given fixed_addresses index (VSYSCALL_PAGE in our case) and 
checks that given index is not greater than end of the fix-mapped addresses. 
After this we set page table entry with the call of the set_pte_vaddr function and increase count of the fix-mapped addresses. 

Now vsyscall area is in the fix-mapped area. That's all about map_vsyscall, if you do not know anything about fix-mapped addresses, 
you can read Fix-Mapped Addresses and ioremap. We will see more about vsyscalls in the vsyscalls and vdso part.
fix-mapped addresses
https://0xax.gitbooks.io/linux-insides/content/MM/linux-mm-2.html



SMP
https://en.wikipedia.org/wiki/Symmetric_multiprocessing
Symmetric multiprocessing or shared-memory multiprocessing (SMP) involves a multiprocessor computer hardware 
and software architecture where two or more identical processors are connected to a single, shared main memory, 
have full access to all input and output devices, and are controlled by a single operating system instance 
that treats all processors equally, reserving none for special purposes.

Getting the SMP configuration
You may remember how we made a search of the SMP configuration in the previous part. Now we need to get the SMP configuration 
if we found it. For this we check smp_found_config variable which we set in the smp_scan_config function 
(read about it the previous part) and call the get_smp_config function:
arch/x86/kernel/setup.c
```
...
get_smp_config();
```
arch/x86/include/asm/mpspec.h
```
static inline void get_smp_config(void)
{
	x86_init.mpparse.get_smp_config(0);
}
```
arch/x86/kernel/mpparse.c
```
void __init default_get_smp_config(unsigned int early)
{
	struct mpf_intel *mpf;

	if (!smp_found_config)
		return;

	if (!mpf_found)
		return;

	if (acpi_lapic && early)
		return;

	if (acpi_lapic && acpi_ioapic)
		return;

	mpf = early_memremap(mpf_base, sizeof(*mpf));
	if (!mpf) {
		pr_err("MPTABLE: error mapping MP table\n");
		return;
	}

	pr_info("Intel MultiProcessor Specification v1.%d\n",
		mpf->specification);
#if defined(CONFIG_X86_LOCAL_APIC) && defined(CONFIG_X86_32)
	if (mpf->feature2 & (1 << 7)) {
		pr_info("    IMCR and PIC compatibility mode.\n");
		pic_mode = 1;
	} else {
		pr_info("    Virtual Wire compatibility mode.\n");
		pic_mode = 0;
	}
#endif
	
	if (mpf->feature1) {
		if (early) {
			mp_lapic_addr = APIC_DEFAULT_PHYS_BASE;
			return;
		}

		pr_info("Default MP configuration #%d\n", mpf->feature1);
		construct_default_ISA_mptable(mpf->feature1);

	} else if (mpf->physptr) {
		if (check_physptr(mpf, early)) {
			early_memunmap(mpf, sizeof(*mpf));
			return;
		}
	} else
		BUG();

	if (!early)
		pr_info("Processors: %d\n", num_processors);
	
	early_memunmap(mpf, sizeof(*mpf));
}
```
This function defines a pointer to the multiprocessor floating pointer structure - mpf_intel (you can read about it in the previous part) 
and does some checks
Here we can see that multiprocessor configuration was found in the smp_scan_config function or just return from the function if not.
The next check is acpi_lapic and early. And as we did this checks, we start to read the SMP configuration.
arch/x86/kernel/setup.c
```
...
prefill_possible_map();
```
arch/x86/kernel/smpboot.c
```
__init void prefill_possible_map(void)
{
	int i, possible;

	/* No boot processor was found in mptable or ACPI MADT */
	if (!num_processors) {
		if (boot_cpu_has(X86_FEATURE_APIC)) {
			int apicid = boot_cpu_physical_apicid;
			int cpu = hard_smp_processor_id();

			pr_warn("Boot CPU (id %d) not listed by BIOS\n", cpu);

			/* Make sure boot cpu is enumerated */
			if (apic->cpu_present_to_apicid(0) == BAD_APICID &&
			    apic->apic_id_valid(apicid))
				generic_processor_info(apicid, boot_cpu_apic_version);
		}

		if (!num_processors)
			num_processors = 1;
	}

	i = setup_max_cpus ?: 1;
	if (setup_possible_cpus == -1) {
		possible = num_processors;
#ifdef CONFIG_HOTPLUG_CPU
		if (setup_max_cpus)
			possible += disabled_cpus;
#else
		if (possible > i)
			possible = i;
#endif
	} else
		possible = setup_possible_cpus;

	total_cpus = max_t(int, possible, num_processors + disabled_cpus);

	/* nr_cpu_ids could be reduced via nr_cpus= */
	if (possible > nr_cpu_ids) {
		pr_warn("%d Processors exceeds NR_CPUS limit of %u\n",
			possible, nr_cpu_ids);
		possible = nr_cpu_ids;
	}

#ifdef CONFIG_HOTPLUG_CPU
	if (!setup_max_cpus)
#endif
	if (possible > i) {
		pr_warn("%d Processors exceeds max_cpus limit of %u\n",
			possible, setup_max_cpus);
		possible = i;
	}

	nr_cpu_ids = possible;

	pr_info("Allowing %d CPUs, %d hotplug CPUs\n",
		possible, max_t(int, possible - num_processors, 0));

	reset_cpu_possible_mask();

	for (i = 0; i < possible; i++)
		set_cpu_possible(i, true);
}
```
As we finished reading it, the next step is - prefill_possible_map function which makes preliminary filling of the possible CPU's cpumask 
(more about it you can read in the Introduction to the cpumasks).




The rest of the setup_arch
Here we are getting to the end of the setup_arch function. The rest of function of course is important, 
but details about these stuff will not will not be included in this part. We will just take a short look on these functions,
because although they are important as I wrote above, but they cover non-generic kernel features related with the NUMA,
SMP, ACPI and APICs, etc.
First of all, the next call of the init_apic_mappings function.As we can understand this function sets the address of the local APIC.
arch/x86/kernel/setup.c
```
...
init_apic_mappings();
```
arch/x86/kernel/apic/apic.c
```
void __init init_apic_mappings(void)
{
	unsigned int new_apicid;
	apic_check_deadline_errata();
	if (x2apic_mode) {
		boot_cpu_physical_apicid = read_apic_id();
		return;
	}

	/* If no local APIC can be found return early */
	if (!smp_found_config && detect_init_APIC()) {
		/* lets NOP'ify apic operations */
		pr_info("APIC: disable apic facility\n");
		apic_disable();
	} else {
		apic_phys = mp_lapic_addr;

		if (!acpi_lapic && !smp_found_config)
			register_lapic_address(apic_phys);
	}

	new_apicid = read_apic_id();
	if (boot_cpu_physical_apicid != new_apicid) {
		boot_cpu_physical_apicid = new_apicid;
		boot_cpu_apic_version = GET_APIC_VERSION(apic_read(APIC_LVR));
	}
}
```

The next is x86_io_apic_ops.init and this function initializes I/O APIC. Please note that we will see
all details related with APIC in the chapter about interrupts and exceptions handling.
```
io_apic_init_mappings() //4.16相关io apic
```

In the next step we reserve standard I/O resources like DMA, TIMER, FPU, etc., with the call of the
x86_init.resources.reserve_resources function. 
```
x86_init.resources.reserve_resources();
```

Following is mcheck_init function initializes Machine check Exception and the last is register_refined_jiffies 
which registers jiffy (There will be separate chapter about timers in the kernel).
```
mcheck_init();
...
register_refined_jiffies(CLOCK_TICK_RATE);
```
That's all, and now we can back to the start_kernel from the setup_arch.





Back to the main.c
After the setup_arch we can see the call of the mm_init_cpumask function. This function sets the cpumask pointer 
to the memory descriptor cpumask
init/main.c
```
setup_arch(&command_line);
...
mm_init_cpumask(&init_mm);
```
include/linux/mm_types.h
```
static inline void mm_init_cpumask(struct mm_struct *mm)
{
#ifdef CONFIG_CPUMASK_OFFSTACK
	mm->cpu_vm_mask_var = &mm->cpumask_allocation;
#endif
	cpumask_clear(mm->cpu_vm_mask_var);
}
```
we pass memory descriptor of the init process to the mm_init_cpumask and depends on CONFIG_CPUMASK_OFFSTACK 
configuration option we clear TLB switch cpumask
TLB  https://en.wikipedia.org/wiki/Translation_lookaside_buffer
A translation lookaside buffer (TLB) is a memory cache that stores the recent translations of virtual memory to physical memory. 
It is used to reduce the time taken to access a user memory location.It can be called an address-translation cache.

In the next step we can see the call of the following function:
init/main.c
```
setup_command_line(command_line);
```
init/main.c
```
static void __init setup_command_line(char *command_line)
{
	saved_command_line =
		memblock_virt_alloc(strlen(boot_command_line) + 1, 0);
	initcall_command_line =
		memblock_virt_alloc(strlen(boot_command_line) + 1, 0);
	static_command_line = memblock_virt_alloc(strlen(command_line) + 1, 0);
	strcpy(saved_command_line, boot_command_line);
	strcpy(static_command_line, command_line);
}
```
This function takes pointer to the kernel command line allocates a couple of buffers to store command line.
We need a couple of buffers, because one buffer used for future reference and accessing to command line and
one for parameter parsing. We will allocate space for the following buffers:
1 saved_command_line - will contain boot command line;
2 initcall_command_line - will contain boot command line. will be used in the do_initcall_level;
3 static_command_line - will contain command line for parameters parsing.

Here we can see that we allocate space for the three buffers which will contain kernel command line for
the different purposes (read above). And as we allocated space, we store boot_command_line in the saved_command_line 
and command_line (kernel command line from the setup_arch) to the static_command_line.

include/linux/bootmem.h
```
static inline void * __init memblock_virt_alloc(
					phys_addr_t size,  phys_addr_t align)
{
	return memblock_virt_alloc_try_nid(size, align, BOOTMEM_LOW_LIMIT,
					    BOOTMEM_ALLOC_ACCESSIBLE,
					    NUMA_NO_NODE);
}

static inline void * __init memblock_virt_alloc_try_nid(phys_addr_t size,
	phys_addr_t align, phys_addr_t min_addr, phys_addr_t max_addr, int nid)
{
	return __alloc_bootmem_node_high(NODE_DATA(nid), size, align,
					  min_addr);
}
```
mm/nobootmem.c
```
void * __init __alloc_bootmem_node_high(pg_data_t *pgdat, unsigned long size,
				   unsigned long align, unsigned long goal)
{
	return __alloc_bootmem_node(pgdat, size, align, goal);
}

//最终调用
static void * __init __alloc_memory_core_early(int nid, u64 size, u64 align,
					u64 goal, u64 limit)
{
	void *ptr;
	u64 addr;
	ulong flags = choose_memblock_flags();

	if (limit > memblock.current_limit)
		limit = memblock.current_limit;

again:
	addr = memblock_find_in_range_node(size, align, goal, limit, nid,
					   flags);
	if (!addr && (flags & MEMBLOCK_MIRROR)) {
		flags &= ~MEMBLOCK_MIRROR;
		pr_warn("Could not allocate %pap bytes of mirrored memory\n",
			&size);
		goto again;
	}
	if (!addr)
		return NULL;

	if (memblock_reserve(addr, size))
		return NULL;

	ptr = phys_to_virt(addr);
	memset(ptr, 0, size);
	/*
	 * The min_count is set to 0 so that bootmem allocated blocks
	 * are never reported as leaks.
	 */
	kmemleak_alloc(ptr, size, 0, 0);
	return ptr;
}
```
We will allocate space with the memblock_virt_alloc function. This function calls memblock_virt_alloc_try_nid 
which allocates boot memory block with memblock_reserve if slab is not available or uses kzalloc_node
(more about it will be in the linux memory management chapter). The memblock_virt_alloc uses BOOTMEM_LOW_LIMIT 
(physical address of the (PAGE_OFFSET + 0x1000000) value) and BOOTMEM_ALLOC_ACCESSIBLE (equal to the current 
value of the memblock.current_limit) as minimum address of the memory region and maximum address of the memory region.

SLAB https://en.wikipedia.org/wiki/Slab_allocation
Slab allocation is a memory management mechanism intended for the efficient memory allocation of objects. 
In comparison with earlier mechanisms, it reduces fragmentation caused by allocations and deallocations. 
This technique is used for retaining allocated memory containing a data object of a certain type for 
reuse upon subsequent allocations of objects of the same type. It is analogous to an object pool, but only applies to memory, 
not other resources.



The next function after the setup_command_line is the setup_nr_cpu_ids. This function setting nr_cpu_ids (number of CPUs) 
according to the last bit in the cpu_possible_mask (more about it you can read in the chapter describes cpumasks concept).
init/main.c
```
setup_nr_cpu_ids();
```
kernel/smp.c
```
void __init setup_nr_cpu_ids(void)
{
	nr_cpu_ids = find_last_bit(cpumask_bits(cpu_possible_mask),NR_CPUS) + 1;
}
```
Here nr_cpu_ids represents number of CPUs, NR_CPUS represents the maximum number of CPUs which we can set in configuration time:
```
linux kernel configuration
processor type and features
...
Maximum number of CPUS
``` 
todo  测试配置


Actually we need to call this function, because NR_CPUS can be greater than actual amount of the CPUs in the your computer.
Here we can see that we call find_last_bit function and pass two parameters to it:
cpu_possible_mask bits;
maximum number of CPUS.

lib/find_bit.c
```
unsigned long find_last_bit(const unsigned long *addr, unsigned long size)
{
	if (size) {
		unsigned long val = BITMAP_LAST_WORD_MASK(size);
		unsigned long idx = (size-1) / BITS_PER_LONG;

		do {
			val &= addr[idx];
			if (val)
				return idx * BITS_PER_LONG + __fls(val);

			val = ~0ul;
		} while (idx--);
	}
	return size;
}
```
In the setup_arch we can find the call of the prefill_possible_map function which calculates and writes to 
the cpu_possible_mask actual number of the CPUs. 
We call the find_last_bit function which takes the address and maximum size to search and returns bit number of the first set bit. 
We passed cpu_possible_mask bits and maximum number of the CPUs.
First of all the find_last_bit function splits given unsigned long address to the words  这里words是idx

where BITS_PER_LONG is 64 on the x86_64. As we got amount of words in the given size of the search data, 
we need to check is given size does not contain partial words with the following check

if it contains partial word, we mask the last word and check it. If the last word is not zero, 
it means that current word contains at least one set bit.

Word
https://en.wikipedia.org/wiki/Word_%28computer_architecture%29
In computing, a word is the natural unit of data used by a particular processor design. A word is a fixed-sized 
datum handled as a unit by the instruction set or the hardware of the processor. The number of bits or digits
in a word (the word size, word width, or word length) is an important characteristic of any specific processor design 
or computer architecture.


arch/x86/include/asm/bitops.h
```
static __always_inline unsigned long __fls(unsigned long word)
{
	asm("bsr %1,%0"
	    : "=r" (word)
	    : "rm" (word));
	return word;
}
```
Here you can see __fls function which returns last set bit in a given word with help of the bsr instruction
The bsr instruction which scans the given operand for first bit set. If the last word is not partial 
  we going through the all words in the given address and trying to find first set bit

Here we put the last word to the tmp variable and check that tmp contains at least one set bit. If a set bit found, 
we return the number of this bit. If no one words do not contains set bit we just return given size

After this nr_cpu_ids will contain the correct amount of the available CPUs.

