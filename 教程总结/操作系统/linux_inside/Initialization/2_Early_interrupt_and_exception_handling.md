




https://0xax.gitbooks.io/linux-insides/content/Initialization/linux-initialization-2.html
Some theory
An interrupt is an event caused by software or hardware to the CPU. For example a user have pressed a key on keyboard. 
On interrupt, CPU stops the current task and transfer control to the special routine which is called - interrupt handler. 
An interrupt handler handles and interrupt and transfer control back to the previously stopped task.
We can split interrupts on three types:
1 Software interrupts - when a software signals CPU that it needs kernel attention. These interrupts are generally 
  used for system calls;
2 Hardware interrupts - when a hardware event happens, for example button is pressed on a keyboard;
3 Exceptions - interrupts generated by CPU, when the CPU detects error, for example division by zero or accessing a memory 
  page which is not in RAM.


Every interrupt and exception is assigned a unique number which is called - vector number. 
Vector number can be any number from 0 to 255. There is common practice to use first 32 vector numbers for exceptions, 
and vector numbers from 32 to 255 are used for user-defined interrupts.

CPU uses vector number as an index in the Interrupt Descriptor Table (we will see description of it soon). 
CPU catches interrupts from the APIC or through its pins. Following table shows 0-31 exceptions:
APIC
Advanced Programmable Interrupt Controller  https://en.wikipedia.org/wiki/Advanced_Programmable_Interrupt_Controller
In computing, Intel's Advanced Programmable Interrupt Controller (APIC) is a family of programmable interrupt controllers. 
As its name suggests, the APIC is more advanced than Intel's 8259 Programmable Interrupt Controller (PIC),
particularly enabling the construction of multiprocessor systems. It is one of several architectural designs intended to 
solve interrupt routing efficiency issues in multiprocessor computer systems.
```
----------------------------------------------------------------------------------------------
|Vector|Mnemonic|Description         |Type |Error Code|Source                   |
----------------------------------------------------------------------------------------------
|0     | #DE    |Divide Error        |Fault|NO        |DIV and IDIV                          |
|---------------------------------------------------------------------------------------------
|1     | #DB    |Reserved            |F/T  |NO        |                                      |
|---------------------------------------------------------------------------------------------
|2     | ---    |NMI                 |INT  |NO        |external NMI                          |
|---------------------------------------------------------------------------------------------
|3     | #BP    |Breakpoint          |Trap |NO        |INT 3                                 |
|---------------------------------------------------------------------------------------------
|4     | #OF    |Overflow            |Trap |NO        |INTO  instruction                     |
|---------------------------------------------------------------------------------------------
|5     | #BR    |Bound Range Exceeded|Fault|NO        |BOUND instruction                     |
|---------------------------------------------------------------------------------------------
|6     | #UD    |Invalid Opcode      |Fault|NO        |UD2 instruction                       |
|---------------------------------------------------------------------------------------------
|7     | #NM    |Device Not Available|Fault|NO        |Floating point or [F]WAIT             |
|---------------------------------------------------------------------------------------------
|8     | #DF    |Double Fault        |Abort|YES       |An instruction which can generate NMI |
|---------------------------------------------------------------------------------------------
|9     | ---    |Reserved            |Fault|NO        |                                      |
|---------------------------------------------------------------------------------------------
|10    | #TS    |Invalid TSS         |Fault|YES       |Task switch or TSS access             |
|---------------------------------------------------------------------------------------------
|11    | #NP    |Segment Not Present |Fault|NO        |Accessing segment register            |
|---------------------------------------------------------------------------------------------
|12    | #SS    |Stack-Segment Fault |Fault|YES       |Stack operations                      |
|---------------------------------------------------------------------------------------------
|13    | #GP    |General Protection  |Fault|YES       |Memory reference                      |
|---------------------------------------------------------------------------------------------
|14    | #PF    |Page fault          |Fault|YES       |Memory reference                      |
|---------------------------------------------------------------------------------------------
|15    | ---    |Reserved            |     |NO        |                                      |
|---------------------------------------------------------------------------------------------
|16    | #MF    |x87 FPU fp error    |Fault|NO        |Floating point or [F]Wait             |
|---------------------------------------------------------------------------------------------
|17    | #AC    |Alignment Check     |Fault|YES       |Data reference                        |
|---------------------------------------------------------------------------------------------
|18    | #MC    |Machine Check       |Abort|NO        |                                      |
|---------------------------------------------------------------------------------------------
|19    | #XM    |SIMD fp exception   |Fault|NO        |SSE[2,3] instructions                 |
|---------------------------------------------------------------------------------------------
|20    | #VE    |Virtualization exc. |Fault|NO        |EPT violations                        |
|---------------------------------------------------------------------------------------------
|21-31 | ---    |Reserved            |INT  |NO        |External interrupts                   |
----------------------------------------------------------------------------------------------
```

To react on interrupt CPU uses special structure - Interrupt Descriptor Table or IDT. IDT is an array of 8-byte descriptors 
like Global Descriptor Table, but IDT entries are called gates. CPU multiplies vector number by 8 to find the IDT entry. 
But in 64-bit mode IDT is an array of 16-byte descriptors and CPU multiplies vector number by 16 to find the entry in the IDT. 
We remember from the previous part that CPU uses special GDTR register to locate Global Descriptor Table, 
so CPU uses special register IDTR for Interrupt Descriptor Table and lidt instruction for loading base address of the table into this register.

64-bit mode IDT entry has following structure:
```
127                                                                             96
 --------------------------------------------------------------------------------
|                                                                               |
|                                Reserved                                       |
|                                                                               |
 --------------------------------------------------------------------------------
95                                                                              64
 --------------------------------------------------------------------------------
|                                                                               |
|                               Offset 63..32                                   |
|                                                                               |
 --------------------------------------------------------------------------------
63                               48 47      46  44   42    39             34    32
 --------------------------------------------------------------------------------
|                                  |       |  D  |   |     |      |   |   |     |
|       Offset 31..16              |   P   |  P  | 0 |Type |0 0 0 | 0 | 0 | IST |
|                                  |       |  L  |   |     |      |   |   |     |
 --------------------------------------------------------------------------------
31                                   16 15                                      0
 --------------------------------------------------------------------------------
|                                      |                                        |
|          Segment Selector            |                 Offset 15..0           |
|                                      |                                        |
 --------------------------------------------------------------------------------
```
Offset - is offset to entry point of an interrupt handler;
DPL - Descriptor Privilege Level;
P - Segment Present flag;
Segment selector - a code segment selector in GDT or LDT
IST - provides ability to switch to a new stack for interrupts handling.

And the last Type field describes type of the IDT entry. There are three different kinds of gates for interrupts:
1 Task gate
2 Interrupt gate
3 Trap gate

Interrupt and trap gates contain a far pointer to the entry point of the interrupt handler. 
Only one difference between these types is how CPU handles IF flag. If interrupt handler was accessed through interrupt gate, 
CPU clear the IF flag to prevent other interrupts while current interrupt handler executes. 
After that current interrupt handler executes, CPU sets the IF flag again with iret instruction.


Other bits in the interrupt descriptor is reserved and must be 0. Now let's look how CPU handles interrupts:
1 CPU save flags register, CS, and instruction pointer on the stack.
2 If interrupt causes an error code (like #PF for example), CPU saves an error on the stack after instruction pointer;
3 After interrupt handler executes, iret instruction will be used to return from it.




Fill and load IDT
arch/x86/kernel/head64.c
```
idt_setup_early_handler();
```
arch/x86/kernel/idt.c
```
void __init idt_setup_early_handler(void)
{
	int i;

	for (i = 0; i < NUM_EXCEPTION_VECTORS; i++)
		set_intr_gate(i, early_idt_handler_array[i]);
    ...
	load_idt(&idt_descr);
}
```
arch/x86/include/asm/segment.h
```
#define NUM_EXCEPTION_VECTORS		32
```

where NUM_EXCEPTION_VECTORS expands to 32. As we can see, We're filling only first 32 IDT entries in the loop,
because all of the early setup runs with interrupts disabled, so there is no need to set up interrupt handlers for vectors greater than 32. 
Here we call set_intr_gate in the loop, which takes two parameters:
1  Number of an interrupt or vector number;
2  Address of the idt handler.
and inserts an interrupt gate to the IDT table which is represented by the &idt_descr array.


arch/x86/include/asm/segment.h
```
#define NUM_EXCEPTION_VECTORS		32
#define EARLY_IDT_HANDLER_SIZE 9
extern const char early_idt_handler_array[NUM_EXCEPTION_VECTORS][EARLY_IDT_HANDLER_SIZE];
```
The early_idt_handler_array is 288 bytes array which contains address of exception entry points every nine bytes. 
Every nine bytes of this array consist of two bytes optional instruction for pushing dummy error code if an exception does not provide it, 
two bytes instruction for pushing vector number to the stack and five bytes of jump to the common exception handler code. 
You will see more detail in the next paragraph.


set_intr_gate
arch/x86/kernel/idt.c
```
static void set_intr_gate(unsigned int n, const void *addr)
{
	struct idt_data data;

	BUG_ON(n > 0xFF);

	memset(&data, 0, sizeof(data));
	data.vector	= n;
	data.addr	= addr;
	data.segment	= __KERNEL_CS;
	data.bits.type	= GATE_INTERRUPT;
	data.bits.p	= 1;

	idt_setup_from_table(idt_table, &data, 1, false);
}
```
it checks that passed vector number is not greater than 255 with BUG_ON macro. We need to do this because we are
limited to have up to 256 interrupts. 
After this, we fill the idt data with the given arguments and others, which will be passed to idt_setup_from_table.
arch/x86/kernel/idt.c
```
static void
idt_setup_from_table(gate_desc *idt, const struct idt_data *t, int size, bool sys)
{
	gate_desc desc;

	for (; size > 0; t++, size--) {
		idt_init_desc(&desc, t);
		write_idt_entry(idt, t->vector, &desc);
		if (sys)
			set_bit(t->vector, system_vectors);
	}
}

static inline void idt_init_desc(gate_desc *gate, const struct idt_data *d)
{
	unsigned long addr = (unsigned long) d->addr;
	gate->offset_low	= (u16) addr;
	gate->segment		= (u16) d->segment;
	gate->bits		= d->bits;
	gate->offset_middle	= (u16) (addr >> 16);
#ifdef CONFIG_X86_64
	gate->offset_high	= (u32) (addr >> 32);
	gate->reserved		= 0;
#endif
}

//arch/x86/include/asm/paravirt.h
static inline void write_idt_entry(gate_desc *dt, int entry, const gate_desc *g)
{
	PVOP_VCALL3(pv_cpu_ops.write_idt_entry, dt, entry, g);
}
```
which fill temporary idt descriptor with the given arguments and others. And then we just copy it to the certain element of the idt_table array.
idt_table is an array of idt entries:
```
/* Must be page-aligned because the real IDT is used in a fixmap. */
gate_desc idt_table[IDT_ENTRIES] __page_aligned_bss;
```

Now we are moving back to main loop code. After main loop finishes, we can load Interrupt Descriptor table with the call of the:
```
load_idt(&idt_descr);
```
idt_descr
```
struct desc_ptr idt_descr __ro_after_init = {
	.size		= (IDT_ENTRIES * 2 * sizeof(unsigned long)) - 1,
	.address	= (unsigned long) idt_table,
};
```
arch/x86/include/asm/paravirt.h
```
static inline void load_idt(const struct desc_ptr *dtr)
{
	PVOP_VCALL1(pv_cpu_ops.load_idt, dtr);
}
```
arch/x86/include/asm/desc.h
```
static __always_inline void native_load_idt(const struct desc_ptr *dtr)
{
	asm volatile("lidt %0"::"m" (*dtr));
}
```

now we have filled and loaded Interrupt Descriptor Table, we know how the CPU acts during an interrupt.
So now time to deal with interrupts handlers.



Early interrupts handlers
arch/x86/kernel/head_64.S   todo early_idt_handler_array  汇编和c中都有怎么回事
```
ENTRY(early_idt_handler_array)
	i = 0
	.rept NUM_EXCEPTION_VECTORS  //32
	.if ((EXCEPTION_ERRCODE_MASK >> i) & 1) == 0
		UNWIND_HINT_IRET_REGS
		pushq $0	# Dummy error code, to make stack frame uniform
	.else
		UNWIND_HINT_IRET_REGS offset=8
	.endif
	pushq $i		# 72(%rsp) Vector number
	jmp early_idt_handler_common
	UNWIND_HINT_IRET_REGS
	i = i + 1
	.fill early_idt_handler_array + i*EARLY_IDT_HANDLER_SIZE - ., 1, 0xcc
	.endr
	UNWIND_HINT_IRET_REGS offset=16
END(early_idt_handler_array)
```
interrupt handlers generation for the first 32 exceptions. We check here, if exception has an error code then we do nothing, 
if exception does not return error code, we push zero to the stack. We do it for that stack was uniform. 
After that we push vector number on the stack and jump on the early_idt_handler_common which is generic interrupt handler for now.
After all, every nine bytes of the early_idt_handler_array array consists of optional push of an error code, 
push of vector number and jump instruction to early_idt_handler_common. 
We can see it in the output of the objdump util:
```
$ objdump -D vmlinux
...
ffffffff81fe5000 <early_idt_handler_array>:
ffffffff81fe5000:       6a 00                   pushq  $0x0
ffffffff81fe5002:       6a 00                   pushq  $0x0
ffffffff81fe5004:       e9 17 01 00 00          jmpq   ffffffff81fe5120 <early_idt_handler_common>
ffffffff81fe5009:       6a 00                   pushq  $0x0
ffffffff81fe500b:       6a 01                   pushq  $0x1
ffffffff81fe500d:       e9 0e 01 00 00          jmpq   ffffffff81fe5120 <early_idt_handler_common>
ffffffff81fe5012:       6a 00                   pushq  $0x0
ffffffff81fe5014:       6a 02                   pushq  $0x2
...
```
CPU pushes flag register, CS and RIP on the stack before calling interrupt handler. So before early_idt_handler_common will be executed, 
stack will contain following data:
```
|--------------------|
| %rflags            |
| %cs                |
| %rip               |
| error code         |
| vector number      |<-- %rsp
|--------------------|
```
early_idt_handler_common 
```
early_idt_handler_common:
	/*
	 * The stack is the hardware frame, an error code or zero, and the
	 * vector number.
	 */
	cld

	incl early_recursion_flag(%rip)

	/* The vector number is currently in the pt_regs->di slot. */
	pushq %rsi				/* pt_regs->si */
	movq 8(%rsp), %rsi			/* RSI = vector number */
	movq %rdi, 8(%rsp)			/* pt_regs->di = RDI */
	pushq %rdx				/* pt_regs->dx */
	pushq %rcx				/* pt_regs->cx */
	pushq %rax				/* pt_regs->ax */
	pushq %r8				/* pt_regs->r8 */
	pushq %r9				/* pt_regs->r9 */
	pushq %r10				/* pt_regs->r10 */
	pushq %r11				/* pt_regs->r11 */
	pushq %rbx				/* pt_regs->bx */
	pushq %rbp				/* pt_regs->bp */
	pushq %r12				/* pt_regs->r12 */
	pushq %r13				/* pt_regs->r13 */
	pushq %r14				/* pt_regs->r14 */
	pushq %r15				/* pt_regs->r15 */
	UNWIND_HINT_REGS

	cmpq $14,%rsi		/* Page fault? */
	jnz 10f
	GET_CR2_INTO(%rdi)	/* Can clobber any volatile register if pv */
	call early_make_pgtable
	andl %eax,%eax
	jz 20f			/* All good */

10:
	movq %rsp,%rdi		/* RDI = pt_regs; RSI is already trapnr */
	call early_fixup_exception

20:
	decl early_recursion_flag(%rip)
	jmp restore_regs_and_return_to_kernel
END(early_idt_handler_common)
```
First of all we increment early_recursion_flag to prevent recursion in the early_idt_handler_common
recursion 递推，递归
Next we save general registers on the stack:
pushq %rsi  ...  UNWIND_HINT_REGS

We need to do it to prevent wrong values of registers when we return from the interrupt handler. 
After this we check the vector number, and if it is #PF or Page Fault, we put value from the cr2 to the rdi register and call early_make_pgtable :
otherwise we call early_fixup_exception function by passing kernel stack pointer

After we decrement the early_recursion_flag, we restore registers which we saved before from the stack 
and return from the handler with iretq.

It is the end of the interrupt handler. We will examine the page fault handling and the other exception handling in order.



Page fault handling
In the previous paragraph we saw the early interrupt handler which checks if the vector number is page fault
and calls early_make_pgtable for building new page tables if it is. We need to have #PF handler in this step 
because there are plans to add ability to load kernel above 4G and make access to boot_params structure above the 4G.


early_make_pgtable  takes one parameter - the value of cr2 register, which contains the address caused page fault. 
Let's look on it:
arch/x86/kernel/head64.c
```
int __init early_make_pgtable(unsigned long address)
{
	unsigned long physaddr = address - __PAGE_OFFSET;
	pmdval_t pmd;

	pmd = (physaddr & PMD_MASK) + early_pmd_flags;

	return __early_make_pgtable(address, pmd);
}
```
We initialize pmd and pass it to the __early_make_pgtable function along with address.
https://github.com/torvalds/linux/blob/v4.16/arch/x86/kernel/head64.c
```
/* Create a new PMD entry */
int __init __early_make_pgtable(unsigned long address, pmdval_t pmd)
{
	unsigned long physaddr = address - __PAGE_OFFSET;
	pgdval_t pgd, *pgd_p;
	p4dval_t p4d, *p4d_p;
	pudval_t pud, *pud_p;
	pmdval_t *pmd_p;

	/* Invalid address or early pgt is done ?  */
	if (physaddr >= MAXMEM || read_cr3_pa() != __pa_nodebug(early_top_pgt))
		return -1;

```
It starts from the definition of some variables which have *val_t types. All of these types are declared 
as alias of unsigned long using typedef.

After we made the check that we have no invalid address, we're getting the address of the Page Global Directory entry 
which contains base address of Page Upper Directory and put its value to the pgd variable:
//todo Page Upper Directory
```
again:
	pgd_p = &early_top_pgt[pgd_index(address)].pgd;
	pgd = *pgd_p;
	...
	if (p4d)
		pud_p = (pudval_t *)((p4d & PTE_PFN_MASK) + __START_KERNEL_map - phys_base);
	else {
		if (next_early_pgt >= EARLY_DYNAMIC_PAGE_TABLES) {
			reset_early_page_tables();
			goto again;
		}

		pud_p = (pudval_t *)early_dynamic_pgts[next_early_pgt++];
		memset(pud_p, 0, sizeof(*pud_p) * PTRS_PER_PUD);
		*p4d_p = (p4dval_t)pud_p - __START_KERNEL_map + phys_base + _KERNPG_TABLE;
	}
```
And we check if pgd is presented. If it is, we assign the base address of the page upper directory table to pud_p
where PTE_PFN_MASK is a macro which mask lower 12 bits of (pte|pmd|pud|pgd)val_t.
arch/x86/include/asm/pgtable_types.h
```
/* Extracts the PFN from a (pte|pmd|pud|pgd)val_t of a 4KB page */
#define PTE_PFN_MASK		((pteval_t)PHYSICAL_PAGE_MASK)

//arch/x86/include/asm/pgtable_64_types.h
#define EARLY_DYNAMIC_PAGE_TABLES	64
```
if pgd is not presented, we check if next_early_pgt is not greater than EARLY_DYNAMIC_PAGE_TABLES which is 64 and 
present a fixed number of buffers to set up new page tables on demand. 
If next_early_pgt is greater than EARLY_DYNAMIC_PAGE_TABLES we reset page tables and start again from again label.
If next_early_pgt is less than EARLY_DYNAMIC_PAGE_TABLES, we assign the next entry of early_dynamic_pgts to pud_p and
  fill whole entry of the page upper directory with 0, then fill the page global directory entry with the base address 

And we fix pud_p to point to correct entry and assign its value to pud with the following:
```
pud_p += pud_index(address);
	pud = *pud_p;
```
And then we do the same routine as above, but to the page middle directory.

In the end we assign the given pmd which is passed by the early_make_pgtable function to the certain entry of 
page middle directory which maps kernel text+data virtual addresses:
```
pmd_p[pmd_index(address)] = pmd;
```
After page fault handler finished its work, as a result, early_top_pgt contains entries which point to the valid addresses.



Other exception handling
In early interrupt phase, exceptions other than page fault are handled by early_fixup_exception function 
and takes two parameters - pointer to kernel stack which consists of saved registers and vector number:
arch/x86/mm/extable.c
```   
/* Restricted version used during very early boot */
void __init early_fixup_exception(struct pt_regs *regs, int trapnr)
{
/* Ignore early NMIs. */
	if (trapnr == X86_TRAP_NMI)
		return;

	if (early_recursion_flag > 2)
		goto halt_loop;

	/*
	 * Old CPUs leave the high bits of CS on the stack
	 * undefined.  I'm not sure which CPUs do this, but at least
	 * the 486 DX works this way.
	 * Xen pv domains are not using the default __KERNEL_CS.
	 */
	if (!xen_pv_domain() && regs->cs != __KERNEL_CS)
		goto fail;
		
}
```
First of all we need to make some checks.Here we just ignore NMI and make sure that we are not in recursive situation.

https://en.wikipedia.org/wiki/Non-maskable_interrupt
In computing, a non-maskable interrupt (NMI) is a hardware interrupt that standard interrupt-masking techniques in the system cannot ignore.
It typically occurs to signal attention for non-recoverable hardware errors. Some NMIs may be masked, 
but only by using proprietary methods specific to the particular NMI. With regard to SPARC, the non-maskable interrupt (NMI), 
despite having the highest priority among interrupts, can be prevented from occurring through the use of an interrupt mask

arch/x86/mm/extable.c
```
if (fixup_exception(regs, trapnr))
		return;
```
The fixup_exception function finds the actual handler and call it. 
```
int fixup_exception(struct pt_regs *regs, int trapnr)
{
	const struct exception_table_entry *e;
	ex_handler_t handler;
    ...
	e = search_exception_tables(regs->ip);
	if (!e)
		return 0;

	handler = ex_fixup_handler(e);
	return handler(e, regs, trapnr);
}

static inline ex_handler_t
ex_fixup_handler(const struct exception_table_entry *x)
{
	return (ex_handler_t)((unsigned long)&x->handler + x->handler);
}
```
The ex_handler_t is a type of function pointer

kernel/extable.c
```
const struct exception_table_entry *search_exception_tables(unsigned long addr)
{
	const struct exception_table_entry *e;

	e = search_extable(__start___ex_table,
			   __stop___ex_table - __start___ex_table, addr);
	if (!e)
		e = search_module_extables(addr);
	return e;
}
```
The search_exception_tables function looks up the given address in the exception table (i.e. the contents of the ELF section,
__ex_table). After that, we get the actual address by ex_fixup_handler function. At last we call actual handler. 
For more information about exception table, you can refer to Documentation/x86/exception-tables.txt.
https://github.com/torvalds/linux/blob/v4.16/Documentation/x86/exception-tables.txt


arch/x86/mm/extable.c
```
if (fixup_bug(regs, trapnr))
		return;
```
arch/x86/kernel/traps.c
```
int fixup_bug(struct pt_regs *regs, int trapnr)
{
	if (trapnr != X86_TRAP_UD)
		return 0;

	switch (report_bug(regs->ip, regs)) {
	case BUG_TRAP_TYPE_NONE:
	case BUG_TRAP_TYPE_BUG:
		break;

	case BUG_TRAP_TYPE_WARN:
		regs->ip += LEN_UD2;
		return 1;
	}

	return 0;
}
```
All what this funtion does is just returns 1 if the exception is generated because #UD (or Invalid Opcode)
occured and the report_bug function returns BUG_TRAP_TYPE_WARN, otherwise returns 0

异常
Exceptions, as described in this article, are a type of interrupt generated by the CPU when an 'error' occurs. 
Some exceptions are not really errors in most cases, such as page faults.
https://wiki.osdev.org/Exceptions#Invalid_Opcode
Invalid Opcode
The Invalid Opcode exception occurs when the processor tries to execute an invalid or undefined opcode, or an instruction with invalid prefixes. It also occurs in other cases, such as:

The instruction length exceeds 15 bytes, but this only occurs with redundant prefixes.
The instruction tries to access a non-existent control register (for example, mov cr6, eax).
The UD instruction is executed.
The saved instruction pointer points to the instruction which caused the exception.