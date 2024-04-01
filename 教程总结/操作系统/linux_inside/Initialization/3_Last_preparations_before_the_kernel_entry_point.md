

https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-3.html
Last preparations before the kernel entry point

boot_params again
In the previous part we stopped at setting Interrupt Descriptor Table and loading it in the IDTR register. 
At the next step after this we can see a call of the copy_bootdata function:
https://github.com/torvalds/linux/blob/v4.16/arch/x86/kernel/head64.c
```
asmlinkage __visible void __init x86_64_start_kernel(char * real_mode_data)
{
...
idt_setup_early_handler();
copy_bootdata(__va(real_mode_data));
....	
}
```
This function takes one argument - virtual address of the real_mode_data. Remember that we passed the
address of the boot_params structure from arch/x86/include/uapi/asm/bootparam.h to the x86_64_start_kernel function
as first argument in arch/x86/kernel/head_64.S:
```
 /* rsi is pointer to real mode structure with interesting info.
       pass it to C */
    movq    %rsi, %rdi
```


__va  todo 没找到
```
#define __va(x)                 ((void *)((unsigned long)(x)+PAGE_OFFSET))
```
where PAGE_OFFSET is __PAGE_OFFSET which is 0xffff880000000000 and the base virtual address of the direct mapping of 
all physical memory. So we're getting virtual address of the boot_params structure and pass it to the copy_bootdata function,
 where we copy real_mod_data to the boot_params

```
static void __init copy_bootdata(char *real_mode_data)
{
	char * command_line;
	unsigned long cmd_line_ptr;

	/*
	 * If SME is active, this will create decrypted mappings of the
	 * boot data in advance of the copy operations.
	 */
	sme_map_bootdata(real_mode_data);

	memcpy(&boot_params, real_mode_data, sizeof boot_params);
	sanitize_boot_params(&boot_params);
	cmd_line_ptr = get_cmd_line_ptr();
	if (cmd_line_ptr) {
		command_line = __va(cmd_line_ptr);
		memcpy(boot_command_line, command_line, COMMAND_LINE_SIZE);
	}

	/*
	 * The old boot data is no longer needed and won't be reserved,
	 * freeing up that memory for use by the system. If SME is active,
	 * we need to remove the mappings that were created so that the
	 * memory doesn't remain mapped as decrypted.
	 */
	sme_unmap_bootdata(real_mode_data);
}
```
arch/x86/include/asm/bootparam_utils.h
```
static void sanitize_boot_params(struct boot_params *boot_params)
{
	....
	if (boot_params->sentinel) {
		/* fields in boot_params are left uninitialized, clear them */
		memset(&boot_params->ext_ramdisk_image, 0,
		       (char *)&boot_params->efi_info -
			(char *)&boot_params->ext_ramdisk_image);
		memset(&boot_params->kbd_status, 0,
		       (char *)&boot_params->hdr -
		       (char *)&boot_params->kbd_status);
		memset(&boot_params->_pad7[0], 0,
		       (char *)&boot_params->edd_mbr_sig_buffer[0] -
			(char *)&boot_params->_pad7[0]);
		memset(&boot_params->_pad8[0], 0,
		       (char *)&boot_params->eddbuf[0] -
			(char *)&boot_params->_pad8[0]);
		memset(&boot_params->_pad9[0], 0, sizeof(boot_params->_pad9));
	}
}
```
arch/x86/kernel/head64.c
```
static unsigned long get_cmd_line_ptr(void)
{
	unsigned long cmd_line_ptr = boot_params.hdr.cmd_line_ptr;

	cmd_line_ptr |= (u64)boot_params.ext_cmd_line_ptr << 32;

	return cmd_line_ptr;
}
```
First of all, note that this function is declared with __init prefix. It means that this function will
be used only during the initialization and used memory will be freed.  todo c 语法，下一章有介绍

We can see declaration of two variables for the kernel command line and copying real_mode_data to the boot_params with 
the memcpy function.
The next call of the sanitize_boot_params function which fills some fields of the boot_params structure like ext_ramdisk_image 
and etc... if bootloaders which fail to initialize unknown fields in boot_params to zero. 
After this we're getting address of the command line with the call of the get_cmd_line_ptr function
   which gets the 64-bit address of the command line from the kernel boot header and returns it.

In the last step we check cmd_line_ptr, getting its virtual address and copy it to the boot_command_line
  which is just an array of bytes:
https://github.com/torvalds/linux/blob/v4.16/init/main.c
```
/* Untouched command line saved by arch-specific code. */
char __initdata boot_command_line[COMMAND_LINE_SIZE];
```

After this we will have copied kernel command line and boot_params structure. In the next step we can see 
call of the load_ucode_bsp function which loads processor microcode, but we will not see it here.

After microcode was loaded we can see the check of the console_loglevel and the early_printk function
which prints Kernel Alive string. But you'll never see this output because early_printk is not initialized yet. 
It is a minor bug in the kernel and i sent the patch - commit and you will see it in the mainline soon. 
So you can skip this code.
https://git.kernel.org/pub/scm/linux/kernel/git/tip/tip.git/commit/?id=91d8f0416f3989e248d3a3d3efb821eda10a85d2



Move on init pages
In the next step, as we have copied boot_params structure, we need to move from the early page tables 
to the page tables for initialization process. We already set early page tables for switchover, 
 and dropped all it in the reset_early_page_tables function and kept only kernel high mapping. After this we call:
```
clear_page(init_top_pgt);
```
arch/x86/kernel/head_64.S
```
#if defined(CONFIG_XEN_PV) || defined(CONFIG_XEN_PVH)
NEXT_PGD_PAGE(init_top_pgt)
	.quad   level3_ident_pgt - __START_KERNEL_map + _KERNPG_TABLE_NOENC
	.org    init_top_pgt + PGD_PAGE_OFFSET*8, 0
	.quad   level3_ident_pgt - __START_KERNEL_map + _KERNPG_TABLE_NOENC
	.org    init_top_pgt + PGD_START_KERNEL*8, 0
	/* (2^48-(2*1024*1024*1024))/(2^39) = 511 */
	.quad   level3_kernel_pgt - __START_KERNEL_map + _PAGE_TABLE_NOENC
	.fill	PTI_USER_PGD_FILL,8,0

NEXT_PAGE(level3_ident_pgt)
	.quad	level2_ident_pgt - __START_KERNEL_map + _KERNPG_TABLE_NOENC
	.fill	511, 8, 0
NEXT_PAGE(level2_ident_pgt)
	/* Since I easily can, map the first 1G.
	 * Don't set NX because code runs from these pages.
	 */
	PMDS(0, __PAGE_KERNEL_IDENT_LARGE_EXEC, PTRS_PER_PMD)
#else
NEXT_PGD_PAGE(init_top_pgt)
	.fill	512,8,0
	.fill	PTI_USER_PGD_FILL,8,0
#endif
```
arch/arm64/lib/clear_page.S
```
ENTRY(clear_page)
	mrs	x1, dczid_el0
	and	w1, w1, #0xf
	mov	x2, #4
	lsl	x1, x2, x1

1:	dc	zva, x0
	add	x0, x0, x1
	tst	x0, #(PAGE_SIZE - 1)
	b.ne	1b
	ret
ENDPROC(clear_page)
```
As you can understand from the function name it clears or fills with zeros page tables. 
todo clear_page这块与文章不一样



The last step in the x86_64_start_kernel function is the call of the:
arch/x86/kernel/head64.c
```
void __init x86_64_start_reservations(char *real_mode_data)
{
	/* version is always not zero if it is copied */
	if (!boot_params.hdr.version)
		copy_bootdata(__va(real_mode_data));

	x86_early_init_platform_quirks();

	switch (boot_params.hdr.hardware_subarch) {
	case X86_SUBARCH_INTEL_MID:
		x86_intel_mid_early_setup();
		break;
	default:
		break;
	}

	start_kernel();
}
```
First of all we can see  the check for boot_params.hdr.version,and if it is zero we call copy_bootdata function 
  again with the virtual address of the real_mode_data

//4.16  没有reserve_ebda_region   todo

x86_early_init_platform_quirks
```
void __init x86_early_init_platform_quirks(void)
{
	x86_platform.legacy.i8042 = X86_LEGACY_I8042_EXPECTED_PRESENT;
	x86_platform.legacy.rtc = 1;
	x86_platform.legacy.warm_reset = 1;
	x86_platform.legacy.reserve_bios_regions = 0;
	x86_platform.legacy.devices.pnpbios = 1;

	switch (boot_params.hdr.hardware_subarch) {
	case X86_SUBARCH_PC:
		x86_platform.legacy.reserve_bios_regions = 1;
		break;
	case X86_SUBARCH_XEN:
		x86_platform.legacy.devices.pnpbios = 0;
		x86_platform.legacy.rtc = 0;
		break;
	case X86_SUBARCH_INTEL_MID:
	case X86_SUBARCH_CE4100:
		x86_platform.legacy.devices.pnpbios = 0;
		x86_platform.legacy.rtc = 0;
		x86_platform.legacy.i8042 = X86_LEGACY_I8042_PLATFORM_ABSENT;
		break;
	}

	if (x86_platform.set_legacy_features)
		x86_platform.set_legacy_features();
}
```

x86_intel_mid_early_setup
```
/*
 * Moorestown specific x86_init function overrides and early setup
 * calls.
 */
void __init x86_intel_mid_early_setup(void)
{
	x86_init.resources.probe_roms = x86_init_noop;
	x86_init.resources.reserve_resources = x86_init_noop;

	x86_init.timers.timer_init = intel_mid_time_init;
	x86_init.timers.setup_percpu_clockev = x86_init_noop;

	x86_init.irqs.pre_vector_init = x86_init_noop;

	x86_init.oem.arch_setup = intel_mid_arch_setup;

	x86_platform.get_nmi_reason = intel_mid_get_nmi_reason;

	x86_init.pci.arch_init = intel_mid_pci_init;
	x86_init.pci.fixup_irqs = x86_init_noop;

	legacy_pic = &null_legacy_pic;

	/*
	 * Do nothing for now as everything needed done in
	 * x86_intel_mid_early_setup() below.
	 */
	x86_init.acpi.reduced_hw_early_init = x86_init_noop;

	pm_power_off = intel_mid_power_off;
	machine_ops.emergency_restart  = intel_mid_reboot;

	/* Avoid searching for BIOS MP tables */
	x86_init.mpparse.find_mptable		= x86_init_noop;
	x86_init.mpparse.early_parse_smp_cfg	= x86_init_noop;
	x86_init.mpparse.parse_smp_cfg		= x86_init_noop;
	set_bit(MP_BUS_ISA, mp_bus_not_pci);
}
```


We finished all preparations before the kernel entry point! The last step in the x86_64_start_reservations 
function is the call of the:
```
start_kernel();
```