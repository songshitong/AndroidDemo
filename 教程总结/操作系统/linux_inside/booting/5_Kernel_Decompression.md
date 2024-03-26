
https://0xax.gitbooks.io/linux-insides/content/Booting/linux-bootstrap-5.html

We will study the steps taken to prepare for kernel decompression, relocation and the process of kernel decompression itself.


Preparing to Decompress the Kernel
Since we have loaded a new Global Descriptor Table and the CPU has transitioned to a new mode (64-bit mode in our case), 
we set up the segment registers again at the beginning of the startup_64 function:
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/head_64.S
```
	.code64
	.org 0x200
ENTRY(startup_64)
/* Setup data segments. */
	xorl	%eax, %eax
	movl	%eax, %ds
	movl	%eax, %es
	movl	%eax, %ss
	movl	%eax, %fs
	movl	%eax, %gs
```
All segment registers besides the cs register are now reset in long mode.

The next step is to compute the difference between the location the kernel was compiled to be loaded at 
and the location where it is actually loaded:
```
#ifdef CONFIG_RELOCATABLE
	leaq	startup_32(%rip) /* - $startup_32 */, %rbp
	movl	BP_kernel_alignment(%rsi), %eax
	decl	%eax
	addq	%rax, %rbp
	notq	%rax
	andq	%rax, %rbp
	cmpq	$LOAD_PHYSICAL_ADDR, %rbp
	jge	1f
#endif
	movq	$LOAD_PHYSICAL_ADDR, %rbp
1:

	/* Target address to relocate to for decompression */
	movl	BP_init_size(%rsi), %ebx
	subl	$_end, %ebx
	addq	%rbp, %rbx
```
The rbp register contains the decompressed kernel's start address. After this code executes, 
the rbx register will contain the address where the kernel code will be relocated to for decompression.
We've already done this before in the startup_32 function, but we need to do this calculation again 
   because the bootloader can use the 64-bit boot protocol now and startup_32 is no longer being executed.

In the next step we set up the stack pointer, reset the flags register and set up the GDT again to 
  overwrite the 32-bit specific values with those from the 64-bit protocol:
```
	/* Set up the stack */
	leaq	boot_stack_end(%rbx), %rsp

    leaq    gdt(%rip), %rax    //这行没有找到 todo
    movq    %rax, gdt64+2(%rip)//这行没有找到 todo
    lgdt    gdt64(%rip)        //这行没有找到 todo

	/* Zero EFLAGS */
	pushq	$0
	popfq
```
If you take a look at the code after the lgdt gdt64(%rip) instruction, you will see that there is some additional code. 
This code builds the trampoline to enable 5-level pagging if needed. We will only consider 4-level paging in this book, 
so this code will be omitted.

As you can see above, the rbx register contains the start address of the kernel decompressor code and 
we just put this address with an offset of boot_stack_end in the rsp register which points to the top of the stack. 
After this step, the stack will be correct.
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

.section ".pgtable","a",@nobits
	.balign 4096
pgtable:
```
boot_stack_end located in the end of the .bss section, right before .pgtable. 
the definitions of .bss and .pgtable
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/vmlinux.lds.S
```
.bss : {
		_bss = . ;
		*(.bss)
		*(.bss.*)
		*(COMMON)
		. = ALIGN(8);	/* For convenience during zeroing */
		_ebss = .;
	}
...
#ifdef CONFIG_X86_64
       . = ALIGN(PAGE_SIZE);
       .pgtable : {
		_pgtable = . ;
		*(.pgtable)
		_epgtable = . ;
	}
#endif	
```


Since the stack is now correct, we can copy the compressed kernel to the address that we got above,
when we calculated the relocation address of the decompressed kernel.
```
/*
 * Copy the compressed kernel to the end of our buffer
 * where decompression in place becomes safe.
 */
	pushq	%rsi
	leaq	(_bss-8)(%rip), %rsi
	leaq	(_bss-8)(%rbx), %rdi
	movq	$_bss /* - $startup_32 */, %rcx
	shrq	$3, %rcx
	std
	rep	movsq
	cld
	popq	%rsi
```
This set of instructions copies the compressed kernel over to where it will be decompressed.

First of all we push rsi to the stack. We need preserve the value of rsi, because this register now 
stores a pointer to boot_params which is a real mode structure that contains booting related data (remember,
this structure was populated at the start of the kernel setup). We pop the pointer to boot_params back to
rsi after we execute this code.
The next two leaq instructions calculate the effective addresses of the rip and rbx registers with 
an offset of _bss - 8 and assign the results to rsi and rdi respectively. Why do we calculate these addresses?
The compressed kernel image is located between this code (from startup_32 to the current code) and the decompression code.
You can verify this by looking at this linker script:
arch/x86/boot/compressed/vmlinux.lds.S
```
. = 0;
	.head.text : {
		_head = . ;
		HEAD_TEXT
		_ehead = . ;
	}
	.rodata..compressed : {
		*(.rodata..compressed)
	}
	.text :	{
		_text = .; 	/* Text */
		*(.text)
		*(.text.*)
		_etext = . ;
	}
```
Note that the .head.text section contains startup_32
```
__HEAD
    .code32
ENTRY(startup_32)
...
```
The .text section contains the decompression code:
arch/x86/boot/compressed/head_64.S
```
.text
relocated:
```
And .rodata..compressed contains the compressed kernel image. So rsi will contain the absolute address of _bss - 8, 
and rdi will contain the relocation relative address of _bss - 8. In the same way we store these addresses in registers, 
we put the address of _bss in the rcx register. As you can see in the vmlinux.lds.S linker script, 
it's located at the end of all sections with the setup/kernel code. Now we can start copying data from rsi to rdi,
8 bytes at a time, with the movsq instruction.

Note that we execute an std instruction before copying the data. This sets the DF flag, which means 
that rsi and rdi will be decremented. In other words, we will copy the bytes backwards. At the end, 
we clear the DF flag with the cld instruction, and restore the boot_params structure to rsi.

Now we have a pointer to the .text section's address after relocation, and we can jump to it:
```
/*
 * Jump to the relocated address.
 */
	leaq	relocated(%rbx), %rax
	jmp	*%rax
```


The final touches before kernel decompression
In the previous paragraph we saw that the .text section starts with the relocated label. The first thing \
we do is to clear the bss section with:
```
	.text
relocated:
/*
 * Clear BSS (stack is currently empty)
 */
	xorl	%eax, %eax
	leaq    _bss(%rip), %rdi
	leaq    _ebss(%rip), %rcx
	subq	%rdi, %rcx
	shrq	$3, %rcx
	rep	stosq
```
We need to initialize the .bss section, because we'll soon jump to C code. Here we just clear eax, 
put the addresses of _bss in rdi and _ebss in rcx, and fill .bss with zeros with the rep stosq instruction.

At the end, we can see a call to the extract_kernel function:
```
/*
 * Do the extraction, and jump to the new kernel..
 */
	pushq	%rsi			/* Save the real mode argument */
	movq	%rsi, %rdi		/* real mode address */
	leaq	boot_heap(%rip), %rsi	/* malloc area for uncompression */
	leaq	input_data(%rip), %rdx  /* input_data */
	movl	$z_input_len, %ecx	/* input_len */
	movq	%rbp, %r8		/* output target address */
	movq	$z_output_len, %r9	/* decompressed length, end of relocs */
	call	extract_kernel		/* returns kernel location in %rax */
	popq	%rsi

/*
 * Jump to the decompressed kernel.
 */
	jmp	*%rax
```    
todo 汇编调用C，并传递参数
Like before, we push rsi onto the stack to preserve the pointer to boot_params. We also copy the contents of rsi to rdi.
Then, we set rsi to point to the area where the kernel will be decompressed.
The last step is to prepare the parameters for the extract_kernel function and call it to decompress the kernel.

The extract_kernel function  takes six arguments:
1 rmode - a pointer to the boot_params structure which is filled by either the bootloader or during early kernel initialization;
2 heap - a pointer to boot_heap which represents the start address of the early boot heap;
3 input_data - a pointer to the start of the compressed kernel or in other words, a pointer to the arch/x86/boot/compressed/vmlinux.bin.bz2 file;
vmlinux是未压缩的内核，是make工作编译出的原始内核，vmlinuz是vmlinux的压缩文件。
vmlinux 是ELF文件，即编译出来的最原始的文件
vmlinux.bin.bz2 是压缩包
4 input_len - the size of the compressed kernel;
5 output - the start address of the decompressed kernel;
6 output_len - the size of the decompressed kernel;

https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/misc.c
```
asmlinkage __visible void *extract_kernel(void *rmode, memptr heap,
				  unsigned char *input_data,
				  unsigned long input_len,
				  unsigned char *output,
				  unsigned long output_len)
{
....
}
```
All arguments will be passed through registers as per the System V Application Binary Interface. 
We've finished all the preparations and can now decompress the kernel

System V ABI  https://wiki.osdev.org/System_V_ABI
The System V Application Binary Interface is a set of specifications that detail calling conventions, 
object file formats, executable file formats, dynamic linking semantics, and much more for systems that complies with 
the X/Open Common Application Environment Specification and the System V Interface Definition. 
It is today the standard ABI used by the major Unix operating systems such as Linux, the BSD systems, 
and many others. The Executable and Linkable Format (ELF) is part of the System V ABI.






Kernel decompression
extract_kernel
This function starts with the video/console initialization that we already saw in the previous parts. 
We need to do this again because we don't know if we started in real mode or if a bootloader was used, 
or whether the bootloader used the 32 or 64-bit boot protocol.

After the first initialization steps, we store pointers to the start of the free memory and to the end of it:

https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/compressed/misc.c
```
	free_mem_ptr     = heap;	/* Heap */
	free_mem_end_ptr = heap + BOOT_HEAP_SIZE;
```
heap is the second parameter of the extract_kernel function as passed to it in
```
leaq    boot_heap(%rip), %rsi
....
	.bss
	.balign 4
boot_heap:
	.fill BOOT_HEAP_SIZE, 1, 0
...	
```
arch/x86/include/asm/boot.h
```
# define BOOT_HEAP_SIZE		0x400000
```
where BOOT_HEAP_SIZE is a macro which expands to 0x10000 (0x400000 in thecase of a bzip2 kernel) and 
represents the size of the heap.

After we initialize the heap pointers, the next step is to call the choose_random_location function
As we can guess from the function name, it chooses a memory location to write the decompressed kernel to. 
It may look weird that we need to find or even choose where to decompress the compressed kernel image, 
but the Linux kernel supports kASLR which allows decompression of the kernel into a random address, for security reasons.
We'll take a look at how the kernel's load address is randomized in the next part

ASLR
https://en.wikipedia.org/wiki/Address_space_layout_randomization
Address space layout randomization (ASLR) is a computer security technique involved in preventing 
exploitation of memory corruption vulnerabilities. In order to prevent an attacker from reliably 
redirecting code execution to, for example, a particular exploited function in memory,
ASLR randomly arranges the address space positions of key data areas of a process, 
including the base of the executable and the positions of the stack, heap and libraries.



Now let's get back to misc.c. After getting the address for the kernel image, we need to check that
the random address we got is correctly aligned, and in general, not wrong:
```
/* Validate memory location choices. */
	if ((unsigned long)output & (MIN_KERNEL_ALIGN - 1))
		error("Destination physical address inappropriately aligned");
	if (virt_addr & (MIN_KERNEL_ALIGN - 1))
		error("Destination virtual address inappropriately aligned");
#ifdef CONFIG_X86_64
	if (heap > 0x3fffffffffffUL)
		error("Destination address too large");
	if (virt_addr + max(output_len, kernel_total_size) > KERNEL_IMAGE_SIZE)
		error("Destination virtual address is beyond the kernel mapping area");
#else
	if (heap > ((-__PAGE_OFFSET-(128<<20)-1) & 0x7fffffff))
		error("Destination address too large");
#endif
#ifndef CONFIG_RELOCATABLE
	if ((unsigned long)output != LOAD_PHYSICAL_ADDR)
		error("Destination address does not match LOAD_PHYSICAL_ADDR");
	if (virt_addr != LOAD_PHYSICAL_ADDR)
		error("Destination virtual address changed when not relocatable");
#endif
```
After all these checks we will see the familiar message:
```
debug_putstr("\nDecompressing Linux... ");
```
Now, we call the __decompress function to decompress the kernel:
```
__decompress(input_data, input_len, NULL, NULL, output, output_len,
			NULL, error);
```
The implementation of the __decompress function depends on what decompression algorithm was chosen during kernel compilation:
arch/arm/boot/compressed/decompress.c
```
#ifdef CONFIG_KERNEL_GZIP
#include "../../../../lib/decompress_inflate.c"
#endif

#ifdef CONFIG_KERNEL_LZO
#include "../../../../lib/decompress_unlzo.c"
#endif

#ifdef CONFIG_KERNEL_LZMA
#include "../../../../lib/decompress_unlzma.c"
#endif
```

After the kernel is decompressed, two more functions are called: parse_elf and handle_relocations. 
The main point of these functions is to move the decompressed kernel image to its correct place in memory. 
This is because the decompression is done in-place, and we still need to move the kernel to the correct address. 
As we already know, the kernel image is an ELF executable. 
The main goal of the parse_elf function is to move loadable segments to the correct address.

In-place algorithm
https://en.wikipedia.org/wiki/In-place_algorithm
In computer science, an in-place algorithm is an algorithm that operates directly on the input data structure
without requiring extra space proportional to the input size. In other words, it modifies the input in place,
without creating a separate copy of the data structure. An algorithm which is not in-place is sometimes 
called not-in-place or out-of-place.

We can see the kernel's loadable segments in the output of the readelf program:
readelf：显示有关ELF文件的信息
```
readelf -l vmlinux

Elf file type is EXEC (Executable file)
Entry point 0x1000000
There are 5 program headers, starting at offset 64

Program Headers:
  Type           Offset             VirtAddr           PhysAddr
                 FileSiz            MemSiz              Flags  Align
  LOAD           0x0000000000200000 0xffffffff81000000 0x0000000001000000
                 0x0000000000893000 0x0000000000893000  R E    200000
  LOAD           0x0000000000a93000 0xffffffff81893000 0x0000000001893000
                 0x000000000016d000 0x000000000016d000  RW     200000
  LOAD           0x0000000000c00000 0x0000000000000000 0x0000000001a00000
                 0x00000000000152d8 0x00000000000152d8  RW     200000
  LOAD           0x0000000000c16000 0xffffffff81a16000 0x0000000001a16000
                 0x0000000000138000 0x000000000029b000  RWE    200000
```


The goal of the parse_elf function is to load these segments to the output address we got from the choose_random_location function.
This function starts by checking the ELF signature:
arch/x86/boot/compressed/misc.c
```
static void parse_elf(void *output)
{
#ifdef CONFIG_X86_64
	Elf64_Ehdr ehdr;
	Elf64_Phdr *phdrs, *phdr;
#else
	Elf32_Ehdr ehdr;
	Elf32_Phdr *phdrs, *phdr;
#endif
	void *dest;
	int i;

	memcpy(&ehdr, output, sizeof(ehdr));
	if (ehdr.e_ident[EI_MAG0] != ELFMAG0 ||
	   ehdr.e_ident[EI_MAG1] != ELFMAG1 ||
	   ehdr.e_ident[EI_MAG2] != ELFMAG2 ||
	   ehdr.e_ident[EI_MAG3] != ELFMAG3) {
		error("Kernel is not a valid ELF file");
		return;
	}
   ...
}
```

If the ELF header is not valid, it prints an error message and halts. If we have a valid ELF file, 
we go through all the program headers from the given ELF file and copy all loadable segments with
correct 2 megabyte aligned addresses to the output buffer:
megabyte:MB
```
	debug_putstr("Parsing ELF... ");

	phdrs = malloc(sizeof(*phdrs) * ehdr.e_phnum);
	if (!phdrs)
		error("Failed to allocate space for phdrs");

	memcpy(phdrs, output + ehdr.e_phoff, sizeof(*phdrs) * ehdr.e_phnum);

	for (i = 0; i < ehdr.e_phnum; i++) {
		phdr = &phdrs[i];

		switch (phdr->p_type) {
		case PT_LOAD:
#ifdef CONFIG_X86_64
			if ((phdr->p_align % 0x200000) != 0)
				error("Alignment of LOAD segment isn't multiple of 2MB");
#endif
#ifdef CONFIG_RELOCATABLE
			dest = output;
			dest += (phdr->p_paddr - LOAD_PHYSICAL_ADDR);
#else
			dest = (void *)(phdr->p_paddr);
#endif
			memmove(dest, output + phdr->p_offset, phdr->p_filesz);
			break;
		default: /* Ignore other PT_* */ break;
		}
	}

	free(phdrs);
```
From this moment, all loadable segments are in the correct place.

The next step after the parse_elf function is to call the handle_relocations function. 
The implementation of this function depends on the CONFIG_X86_NEED_RELOCS kernel configuration option and if it is enabled,
this function adjusts addresses in the kernel image. This function is also only called if the CONFIG_RANDOMIZE_BASE configuration 
option was enabled during kernel configuration. The implementation of the handle_relocations function is easy enough.
This function subtracts the value of LOAD_PHYSICAL_ADDR from the value of the base load address of the kernel and
thus we obtain the difference between where the kernel was linked to load and where it was actually loaded.
After this we can relocate the kernel since we know the actual address where the kernel was loaded, 
the address where it was linked to run and the relocation table which is at the end of the kernel image.
arch/x86/boot/compressed/misc.c
```
#if CONFIG_X86_NEED_RELOCS
static void handle_relocations(void *output, unsigned long output_len,
			       unsigned long virt_addr)
{
	int *reloc;
	unsigned long delta, map, ptr;
	unsigned long min_addr = (unsigned long)output;
	unsigned long max_addr = min_addr + (VO___bss_start - VO__text);

	/*
	 * Calculate the delta between where vmlinux was linked to load
	 * and where it was actually loaded.
	 */
	delta = min_addr - LOAD_PHYSICAL_ADDR;

	/*
	 * The kernel contains a table of relocation addresses. Those
	 * addresses have the final load address of the kernel in virtual
	 * memory. We are currently working in the self map. So we need to
	 * create an adjustment for kernel memory addresses to the self map.
	 * This will involve subtracting out the base address of the kernel.
	 */
	map = delta - __START_KERNEL_map;

	/*
	 * 32-bit always performs relocations. 64-bit relocations are only
	 * needed if KASLR has chosen a different starting address offset
	 * from __START_KERNEL_map.
	 */
	if (IS_ENABLED(CONFIG_X86_64))
		delta = virt_addr - LOAD_PHYSICAL_ADDR;

	if (!delta) {
		debug_putstr("No relocation needed... ");
		return;
	}
	debug_putstr("Performing relocations... ");

	/*
	 * Process relocations: 32 bit relocations first then 64 bit after.
	 * Three sets of binary relocations are added to the end of the kernel
	 * before compression. Each relocation table entry is the kernel
	 * address of the location which needs to be updated stored as a
	 * 32-bit value which is sign extended to 64 bits.
	 *
	 * Format is:
	 *
	 * kernel bits...
	 * 0 - zero terminator for 64 bit relocations
	 * 64 bit relocation repeated
	 * 0 - zero terminator for inverse 32 bit relocations
	 * 32 bit inverse relocation repeated
	 * 0 - zero terminator for 32 bit relocations
	 * 32 bit relocation repeated
	 *
	 * So we work backwards from the end of the decompressed image.
	 */
	for (reloc = output + output_len - sizeof(*reloc); *reloc; reloc--) {
		long extended = *reloc;
		extended += map;

		ptr = (unsigned long)extended;
		if (ptr < min_addr || ptr > max_addr)
			error("32-bit relocation outside of kernel!\n");

		*(uint32_t *)ptr += delta;
	}
#ifdef CONFIG_X86_64
	while (*--reloc) {
		long extended = *reloc;
		extended += map;

		ptr = (unsigned long)extended;
		if (ptr < min_addr || ptr > max_addr)
			error("inverse 32-bit relocation outside of kernel!\n");

		*(int32_t *)ptr -= delta;
	}
	for (reloc--; *reloc; reloc--) {
		long extended = *reloc;
		extended += map;

		ptr = (unsigned long)extended;
		if (ptr < min_addr || ptr > max_addr)
			error("64-bit relocation outside of kernel!\n");

		*(uint64_t *)ptr += delta;
	}
#endif
}
#else
static inline void handle_relocations(void *output, unsigned long output_len,
				      unsigned long virt_addr)
{ }
#endif
```


After the kernel is relocated, we return from the extract_kernel function
The address of the kernel will be in the rax register and we jump to it:
```
jmp    *%rax
```
Now we are in the kernel!


5-level pagging  todo
https://lwn.net/Articles/708526/