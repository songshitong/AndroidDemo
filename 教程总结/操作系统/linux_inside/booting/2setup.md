kernel setup

https://0xax.gitbooks.io/linux-insides/content/Booting/linux-bootstrap-2.html
In this part, we will continue to research the kernel setup code and go over
what protected mode is,
the transition into it,
the initialization of the heap and the console,
memory detection, CPU validation and keyboard initialization
and much much more.


Long Mode https://en.wikipedia.org/wiki/Long_mode
In the x86-64 computer architecture, long mode is the mode where a 64-bit operating system can access 64-bit instructions and registers. 
64-bit programs are run in a sub-mode called 64-bit mode, while 32-bit programs and 16-bit protected mode programs 
are executed in a sub-mode called compatibility mode. Real mode or virtual 8086 mode programs cannot be natively run in long mode.

protected mode  https://en.wikipedia.org/wiki/Protected_mode
In computing, protected mode, also called protected virtual address mode, is an operational mode of 
x86-compatible central processing units (CPUs). It allows system software to use features such as segmentation, 
virtual memory, paging and safe multi-tasking designed to increase an operating system's control over application software.


Before we can move to the native Intel64 Long Mode, the kernel must switch the CPU into protected mode.
Protected mode brought many changes, but the main one is the difference in memory management. 
The 20-bit address bus was replaced with a 32-bit address bus. It allowed access to 4 Gigabytes of 
memory vs the 1 Megabyte in Real mode. Also, paging support was added

Memory management in Protected mode is divided into two, almost independent parts:
Segmentation
Paging
Here we will only talk about segmentation


Memory segmentation in protected mode.
There are no 64 Kilobyte fixed-size segments. Instead, the size and location of each segment is described 
by an associated data structure called the Segment Descriptor. These segment descriptors are stored in 
a data structure called the Global Descriptor Table (GDT).

The GDT is a structure which resides in memory. It has no fixed place in the memory, so its address is
stored in the special GDTR register. Later we will see how the GDT is loaded in the Linux kernel code

GDTR is a 48-bit register and consists of two parts:
1 the size(16-bit) of the global descriptor table;
2 the address(32-bit) of the global descriptor table.
the GDT contains segment descriptors which describe memory segments. Each descriptor is 64-bits in size.
The general scheme of a descriptor is:
```
 63         56         51   48    45           39        32 
------------------------------------------------------------
|             | |B| |A|       | |   | |0|E|W|A|            |
| BASE 31:24  |G|/|L|V| LIMIT |P|DPL|S|  TYPE | BASE 23:16 |
|             | |D| |L| 19:16 | |   | |1|C|R|A|            |
------------------------------------------------------------

 31                         16 15                         0 
------------------------------------------------------------
|                             |                            |
|        BASE 15:0            |       LIMIT 15:0           |
|                             |                            |
------------------------------------------------------------
```
1 Limit[20-bits] is split between bits 0-15 and 48-51. It defines the length_of_segment - 1. It depends on the G(Granularity) bit.  
Granularity 间隔，尺寸，粒度，颗粒度，精细度
if G (bit 55) is 0 and the segment limit is 0, the size of the segment is 1 Byte   //1B
if G is 1 and the segment limit is 0, the size of the segment is 4096 Bytes       //4kB = 1 Page
if G is 0 and the segment limit is 0xfffff, the size of the segment is 1 Megabyte  //1M
if G is 1 and the segment limit is 0xfffff, the size of the segment is 4 Gigabytes //4G

2 Base[32-bits] is split between bits 16-31, 32-39 and 56-63. It defines the physical address of the segment's starting location.

3 Type/Attribute[5-bits] is represented by bits 40-44. It defines the type of segment and how it can be accessed.
The S flag at bit 44 specifies the descriptor type. If S is 0 then this segment is a system segment, 
whereas if S is 1 then this is a code or data segment (Stack segments are data segments which must be read/write segments).

To determine if the segment is a code or data segment, we can check its Ex(bit 43) Attribute (marked as 0 in the above diagram).
If it is 0, then the segment is a Data segment, otherwise, it is a code segment.
A segment can be of one of the following types:
```
--------------------------------------------------------------------------------------
|           Type Field        | Descriptor Type | Description                        |
|-----------------------------|-----------------|------------------------------------|
| Decimal                     |                 |                                    |
|             0    E    W   A |                 |                                    |
| 0           0    0    0   0 | Data            | Read-Only                          |
| 1           0    0    0   1 | Data            | Read-Only, accessed                |
| 2           0    0    1   0 | Data            | Read/Write                         |
| 3           0    0    1   1 | Data            | Read/Write, accessed               |
| 4           0    1    0   0 | Data            | Read-Only, expand-down             |
| 5           0    1    0   1 | Data            | Read-Only, expand-down, accessed   |
| 6           0    1    1   0 | Data            | Read/Write, expand-down            |
| 7           0    1    1   1 | Data            | Read/Write, expand-down, accessed  |
|                  C    R   A |                 |                                    |
| 8           1    0    0   0 | Code            | Execute-Only                       |
| 9           1    0    0   1 | Code            | Execute-Only, accessed             |
| 10          1    0    1   0 | Code            | Execute/Read                       |
| 11          1    0    1   1 | Code            | Execute/Read, accessed             |
| 12          1    1    0   0 | Code            | Execute-Only, conforming           |
| 14          1    1    0   1 | Code            | Execute-Only, conforming, accessed |
| 13          1    1    1   0 | Code            | Execute/Read, conforming           |
| 15          1    1    1   1 | Code            | Execute/Read, conforming, accessed |
--------------------------------------------------------------------------------------
```
The next three bits (40, 41, 42) are either EWA(Expansion Writable Accessible) or CRA(Conforming Readable Accessible).
1 if E(bit 42) is 0, expand up, otherwise, expand down. Read more here. //https://www.sudleyplace.com/dpmione/expanddown.html
2 if W(bit 41)(for Data Segments) is 1, write access is allowed, and if it is 0, the segment is read-only. 
  Note that read access is always allowed on data segments.
3 A(bit 40) controls whether the segment can be accessed by the processor or not.
4 C(bit 43) is the conforming bit(for code selectors). If C is 1, the segment code can be executed f
   rom a lower level privilege (e.g. user) level. If C is 0, it can only be executed from the same privilege level.
5 R(bit 41) controls read access to code segments; when it is 1, the segment can be read from. 
  Write access is never granted for code segments.


DPL[2-bits] (Descriptor Privilege Level) comprises the bits 45-46. It defines the privilege level of the segment.
It can be 0-3 where 0 is the most privileged level.

The P flag(bit 47) indicates if the segment is present in memory or not. If P is 0, the segment will 
   be presented as invalid and the processor will refuse to read from this segment.

AVL flag(bit 52) - Available and reserved bits. It is ignored in Linux.

The L flag(bit 53) indicates whether a code segment contains native 64-bit code. If it is set, 
  then the code segment executes in 64-bit mode.

The D/B flag(bit 54) (Default/Big flag) represents the operand size i.e 16/32 bits. If set, operand size is 32 bits. 
  Otherwise, it is 16 bits.  //operand 操作数，运算数


Each Segment Descriptor has an associated Segment Selector which is a 16-bit structure:
```
 15             3 2  1     0
-----------------------------
|      Index     | TI | RPL |
-----------------------------
```
Index stores the index number of the descriptor in the GDT.[3-15]
TI(Table Indicator) indicates where to search for the descriptor. If it is 0 then the descriptor is 
   searched for in the Global Descriptor Table(GDT). Otherwise, it will be searched for in the Local Descriptor Table(LDT).
RPL contains the Requester's Privilege Level.

Every segment register has a visible and a hidden part.
Visible - The Segment Selector is stored here.
Hidden - The Segment Descriptor (which contains the base, limit, attributes & flags) is stored here.


The following steps are needed to get a physical address in protected mode:
1 The segment selector must be loaded in one of the segment registers.
2 The CPU tries to find a segment descriptor at the offset GDT address + Index from the selector and then 
   loads the descriptor into the hidden part of the segment register.
3 If paging is disabled, the linear address of the segment, or its physical address, is given by the formula:
  Base address (found in the descriptor obtained in the previous step) + Offset.


The algorithm for the transition from real mode into protected mode is:
1 Disable interrupts
2 Describe and load the GDT with the lgdt instruction
3 Set the PE (Protection Enable) bit in CR0 (Control Register 0)
4 Jump to protected mode code



Copying boot parameters into the "zeropage"
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/main.c
```
/*
 * Copy the header into the boot parameter block.  Since this
 * screws up the old-style command line protocol, adjust by
 * filling in the new-style command line pointer instead.
 */

static void copy_boot_params(void)
{
	...
	memcpy(&boot_params.hdr, &hdr, sizeof hdr);
    if (!boot_params.hdr.cmd_line_ptr &&
	    ...
		boot_params.hdr.cmd_line_ptr =
			(cmdline_seg << 4) + oldcmd->cl_offset;
	}
}
...
void main(void)
{
/* First, copy the boot header into the "zeropage" */
	copy_boot_params();

	/* Initialize the early-boot console */
	console_init();
	if (cmdline_find_option_bool("debug"))
		puts("early console in setup code\n");

	/* End of heap check */
	init_heap();

	/* Make sure we have all the proper CPU support */
	if (validate_cpu()) {
		puts("Unable to boot - please use a kernel appropriate "
		     "for your CPU.\n");
		//puts的汇编实现https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/tty.c     
		die();
	}

	/* Tell the BIOS what CPU mode we intend to run in. */
	set_bios_mode();

	/* Detect memory layout */
	detect_memory();

	/* Set keyboard repeat rate (why?) and query the lock flags */
	keyboard_init();

	/* Query Intel SpeedStep (IST) information */
	query_ist();

	/* Query APM information */
#if defined(CONFIG_APM) || defined(CONFIG_APM_MODULE)
	query_apm_bios();
#endif

	/* Query EDD information */
#if defined(CONFIG_EDD) || defined(CONFIG_EDD_MODULE)
	query_edd();
#endif

	/* Set the video mode */
	set_video();

	/* Do the last things and invoke protected mode */
	go_to_protected_mode();}
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/include/uapi/asm/bootparam.h
```
/* The so-called "zeropage" */
struct boot_params {
    ...
	struct setup_header hdr;    /* setup header */	/* 0x1f1 */
	...
} __attribute__((packed));


struct setup_header {
	__u8	setup_sects;
	__u16	root_flags;
	__u32	syssize;
	__u16	ram_size;
	__u16	vid_mode;
	__u16	root_dev;
	__u16	boot_flag;
	__u16	jump;
	__u32	header;
	__u16	version;
	__u32	realmode_swtch;
	__u16	start_sys_seg;
	__u16	kernel_version;
	__u8	type_of_loader;
	__u8	loadflags;
	__u16	setup_move_size;
	__u32	code32_start;
	__u32	ramdisk_image;
	__u32	ramdisk_size;
	__u32	bootsect_kludge;
	__u16	heap_end_ptr;
	__u8	ext_loader_ver;
	__u8	ext_loader_type;
	__u32	cmd_line_ptr;
	__u32	initrd_addr_max;
	__u32	kernel_alignment;
	__u8	relocatable_kernel;
	__u8	min_alignment;
	__u16	xloadflags;
	__u32	cmdline_size;
	__u32	hardware_subarch;
	__u64	hardware_subarch_data;
	__u32	payload_offset;
	__u32	payload_length;
	__u64	setup_data;
	__u64	pref_address;
	__u32	init_size;
	__u32	handover_offset;
} __attribute__((packed));
```
copy_boot_params does two things:
1 It copies hdr from header.S to the setup_header field in boot_params structure.
2 It updates the pointer to the kernel command line if the kernel was loaded with the old command line protocol.

linux inside有关于memcpy的汇编实现，见原文



Console initialization
arch/x86/boot/early_serial_console.c
```
static void parse_earlyprintk(void)
{
	int baud = DEFAULT_BAUD;
	char arg[32];
	int pos = 0;
	int port = 0;

	if (cmdline_find_option("earlyprintk", arg, sizeof arg) > 0) {
		char *e;

		if (!strncmp(arg, "serial", 6)) {
			port = DEFAULT_SERIAL_PORT; //#define DEFAULT_SERIAL_PORT 0x3f8 /* ttyS0 */
			pos += 6;
		}

		if (arg[pos] == ',')
			pos++;

		/*
		 * make sure we have
		 *	"serial,0x3f8,115200"
		 *	"serial,ttyS0,115200"
		 *	"ttyS0,115200"
		 */
		if (pos == 7 && !strncmp(arg + pos, "0x", 2)) {
			port = simple_strtoull(arg + pos, &e, 16);
			if (port == 0 || arg + pos == e)
				port = DEFAULT_SERIAL_PORT;
			else
				pos = e - arg;
		} else if (!strncmp(arg + pos, "ttyS", 4)) {
			static const int bases[] = { 0x3f8, 0x2f8 };
			int idx = 0;

			/* += strlen("ttyS"); */
			pos += 4;

			if (arg[pos++] == '1')
				idx = 1;

			port = bases[idx];
		}

		if (arg[pos] == ',')
			pos++;

		baud = simple_strtoull(arg + pos, &e, 0);
		if (baud == 0 || arg + pos == e)
			baud = DEFAULT_BAUD; //#define DEFAULT_BAUD 9600
	}

	if (port)
		early_serial_init(port, baud);
}

void console_init(void)
{
	parse_earlyprintk();

	if (!early_serial_base)
		parse_console_uart8250();
}
```
It tries to find the earlyprintk option in the command line and if the search was successful, 
it parses the port address and baud rate of the serial port and initializes the serial port. 
The value of the earlyprintk command line option can be one of these:
serial,0x3f8,115200
serial,ttyS0,115200
ttyS0,115200


Heap initialization
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/main.c
```
static void init_heap(void)
{
	char *stack_end;

	if (boot_params.hdr.loadflags & CAN_USE_HEAP) {
		asm("leal %P1(%%esp),%0"
		    : "=r" (stack_end) : "i" (-STACK_SIZE));

		heap_end = (char *)
			((size_t)boot_params.hdr.heap_end_ptr + 0x200); //heap_end_ptr or _end + 512 (0x200h)
		if (heap_end > stack_end)
			heap_end = stack_end;
	} else {
		/* Boot protocol 2.00 only, no heap available */
		puts("WARNING: Ancient bootloader, some functionality "
		     "may be limited!\n");
	}
}
```
First of all init_heap checks the CAN_USE_HEAP flag from the loadflags structure in the 
kernel setup header and calculates the end of the stack if this flag was set
or in other words stack_end = esp - STACK_SIZE.



CPU validation
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/cpu.c
```
int validate_cpu(void)
{
	u32 *err_flags;
	int cpu_level, req_level;

	check_cpu(&cpu_level, &req_level, &err_flags);

	if (cpu_level < req_level) {
		printf("This kernel requires an %s CPU, ",
		       cpu_name(req_level));
		printf("but only detected an %s CPU.\n",
		       cpu_name(cpu_level));
		return -1;
	}

	if (err_flags) {
		puts("This kernel requires the following features "
		     "not present on the CPU:\n");
		show_cap_strs(err_flags);
		putchar('\n');
		return -1;
	} else if (check_knl_erratum()) {
		return -1;
	} else {
		return 0;
	}
}
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/cpucheck.c
check_cpu   cpu.level 检测AMD，VIA C3，Transmeta，Pentium M
```
/*
 * Returns -1 on error.
 *
 * *cpu_level is set to the current CPU level; *req_level to the required
 * level.  x86-64 is considered level 64 for this purpose.
 *
 * *err_flags_ptr is set to the flags error array if there are flags missing.
 */
int check_cpu(int *cpu_level_ptr, int *req_level_ptr, u32 **err_flags_ptr)
{
	int err;

	memset(&cpu.flags, 0, sizeof cpu.flags);
	cpu.level = 3;

	if (has_eflag(X86_EFLAGS_AC))
		cpu.level = 4;

	get_cpuflags();
	err = check_cpuflags();

	if (test_bit(X86_FEATURE_LM, cpu.flags))
		cpu.level = 64;

	if (err == 0x01 &&
	    !(err_flags[0] &
	      ~((1 << X86_FEATURE_XMM)|(1 << X86_FEATURE_XMM2))) &&
	    is_amd()) {  
		/* If this is an AMD and we're only missing SSE+SSE2, try to
		   turn them on */

		u32 ecx = MSR_K7_HWCR;
		u32 eax, edx;

		asm("rdmsr" : "=a" (eax), "=d" (edx) : "c" (ecx));
		eax &= ~(1 << 15);
		asm("wrmsr" : : "a" (eax), "d" (edx), "c" (ecx));

		get_cpuflags();	/* Make sure it really did something */
		err = check_cpuflags();
	} else if (err == 0x01 &&
		   !(err_flags[0] & ~(1 << X86_FEATURE_CX8)) &&
		   is_centaur() && cpu.model >= 6) {
		/* If this is a VIA C3, we might have to enable CX8
		   explicitly */

		u32 ecx = MSR_VIA_FCR;
		u32 eax, edx;

		asm("rdmsr" : "=a" (eax), "=d" (edx) : "c" (ecx));
		eax |= (1<<1)|(1<<7);
		asm("wrmsr" : : "a" (eax), "d" (edx), "c" (ecx));

		set_bit(X86_FEATURE_CX8, cpu.flags);
		err = check_cpuflags();
	} else if (err == 0x01 && is_transmeta()) {
		/* Transmeta might have masked feature bits in word 0 */

		u32 ecx = 0x80860004;
		u32 eax, edx;
		u32 level = 1;

		asm("rdmsr" : "=a" (eax), "=d" (edx) : "c" (ecx));
		asm("wrmsr" : : "a" (~0), "d" (edx), "c" (ecx));
		asm("cpuid"
		    : "+a" (level), "=d" (cpu.flags[0])
		    : : "ecx", "ebx");
		asm("wrmsr" : : "a" (eax), "d" (edx), "c" (ecx));

		err = check_cpuflags();
	} else if (err == 0x01 &&
		   !(err_flags[0] & ~(1 << X86_FEATURE_PAE)) &&
		   is_intel() && cpu.level == 6 &&
		   (cpu.model == 9 || cpu.model == 13)) {
		/* PAE is disabled on this Pentium M but can be forced */
		if (cmdline_find_option_bool("forcepae")) {
			puts("WARNING: Forcing PAE in CPU flags\n");
			set_bit(X86_FEATURE_PAE, cpu.flags);
			err = check_cpuflags();
		}
		else {
			puts("WARNING: PAE disabled. Use parameter 'forcepae' to enable at your own risk!\n");
		}
	}
	if (!err)
		err = check_knl_erratum();

	if (err_flags_ptr)
		*err_flags_ptr = err ? err_flags : NULL;
	if (cpu_level_ptr)
		*cpu_level_ptr = cpu.level;
	if (req_level_ptr)
		*req_level_ptr = req_level;

	return (cpu.level < req_level || err) ? -1 : 0;
}
```
It calls the check_cpu function and passes cpu level and required cpu level to it and checks that the kernel 
launches on the right cpu level.

set_bios_mode()
```
/*
 * Tell the BIOS what CPU mode we intend to run in.
 */
static void set_bios_mode(void)
{
#ifdef CONFIG_X86_64
	struct biosregs ireg;

	initregs(&ireg);
	ireg.ax = 0xec00;
	ireg.bx = 2;
	intcall(0x15, &ireg, NULL);
#endif
}
```
this function is implemented only for the x86_64 mode
The set_bios_mode function executes the 0x15 BIOS interrupt to tell the BIOS that long mode (if bx == 2) will be used.



Memory detection
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/memory.c
```
int detect_memory(void)
{
	int err = -1;

	if (detect_memory_e820() > 0)
		err = 0;

	if (!detect_memory_e801())
		err = 0;

	if (!detect_memory_88())
		err = 0;

	return err;
}
```
detect_memory basically provides a map of available RAM to the CPU. It uses different programming interfaces 
for memory detection like 0xe820, 0xe801 and 0x88. We will see only the implementation of the 0xE820 interface here.
todo intcall的实现  https://zhuanlan.zhihu.com/p/674987474
0x15 中断一共有 3 个子功能： //https://blog.csdn.net/jiaruitao777/article/details/103383389
EAX=0xe820 ;遍历主机上全部内存
AX=0xe801 ；分别检测低 15MB 和 16MB~4GB 的内存，最大支持 4GB。
AH=0x88 ；最多检测 64MB 内存，实际内存超过64MB 也按64MB返回。
``` 
static int detect_memory_e820(void)
{
	int count = 0;
	struct biosregs ireg, oreg;
	struct boot_e820_entry *desc = boot_params.e820_table;
	static struct boot_e820_entry buf; /* static so it is zeroed */

	initregs(&ireg);
	ireg.ax  = 0xe820;
	ireg.cx  = sizeof buf;
	ireg.edx = SMAP;
	ireg.di  = (size_t)&buf;

	do {
		intcall(0x15, &ireg, &oreg);  //调用中断
		ireg.ebx = oreg.ebx; /* for next iteration... */
		if (oreg.eflags & X86_EFLAGS_CF)
			break;
		if (oreg.eax != SMAP) {
			count = 0;
			break;
		}

		*desc++ = buf;
		count++;
	} while (ireg.ebx && count < ARRAY_SIZE(boot_params.e820_table));

	return boot_params.e820_entries = count;
}
```
ireg:
ax contains the number of the function (0xe820 in our case)
cx contains the size of the buffer which will contain data about the memory
edx must contain the SMAP magic number
es:di must contain the address of the buffer which will contain memory data
ebx has to be zero.

this function collects data from the address allocation table and writes this data into the e820_entry array:
start of memory segment
size of memory segment
type of memory segment (whether the particular segment is usable or reserved)
You can see the result of this in the dmesg output, something like:
```
[    0.000000] e820: BIOS-provided physical RAM map:
[    0.000000] BIOS-e820: [mem 0x0000000000000000-0x000000000009fbff] usable
[    0.000000] BIOS-e820: [mem 0x000000000009fc00-0x000000000009ffff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000000f0000-0x00000000000fffff] reserved
[    0.000000] BIOS-e820: [mem 0x0000000000100000-0x000000003ffdffff] usable
[    0.000000] BIOS-e820: [mem 0x000000003ffe0000-0x000000003fffffff] reserved
[    0.000000] BIOS-e820: [mem 0x00000000fffc0000-0x00000000ffffffff] reserved
```




Keyboard initialization
```
/*
 * Query the keyboard lock status as given by the BIOS, and
 * set the keyboard repeat rate to maximum.  Unclear why the latter
 * is done here; this might be possible to kill off as stale code.
 */
static void keyboard_init(void)
{
	struct biosregs ireg, oreg;
	initregs(&ireg);

	ireg.ah = 0x02;		/* Get keyboard status */
	intcall(0x16, &ireg, &oreg);
	boot_params.kbd_status = oreg.al;

	ireg.ax = 0x0305;	/* Set keyboard repeat rate */
	intcall(0x16, &ireg, NULL);
}
```
1 calls the 0x16 interrupt to query the status of the keyboard.
2 calls 0x16 again to set the repeat rate and delay.


Querying
The next couple of steps are queries for different parameters. We will not dive into details about 
these queries but we will get back to them in later parts.
```
/* Query Intel SpeedStep (IST) information */
	query_ist();
/* Query APM information */
#if defined(CONFIG_APM) || defined(CONFIG_APM_MODULE)
	query_apm_bios();
#endif
/* Query EDD information */
#if defined(CONFIG_EDD) || defined(CONFIG_EDD_MODULE)
	query_edd();
#endif
	

/*
 * Get Intel SpeedStep (IST) information.
 */
static void query_ist(void)
{
	struct biosregs ireg, oreg;
	/* Some older BIOSes apparently crash on this call, so filter
	   it from machines too old to have SpeedStep at all. */
	if (cpu.level < 6)
		return;

	initregs(&ireg);
	ireg.ax  = 0xe980;	 /* IST Support */
	ireg.edx = 0x47534943;	 /* Request value */
	intcall(0x15, &ireg, &oreg);

	boot_params.ist_info.signature  = oreg.eax;
	boot_params.ist_info.command    = oreg.ebx;
	boot_params.ist_info.event      = oreg.ecx;
	boot_params.ist_info.perf_level = oreg.edx;
}	
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/apm.c
```
int query_apm_bios(void)
{
	struct biosregs ireg, oreg;

	/* APM BIOS installation check */
	initregs(&ireg);
	ireg.ah = 0x53;
	intcall(0x15, &ireg, &oreg);

	if (oreg.flags & X86_EFLAGS_CF)
		return -1;		/* No APM BIOS */

	if (oreg.bx != 0x504d)		/* "PM" signature */
		return -1;

	if (!(oreg.cx & 0x02))		/* 32 bits supported? */
		return -1;

	/* Disconnect first, just in case */
	ireg.al = 0x04;
	intcall(0x15, &ireg, NULL);

	/* 32-bit connect */
	ireg.al = 0x03;
	intcall(0x15, &ireg, &oreg);

	boot_params.apm_bios_info.cseg        = oreg.ax;
	boot_params.apm_bios_info.offset      = oreg.ebx;
	boot_params.apm_bios_info.cseg_16     = oreg.cx;
	boot_params.apm_bios_info.dseg        = oreg.dx;
	boot_params.apm_bios_info.cseg_len    = oreg.si;
	boot_params.apm_bios_info.cseg_16_len = oreg.hsi;
	boot_params.apm_bios_info.dseg_len    = oreg.di;

	if (oreg.flags & X86_EFLAGS_CF)
		return -1;

	/* Redo the installation check as the 32-bit connect;
	   some BIOSes return different flags this way... */

	ireg.al = 0x00;
	intcall(0x15, &ireg, &oreg);

	if ((oreg.eflags & X86_EFLAGS_CF) || oreg.bx != 0x504d) {
		/* Failure with 32-bit connect, try to disconect and ignore */
		ireg.al = 0x04;
		intcall(0x15, &ireg, NULL);
		return -1;
	}

	boot_params.apm_bios_info.version = oreg.ax;
	boot_params.apm_bios_info.flags   = oreg.cx;
	return 0;
}
```
query_edd  https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/edd.c
```
void query_edd(void)
{
	char eddarg[8];
	int do_mbr = 1;
#ifdef CONFIG_EDD_OFF
	int do_edd = 0;
#else
	int do_edd = 1;
#endif
	int be_quiet;
	int devno;
	struct edd_info ei, *edp;
	u32 *mbrptr;

	if (cmdline_find_option("edd", eddarg, sizeof eddarg) > 0) {
		if (!strcmp(eddarg, "skipmbr") || !strcmp(eddarg, "skip")) {
			do_edd = 1;
			do_mbr = 0;
		}
		else if (!strcmp(eddarg, "off"))
			do_edd = 0;
		else if (!strcmp(eddarg, "on"))
			do_edd = 1;
	}

	be_quiet = cmdline_find_option_bool("quiet");

	edp    = boot_params.eddbuf;
	mbrptr = boot_params.edd_mbr_sig_buffer;

	if (!do_edd)
		return;

	/* Bugs in OnBoard or AddOnCards Bios may hang the EDD probe,
	 * so give a hint if this happens.
	 */

	if (!be_quiet)
		printf("Probing EDD (edd=off to disable)... ");

	for (devno = 0x80; devno < 0x80+EDD_MBR_SIG_MAX; devno++) {
		/*
		 * Scan the BIOS-supported hard disks and query EDD
		 * information...
		 */
		if (!get_edd_info(devno, &ei)
		    && boot_params.eddbuf_entries < EDDMAXNR) {
			memcpy(edp, &ei, sizeof ei);
			edp++;
			boot_params.eddbuf_entries++;
		}

		if (do_mbr && !read_mbr_sig(devno, &ei, mbrptr++))
			boot_params.edd_mbr_sig_buf_entries = devno-0x80+1;
	}

	if (!be_quiet)
		printf("ok\n");
}


static int get_edd_info(u8 devno, struct edd_info *ei)
{
	struct biosregs ireg, oreg;
	memset(ei, 0, sizeof *ei);
	/* Check Extensions Present */
	initregs(&ireg);
	ireg.ah = 0x41;
	ireg.bx = EDDMAGIC1;
	ireg.dl = devno;
	intcall(0x13, &ireg, &oreg);

	if (oreg.eflags & X86_EFLAGS_CF)
		return -1;	/* No extended information */

	if (oreg.bx != EDDMAGIC2)
		return -1;

	ei->device  = devno;
	ei->version = oreg.ah;		 /* EDD version number */
	ei->interface_support = oreg.cx; /* EDD functionality subsets */

	/* Extended Get Device Parameters */

	ei->params.length = sizeof(ei->params);
	ireg.ah = 0x48;
	ireg.si = (size_t)&ei->params;
	intcall(0x13, &ireg, &oreg);

	/* Get legacy CHS parameters */

	/* Ralf Brown recommends setting ES:DI to 0:0 */
	ireg.ah = 0x08;
	ireg.es = 0;
	intcall(0x13, &ireg, &oreg);

	if (!(oreg.eflags & X86_EFLAGS_CF)) {
		ei->legacy_max_cylinder = oreg.ch + ((oreg.cl & 0xc0) << 2);
		ei->legacy_max_head = oreg.dh;
		ei->legacy_sectors_per_track = oreg.cl & 0x3f;
	}

	return 0;
}

```
1 The first step is getting Intel SpeedStep information by calling the query_ist function. 
It checks the CPU level and if it is correct, calls 0x15 to get the info and saves the result to boot_params.
2 Next, the query_apm_bios function gets Advanced Power Management information from the BIOS.
3 The last is the query_edd function, which queries Enhanced Disk Drive information from the BIOS.

SpeedStep https://zh.wikipedia.org/wiki/SpeedStep
SpeedStep技术，最早用于Pentium III Mobile处理器——一种笔记本所用的移动版CPU中，使CPU能在高、低两个确定的频率间切换，
而且这种切换不是即时调整的，通常设置为当用电池时降为低频，而在用交流电源时恢复到高频（全速）。由于降为低频的同时也会降低电压和功耗，
一方面CPU本身耗电量减少，另一方面发热量也会减少，这样还能缩减甚至完全避免使用风扇散热，进一步的节约了用电，因此能延长电池的使用时间；
另一方面在用交流电的时候又能恢复为全速工作以获得最高性能

https://en.wikipedia.org/wiki/Advanced_Power_Management
Advanced power management (APM) is a technical standard for power management developed by Intel and 
Microsoft and released in 1992 which enables an operating system running an IBM-compatible personal 
computer to work with the BIOS (part of the computer's firmware) to achieve power management.

Enhanced Disk Drive  https://lwn.net/Articles/12544/    配置启动盘
BIOS Enhanced Disk Device Services (EDD) 3.0 provides the ability for
disk adapter BIOSs to tell the OS what it believes is the boot disk.