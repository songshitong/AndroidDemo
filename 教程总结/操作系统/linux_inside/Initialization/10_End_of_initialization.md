

This part will be the last part of the Kernel initialization process chapter, so let's finish it.


After the call of the acpi_early_init function from the init/main.c, we can see the following code:
init/main.c
```
static void __init mm_init(void)
{
	...
	/* Should be run before the first non-init thread is created */
	init_espfix_bsp();
	...
}
```
include/asm-generic/pgtable.h
```
#ifndef CONFIG_X86_ESPFIX64
static inline void init_espfix_bsp(void) { }
#endif
```


Here we can see the call of the init_espfix_bsp function which depends on the CONFIG_X86_ESPFIX64 kernel configuration option. 
As we can understand from the function name, it does something with the stack. This function is defined 
in the arch/x86/kernel/espfix_64.c and prevents leaking of 31:16 bits of the esp register during returning to 16-bit stack. 
First of all we install espfix page upper directory into the kernel page directory in the init_espfix_bs:
arch/x86/kernel/espfix_64.c
```
void __init init_espfix_bsp(void)
{
	pgd_t *pgd;
	p4d_t *p4d;

	/* Install the espfix pud into the kernel page directory */
	pgd = &init_top_pgt[pgd_index(ESPFIX_BASE_ADDR)];
	p4d = p4d_alloc(&init_mm, pgd, ESPFIX_BASE_ADDR);
	p4d_populate(&init_mm, p4d, espfix_pud_page);

	/* Randomize the locations */
	init_espfix_random();

	/* The rest is the same as for any other processor */
	init_espfix_ap(0);
}
```
Where ESPFIX_BASE_ADDR is:
```
#define P4D_SHIFT	39
#define ESPFIX_PGD_ENTRY	_AC(-2, UL)
#define ESPFIX_BASE_ADDR	(ESPFIX_PGD_ENTRY << P4D_SHIFT)
```
