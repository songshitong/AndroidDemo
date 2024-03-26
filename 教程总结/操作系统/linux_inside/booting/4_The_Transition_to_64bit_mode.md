

SSE
https://en.wikipedia.org/wiki/Streaming_SIMD_Extensions
In computing, Streaming SIMD Extensions (SSE) is a single instruction, multiple data (SIMD) instruction
set extension to the x86 architecture, designed by Intel and introduced in 1999 in their Pentium III
series of central processing units (CPUs) shortly after the appearance of Advanced Micro Devices (AMD's) 3DNow!.
SSE contains 70 new instructions (65 unique mnemonics using 70 encodings), most of which work on single
precision floating-point data. SIMD instructions can greatly increase performance when exactly the same
operations are to be performed on multiple data objects. Typical applications are digital signal
processing and graphics processing.


https://0xax.gitbooks.io/linux-insides/content/Booting/linux-bootstrap-4.html
This is the fourth part of the Kernel booting process.
we will learn about the first steps taken in protected mode, like checking if the CPU supports long mode and SSE.
We will initialize the page tables with paging and, at the end, transition the CPU to long mode.


In the previous part we stopped at the jump to the 32-bit entry point in arch/x86/boot/pmjump.S:
```
jmpl    *%eax
```
We can read about this in the linux kernel x86 boot protocol:
```
When using bzImage, the protected-mode kernel was relocated to 0x100000
```
Let's make sure that this is so by looking at the register values at the 32-bit entry point:
```
eax            0x100000    1048576
ecx            0x0        0
edx            0x0        0
ebx            0x0        0
esp            0x1ff5c    0x1ff5c
ebp            0x0        0x0
esi            0x14470    83056
edi            0x0        0
eip            0x100000    0x100000
eflags         0x46        [ PF ZF ]
cs             0x10    16
ss             0x18    24
ds             0x18    24
es             0x18    24
fs             0x18    24
gs             0x18    24
```
We can see here that the cs register contains a value of 0x10 , the eip register contains the value 0x100000 and
the base address of all segments including the code segment are zero.

So, the physical address where the kernel is loaded would be 0:0x100000 or just 0x100000, as specified by the boot protocol.
Now let's start with the 32-bit entry point.

The 32-bit entry point
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/head_64.S
```
	__HEAD
	.code32
ENTRY(startup_32)
 ...
ENDPROC(startup_32)
```
why the directory is named compressed?
bzimage is a gzipped package consisting of vmlinux, header and kernel setup code. 

区分head_64还是head_32
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/Makefile
vmlinux-objs-y := $(obj)/vmlinux.lds $(obj)/head_$(BITS).o $(obj)/misc.o \
	$(obj)/string.o $(obj)/cmdline.o $(obj)/error.o \
	$(obj)/piggy.o $(obj)/cpuflags.o

BITS定义	
https://github.com/torvalds/linux/blob/v4.16/arch/x86/Makefile	
ifeq ($(CONFIG_X86_32),y)
        BITS := 32
      ...
else
        BITS := 64
```

The main goal of the code in head_64.S is to prepare to enter long mode, enter it and then decompress the kernel. 


Reload the segments if needed

https://github.com/torvalds/linux/blob/v4.16/include/linux/init.h
```
#define __HEAD		.section	".head.text","ax"
```
.head.text is the name of the section and ax is a set of flags. In our case, these flags show us that this section 
is executable or in other words contains code. 
linker script:
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/vmlinux.lds.S
```
SECTIONS
{
	. = 0;
	.head.text : {
		_head = . ;
		HEAD_TEXT
		_ehead = . ;
	}
	..
}	
```
语法参考GNU LD linker scripting language   https://sourceware.org/binutils/docs/ld/Scripts.html#Scripts  todo
the . symbol is a special linker variable, the location counter. The value assigned to it is an offset relative to the segment.
In our case, we set the location counter to zero. This means that our code is linked to run from an offset of 0 in memory.
This is also stated in the comments:
```
Be careful parts of head_64.S assume startup_32 is at address 0.
```


https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/head_64.S
```
ENTRY(startup_32)
	cld
	/*
	 * Test KEEP_SEGMENTS flag to see if the bootloader is asking
	 * us to not reload segments
	 */
	testb $KEEP_SEGMENTS, BP_loadflags(%esi)
	jnz 1f

	cli
	movl	$(__BOOT_DS), %eax
	movl	%eax, %ds
	movl	%eax, %es
	movl	%eax, %ss
```
the cld instruction which clears the DF bit in the flags register. When the direction flag is clear,
all string operations like stos, scas and others will increment the index registers esi or edi.
We need to clear the direction flag because later we will use strings operations to perform various 
operations such as clearing space for page tables

https://metaso.cn/search/8460511007210287104
在x86汇编语言中，"CLD"指令的作用是将处理器中的方向标志位（Direction Flag，简称DF）清零。方向标志位是一个标志位，
用于指示字符串操作（如复制、比较等）是从低地址到高地址还是从高地址到低地址执行的 。具体来说，CLD指令的功能是将标志寄存器Flag的方向标志位DF设置为0，
这样在进行字符串操作时，变址寄存器SI或DI的地址指针会自动增加，从而使得字串处理由前往后进行 。相对应的指令是"STD"，其功能是将DF置位为1，
即DF=1，这通常用于控制字符串操作的方向

the next step is to check the KEEP_SEGMENTS flag in the loadflags kernel setup header field.
in the linux boot protocol documentation:
https://www.kernel.org/doc/Documentation/x86/boot.txt
```
 Bit 6 (write): KEEP_SEGMENTS
	Protocol: 2.07+
	- If 0, reload the segment registers in the 32bit entry point.
	- If 1, do not reload the segment registers in the 32bit entry point.
		Assume that %cs %ds %ss %es are all set to flat segments with
		a base of 0 (or the equivalent for their environment).
```

if the KEEP_SEGMENTS bit is not set in loadflags, we need to set the ds, ss and es segment registers 
to the index of the data segment with a base of 0.

why do we need to care about the values in the segment registers again? 
The answer is easy. The Linux kernel also has a 32-bit boot protocol and if a bootloader uses that to load the Linux kernel,
all the code before the startup_32 function will be missed. In this case, the startup_32 function would be
the first entry point to the Linux kernel right after the bootloader and there are no guarantees that
the segment registers will be in a known state.



the next step is to calculate the difference between where the kernel is compiled to run, and where we loaded it.
Remember that setup.ld.S contains the following definition: . = 0 at the start of the .head.text section. 
This means that the code in this section is compiled to run at the address 0. We can see this in the output of objdump:
```
arch/x86/boot/compressed/vmlinux:     file format elf64-x86-64


Disassembly of section .head.text:

0000000000000000 <startup_32>:
   0:   fc                      cld
   1:   f6 86 11 02 00 00 40    testb  $0x40,0x211(%rsi)
```
The objdump util tells us that the address of the startup_32 function is 0 but that isn't so. 
We now need to know where we actually are.  currently we are in protected mode

We will use a common pattern to find the address of the startup_32 function. We need to define a label,
make a call to it and pop the top of the stack to a register:
```
call label
label: pop %reg
```
After this, the register indicated by %reg will contain the address of label
```
	leal	(BP_scratch+4)(%esi), %esp
	call	1f
1:	popl	%ebp
	subl	$1b, %ebp
```
the previous part, the esi register contains the address of the boot_params structure which was filled before 
we moved to the protected mode. The boot_params structure contains a special field scratch with an offset of 0x1e4. 
This four byte field is a temporary stack for the call instruction. We set esp to the address four bytes after 
the BP_scratch field of the boot_params structure. We add 4 bytes to the base of the BP_scratch field because, 
as just described, it will be a temporary stack and the stack grows from the top to bottom in the x86_64 architecture.
So our stack pointer will point to the top of the temporary stack. 

Next,  We make a call to the 1f label and pop the top of the stack onto ebp. This works because call stores 
the return address of the current function on the top of the stack. We now have the address of the 1f label 
and can now easily get the address of the startup_32 function. We just need to subtract the address 
of the label from the address we got from the stack:
```
startup_32 (0x0)     +-----------------------+
                     |                       |
                     |                       |
                     |                       |
                     |                       |
                     |                       |
                     |                       |
                     |                       |
                     |                       |
1f (0x0 + 1f offset) +-----------------------+ %ebp - real physical address
                     |                       |
                     |                       |
                     +-----------------------+
```
The startup_32 function is linked to run at the address 0x0 and this means that 1f has the address 0x0 + offset to 1f, 
which is approximately 0x21 bytes. The ebp register contains the real physical address of the 1f label. 
So, if we subtract 1f from the ebp register, we will get the real physical address of the startup_32 function.

The Linux kernel boot protocol saysthe base of the protected mode kernel is 0x100000. We can verify this with gdb. 
Let's start the debugger and add a breakpoint at the address of 1f, which is 0x100021. If this is correct we will 
see the value 0x100021 in the ebp register:
```
$ gdb
(gdb)$ target remote :1234
Remote debugging using :1234
0x0000fff0 in ?? ()
(gdb)$ br *0x100022
Breakpoint 1 at 0x100022
(gdb)$ c
Continuing.

Breakpoint 1, 0x00100022 in ?? ()
(gdb)$ i r
eax            0x18    0x18
ecx            0x0    0x0
edx            0x0    0x0
ebx            0x0    0x0
esp            0x144a8    0x144a8
ebp            0x100021    0x100021
esi            0x142c0    0x142c0
edi            0x0    0x0
eip            0x100022    0x100022
eflags         0x46    [ PF ZF ]
cs             0x10    0x10
ss             0x18    0x18
ds             0x18    0x18
es             0x18    0x18
fs             0x18    0x18
gs             0x18    0x18
```
If we execute the next instruction, subl $1b, %ebp, we will see:
```
(gdb) nexti
...
ebp            0x100000    0x100000
...
```
we've verified that the address of the startup_32 function is 0x100000. After we know the address of the startup_32 label, 
we can prepare for the transition to long mode. Our next goal is to setup the stack and verify that the CPU supports long mode and SSE.



Stack setup and CPU verification
We can't set up the stack until we know where in memory the startup_32 label is. If we imagine the stack as an array, 
the stack pointer register esp must point to the end of it. Of course, we can define an array in our code, 
but we need to know its actual address to configure the stack pointer correctly.
```
/* setup a stack and make sure cpu supports long mode. */
	movl	$boot_stack_end, %eax
	addl	%ebp, %eax
	movl	%eax, %esp

	call	verify_cpu
	testl	%eax, %eax
	jnz	no_longmode
```
boot_stack_end is defined:
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/head_64.S
```
/*
 * Stack and heap for uncompression
 */
	.bss
	.balign 4
boot_heap:
	.fill BOOT_HEAP_SIZE, 1, 0
boot_stack:
	.fill BOOT_STACK_SIZE, 1, 0
boot_stack_end:
```
we put the address of boot_stack_end into the eax register, so the eax register contains the address
of boot_stack_end as it was linked, which is 0x0 + boot_stack_end. To get the real address of boot_stack_end, 
we need to add the real address of the startup_32 function. We've already found this address and put it into the ebp register.

After we have set up the stack, the next step is CPU verification. Since we are transitioning to long mode,
we need to check that the CPU supports long mode and SSE
```
call	verify_cpu
	testl	%eax, %eax
	jnz	no_longmode
```

https://github.com/torvalds/linux/blob/v4.16/arch/x86/kernel/verify_cpu.S
```
...
#ifndef __x86_64__
	pushfl				# standard way to check for cpuid
	popl	%eax
	movl	%eax,%ebx
	xorl	$0x200000,%eax
	pushl	%eax
	popfl
	pushfl
	popl	%eax
	cmpl	%eax,%ebx
	jz	.Lverify_cpu_no_longmode	# cpu has no cpuid
...
.Lverify_cpu_no_longmode:
	popf				# Restore caller passed flags
	movl $1,%eax
	ret
.Lverify_cpu_sse_ok:
	popf				# Restore caller passed flags
	xorl %eax, %eax
	ret	
```
just contains a couple of calls to the cpuid instruction. This instruction is used to get information about the processor. 
In our case, it checks for long mode and SSE support and sets the eax register to 0 on success and 1 on failure.

If the value of eax is not zero, we jump to the no_longmode label which just stops the CPU with 
the hlt instruction while no hardware interrupt can happen:
```
no_longmode:
	/* This isn't an x86-64 CPU so hang */
1:
	hlt
	jmp     1b
```

https://metaso.cn/search/8460533247083786240
在x86汇编语言中，"HLT"指令是一个停机指令，用于让处理器停止执行当前的指令序列。当处理器执行到HLT指令时，它会进入一个低功耗状态，并停止运行其他指令，
直到接收到中断信号




Calculate the relocation address
The next step is to calculate the relocation address for decompression if needed. First, we need to know 
what it means for a kernel to be relocatable. We already know that the base address of the 32-bit entry point 
of the Linux kernel is 0x100000, but that is a 32-bit entry point. 
The default base address of the Linux kernel is determined by the value of the CONFIG_PHYSICAL_START kernel configuration option.
Its default value is 0x1000000 or 16 MB. The main problem here is that if the Linux kernel crashes,
a kernel developer must have a rescue kernel for kdump which is configured to load from a different address. 
The Linux kernel provides a special configuration option to solve this problem: CONFIG_RELOCATABLE. 
As we can read in the documentation of the Linux kernel:
```
This builds a kernel image that retains relocation information
so it can be loaded someplace besides the default 1MB.

Note: If CONFIG_RELOCATABLE=y, then the kernel runs from the address
it has been loaded at and the compile time physical address
(CONFIG_PHYSICAL_START) is used as the minimum location.
```


Reload the segments if needed
```
#define __HEAD        .section    ".head.text","ax"
```
Here, .head.text is the name of the section and ax is a set of flags. In our case, these flags show us 
that this section is [executable]
In simple terms, this means that a Linux kernel with this option set can be booted from different addresses.
Technically, this is done by compiling the decompressor as position independent code.

position independent code
https://en.wikipedia.org/wiki/Position-independent_code
在计算机领域中，地址无关代码 (英文: position-independent code，缩写为PIC)，又称地址无关可执行文件 (英文: position-independent executable，缩写为PIE)
，是指可在主存储器中任意位置正确地运行，而不受其绝对地址影响的一种机器码。PIC广泛使用于共享库，使得同一个库中的代码能够被加载到不同进程的地址空间中。
PIC还用于缺少内存管理单元的计算机系统中， 使得操作系统能够在单一的地址空间中将不同的运行程序隔离开来。


we can see that the decompressor is indeed compiled with the -fPIC flag:
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/Makefile
```
KBUILD_CFLAGS += -fno-strict-aliasing $(call cc-option, -fPIE, -fPIC)
```
When we are using position-independent code an address is obtained by adding the address field of the 
instruction to the value of the program counter. We can load code which uses such addressing from any address. 
That's why we had to get the real physical address of startup_32.

Our current goal is to calculate an address where we can relocate the kernel for decompression. 
The calculation of this address depends on the CONFIG_RELOCATABLE kernel configuration option.
linux/arch/x86/boot/compressed/head_64.S
```
#ifdef CONFIG_RELOCATABLE
	movl	%ebp, %ebx
	movl	BP_kernel_alignment(%esi), %eax
	decl	%eax
	addl	%eax, %ebx
	notl	%eax
	andl	%eax, %ebx
	cmpl	$LOAD_PHYSICAL_ADDR, %ebx
	jge	1f
#endif
	movl	$LOAD_PHYSICAL_ADDR, %ebx
```
Remember that the value of the ebp register is the physical address of the startup_32 label. 
If the CONFIG_RELOCATABLE kernel configuration option is enabled during kernel configuration, 
we put this address in the ebx register, align it to a multiple of 2MB and compare it with the result of the LOAD_PHYSICAL_ADDR macro
https://github.com/torvalds/linux/blob/v4.16/arch/x86/include/asm/boot.h
```
/* Physical address where kernel should be loaded. */
#define LOAD_PHYSICAL_ADDR ((CONFIG_PHYSICAL_START \
				+ (CONFIG_PHYSICAL_ALIGN - 1)) \
				& ~(CONFIG_PHYSICAL_ALIGN - 1))
```
As we can see it just expands to the aligned CONFIG_PHYSICAL_ALIGN value which represents the physical address 
where the kernel will be loaded.
After comparing LOAD_PHYSICAL_ADDR and the value of the ebx register, we add the offset from startup_32 where we will decompress 
the compressed kernel image. If the CONFIG_RELOCATABLE option is not enabled during kernel configuration, 
we just add z_extract_offset to the default address where the kernel is loaded.

After all of these calculations, ebp will contain the address where we loaded the kernel and ebx will 
contain the address where the decompressed kernel will be relocated. But that is not the end. 
The compressed kernel image should be moved to the end of the decompression buffer to simplify calculations 
regarding where the kernel will be located later.
```
1:

	/* Target address to relocate to for decompression */
	movl	BP_init_size(%esi), %eax
	subl	$_end, %eax
	addl	%eax, %ebx	
```
we put the value from the boot_params.BP_init_size field (or the kernel setup header value from hdr.init_size) in the eax register.
The BP_init_size field contains the larger of the compressed and uncompressed vmlinux sizes. 
Next we subtract the address of the _end symbol from this value and add the result of the subtraction 
to the ebx register which will store the base address for kernel decompression.

https://en.wikipedia.org/wiki/Vmlinux
在linux系统中，vmlinux（vmlinuz）是一个包含linux kernel的静态链接的可执行文件，文件类型可能是linux接受的可执行文件格式之一（ELF、COFF或a.out），
vmlinux若要用于调试时则必须要在引导前增加symbol table。



Preparation before entering long mode
After we get the address to relocate the compressed kernel image to, we need to do one last step before we can transition to 64-bit mode.
First, we need to update the Global Descriptor Table with 64-bit segments because a relocatable kernel 
is runnable at any address below 512GB:
```
/*
 * Prepare for entering 64 bit mode
 */

	/* Load new GDT with the 64bit segments using 32bit descriptor */
	addl	%ebp, gdt+2(%ebp)
	lgdt	gdt(%ebp)
```
Here we adjust the base address of the Global Descriptor table to the address where 
we actually loaded the kernel and load the Global Descriptor Table with the lgdt instruction.

To understand the magic with gdt offsets we need to look at the definition of the Global Descriptor Table.
We can find its definition in the same source code
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/head_64.S
```
gdt:
	.word	gdt_end - gdt
	.long	gdt
	.word	0
	.quad	0x00cf9a000000ffff	/* __KERNEL32_CS */
	.quad	0x00af9a000000ffff	/* __KERNEL_CS */
	.quad	0x00cf92000000ffff	/* __KERNEL_DS */
	.quad	0x0080890000000000	/* TS descriptor */
	.quad   0x0000000000000000	/* TS continued */
gdt_end:
```
We can see that it is located in the .data section and contains five descriptors: the first is a 32-bit descriptor 
for the kernel code segment, a 64-bit kernel segment, a kernel data segment and two task descriptors.

We already loaded the Global Descriptor Table in the previous part, and now we're doing almost the same here,
but we set descriptors to use CS.L = 1 and CS.D = 0 for execution in 64 bit mode. As we can see, 
the definition of the gdt starts with a two byte value: gdt_end - gdt which represents the address of 
the last byte in the gdt table or the table limit. The next four bytes contain the base address of the gdt.


After we have loaded the Global Descriptor Table with the lgdt instruction, we must enable PAE 
by putting the value of the cr4 register into eax, setting the 5th bit and loading it back into cr4:
https://zh.wikipedia.org/zh-cn/%E7%89%A9%E7%90%86%E5%9C%B0%E5%9D%80%E6%89%A9%E5%B1%95
物理地址扩展（Physical Address Extension，缩写为PAE），又释实体位置延伸，是x86处理器的一个功能，让中央处理器在32位操作系统下访问超过4GB的物理内存。
```
/* Enable PAE mode */
	movl	%cr4, %eax
	orl	$X86_CR4_PAE, %eax
	movl	%eax, %cr4
```



here is some information about long mode
Long mode is the native mode for x86_64 processors. First, let's look at some differences between x86_64 and x86.

64-bit mode provides the following features:
1 8 new general purpose registers from r8 to r15
2  All general purpose registers are 64-bit now
3  A 64-bit instruction pointer - RIP
4  A new operating mode - Long mode;
5  64-Bit Addresses and Operands;
6 RIP Relative Addressing (we will see an example of this in the coming parts).

Long mode is an extension of the legacy protected mode. It consists of two sub-modes:
1 64-bit mode;
2 compatibility mode.

To switch into 64-bit mode we need to do the following things:
1 Enable PAE;
2 Build page tables and load the address of the top level page table into the cr3 register;
3 Enable EFER.LME;
4 Enable paging.


We already enabled PAE by setting the PAE bit in the cr4 control register. Our next goal is to build the structure for paging.
We will discuss this in the next paragraph.




Early page table initialization
We already know that before we can move into 64-bit mode, we need to build page tables. 
Let's look at how the early 4G boot page tables are built.
The Linux kernel uses 4-level paging, and we generally build 6 page tables:
1 One PML4 or Page Map Level 4 table with one entry;
2 One PDP or Page Directory Pointer table with four entries;
3 Four Page Directory tables with a total of 2048 entries.

Let's look at how this is implemented. First, we clear the buffer for the page tables in memory.
Every table is 4096 bytes, so we need clear a 24 kilobyte buffer:
```
/* Initialize Page tables to 0 */
	leal	pgtable(%ebx), %edi
	xorl	%eax, %eax
	movl	$(BOOT_INIT_PGT_SIZE/4), %ecx
	rep	stosl
```
We put the address of pgtable with an offset of ebx (remember that ebx points to the location in memory 
where the kernel will be decompressed later) into the edi register, clear the eax register and set the ecx register to 6144.

The rep stosl instruction will write the value of eax to the memory location where edi points to, 
increment edi by 4, and decrement ecx by 1. This operation will be repeated while the value of the ecx register is greater than zero. 
That's why we put 6144 or BOOT_INIT_PGT_SIZE/4 in ecx.

https://metaso.cn/search/8460567789345652736
rep stosl指令在x86汇编中的主要作用是重复将%eax中的4个字节复制到%di指向的位置，直到ecx寄存器的值变为0。这种用法通常用于需要重复写入数据到内存特定位置的场景，
如内存填充或数据复制等操作

pgtable
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/head_64.S
```
/*
 * Space for page tables (not in .bss so not zeroed)
 */
	.section ".pgtable","a",@nobits
	.balign 4096
pgtable:
	.fill BOOT_PGT_SIZE, 1, 0
```
it is located in the .pgtable section and its size depends on the CONFIG_X86_VERBOSE_BOOTUP kernel configuration option:
arch/x86/include/asm/boot.h
```
#  ifdef CONFIG_X86_VERBOSE_BOOTUP
#   define BOOT_PGT_SIZE    (19*4096)
#  else /* !CONFIG_X86_VERBOSE_BOOTUP */
#   define BOOT_PGT_SIZE    (17*4096)
#  endif
# else /* !CONFIG_RANDOMIZE_BASE */
#  define BOOT_PGT_SIZE        BOOT_INIT_PGT_SIZE
# endif
```

After we have a buffer for the pgtable structure, we can start to build the top level page table - PML4 - with:
```
/* Build Level 4 */
	leal	pgtable + 0(%ebx), %edi
	leal	0x1007 (%edi), %eax
	movl	%eax, 0(%edi)
	addl	%edx, 4(%edi)
```
we put the address of pgtable relative to ebx or in other words relative to address of startup_32 in the edi register. 
Next, we put this address with an offset of 0x1007 into the eax register. 0x1007 is the result of 
    adding the size of the PML4 table which is 4096 or 0x1000 bytes with 7. The 7 here represents the flags associated with the PML4 entry.  
   In our case, these flags are PRESENT+RW+USER. 
In the end, we just write the address of the first PDP entry to the PML4 table.


In the next step we will build four Page Directory entries in the Page Directory Pointer table with the same 
PRESENT+RW+USE flags:
```
/* Build Level 3 */
	leal	pgtable + 0x1000(%ebx), %edi
	leal	0x1007(%edi), %eax
	movl	$4, %ecx
1:	movl	%eax, 0x00(%edi)
	addl	%edx, 0x04(%edi)
	addl	$0x00001000, %eax
	addl	$8, %edi
	decl	%ecx
	jnz	1b
```
We set edi to the base address of the page directory pointer which is at an offset of 4096 or 0x1000 bytes 
from the pgtable table and eax to the address of the first page directory pointer entry. 
We also set ecx to 4 to act as a counter in the following loop and write the address of the first page directory pointer table entry 
to the edi register. After this, edi will contain the address of the first page directory pointer entry with flags 0x7.
Next we calculate the address of the following page directory pointer entries — each entry is 8 bytes — 
and write their addresses to eax.

The last step in building the paging structure is to build the 2048 page table entries with 2-MByte pages:
```
/* Build Level 2 */
	leal	pgtable + 0x2000(%ebx), %edi
	movl	$0x00000183, %eax
	movl	$2048, %ecx
1:	movl	%eax, 0(%edi)
	addl	%edx, 4(%edi)
	addl	$0x00200000, %eax
	addl	$8, %edi
	decl	%ecx
	jnz	1b
```
Here we do almost the same things that we did in the previous example, all entries are associated with these flags - $0x00000183 - PRESENT + WRITE + MBZ. 
In the end, we will have a page table with 2048 2-MByte pages, which represents a 4 Gigabyte block of memory:
```
>>> 2048 * 0x00200000
4294967296
```
Since we've just finished building our early page table structure which maps 4 gigabytes of memory, 
we can put the address of the high-level page table - PML4 - into the cr3 control register:
```
	/* Enable the boot page tables */
	leal	pgtable(%ebx), %eax
	movl	%eax, %cr3
```



The transition to 64-bit mode
First of all we need to set the EFER.LME flag in the MSR to 0xC0000080:


MSR  https://en.wikipedia.org/wiki/Model-specific_register
A model-specific register (MSR) is any of various control registers in the x86 system architecture used for debugging, 
program execution tracing, computer performance monitoring, and toggling certain CPU features.
```
/* Enable Long mode in EFER (Extended Feature Enable Register) */
	movl	$MSR_EFER, %ecx
	rdmsr
	btsl	$_EFER_LME, %eax
	wrmsr
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/include/asm/msr-index.h
```
#define MSR_EFER		0xc0000080 /* extended feature register */
```
Here we put the MSR_EFER flag  in the ecx register
and execute the rdmsr instruction which reads the MSR register. After rdmsr executes, 
the resulting data is stored in edx:eax according to the MSR register specified in ecx. We check the current EFER_LME bit, 
transfer it into the carry flag and update the bit, all with the btsl instruction. 
Then we write data from edx:eax back to the MSR register with the wrmsr instruction.

In the next step, we push the address of the kernel segment code to the stack (we defined it in the GDT) 
and put the address of the startup_64 routine in eax.
```
pushl	$__KERNEL_CS
leal	startup_64(%ebp), %eax
```
After this we push eax to the stack and enable paging by setting the PG and PE bits in the cr0 register:
```
pushl	%eax

	/* Enter paged protected Mode, activating Long Mode */
	movl	$(X86_CR0_PG | X86_CR0_PE), %eax /* Enable Paging and Protected mode */
	movl	%eax, %cr0
/* Jump from 32bit compatibility mode into 64bit mode. */
	lret	
```
we pushed the address of the startup_64 function to the stack in the previous step. 
The CPU extracts startup_64's address from the stack and jumps there.


After all of these steps we're finally in 64-bit mode:
```
.code64
	.org 0x200
ENTRY(startup_64)
...
```


the history of virtual memory
https://wiki.osdev.org/Paging
x86 Paging Tutorial