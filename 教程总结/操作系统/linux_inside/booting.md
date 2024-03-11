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