
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

initrd to the direct mapping area with the relocate_initrd function. In the start of the relocate_initrd 
function we try to find a free area with the memblock_find_in_range function: