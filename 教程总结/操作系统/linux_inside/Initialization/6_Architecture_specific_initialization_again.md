
https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-6.html


The next function after x86_configure_nx is parse_early_param. and as you can understand from its name, 
this function parses kernel command line and setups different services depends on the given parameters
(all kernel command line parameters you can find are in the Documentation/kernel-parameters.txt).
https://github.com/torvalds/linux/blob/master/Documentation/admin-guide/kernel-parameters.rst

You may remember how we setup earlyprintk in the earliest part. On the early stage we looked for kernel parameters
and their value with the cmdline_find_option function and __cmdline_find_option, __cmdline_find_option_bool
helpers from the arch/x86/boot/cmdline.c. There we're in the generic kernel part which does not depend on architecture 
and here we use another approach. If you are reading linux kernel source code, you already note calls like this:
```
early_param("gbpages", parse_direct_gbpages_on);
```
early_param macro takes two parameters:
1 command line parameter name;
2 function which will be called if given parameter is passed.
and defined as:
include/linux/init.h
```
#define early_param(str, fn)						\
	__setup_param(str, fn, fn, 1)
	

#define __setup_param(str, unique_id, fn, early)			\
	static const char __setup_str_##unique_id[] __initconst		\
		__aligned(1) = str; 					\
	static struct obs_kernel_param __setup_##unique_id		\
		__used __section(.init.setup)				\
		__attribute__((aligned((sizeof(long)))))		\
		= { __setup_str_##unique_id, fn, early }	
```
This macro defines __setup_str_*_id variable (where * depends on given function name) and assigns it to
the given command line parameter name. In the next line we can see definition of the __setup_* variable 
which type is obs_kernel_param and its initialization. obs_kernel_param structure defined as:
include/linux/init.h
```
struct obs_kernel_param {
	const char *str;
	int (*setup_func)(char *);
	int early;
};
```
and contains three fields:
1 name of the kernel parameter;
2 function which setups something depend on parameter;
3 field determines is parameter early (1) or not (0).

Note that __set_param macro defines with __section(.init.setup) attribute. It means that all __setup_str_* will 
be placed in the .init.setup section, moreover, they will be placed between __setup_start and __setup_end:
include/asm-generic/vmlinux.lds.h
```
#define INIT_SETUP(initsetup_align)					\
		. = ALIGN(initsetup_align);				\
		VMLINUX_SYMBOL(__setup_start) = .;			\
		KEEP(*(.init.setup))					\
		VMLINUX_SYMBOL(__setup_end) = .;
```

Now we know how parameters are defined, let's back to the parse_early_param implementation:
arch/x86/kernel/setup.c
```
...
parse_early_param
```
https://github.com/torvalds/linux/blob/v4.16/init/main.c
```
/* Arch code calls this early on, or if not, just before other parsing. */
void __init parse_early_param(void)
{
	static int done __initdata;
	static char tmp_cmdline[COMMAND_LINE_SIZE] __initdata;

	if (done)
		return;

	/* All fall through to do_early_param. */
	strlcpy(tmp_cmdline, boot_command_line, COMMAND_LINE_SIZE);
	parse_early_options(tmp_cmdline);
	done = 1;
}

void __init parse_early_options(char *cmdline)
{
	parse_args("early options", cmdline, NULL, 0, 0, 0, NULL,
		   do_early_param);
}

/* Check for early params. */
static int __init do_early_param(char *param, char *val,
				 const char *unused, void *arg)
{
	const struct obs_kernel_param *p;
	for (p = __setup_start; p < __setup_end; p++) {
		if ((p->early && parameq(param, p->str)) ||
		    (strcmp(param, "console") == 0 &&
		     strcmp(p->str, "earlycon") == 0)
		) {
			if (p->setup_func(val) != 0)
				pr_warn("Malformed early option '%s'\n", param);
		}
	}
	/* We accept everything at this stage. */
	return 0;
}
```
The parse_early_param function defines two static variables. First done check that parse_early_param already called 
and the second is temporary storage for kernel command line.
After this we copy boot_command_line to the temporary command line which we just defined and call the parse_early_options 
function from the same source code main.c file. 

parse_early_options calls the parse_args function from the kernel/params.c where parse_args parses given command line
  and calls do_early_param function.

This function goes from the __setup_start to __setup_end, and calls the function from the obs_kernel_param 
if a parameter is early. After this all services which are depend on early command line parameters were setup 
and the next call after the parse_early_param is x86_report_nx.

arch/x86/kernel/setup.c
```
...
parse_early_param();
...
x86_report_nx();
```
arch/x86/mm/setup_nx.c
```
void __init x86_report_nx(void)
{
	if (!boot_cpu_has(X86_FEATURE_NX)) {
		printk(KERN_NOTICE "Notice: NX (Execute Disable) protection "
		       "missing in CPU!\n");
	} else {
#if defined(CONFIG_X86_64) || defined(CONFIG_X86_PAE)
		if (disable_nx) {
			printk(KERN_INFO "NX (Execute Disable) protection: "
			       "disabled by kernel command line option\n");
		} else {
			printk(KERN_INFO "NX (Execute Disable) protection: "
			       "active\n");
		}
#else
		/* 32bit non-PAE kernel, NX cannot be used */
		printk(KERN_NOTICE "Notice: NX (Execute Disable) protection "
		       "cannot be enabled: non-PAE kernel!\n");
#endif
	}
}
```
we already set NX-bit with the x86_configure_nx. The next x86_report_nx function
just prints information about the NX. Note that we call x86_report_nx not right after the x86_configure_nx,
but after the call of the parse_early_param. The answer is simple: we call it after the parse_early_param 
because the kernel support noexec parameter:
```
noexec        [X86]
            On X86-32 available only on PAE configured kernels.
            noexec=on: enable non-executable mappings (default)
            noexec=off: disable non-executable mappings
```
We can see it in the booting time:
```
bootconsole [earlyser0] enabled
NX(Execute Disable) protection: active
SMBIOS 2.8 present.
```


After this we can see call of the:
arch/x86/kernel/setup.c
```
	x86_report_nx();

	/* after early param, so could get panic from serial */
	memblock_x86_reserve_range_setup_data();
	
	
static void __init memblock_x86_reserve_range_setup_data(void)
{
	struct setup_data *data;
	u64 pa_data;

	pa_data = boot_params.hdr.setup_data;
	while (pa_data) {
		data = early_memremap(pa_data, sizeof(*data));
		memblock_reserve(pa_data, sizeof(*data) + data->len);
		pa_data = data->next;
		early_memunmap(data, sizeof(*data));
	}
}	
```

This function  remaps memory for the setup_data and reserved memory block for the setup_data (more about setup_data 
you can read in the previous part and about ioremap and memblock you can read in the Linux kernel memory management).

In the next step we can see following conditional statement:
arch/x86/kernel/setup.c
```
...
memblock_x86_reserve_range_setup_data();

	if (acpi_mps_check()) {
#ifdef CONFIG_X86_LOCAL_APIC
		disable_apic = 1;
#endif
		setup_clear_cpu_cap(X86_FEATURE_APIC);
	}
```

The first acpi_mps_check function depends on CONFIG_X86_LOCAL_APIC and CONFIG_x86_MPPARSE configuration options:
arch/x86/kernel/acpi/boot.c
```
int __init acpi_mps_check(void)
{
#if defined(CONFIG_X86_LOCAL_APIC) && !defined(CONFIG_X86_MPPARSE)
/* mptable code is not built-in*/
	if (acpi_disabled || acpi_noirq) {
		printk(KERN_WARNING "MPS support code is not built-in.\n"
		       "Using acpi=off or acpi=noirq or pci=noacpi "
		       "may have problem\n");
		return 1;
	}
#endif
	return 0;
}
```
It checks the built-in MPS or MultiProcessor Specification table. If CONFIG_X86_LOCAL_APIC is set and CONFIG_x86_MPPAARSE is not set, 
acpi_mps_check prints warning message if the one of the command line options: acpi=off, acpi=noirq or pci=noacpi passed 
to the kernel. If acpi_mps_check returns 1 it means that we disable local APIC and clear X86_FEATURE_APIC bit 
in the of the current CPU with the setup_clear_cpu_cap macro. (more about CPU mask you can read in the CPU masks)

https://0xax.gitbooks.io/linux-insides/content/Concepts/linux-cpu-2.html

MultiProcessor_Specification https://en.wikipedia.org/wiki/MultiProcessor_Specification
The MultiProcessor Specification (MPS) for the x86 architecture is an open standard describing enhancements 
to both operating systems and firmware, which will allow them to work with x86-compatible processors 
in a multi-processor configuration. MPS covers Advanced Programmable Interrupt Controller (APIC) architectures

APIC  https://en.wikipedia.org/wiki/Advanced_Programmable_Interrupt_Controller
In computing, Intel's Advanced Programmable Interrupt Controller (APIC) is a family of programmable interrupt controllers. 
As its name suggests, the APIC is more advanced than Intel's 8259 Programmable Interrupt Controller (PIC),
particularly enabling the construction of multiprocessor systems. It is one of several architectural designs 
intended to solve interrupt routing efficiency issues in multiprocessor computer systems.



Early PCI dump

PCI  http://en.wikipedia.org/wiki/Conventional_PCI
Peripheral Component Interconnect (PCI) is a local computer bus for attaching hardware devices in a computer 
and is part of the PCI Local Bus standard. The PCI bus supports the functions found on a processor bus but 
in a standardized format that is independent of any given processor's native bus.

In the next step we make a dump of the PCI devices with the following code:
arch/x86/kernel/setup.c
```
...
#ifdef CONFIG_PCI
	if (pci_early_dump_regs)
		early_dump_pci_devices();
#endif
```
pci_early_dump_regs  depends on the kernel command line parameter: pci=earlydump
arch/x86/pci/common.c
```
unsigned int pci_early_dump_regs;
...
char *__init pcibios_setup(char *str)
{
  ...
  else if (!strcmp(str, "earlydump")) {
		pci_early_dump_regs = 1;
		return NULL;
	}
...	
}

```
drivers/pci/pci.c
```
static int __init pci_setup(char *str)
{
	while (str) {
		char *k = strchr(str, ',');
		if (k)
			*k++ = 0;
		if (*str && (str = pcibios_setup(str)) && *str) {
			if (!strcmp(str, "nomsi")) {
				pci_no_msi();
			} else if (!strcmp(str, "noaer")) {
...			
```
pci_setup function gets the string after the pci= and analyzes it. This function calls pcibios_setup 
which defined as __weak   and every architecture defines the same function 
which overrides __weak analog. For example x86_64 architecture-dependent version is in the arch/x86/pci/common.c:
todo __weak
```
char *__init pcibios_setup(char *str)
{
}
```
So, if CONFIG_PCI option is set and we passed pci=earlydump option to the kernel command line, 
next function which will be called - early_dump_pci_devices  

arch/x86/pci/early.c
```
int early_pci_allowed(void)
{
	return (pci_probe & (PCI_PROBE_CONF1|PCI_PROBE_NOEARLY)) ==
			PCI_PROBE_CONF1;
}

void early_dump_pci_device(u8 bus, u8 slot, u8 func)
{
	int i;
	int j;
	u32 val;

	printk(KERN_INFO "pci 0000:%02x:%02x.%d config space:",
	       bus, slot, func);

	for (i = 0; i < 256; i += 4) {
		if (!(i & 0x0f))
			printk("\n  %02x:",i);

		val = read_pci_config(bus, slot, func, i); //汇编读取
		for (j = 0; j < 4; j++) {
			printk(" %02x", val & 0xff);
			val >>= 8;
		}
	}
	printk("\n");
}


void early_dump_pci_devices(void)
{
	unsigned bus, slot, func;

	if (!early_pci_allowed())
		return;

	for (bus = 0; bus < 256; bus++) {
		for (slot = 0; slot < 32; slot++) {
			for (func = 0; func < 8; func++) {
				u32 class;
				u8 type;

				class = read_pci_config(bus, slot, func,
							PCI_CLASS_REVISION);
				if (class == 0xffffffff)
					continue;

				early_dump_pci_device(bus, slot, func);

				if (func == 0) {
					type = read_pci_config_byte(bus, slot,
								    func,
							       PCI_HEADER_TYPE);
					if (!(type & 0x80))
						break;
				}
			}
		}
	}
}
```
This function checks noearly pci parameter and returns if it was passed. Each PCI domain can host up to 256 buses 
and each bus hosts up to 32 devices. So, we goes in a loop
and read the pci config with the read_pci_config function.

That's all. We will not go deep in the pci details, but will see more details in the special Drivers/PCI part. 
//这个章节不存在了



Finish with memory parsing
After the early_dump_pci_devices, there are a couple of function related with available memory and e820 
which we collected in the First steps in the kernel setup part:
arch/x86/kernel/setup.c
```
e820__reserve_setup_data();
e820__finish_early_params();
...
e820_add_kernel_range();
trim_bios_range();
..
max_pfn = e820__end_of_ram_pfn();
```
arch/x86/kernel/e820.c
```
void __init e820__reserve_setup_data(void)
{
	struct setup_data *data;
	u64 pa_data;

	pa_data = boot_params.hdr.setup_data;
	if (!pa_data)
		return;

	while (pa_data) {
		data = early_memremap(pa_data, sizeof(*data));
		e820__range_update(pa_data, sizeof(*data)+data->len, E820_TYPE_RAM, E820_TYPE_RESERVED_KERN);
		e820__range_update_kexec(pa_data, sizeof(*data)+data->len, E820_TYPE_RAM, E820_TYPE_RESERVED_KERN);
		pa_data = data->next;
		early_memunmap(data, sizeof(*data));
	}

	e820__update_table(e820_table);
	e820__update_table(e820_table_kexec);

	pr_info("extended physical RAM map:\n");
	e820__print_table("reserve setup_data");
}

void __init e820__finish_early_params(void)
{
	if (userdef) {
		if (e820__update_table(e820_table) < 0)
			early_panic("Invalid user supplied memory map");

		pr_info("user-defined physical RAM map:\n");
		e820__print_table("user");
	}
}
```
the first function is e820_reserve_setup_data. This function does almost the same as memblock_x86_reserve_range_setup_data which we saw above,
but it also calls e820_update_range which adds new regions to the e820map with the given type which is E820_RESERVED_KERN in our case. 

Besides this two functions we can see a couple of functions related to the e820. You can see it in the listing above.
e820_add_kernel_range function takes the physical address of the kernel start and end:
arch/x86/kernel/setup.c
```
static void __init e820_add_kernel_range(void)
{
	u64 start = __pa_symbol(_text);
	u64 size = __pa_symbol(_end) - start;

	if (e820__mapped_all(start, start + size, E820_TYPE_RAM))
		return;

	pr_warn(".text .data .bss are not marked as E820_TYPE_RAM!\n");
	e820__range_remove(start, size, E820_TYPE_RAM, 0);
	e820__range_add(start, size, E820_TYPE_RAM);
}
```
checks that .text .data and .bss marked as E820RAM in the e820map and prints the warning message if not.

The next function trm_bios_range update first 4096 bytes in e820Map as E820_RESERVED and sanitizes it again
  with the call of the sanitize_e820_map.
arch/x86/kernel/setup.c
```
static void __init trim_bios_range(void)
{
	e820__range_update(0, PAGE_SIZE, E820_TYPE_RAM, E820_TYPE_RESERVED);

	e820__range_remove(BIOS_BEGIN, BIOS_END - BIOS_BEGIN, E820_TYPE_RAM, 1);

	e820__update_table(e820_table);
}
```
After this we get the last page frame number with the call of the e820_end_of_ram_pfn function. 
Every memory page has a unique number - Page frame number and e820_end_of_ram_pfn function returns the maximum 
with the call of the e820_end_pfn:
arch/x86/kernel/e820.c
```
unsigned long __init e820__end_of_ram_pfn(void)
{
	return e820_end_pfn(MAX_ARCH_PFN, E820_TYPE_RAM);
}

/*
 * Find the highest page frame number we have available
 */
static unsigned long __init e820_end_pfn(unsigned long limit_pfn, enum e820_type type)
{
	int i;
	unsigned long last_pfn = 0;
	unsigned long max_arch_pfn = MAX_ARCH_PFN;

	for (i = 0; i < e820_table->nr_entries; i++) {
		struct e820_entry *entry = &e820_table->entries[i];
		unsigned long start_pfn;
		unsigned long end_pfn;

		if (entry->type != type)
			continue;

		start_pfn = entry->addr >> PAGE_SHIFT;
		end_pfn = (entry->addr + entry->size) >> PAGE_SHIFT;

		if (start_pfn >= limit_pfn)
			continue;
		if (end_pfn > limit_pfn) {
			last_pfn = limit_pfn;
			break;
		}
		if (end_pfn > last_pfn)
			last_pfn = end_pfn;
	}

	if (last_pfn > max_arch_pfn)
		last_pfn = max_arch_pfn;

	pr_info("e820: last_pfn = %#lx max_arch_pfn = %#lx\n",
			 last_pfn, max_arch_pfn);
	return last_pfn;
}
```
where e820_end_pfn takes maximum page frame number on the certain architecture (MAX_ARCH_PFN is 0x400000000 for x86_64).
In the e820_end_pfn we go through the all e820 slots and check that e820 entry has E820_RAM  type 
because we calculate page frame numbers only for these types, gets the base address and end 
address of the page frame number for the current e820 entry and makes some checks for these addresses
e820_type
arch/x86/include/asm/e820/types.h
```
enum e820_type {
	E820_TYPE_RAM		= 1,
	E820_TYPE_RESERVED	= 2,
	E820_TYPE_ACPI		= 3,
	E820_TYPE_NVS		= 4,
	E820_TYPE_UNUSABLE	= 5,
	E820_TYPE_PMEM		= 7,

	/*
	 * This is a non-standardized way to represent ADR or
	 * NVDIMM regions that persist over a reboot.
	 *
	 * The kernel will ignore their special capabilities
	 * unless the CONFIG_X86_PMEM_LEGACY=y option is set.
	 *
	 * ( Note that older platforms also used 6 for the same
	 *   type of memory, but newer versions switched to 12 as
	 *   6 was assigned differently. Some time they will learn... )
	 */
	E820_TYPE_PRAM		= 12,

	/*
	 * Reserved RAM used by the kernel itself if
	 * CONFIG_INTEL_TXT=y is enabled, memory of this type
	 * will be included in the S3 integrity calculation
	 * and so should not include any memory that the BIOS
	 * might alter over the S3 transition:
	 */
	E820_TYPE_RESERVED_KERN	= 128,
};
```
After this we check that last_pfn which we got in the loop is not greater that maximum page frame number 
for the certain architecture (x86_64 in our case), print information about last page frame number and return it. 
We can see the last_pfn in the dmesg output:  todo
```
[    0.000000] e820: last_pfn = 0x41f000 max_arch_pfn = 0x400000000
```

After this, as we have calculated the biggest page frame number, we calculate max_low_pfn which is the biggest 
page frame number in the low memory or below first 4 gigabytes. If installed more than 4 gigabytes of RAM,
max_low_pfn will be result of the e820_end_of_low_ram_pfn function which does the same e820_end_of_ram_pfn 
but with 4 gigabytes limit, in other way max_low_pfn will be the same as max_pfn:
arch/x86/kernel/setup.c
```
if (max_pfn > (1UL<<(32 - PAGE_SHIFT)))
		max_low_pfn = e820__end_of_low_ram_pfn();
	else
		max_low_pfn = max_pfn;

	high_memory = (void *)__va(max_pfn * PAGE_SIZE - 1) + 1;
```
Next we calculate high_memory (defines the upper bound on direct map memory) with __va macro which 
 returns a virtual address by the given physical memory



DMI
https://en.wikipedia.org/wiki/Desktop_Management_Interface
The Desktop Management Interface (DMI) generates a standard framework for managing and tracking components in a desktop,
notebook or server computer, by abstracting these components from the software that manages them.

DMI scanning
The next step after manipulations with different memory regions and e820 slots is collecting information about computer. 
We will get all information with the Desktop Management Interface and following functions:
manipulations  操作；操作法；管理措施；操作处理；处理; 变换; 操纵证券市场；操纵证券交易
arch/x86/kernel/setup.c
```
...
dmi_scan_machine();
dmi_memdev_walk();
```
drivers/firmware/dmi_scan.c
```
void __init dmi_scan_machine(void)
{
char __iomem *p, *q;
char buf[32];
    if (efi_enabled(EFI_CONFIG_TABLES)) {
    ...
    }else if (IS_ENABLED(CONFIG_DMI_SCAN_MACHINE_NON_EFI_FALLBACK)) {
        p = dmi_early_remap(0xF0000, 0x10000);
		if (p == NULL)
			goto error;
		memcpy_fromio(buf, p, 16);
		for (q = p + 16; q < p + 0x10000; q += 16) {
			memcpy_fromio(buf + 16, q, 16);
			if (!dmi_smbios3_present(buf)) {
				dmi_available = 1;
				dmi_early_unmap(p, 0x10000);
				return;
			}
			memcpy(buf, buf + 16, 16);
		}
	    memset(buf, 0, 16);
		for (q = p; q < p + 0x10000; q += 16) {
			memcpy_fromio(buf + 16, q, 16);
			if (!dmi_present(buf)) {
				dmi_available = 1;
				dmi_early_unmap(p, 0x10000);
				return;
			}
			memcpy(buf, buf + 16, 16);
		}
		dmi_early_unmap(p, 0x10000);
    }
}
```

This function goes through the System Management BIOS structures and extracts information. There are two ways specified
to gain access to the SMBIOS table: get the pointer to the SMBIOS table from the EFI's configuration table 
and scanning the physical memory between 0xF0000 and 0xFFFFF addresses, 0x10000 bytes totally.

https://en.wikipedia.org/wiki/UEFI
Unified Extensible Firmware Interface (UEFI, /ˈjuːɪfaɪ/ or as an acronym) is a specification that defines
the architecture of the platform firmware used for booting the computer hardware and its interface for 
interaction with the operating system.

Let's look on the second approach. dmi_scan_machine function remaps memory addresses start from 0xF0000 to 
0xFFFFF with size of 0x10000 bytes with the dmi_early_remap which just expands to the early_ioremap
it iterates over all DMI header address and find search _SM_ string:

_SM_ string must be between 000F0000h and 0x000FFFFF. Here we copy 16 bytes to the buf with memcpy_fromio 
which is the same memcpy and execute dmi_smbios3_present and dmi_present on the buffer. These functions check 
that first 4 bytes is _SM_ string, get SMBIOS version and gets _DMI_ attributes as DMI structure table length, 
table address and etc...
drivers/firmware/dmi_scan.c
```
static int __init dmi_smbios3_present(const u8 *buf)
{
	if (memcmp(buf, "_SM3_", 5) == 0 &&
	    buf[6] < 32 && dmi_checksum(buf, buf[6])) {
		dmi_ver = get_unaligned_be32(buf + 6) & 0xFFFFFF;
		dmi_num = 0;			/* No longer specified */
		dmi_len = get_unaligned_le32(buf + 12);
		dmi_base = get_unaligned_le64(buf + 16);
		smbios_entry_point_size = buf[6];
		memcpy(smbios_entry_point, buf, smbios_entry_point_size);

		if (dmi_walk_early(dmi_decode) == 0) {
			pr_info("SMBIOS %d.%d.%d present.\n",
				dmi_ver >> 16, (dmi_ver >> 8) & 0xFF,
				dmi_ver & 0xFF);
			dmi_format_ids(dmi_ids_string, sizeof(dmi_ids_string));
			pr_info("DMI: %s\n", dmi_ids_string);
			return 0;
		}
	}
	return 1;
}
```


After one of these functions finish, you will see the result of it in the dmesg output:  todo
```
[    0.000000] SMBIOS 2.7 present.
[    0.000000] DMI: Gigabyte Technology Co., Ltd. Z97X-UD5H-BK/Z97X-UD5H-BK, BIOS F6 06/17/2014
```

In the end of the dmi_scan_machine, we unmap the previously remapped memory:
```
dmi_early_unmap(p, 0x10000);
```

The second function is - dmi_memdev_walk. As you can understand it goes over memory devices.
```
void __init dmi_memdev_walk(void)
{
	if (!dmi_available)
		return;

	if (dmi_walk_early(count_mem_devices) == 0 && dmi_memdev_nr) {
		dmi_memdev = dmi_alloc(sizeof(*dmi_memdev) * dmi_memdev_nr);
		if (dmi_memdev)
			dmi_walk_early(save_mem_devices);
	}
}
```
It checks that DMI available (we got it in the previous function - dmi_scan_machine) and collects information 
about memory devices with dmi_walk_early and dmi_alloc which defined as:
arch/x86/include/asm/dmi.h
```
static __always_inline __init void *dmi_alloc(unsigned len)
{
	return extend_brk(len, sizeof(int));
}
```
arch/x86/kernel/setup.c
```
void * __init extend_brk(size_t size, size_t align)
{
	size_t mask = align - 1;
	void *ret;

	BUG_ON(_brk_start == 0);
	BUG_ON(align & mask);

	_brk_end = (_brk_end + mask) & ~mask;
	BUG_ON((char *)(_brk_end + size) > __brk_limit);

	ret = (void *)_brk_end;
	_brk_end += size;

	memset(ret, 0, size);

	return ret;
}
```



SMP
https://en.wikipedia.org/wiki/Symmetric_multiprocessing
Symmetric multiprocessing or shared-memory multiprocessing (SMP) involves a multiprocessor computer hardware 
and software architecture where two or more identical processors are connected to a single, shared main memory, 
have full access to all input and output devices, and are controlled by a single operating system instance 
that treats all processors equally, reserving none for special purposes. Most multiprocessor systems today
use an SMP architecture.

SMP config
The next step is parsing of the SMP configuration. We do it with the call of the find_smp_config function which just calls function:
arch/x86/kernel/setup.c
```
find_smp_config();
```
arch/x86/include/asm/mpspec.h
```
static inline void find_smp_config(void)
{
	x86_init.mpparse.find_smp_config();
}
```
arch/x86/kernel/x86_init.c
```
struct x86_init_ops x86_init __initdata = {
...
	.mpparse = {
		.mpc_record		= x86_init_uint_noop,
		.setup_ioapic_ids	= x86_init_noop,
		.mpc_apic_id		= default_mpc_apic_id,
		.smp_read_mpc_oem	= default_smp_read_mpc_oem,
		.mpc_oem_bus_info	= default_mpc_oem_bus_info,
		.find_smp_config	= default_find_smp_config,
		.get_smp_config		= default_get_smp_config,
	},
...
```
x86_init.mpparse.find_smp_config is the default_find_smp_config function  
In the default_find_smp_config function we are scanning a couple of memory regions for SMP config and return if they are found:
arch/x86/kernel/mpparse.c
```
void __init default_find_smp_config(void)
{
	unsigned int address;
	if (smp_scan_config(0x0, 0x400) ||
	    smp_scan_config(639 * 0x400, 0x400) ||
	    smp_scan_config(0xF0000, 0x10000))
		return;
	
	address = get_bios_ebda();
	if (address)
		smp_scan_config(address, 0x400);
}

static int __init smp_scan_config(unsigned long base, unsigned long length)
{
	unsigned int *bp;
	struct mpf_intel *mpf;
	int ret = 0;
	
}
```
In the default_find_smp_config function we are scanning a couple of memory regions for SMP config and return if they are found
First of all smp_scan_config function defines a couple of variables:
bp is virtual address of the memory region where we will scan SMP config, mpf is the pointer to the mpf_intel structure

All information stores in the multiprocessor configuration data structure. mpf_intel presents this structure and looks:
arch/x86/include/asm/mpspec_def.h
```
/* Intel MP Floating Pointer Structure */
struct mpf_intel {
	char signature[4];		/* "_MP_"			*/
	unsigned int physptr;		/* Configuration table address	*/
	unsigned char length;		/* Our length (paragraphs)	*/
	unsigned char specification;	/* Specification version	*/
	unsigned char checksum;		/* Checksum (makes sum 0)	*/
	unsigned char feature1;		/* Standard or configuration ?	*/
	unsigned char feature2;		/* Bit7 set for IMCR|PIC	*/
	unsigned char feature3;		/* Unused (0)			*/
	unsigned char feature4;		/* Unused (0)			*/
	unsigned char feature5;		/* Unused (0)			*/
};
```
As we can read in the documentation - one of the main functions of the system BIOS is to construct 
the MP floating pointer structure and the MP configuration table. And operating system must have access 
to this information about the multiprocessor configuration and mpf_intel stores the physical address 
(look at second parameter) of the multiprocessor configuration table.
结构体的含义
Mp Floating Pointer Structure
https://www.manualslib.com/manual/77733/Intel-Multiprocessor.html?page=39
MultiProcessor Specification
https://github.com/x86-8/x86-8-docs/blob/master/SPEC/intel/24201606-Multiprocessor%20Specification.pdf

So, smp_scan_config going in a loop through the given memory range and tries to find MP floating pointer structure there.
It checks that current byte points to the SMP signature, checks checksum, checks if mpf->specification is 
1 or 4(it must be 1 or 4 by specification) in the loop:
```
static int __init smp_scan_config(unsigned long base, unsigned long length)
{
while (length > 0) {
		bp = early_memremap(base, length);
		mpf = (struct mpf_intel *)bp;
		if ((*bp == SMP_MAGIC_IDENT) &&
		    (mpf->length == 1) &&
		    !mpf_checksum((unsigned char *)bp, 16) &&
		    ((mpf->specification == 1)
		     || (mpf->specification == 4))) {
            ....
			memblock_reserve(base, sizeof(*mpf));
			if (mpf->physptr)
				smp_reserve_memory(mpf);

			ret = 1;
		}
		....
	}
}
```
reserves given memory block if search is successful with memblock_reserve and reserves physical address 
of the multiprocessor configuration table. You can find documentation about this in the - MultiProcessor Specification. 
You can read More details in the special part about SMP.




routines
常规; 无聊; (演出中的)一套动作，一系列笑话(等); 正常顺序; 生活乏味
Additional early memory initialization routines
In the next step of the setup_arch we can see the call of the early_alloc_pgt_buf function which 
allocates the page table buffer for early stage. The page table buffer will be placed in the brk area.
arch/x86/kernel/setup.c
```
...
early_alloc_pgt_buf
```
arch/x86/mm/init.c
```
void  __init early_alloc_pgt_buf(void)
{
	unsigned long tables = INIT_PGT_BUF_SIZE;
	phys_addr_t base;

	base = __pa(extend_brk(tables, PAGE_SIZE));

	pgt_buf_start = base >> PAGE_SHIFT;
	pgt_buf_end = pgt_buf_start;
	pgt_buf_top = pgt_buf_start + (tables >> PAGE_SHIFT);
}
```
First of all it get the size of the page table buffer, it will be INIT_PGT_BUF_SIZE which is (6 * PAGE_SIZE)

As we got the size of the page table buffer, we call extend_brk function with two parameters: size and align. 
As you can understand from its name, this function extends the brk area. As we can see in the linux kernel linker script brk 
is in memory right after the BSS:
arch/x86/kernel/vmlinux.lds.S
```
/* BSS */
	. = ALIGN(PAGE_SIZE);
	.bss : AT(ADDR(.bss) - LOAD_OFFSET) {
		__bss_start = .;
		*(.bss..page_aligned)
		*(.bss)
		. = ALIGN(PAGE_SIZE);
		__bss_stop = .;
	}

	. = ALIGN(PAGE_SIZE);
	.brk : AT(ADDR(.brk) - LOAD_OFFSET) {
		__brk_base = .;
		. += 64 * 1024;		/* 64k alignment slop space */
		*(.brk_reservation)	/* areas brk users have reserved */
		__brk_limit = .;
	}
```
Or we can find it with readelf util:  todo
```
[25] .bss  NOBITS  ffffffff8199d000             00d9d000
00000000000b4000   0000000000000000  WA  0  0   4096
[26] .brk  NOBITS  ffffffff81a51000           00d9do00   
0000000000026000   0000000000000000  WA  0  0   1
```

After that we got physical address of the new brk with the __pa macro, we calculate the base address 
and the end of the page table buffer. 
In the next step as we got page table buffer, we reserve memory block for the brk area with the reserve_brk function:
arch/x86/kernel/setup.c
```
...
reserve_brk();

...
static void __init reserve_brk(void)
{
	if (_brk_end > _brk_start)
		memblock_reserve(__pa_symbol(_brk_start),
				 _brk_end - _brk_start);

	/* Mark brk area as locked down and no longer taking any
	   new allocations */
	_brk_start = 0;
}
```
Note that in the end of the reserve_brk, we set brk_start to zero, because after this we will not allocate it anymore. 

The next step after reserving memory block for the brk, we need to unmap out-of-range memory areas in the kernel mapping 
with the cleanup_highmap function. Remember that kernel mapping is __START_KERNEL_map and _end - _text or 
level2_kernel_pgt maps the kernel _text, data and bss.
arch/x86/mm/init_64.c
```
unsigned long vaddr = __START_KERNEL_map;
	unsigned long vaddr_end = __START_KERNEL_map + KERNEL_IMAGE_SIZE;
	unsigned long end = roundup((unsigned long)_brk_end, PMD_SIZE) - 1;
	pmd_t *pmd = level2_kernel_pgt;

	if (max_pfn_mapped)
		vaddr_end = __START_KERNEL_map + (max_pfn_mapped << PAGE_SHIFT);

	for (; vaddr + PMD_SIZE - 1 < vaddr_end; pmd++, vaddr += PMD_SIZE) {
		if (pmd_none(*pmd))
			continue;
		if (vaddr < (unsigned long) _text || vaddr > end)
			set_pmd(pmd, __pmd(0));
	}
```
Now, as we defined start and end of the kernel mapping, we go in the loop through the all kernel page 
middle directory entries and clean entries which are not between _text and end

After this we set the limit for the memblock allocation with the memblock_set_current_limit function 
(read more about memblock you can in the Linux kernel memory management Part 2), it will be ISA_END_ADDRESS or 0x100000 
and fill the memblock information according to e820 with the call of the e820__memblock_setup function. 
arch/x86/kernel/setup.c
```
...
memblock_set_current_limit(ISA_END_ADDRESS);
```
https://github.com/torvalds/linux/blob/v4.16/mm/memblock.c
```
void __init_memblock memblock_set_current_limit(phys_addr_t limit)
{
	memblock.current_limit = limit;
}
```

You can see the result of this function in the kernel initialization time:  todo
```
MEMBLOCK configuration:
 memory size = 0x1fff7ec00 reserved size = 0x1e30000
 memory.cnt  = 0x3
 memory[0x0]    [0x00000000001000-0x0000000009efff], 0x9e000 bytes flags: 0x0
 memory[0x1]    [0x00000000100000-0x000000bffdffff], 0xbfee0000 bytes flags: 0x0
 memory[0x2]    [0x00000100000000-0x0000023fffffff], 0x140000000 bytes flags: 0x0
 reserved.cnt  = 0x3
 reserved[0x0]    [0x0000000009f000-0x000000000fffff], 0x61000 bytes flags: 0x0
 reserved[0x1]    [0x00000001000000-0x00000001a57fff], 0xa58000 bytes flags: 0x0
 reserved[0x2]    [0x0000007ec89000-0x0000007fffffff], 0x1377000 bytes flags: 0x0
```

arch/x86/kernel/setup.c
```
e820__memblock_alloc_reserved_mpc_new
...
reserve_real_mode();
trim_low_memory_range();
init_mem_mapping();
idt_setup_early_pf();
```
arch/x86/kernel/mpparse.c
```
void __init e820__memblock_alloc_reserved_mpc_new(void)
{
	if (enable_update_mptable && alloc_mptable)
		mpc_new_phys = e820__memblock_alloc_reserved(mpc_new_length, 4);
}
```
arch/x86/kernel/e820.c
```
u64 __init e820__memblock_alloc_reserved(u64 size, u64 align)
{
	u64 addr;
	addr = __memblock_alloc_base(size, align, MEMBLOCK_ALLOC_ACCESSIBLE);
	if (addr) {
		e820__range_update_kexec(addr, size, E820_TYPE_RAM, E820_TYPE_RESERVED);
		pr_info("e820: update e820_table_kexec for e820__memblock_alloc_reserved()\n");
		e820__update_table_kexec();
	}

	return addr;
}
```
arch/x86/realmode/init.c
```
void __init reserve_real_mode(void)
{
	phys_addr_t mem;
	size_t size = real_mode_size_needed();

	if (!size)
		return;

	WARN_ON(slab_is_available());

	/* Has to be under 1M so we can execute real-mode AP code. */
	mem = memblock_find_in_range(0, 1<<20, size, PAGE_SIZE);
	if (!mem) {
		pr_info("No sub-1M memory is available for the trampoline\n");
		return;
	}

	memblock_reserve(mem, size);
	set_real_mode_mem(mem, size);
}
```
e820__memblock_alloc_reserved_mpc_new,allocates additional slots in the e820map for MultiProcessor Specification table,
reserve_real_mode - reserves low memory from 0x0 to 1 megabyte for the trampoline to the real mode (for rebooting, etc.),
trim_platform_memory_ranges - trims certain memory regions started from 0x20050000, 0x20110000, etc. 
   these regions must be excluded because Sandy Bridge has problems with these regions,
trim_low_memory_range reserves the first 4 kilobyte page in memblock,
arch/x86/kernel/setup.c
```
static void __init trim_low_memory_range(void)
{
	memblock_reserve(0, ALIGN(reserve_low, PAGE_SIZE));
}
```
setups #PF handler (we will look on it in the chapter about interrupts)
arch/x86/kernel/idt.c
```
static const __initconst struct idt_data early_pf_idts[] = {
	INTG(X86_TRAP_PF,		page_fault),
};
void __init idt_setup_early_pf(void)
{
	idt_setup_from_table(idt_table, early_pf_idts,
			     ARRAY_SIZE(early_pf_idts), true);
}
```

setup_real_mode function setups trampoline to the real mode code.

init_mem_mapping function reconstructs direct memory mapping and setups the direct mapping of the physical memory at PAGE_OFFSET,
arch/x86/mm/init.c
```
void __init init_mem_mapping(void)
{
	unsigned long end;
	pti_check_boottime_disable();
	probe_page_size_mask();
	setup_pcid();
#ifdef CONFIG_X86_64
	end = max_pfn << PAGE_SHIFT;
...
	init_memory_mapping(0, ISA_END_ADDRESS);

	init_trampoline();

	if (memblock_bottom_up()) {
		unsigned long kernel_end = __pa_symbol(_end);
		memory_map_bottom_up(kernel_end, end);
		memory_map_bottom_up(ISA_END_ADDRESS, kernel_end);
	} else {
		memory_map_top_down(ISA_END_ADDRESS, end);
	}

#ifdef CONFIG_X86_64
	if (max_pfn > max_low_pfn) {
		/* can we preseve max_low_pfn ?*/
		max_low_pfn = max_pfn;
	}
...

	load_cr3(swapper_pg_dir);
	__flush_tlb_all();
	x86_init.hyper.init_mem_mapping();
	early_memtest(0, max_pfn_mapped << PAGE_SHIFT);
}
```

https://en.wikipedia.org/wiki/Sandy_Bridge
Sandy Bridge is the codename for Intel's 32 nm microarchitecture used in the second generation of the Intel Core processors (Core i7, i5, i3).
The Sandy Bridge microarchitecture is the successor to Nehalem and Westmere microarchitecture.

文章中setup_real_mode 在4.16改为驱动调用，不在setup_arch里面了
arch/x86/realmode/init.c
```
static int __init init_real_mode(void)
{
	if (!real_mode_header)
		panic("Real mode trampoline was not allocated");

	setup_real_mode();
	set_real_mode_permissions();

	return 0;
}
early_initcall(init_real_mode);
```