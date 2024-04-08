
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



