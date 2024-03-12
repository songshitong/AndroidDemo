https://0xax.gitbooks.io/linux-insides/content/Booting/linux-bootstrap-1.html

base on x86_64 architecture
Linux kernel version  3.18

Real mode/real address mode                    https://en.wikipedia.org/wiki/Real_mode
addresses in real mode always correspond to real locations in memory
Real mode provides no support for memory protection, multitasking, or code privilege levels.


From the bootloader to the kernel
The 80386 and later CPUs define the following predefined data in CPU registers after the computer resets:
```
IP          0xfff0
CS selector 0xf000
CS base     0xffff0000
```

Memory segmentation
Memory segmentation is used to make use of all the address space available. All memory is divided into small,
fixed-size segments of 65536 bytes (64 KB). 


An address consists of two parts: a segment selector, which has a base address; and an offset from this base address
in real mode,physical address 
```
PhysicalAddress = Segment Selector * 16 + Offset
```
if CS:IP is 0x2000:0x0010, then the corresponding physical address will be:
```
>>> hex((0x2000 << 4) + 0x0010)
'0x20010'
```


The starting address is formed by adding the base address to the value in the EIP register:
```
>>> 0xffff0000 + 0xfff0
'0xfffffff0'
```
This point is called the reset vector. It's the memory location at which the CPU expects to find 
the first instruction to execute after reset. 
It contains a jump (jmp) instruction that usually points to the BIOS (Basic Input/Output System) entry point.

coreboot/LinuxBIOS
Fast, secure and flexible Open Source firmware

src/cpu/x86/16bit/reset16.inc
https://github.com/coreboot/coreboot/blob/4.4/src/cpu/x86/16bit/reset16.inc
```
    .section ".reset", "ax", %progbits
    .code16
.globl    _start
_start:
    .byte  0xe9   //jmp instruction
    .int   _start16bit - ( . + 2 )
    ...
```
src/cpu/x86/16bit/reset16.ld
```
SECTIONS {
    /* Trigger an error if I have an unuseable start address */
    _bogus = ASSERT(_start16bit >= 0xffff0000, "_start16bit too low. Please report.");
    _ROMTOP = 0xfffffff0;
    . = _ROMTOP;
    .reset . : {
        *(.reset);
        . = 15;
        BYTE(0x00);
    }
}
```
the reset section is 16 bytes and is compiled to start from the address 0xfffffff0



Now the BIOS starts. After initializing and checking the hardware, the BIOS needs to find a bootable device. 
A boot order is stored in the BIOS configuration, controlling which devices the BIOS attempts to boot from.
When attempting to boot from a hard drive, the BIOS tries to find a boot sector. 
On hard drives partitioned with an MBR partition layout, the boot sector is stored in the first 446 bytes of the first sector, 
where each sector is 512 bytes. The final two bytes of the first sector are 0x55 and 0xaa,
which designates to the BIOS that this device is bootable.
```
;
; Note: this example is written in Intel Assembly syntax
;
[BITS 16]

boot:
    mov al, '!'
    mov ah, 0x0e
    mov bh, 0x00
    mov bl, 0x07

    int 0x10  //interrupt
    jmp $

times 510-($-$$) db 0

db 0x55
db 0xaa
```
Build and run this with:
```
nasm -f bin boot.nasm && qemu-system-x86_64 boot
```
This will instruct QEMU to use the boot binary that we just built as a disk image
QEMU will treat the binary as the master boot record (MBR) of a disk image.

A real-world boot sector has code for continuing the boot process and a partition table instead of a 
bunch of 0's and an exclamation mark. :) From this point onwards, the BIOS hands control over to the bootloader.


MBR partition layout https://en.wikipedia.org/wiki/Master_boot_record
A master boot record (MBR) is a special type of boot sector at the very beginning of partitioned computer 
mass storage devices like fixed disks or removable drives intended for use with IBM PC-compatible systems and beyond.

The MBR holds the information on how the disc's sectors (aka "blocks") are divided into partitions, 
each partition notionally containing a file system. The MBR also contains executable code to function as a loader for 
the installed operating system—usually by passing control over to the loader's second stage, 
or in conjunction with each partition's volume boot record (VBR). This MBR code is usually referred to as a boot loader.


Bootloader
There are a number of bootloaders that can boot Linux, such as GRUB 2 and syslinux.
The Linux kernel has a Boot protocol which specifies the requirements for a bootloader to implement Linux support. 
This example will describe GRUB 2.

linux boot protocol
https://github.com/torvalds/linux/blob/v4.16/Documentation/x86/boot.txt#L156

As we can see in the kernel boot protocol, memory will be mapped as follows after loading the kernel:
```
        | Protected-mode kernel  |
100000   +------------------------+
         | I/O memory hole        |
0A0000   +------------------------+
         | Reserved for BIOS      | Leave as much as possible unused
         ~                        ~
         | Command line           | (Can also be below the X+10000 mark)
X+10000  +------------------------+
         | Stack/heap             | For use by the kernel real-mode code.
X+08000  +------------------------+
         | Kernel setup           | The kernel real-mode code.
         | Kernel boot sector     | The kernel legacy boot sector.
       X +------------------------+
         | Boot loader            | <- Boot sector entry point 0x7C00
001000   +------------------------+
         | Reserved for MBR/BIOS  |
000800   +------------------------+
         | Typically used by MBR  |
000600   +------------------------+
         | BIOS use only          |
000000   +------------------------+
```
When the bootloader transfers control to the kernel, it starts at:
```
X + sizeof(KernelBootSector) + 1
```
where X is the address of the kernel boot sector being loaded. we can see in a memory dump

The bootloader has now loaded the Linux kernel into memory, filled the header fields, and then 
jumped to the corresponding memory address. We now move directly to the kernel setup code.



The Beginning of the Kernel Setup Stage


MZ  https://en.wikipedia.org/wiki/DOS_MZ_executable
The DOS MZ executable format is the executable file format used for .EXE files in DOS.
The file can be identified by the ASCII string "MZ" (hexadecimal: 4D 5A) at the beginning of the file (the "magic number").
"MZ" are the initials of Mark Zbikowski, one of the leading developers of MS-DOS.

UEFI  https://en.wikipedia.org/wiki/UEFI
Unified Extensible Firmware Interface  is a specification that defines the architecture of the platform firmware
used for booting the computer hardware and its interface for interaction with the operating system


Execution of the setup part starts from arch/x86/boot/header.S at the _start symbol
```
	.globl	_start
_start:
		.byte	0xeb		#  汇编jump指令
		.byte	start_of_setup-1f
1:
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/header.S

https://blog.csdn.net/lamdoc/article/details/8979243
1f forward  向前跳转到 局部标签1处执行 
1b back     向后跳转到 局部标签1处执行

GRUB2 source code :
```
gs = fs = es = ds = ss = 0x1000
cs = 0x1020
```

After the jump to start_of_setup, the kernel needs to do the following:
1 Make sure that all segment register values are equal
2 Set up a correct stack, if needed
3 Set up bss
4 Jump to the C code in arch/x86/boot/main.c


Aligning the Segment Registers
First of all, the kernel ensures that the ds and es segment registers point to the same address. 
Next, it clears the direction flag using the cld instruction:
//中英文的内容已经不一样了
```
start_of_setup:
# Force %es = %ds
	movw	%ds, %ax
	movw	%ax, %es
	cld
	
...
   pushw   %ds
   pushw   $6f
   lretw	
```
which pushes the value of ds to the stack, followed by the address of the 6 label and executes the lretw instruction. 
When the lretw instruction is called, it loads the address of label 6 into the instruction pointer register 
and loads cs with the value of ds. Afterward, ds and cs will have the same values.

instruction pointer  https://en.wikipedia.org/wiki/Program_counter
The program counter (PC), commonly called the instruction pointer (IP) in Intel x86 and Itanium microprocessors,
and sometimes called the instruction address register (IAR), the instruction counter, or just part of the instruction sequencer,
is a processor register that indicates where a computer is in its program sequence.


Stack Setup
Almost all of the setup code is for preparing the C language environment in real mode. 
The next step is checking the ss register's value and setting up a correct stack if ss is wrong:
```
	movw	%ss, %dx
	cmpw	%ax, %dx	# %ds == %ss?
	movw	%sp, %dx
	je	2f		# -> assume %sp is reasonably set
```
This can lead to 3 different scenarios:
1 ss has a valid value 0x1000 (as do all the other segment registers besides cs)
2 ss is invalid and the CAN_USE_HEAP flag is set (see below)
3 ss is invalid and the CAN_USE_HEAP flag is not set (see below)


if ss has a correct address (0x1000), we go to label 2:
```
2:	# Now %dx should point to the end of our stack space
	andw	$~3, %dx	# dword align (might as well...)
	jnz	3f
	movw	$0xfffc, %dx	# Make sure we're not zero
3:	movw	%ax, %ss
	movzwl	%dx, %esp	# Clear upper half of %esp
	sti			# Now we should have a working stack
```
Here we set the alignment of dx (which contains the value of sp as given by the bootloader) to 4 bytes and check if it is zero.
If it is, we set dx to 0xfffc (The last 4-byte aligned address in a 64KB segment). If it is not zero, 
we continue to use the value of sp given by the bootloader (0xf7f4 in my case). Afterwards, 
we put the value of ax (0x1000) into ss. We now have a correct stack:
------  <-esp
stack
-----   <- _end
kernel setup 
kernel legacy boot sector(4d 5a)
-------  <- %ss-0x10000



The second scenario, (ss != ds). First, we put the value of _end (the address of the end of the setup code)
into dx and check the loadflags header field using the testb instruction to see whether we can use the heap.
loadflags is a bitmask header defined as:

```
# Invalid %ss, make up a new stack
	movw	$_end, %dx
	testb	$CAN_USE_HEAP, loadflags
	jz	1f
	movw	heap_end_ptr, %dx
1:	addw	$STACK_SIZE, %dx
	jnc	2f
	xorw	%dx, %dx	# Prevent wraparound
...
loadflags:
		.byte	LOADED_HIGH	# The kernel is to be loaded high	
	
```
_end定义
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/setup.ld    todo ld文件学习 汇编继续学习
```
. = ALIGN(16);
	.bss		:
	{
		__bss_start = .;
		*(.bss)
		__bss_end = .;
	}
	. = ALIGN(16);
	_end = .;
```
loadflags
https://github.com/torvalds/linux/blob/v4.16/arch/x86/include/uapi/asm/bootparam.h
```
/* loadflags */
#define LOADED_HIGH	(1<<0)
#define KASLR_FLAG	(1<<1)
#define QUIET_FLAG	(1<<5)
#define KEEP_SEGMENTS	(1<<6)
#define CAN_USE_HEAP	(1<<7)
```
linux boot protocol
https://www.kernel.org/doc/Documentation/x86/boot.txt
```
This field is a bitmask.

  Bit 0 (read):	LOADED_HIGH
	- If 0, the protected-mode code is loaded at 0x10000.
	- If 1, the protected-mode code is loaded at 0x100000.
...
Bit 7 (write): CAN_USE_HEAP
	Set this bit to 1 to indicate that the value entered in the
	heap_end_ptr is valid.  If this field is clear, some setup code
	functionality will be disabled.
```
If the CAN_USE_HEAP bit is set, we put heap_end_ptr into dx (which points to _end) and
add STACK_SIZE (the minimum stack size, 1024 bytes) to it. After this, if dx is not carried 
(it will not be carried, dx = _end + 1024), jump to label 2 (as in the previous case) and make a correct stack.
------  <-esp-0xfffc
stack
-----   <- _end
kernel setup
kernel legacy boot sector(4d 5a)
-------  <- %ss,%ds...-0x10000


When CAN_USE_HEAP is not set, we just use a minimal stack from _end to _end + STACK_SIZE:
------  <-   esp: _end+STACK_SIZE
stack
-----   <- _end
kernel setup
kernel legacy boot sector(4d 5a)
-------  <- %ss-0x10000



BSS Setup
block starting symbol   https://en.wikipedia.org/wiki/.bss
In computer programming, the block starting symbol (abbreviated to .bss or bss) is the portion of an object file,
executable, or assembly language code that contains statically allocated variables that are declared 
but have not been assigned a value yet. It is often referred to as the "bss section" or "bss segment".

signature checking:
```
6:
# Check signature at end of setup
	cmpl	$0x5a5aaa55, setup_sig
	jne	setup_bad
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/setup.ld
```
.signature	: {
		setup_sig = .;
		LONG(0x5a5aaa55)
	}
```
This simply compares the setup_sig with the magic number 0x5a5aaa55. If they are not equal, a fatal error is reported.

If the magic number matches, knowing we have a set of correct segment registers and a stack, 
we only need to set up the BSS section before jumping into the C code.

The BSS section is used to store statically allocated, uninitialized data. Linux carefully ensures 
this area of memory is first zeroed using the following code:
```
# Zero the bss
	movw	$__bss_start, %di
	movw	$_end+3, %cx
	xorl	%eax, %eax
	subw	%di, %cx
	shrw	$2, %cx
	rep; stosl
``` 
todo 学习这段汇编
the __bss_start address is moved into di. Next, the _end + 3 address (+3 - aligns to 4 bytes) is moved into cx. 
The eax register is cleared (using the xor instruction), and the bss section size (cx - di) is calculated and put into cx.
Then, cx is divided by four (the size of a 'word'), and the stosl instruction is used repeatedly, 
storing the value of eax (zero) into the address pointed to by di, automatically increasing di by four, 
repeating until cx reaches zero. The net effect of this code is that zeros are written through all words in memory from __bss_start to _end:

------  <-   esp: _end
BSS section
-----   <-  _bss_start
kernel setup
kernel legacy boot sector(4d 5a)
-------  <-  %ss-0x10000


Jump to main
That's all! We have the stack and BSS, so we can jump to the main() C function:
```
# Jump to C code (should not return)
	calll	main
```
main.c的位置https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/main.c