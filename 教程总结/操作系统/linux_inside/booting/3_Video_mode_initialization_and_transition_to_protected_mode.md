
https://0xax.gitbooks.io/linux-insides/content/Booting/linux-bootstrap-3.html
In this part, we will look at:
video mode initialization in the kernel setup code,
the preparations made before switching into protected mode,
the transition to protected mode

boot video mode的介绍
https://docs.kernel.org/admin-guide/svga.html  包含video mode的设置
```
The ASK_VGA mode causes the kernel to offer a video mode menu upon bootup. It displays a "Press <RETURN> 
to see video modes available, <SPACE> to continue or wait 30 secs" message. If you press <RETURN>, 
you enter the menu, if you press <SPACE> or wait 30 seconds, the kernel will boot up in the standard 80x25 mode.

The menu looks like:
Video adapter: <name-of-detected-video-adapter>
Mode:    COLSxROWS:
0  0F00  80x25
1  0F01  80x50
2  0F02  80x43
3  0F03  80x26
....
Enter mode number or ``scan``: <flashing-cursor-here>
```


https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/main.c
```
void main(void)
{
...
/* Set the video mode */
	set_video();
...	
}
```
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/video.c
```
void set_video(void)
{
	u16 mode = boot_params.hdr.vid_mode; //value copy from bootloader
	RESET_HEAP();
	store_mode_params();
	save_screen();
	probe_cards(0);
	for (;;) {
		if (mode == ASK_VGA)
			mode = mode_menu();

		if (!set_mode(mode))
			break;

		printf("Undefined video mode number: %x\n", mode);
		mode = ASK_VGA;
	}
	boot_params.hdr.vid_mode = mode;
	vesa_store_edid();
	store_mode_params();

	if (do_restore)
		restore_screen(); //the screen is restored to an earlier state
}
```


vid_mode在boot protocol协议的描述
https://www.kernel.org/doc/Documentation/x86/boot.txt
```
...
  This field is obsolete.

Field name:	vid_mode
Type:		modify (obligatory)
Offset/size:	0x1fa/2
...
  vga=<mode>
	<mode> here is either an integer (in C notation, either
	decimal, octal, or hexadecimal) or one of the strings
	"normal" (meaning 0xFFFF), "ext" (meaning 0xFFFE) or "ask"
	(meaning 0xFFFD).  This value should be entered into the
	vid_mode field, as it is used by the kernel before the command
	line is parsed.
```


Kernel data types
```
Type	char	short	int	long	u8	u16	u32	u64
Size	1	    2	    4	 8	     1	2	4	8
```

RESET_HEAP    
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/boot.h
```
#define RESET_HEAP() ((void *)( HEAP = _end ))
```
in the second part,  we initialized the heap with the init_heap function.
it resets the heap by setting the HEAP variable to _end, where _end is just extern char _end[];
GET_HEAP  todo a的位运算
```
static inline char *__get_heap(size_t s, size_t a, size_t n)
{
	char *tmp;

	HEAP = (char *)(((size_t)HEAP+(a-1)) & ~(a-1));  //计算heap
	tmp = HEAP; 
	HEAP += s*n; //移动heap
	return tmp;
}
#define GET_HEAP(type, n) \
	((type *)__get_heap(sizeof(type),__alignof__(type),(n)))
	
static inline bool heap_free(size_t n)
{
	return (int)(heap_end-HEAP) >= (int)n;  //是否可用   heap_end在第二部分有
}	
```



store_mode_params
```
static void store_cursor_position(void)
{
	struct biosregs ireg, oreg;

	initregs(&ireg);
	ireg.ah = 0x03;
	intcall(0x10, &ireg, &oreg);

	boot_params.screen_info.orig_x = oreg.dl;
	boot_params.screen_info.orig_y = oreg.dh;
	//it returns row and column in the DL and DH registers. Row and column will be stored in the 
	//orig_x and orig_y fields of the boot_params.screen_info structure.

	if (oreg.ch & 0x20)
		boot_params.screen_info.flags |= VIDEO_FLAGS_NOCURSOR;

	if ((oreg.ch & 0x1f) > (oreg.cl & 0x1f))
		boot_params.screen_info.flags |= VIDEO_FLAGS_NOCURSOR;
}

include/uapi/linux/screen_info.h
struct screen_info {
	__u8  orig_x;		/* 0x00 */
	__u8  orig_y;		/* 0x01 */
	....
	__u8  flags;		/* 0x08 */
....
} __attribute__((packed));

static void store_video_mode(void)
{
	struct biosregs ireg, oreg;

	/* N.B.: the saving of the video page here is a bit silly,
	   since we pretty much assume page 0 everywhere. */
	initregs(&ireg);
	ireg.ah = 0x0f;
	intcall(0x10, &ireg, &oreg);
    //It just gets the current video mode and stores it in boot_params.screen_info.orig_video_mode.
	/* Not all BIOSes are clean with respect to the top bit */
	boot_params.screen_info.orig_video_mode = oreg.al & 0x7f;
	boot_params.screen_info.orig_video_page = oreg.bh;
}

arch/x86/boot/boot.h
static inline u16 rdfs16(addr_t addr)
{
	u16 v;
	asm volatile("movw %%fs:%1,%0" : "=r" (v) : "m" (*(u16 *)addr));
	return v;
}

static void store_mode_params(void)
{
	u16 font_size;
	int x, y;

	/* For graphics mode, it is up to the mode-setting driver
	   (currently only video-vesa.c) to store the parameters */
	if (graphic_mode)
		return;

	store_cursor_position();
	store_video_mode();

	if (boot_params.screen_info.orig_video_mode == 0x07) {
		/* MDA, HGC, or VGA in monochrome mode */   //monochrome 单色，黑白
		video_segment = 0xb000;
	} else {
		/* CGA, EGA, VGA and so forth */  //color mode
		video_segment = 0xb800;
	}

    //store font size 
	set_fs(0);  //asm volatile("movw %0,%%fs" : : "rm" (seg));   todo 汇编
	font_size = rdfs16(0x485); /* Font size, BIOS area */  // 0x485 (this memory location is used to get the font size)
	boot_params.screen_info.orig_video_points = font_size;

	x = rdfs16(0x44a); //get the amount of columns and rows
	y = (adapter == ADAPTER_CGA) ? 25 : rdfs8(0x484)+1;

	if (force_x)
		x = force_x;
	if (force_y)
		y = force_y;

	boot_params.screen_info.orig_video_cols  = x;
	boot_params.screen_info.orig_video_lines = y;
}
```
stores video mode parameters in the boot_params.screen_info structure
store_mode_params checks the current video mode and sets the video_segment. After the BIOS transfers 
 control to the boot sector, the following addresses are for video memory:
```
0xB000:0x0000     32 Kb     Monochrome Text Video Memory
0xB800:0x0000     32 Kb     Color Text Video Memory
```


save_screen()   linux/arch/x86/boot/video.c
```
static void save_screen(void)
{
	/* Should be called after store_mode_params() */
	saved.x = boot_params.screen_info.orig_video_cols;
	saved.y = boot_params.screen_info.orig_video_lines;
	saved.curx = boot_params.screen_info.orig_x;
	saved.cury = boot_params.screen_info.orig_y;

	if (!heap_free(saved.x*saved.y*sizeof(u16)+512))
		return;		/* Not enough heap to save the screen */

	saved.data = GET_HEAP(u16, saved.x*saved.y);  //allocates space in the heap

	set_fs(video_segment);
	copy_from_fs(saved.data, 0, saved.x*saved.y*sizeof(u16));
}
```
saves the contents of the screen to the heap


probe_cards(0)
linux/arch/x86/boot/video-mode.c
```
void probe_cards(int unsafe)
{
	struct card_info *card;
	static u8 probed[2];

	if (probed[unsafe])
		return;

	probed[unsafe] = 1;

	for (card = video_cards; card < video_cards_end; card++) {
		if (card->unsafe == unsafe) {
			if (card->probe)
				card->nmodes = card->probe();  //probe 探针，取样器，探测仪
			else
				card->nmodes = 0;
		}
	}
}
```
It goes over all video_cards and collects the number of modes provided by the cards
video_cards is not declared anywhere.  every video mode presented in the x86 kernel setup code has a definition 
that looks like this:
linux/arch/x86/boot/video.h
```
linux/arch/x86/boot/video-vga.c
static __videocard video_vga = {
    .card_name    = "VGA",
    .probe        = vga_probe,
    .set_mode    = vga_set_mode,
};

#define __videocard struct card_info __attribute__((used,section(".videocards"))) //card_info structure is in the .videocards segment

struct card_info {
	const char *card_name;
	int (*set_mode)(struct mode_info *mode);
	int (*probe)(void);
	struct mode_info *modes;
	int nmodes;		/* Number of probed modes so far */
	int unsafe;		/* Probing is unsafe, only do after "scan" */
	u16 xmode_first;	/* Unprobed modes to try to call anyway */
	u16 xmode_n;		/* Size of unprobed mode range */
};

arch/x86/boot/setup.ld
.videocards	: {
		video_cards = .;
		*(.videocards)
		video_cards_end = .;
	}
```
It means that video_cards is just a memory address and all card_info structures are placed in this segment.
It means that all card_info structures are placed between video_cards and video_cards_end, 
so we can use a loop to go over all of it. After probe_cards executes we have a bunch of structures 
like static __videocard video_vga with the nmodes (the number of video modes) filled in.



```
static unsigned int mode_menu(void)
{
	int key;
	unsigned int sel;

	puts("Press <ENTER> to see video modes available, "
	     "<SPACE> to continue, or wait 30 sec\n");

	kbd_flush();
	while (1) {
		key = getchar_timeout();
		if (key == ' ' || key == 0)
			return VIDEO_CURRENT_MODE; /* Default */
		if (key == '\r')
			break;
		putchar('\a');	/* Beep! */
	}


	for (;;) {
		display_menu();

		puts("Enter a video mode or \"scan\" to scan for "
		     "additional modes: ");
		sel = get_entry();
		if (sel != SCAN)
			return sel;

		probe_cards(1);
	}
}

void set_video(void)
{
	...
	probe_cards(0);
	for (;;) {
		if (mode == ASK_VGA)
			mode = mode_menu();

		if (!set_mode(mode))
			break;

		printf("Undefined video mode number: %x\n", mode);
		mode = ASK_VGA;
	}
	....
}
```
There is an infinite loop which tries to set up the video mode with the set_mode function or prints a
menu if we passed vid_mode=ask to the kernel command line or if video mode is undefined.

set_mode
linux/arch/x86/boot/video-mode.c
```
int set_mode(u16 mode)
{
	int rv;
	u16 real_mode;

	/* Very special mode numbers... */
	if (mode == VIDEO_CURRENT_MODE)
		return 0;	/* Nothing to do... */
	else if (mode == NORMAL_VGA)
		mode = VIDEO_80x25;
	else if (mode == EXTENDED_VGA)
		mode = VIDEO_8POINT;

	rv = raw_set_mode(mode, &real_mode);
	if (rv)
		return rv;

	if (mode & VIDEO_RECALC)
		vga_recalc_vertical();

	/* Save the canonical mode number for the kernel, not
	   an alias, size specification or menu position */
#ifndef _WAKEUP
	boot_params.hdr.vid_mode = real_mode;
#endif
	return 0;
}


static int raw_set_mode(u16 mode, u16 *real_mode)
{
	int nmode, i;
	struct card_info *card;
	struct mode_info *mi;

	/* Drop the recalc bit if set */
	mode &= ~VIDEO_RECALC;

	/* Scan for mode based on fixed ID, position, or resolution */
	nmode = 0;
	for (card = video_cards; card < video_cards_end; card++) {
		mi = card->modes;
		for (i = 0; i < card->nmodes; i++, mi++) {
			int visible = mi->x || mi->y;

			if ((mode == nmode && visible) ||
			    mode == mi->mode ||
			    mode == (mi->y << 8)+mi->x) {
				*real_mode = mi->mode;
				return card->set_mode(mi);
			}

			if (visible)
				nmode++;
		}
	}

	/* Nothing found?  Is it an "exceptional" (unprobed) mode? */
	for (card = video_cards; card < video_cards_end; card++) {
		if (mode >= card->xmode_first &&
		    mode < card->xmode_first+card->xmode_n) {
			struct mode_info mix;
			*real_mode = mix.mode = mode;
			mix.x = mix.y = 0;
			return card->set_mode(&mix);
		}
	}

	/* Otherwise, failure... */
	return -1;
}

```
The raw_set_mode calls the selected card's set_mode function, i.e. card->set_mode(struct mode_info*)

for __videocard video_vga  set_mode is vga_set_mode
linux/arch/x86/boot/video-vga.c
```
static int vga_set_mode(struct mode_info *mode)
{
	/* Set the basic mode */
	vga_set_basic_mode();

	/* Override a possibly broken BIOS */
	force_x = mode->x;
	force_y = mode->y;

	switch (mode->mode) {
	case VIDEO_80x25:
		break;
	case VIDEO_8POINT:
		vga_set_8font();
		break;
	case VIDEO_80x43:
		vga_set_80x43();
		break;
	case VIDEO_80x28:
		vga_set_14font();
		break;
	case VIDEO_80x30:
		vga_set_80x30();
		break;
	case VIDEO_80x34:
		vga_set_80x34();
		break;
	case VIDEO_80x60:
		vga_set_80x60();
		break;
	}

	return 0;
}
```
Every function which sets up video mode just calls the 0x10 BIOS interrupt with a certain value in the AH register.


vesa_store_edid
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/video-vesa.c
```
void vesa_store_edid(void)
{
#ifdef CONFIG_FIRMWARE_EDID
	struct biosregs ireg, oreg;

	/* Apparently used as a nonsense token... */
	memset(&boot_params.edid_info, 0x13, sizeof boot_params.edid_info);

	if (vginfo.version < 0x0200)
		return;		/* EDID requires VBE 2.0+ */

	initregs(&ireg);
	ireg.ax = 0x4f15;		/* VBE DDC */
	/* ireg.bx = 0x0000; */		/* Report DDC capabilities */
	/* ireg.cx = 0;	*/		/* Controller 0 */
	ireg.es = 0;			/* ES:DI must be 0 by spec */
	intcall(0x10, &ireg, &oreg);

	if (oreg.ax != 0x004f)
		return;		/* No EDID */

	/* BH = time in seconds to transfer EDD information */
	/* BL = DDC level supported */

	ireg.ax = 0x4f15;		/* VBE DDC */
	ireg.bx = 0x0001;		/* Read EDID */
	/* ireg.cx = 0; */		/* Controller 0 */
	/* ireg.dx = 0;	*/		/* EDID block number */
	ireg.es = ds();
	ireg.di =(size_t)&boot_params.edid_info; /* (ES:)Pointer to block */
	intcall(0x10, &ireg, &oreg);
#endif /* CONFIG_FIRMWARE_EDID */
}
```
This function simply stores the EDID (Extended Display Identification Data) information for kernel use

https://en.wikipedia.org/wiki/Extended_Display_Identification_Data
Extended Display Identification Data (EDID) and Enhanced EDID (E-EDID) are metadata formats for display 
devices to describe their capabilities to a video source (e.g., graphics card or set-top box). 
The data format is defined by a standard published by the Video Electronics Standards Association (VESA).

The EDID data structure includes manufacturer name and serial number, product type, phosphor or filter 
type (as chromaticity data), timings supported by the display, display size, luminance data and 
(for digital displays only) pixel mapping data.





Last preparation before transition into protected mode
linux/arch/x86/boot/main.c
```
void main(void)
{
   ....
	/* Do the last things and invoke protected mode */
	go_to_protected_mode();
}
```
linux/arch/x86/boot/pm.c
```
void go_to_protected_mode(void)
{
	/* Hook before leaving real mode, also disables interrupts */
	realmode_switch_hook();

	/* Enable the A20 gate */
	if (enable_a20()) { //开启失败
		puts("A20 gate not responding, unable to boot...\n");
		die();
	}

	/* Reset coprocessor (IGNNE#) */
	reset_coprocessor();

	/* Mask all interrupts in the PIC */
	mask_all_interrupts();

	/* Actual transition to protected mode... */
	setup_idt();
	setup_gdt();
	protected_mode_jump(boot_params.hdr.code32_start,
			    (u32)&boot_params + (ds() << 4));
}

/*
 * Invoke the realmode switch hook if present; otherwise
 * disable all interrupts.
 */
static void realmode_switch_hook(void)
{
	if (boot_params.hdr.realmode_swtch) {
		asm volatile("lcallw *%0"
			     : : "m" (boot_params.hdr.realmode_swtch)
			     : "eax", "ebx", "ecx", "edx");
	} else {
		asm volatile("cli"); //a cli instruction which clears the interrupt flag (IF),After this, external interrupts are disabled
		outb(0x80, 0x70); /* Disable NMI */
		io_delay();
	}
}
```
This function invokes the real mode switch hook if it is present and disables NMI. Hooks are used if the bootloader
runs in a hostile environment. You can read more about hooks in the boot protocol (see ADVANCED BOOT LOADER HOOKS).
//hostile 敌意
ADVANCED BOOT LOADER HOOKS  https://www.kernel.org/doc/Documentation/x86/boot.txt
If the boot loader runs in a particularly hostile environment (such as
LOADLIN, which runs under DOS) it may be impossible to follow the
standard memory location requirements.  Such a boot loader may use the
following hooks that, if set, are invoked by the kernel at the
appropriate time.  The use of these hooks should probably be
considered an absolutely last resort!

IMPORTANT: All the hooks are required to preserve %esp, %ebp, %esi and
%edi across invocation.

realmode_swtch:
A 16-bit real mode far subroutine invoked immediately before
entering protected mode.  The default routine disables NMI, so
your routine should probably do so, too.


https://en.wikipedia.org/wiki/Non-maskable_interrupt
NMI
In computing, a non-maskable interrupt (NMI) is a hardware interrupt that standard interrupt-masking 
techniques in the system cannot ignore. It typically occurs to signal attention for non-recoverable hardware errors. 
Some NMIs may be masked, but only by using proprietary methods specific to the particular NMI. 
With regard to SPARC, the non-maskable interrupt (NMI), despite having the highest priority among interrupts,
can be prevented from occurring through the use of an interrupt mask.

An NMI is often used when response time is critical or when an interrupt should never be disabled during 
normal system operation. Such uses include reporting non-recoverable hardware errors, system debugging and profiling, 
and handling of special cases like system resets.


An interrupt is a signal to the CPU which is emitted by hardware or software. After getting such a signal, 
the CPU suspends the current instruction sequence, saves its state and transfers control to the interrupt handler. 
After the interrupt handler has finished it's work, it transfers control back to the interrupted instruction.
Non-maskable interrupts (NMI) are interrupts which are always processed, independently of permission. 
They cannot be ignored and are typically used to signal for non-recoverable hardware errors. 
We will not dive into the details of interrupts now but we will be discussing them in the coming posts.

io_delay
```
static inline void io_delay(void)
{
	const u16 DELAY_PORT = 0x80;
	outb(0, DELAY_PORT); // writing the byte 0x80 (disabled bit) to 0x70 (the CMOS Address register)
}
```
io_delay causes a small delay
To output any byte to the port 0x80 should delay exactly 1 microsecond. So we can write any value
(the value from AL in our case) to the 0x80 port.


A20 line   https://en.wikipedia.org/wiki/A20_line
The A20, or address line 20, is one of the electrical lines that make up the system bus of an x86-based computer system. 
The A20 line in particular is used to transmit the 21st bit on the address bus.
https://www.zhihu.com/question/29375534
兼容老版本的地址总线

enable_a20()
linux/arch/x86/boot/a20.c
```
static int a20_test(int loops)
{
	int ok = 0;
	int saved, ctr;

	set_fs(0x0000);
	set_gs(0xffff); //we put 0x0000 in the FS register and 0xffff in the GS register

	saved = ctr = rdfs32(A20_TEST_ADDR);

	while (loops--) {
		wrfs32(++ctr, A20_TEST_ADDR);
		io_delay();	/* Serialize and make delay constant */
		ok = rdgs32(A20_TEST_ADDR+0x10) ^ ctr;
		if (ok) 
		//In a case when a20 line is disabled, the address will be overlapped, in other case if it's not zero a20 
		//line is already enabled the A20 line
			break;
	}

	wrfs32(saved, A20_TEST_ADDR);
	return ok;
}

/* Quick test to see if A20 is already enabled */
static int a20_test_short(void)
{
	return a20_test(A20_TEST_SHORT);
}

static void enable_a20_bios(void)
{
	struct biosregs ireg;

	initregs(&ireg);
	ireg.ax = 0x2401;
	intcall(0x15, &ireg, NULL);
}

int enable_a20(void)
{
       int loops = A20_ENABLE_LOOPS;
       int kbc_err;

       while (loops--) {
	       /* First, check to see if A20 is already enabled
		  (legacy free, etc.) */
	       if (a20_test_short())
		       return 0;
	       
	       /* Next, try the BIOS (INT 0x15, AX=0x2401) */  中断打开
	       enable_a20_bios();
	       if (a20_test_short())
		       return 0;
	       
	       /* Try enabling A20 through the keyboard controller */
	       kbc_err = empty_8042(); //通过不同的方式打开

	       if (a20_test_short())
		       return 0; /* BIOS worked, but with delayed reaction */
	
	       if (!kbc_err) {
		       enable_a20_kbc(); 
		       if (a20_test_long())
			       return 0;
	       }
	       
	       /* Finally, try enabling the "fast A20 gate" */
	       enable_a20_fast(); 
	       if (a20_test_long())
		       return 0;
       }
       
       return -1;
}
```

die()
https://github.com/torvalds/linux/blob/v4.16/arch/x86/boot/header.S
```
die:
	hlt
	jmp	die

	.size	die, .-die

	.section ".initdata", "a"
```


After the A20 gate is successfully enabled, the reset_coprocessor function is called:
```
static void reset_coprocessor(void)
{
	outb(0, 0xf0);
	io_delay();
	outb(0, 0xf1);
	io_delay();
}
```
This function clears the Math Coprocessor by writing 0 to 0xf0 and then resets it by writing 0 to 0xf1.


mask_all_interrupts
```
static void mask_all_interrupts(void)
{
	outb(0xff, 0xa1);	/* Mask all interrupts on the secondary PIC */
	io_delay();
	outb(0xfb, 0x21);	/* Mask all but cascade on the primary PIC */
	io_delay();
}
```
This masks all interrupts on the secondary PIC (Programmable Interrupt Controller) and primary 
PIC except for IRQ2 on the primary PIC.

PIC https://en.wikipedia.org/wiki/Programmable_interrupt_controller
In computing, a programmable interrupt controller (PIC) is an integrated circuit that helps a microprocessor 
(or CPU) handle interrupt requests (IRQs) coming from multiple different sources (like external I/O devices) which may occur simultaneously.


Set up the Interrupt Descriptor Table
setup_idt()       //interrupt Descriptor table (IDT)
```
static void setup_idt(void)
{
	static const struct gdt_ptr null_idt = {0, 0};
	asm volatile("lidtl %0" : : "m" (null_idt));
}
```
which sets up the Interrupt Descriptor Table (describes interrupt handlers and etc.). For now, 
the IDT is not installed (we will see it later), but now we just load the IDT with the lidtl instruction. 
null_idt contains the address and size of the IDT, but for now they are just zero. null_idt is a gdt_ptr structure, 
it is defined as:
```
struct gdt_ptr {
	u16 len;
	u32 ptr;
} __attribute__((packed));
```
__attribute__((packed)) means that the size of gdt_ptr is the minimum required size.
todo __attribute__((packed))



Set up Global Descriptor Table
setup_gdt();
```
linux/arch/x86/include/asm/segment.h    //参考2部分GDT的格式
#define GDT_ENTRY(flags, base, limit)			\
	((((base)  & _AC(0xff000000,ULL)) << (56-24)) |	\
	 (((flags) & _AC(0x0000f0ff,ULL)) << 40) |	\
	 (((limit) & _AC(0x000f0000,ULL)) << (48-16)) |	\
	 (((base)  & _AC(0x00ffffff,ULL)) << 16) |	\
	 (((limit) & _AC(0x0000ffff,ULL))))
	 
#define GDT_ENTRY_BOOT_CS	2 //为什么从2开始  the first is a mandatory null descriptor (index - 0) and the second is not used (index - 1).
#define GDT_ENTRY_BOOT_DS	3
#define GDT_ENTRY_BOOT_TSS	4  
..


static void setup_gdt(void)
{
	/* There are machines which are known to not boot with the GDT
	   being 8-byte unaligned.  Intel recommends 16 byte alignment. */
	static const u64 boot_gdt[] __attribute__((aligned(16))) = {
		/* CS: code, read/execute, 4 GB, base 0 */
		[GDT_ENTRY_BOOT_CS] = GDT_ENTRY(0xc09b, 0, 0xfffff),
		/* DS: data, read/write, 4 GB, base 0 */
		[GDT_ENTRY_BOOT_DS] = GDT_ENTRY(0xc093, 0, 0xfffff),
		/* TSS: 32-bit tss, 104 bytes, base 4096 */
		/* We only have a TSS here to keep Intel VT happy;
		   we don't actually use it for anything. */
		[GDT_ENTRY_BOOT_TSS] = GDT_ENTRY(0x0089, 4096, 103),
	};
	/* Xen HVM incorrectly stores a pointer to the gdt_ptr, instead
	   of the gdt_ptr contents.  Thus, make it static so it will
	   stay in memory, at least long enough that we switch to the
	   proper kernel GDT. */
	static struct gdt_ptr gdt;

	gdt.len = sizeof(boot_gdt)-1;  //the last valid address in the GDT
	gdt.ptr = (u32)&boot_gdt + (ds() << 4); //real mode

	asm volatile("lgdtl %0" : : "m" (gdt)); // load the GDT into the GDTR register
}
```
__attribute__((aligned(16))) attribute. It means that this structure will be aligned by 16 bytes  todo
There is a definition of the boot_gdt array in this function, which contains the definition of the three segments:
CS,DS,TSS (Task State Segment)
TSS,并没有使用，更多关于tss
https://github.com/torvalds/linux/commit/88089519f302f1296b4739be45699f06f728ec31

GDT_ENTRY(0xc09b, 0, 0xfffff)的segment是
```
1100 0000 1001 1011
```
Let's try to understand what every bit means. We will go through all bits from left to right:
1 - (G) granularity bit
1 - (D) if 0 16-bit segment; 1 = 32-bit segment
0 - (L) executed in 64-bit mode if 1
0 - (AVL) available for use by system software
0000 - 4-bit length 19:16 bits in the descriptor
1 - (P) segment presence in memory
00 - (DPL) - privilege level, 0 is the highest privilege
1 - (S) code or data segment, not a system segment
101 - segment type execute/read/
1 - accessed bit



Actual transition into protected mode
This is the end of the go_to_protected_mode function. We loaded the IDT and GDT, disabled interrupts 
and now can switch the CPU into protected mode. The last step is calling the protected_mode_jump function
protected_mode_jump(boot_params.hdr.code32_start,(u32)&boot_params + (ds() << 4));
arch/x86/boot/pmjump.S
```
/*
 * void protected_mode_jump(u32 entrypoint, u32 bootparams);
 */
GLOBAL(protected_mode_jump)
	movl	%edx, %esi		# Pointer to boot_params table

	xorl	%ebx, %ebx  
	movw	%cs, %bx  //put the address of boot_params in the esi register and the address of the code segment register cs in bx
	shll	$4, %ebx
	addl	%ebx, 2f  //which is (cs << 4) + in_pm32, the physical address to jump after transitioned to 32-bit mode
	jmp	1f			# Short jump to serialize on 386/486
1:

	movw	$__BOOT_DS, %cx    // put the data segment and the task state segment in the cx and di registers
	movw	$__BOOT_TSS, %di

	movl	%cr0, %edx
	orb	$X86_CR0_PE, %dl	# Protected mode
	movl	%edx, %cr0  //set the PE (Protection Enable) bit in the CR0 control register

	# Transition to 32-bit mode
	.byte	0x66, 0xea		# ljmpl opcode   0x66 is the operand-size prefix which allows us to mix 16-bit and 32-bit code
2:	.long	in_pm32			# offset   the segment offset under protect mode, which has value (cs << 4) + in_pm32 derived from real mode
	.word	__BOOT_CS		# segment   the code segment we want to jump to
ENDPROC(protected_mode_jump)

	.code32   // finally in protected mode
	.section ".text32","ax"
```
todo 汇编

Let's look at the first steps taken in protected mode
```
GLOBAL(in_pm32)
	# Set up data segments for flat 32-bit mode
	movl	%ecx, %ds  //we saved $__BOOT_DS in the cx register. Now we fill it with all segment registers besides cs (cs is already __BOOT_CS)
	movl	%ecx, %es
	movl	%ecx, %fs
	movl	%ecx, %gs
	movl	%ecx, %ss
	# The 32-bit code sets up its own stack, but this way we do have
	# a valid stack if some debugging hack wants to use it.
	addl	%ebx, %esp  //setup a valid stack for debugging purposes

	# Set up TR to make Intel VT happy
	ltr	%di

	# Clear registers to allow for future extensions to the
	# 32-bit boot protocol    
	xorl	%ecx, %ecx   //clear the general purpose registers
	xorl	%edx, %edx
	xorl	%ebx, %ebx
	xorl	%ebp, %ebp
	xorl	%edi, %edi

	# Set up LDTR to make Intel VT happy
	lldt	%cx

	jmpl	*%eax			# Jump to the 32-bit entrypoint   //eax contains the address of the 32-bit entry (we passed it as the first parameter into protected_mode_jump)
ENDPROC(in_pm32)
```
We're in protected mode and stop at its entry point