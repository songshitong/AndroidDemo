https://blog.csdn.net/u014535072/article/details/111405069

binder驱动相关      Binder跟键盘、显示器一样属于一种外设（没有实体的外设） linux在/dev下标记外设
binder驱动文件
通过adb shell进入Android设备，看下他的/dev目录长啥样
```lmi:/dev $ ls | grep "binder"
binder -> /dev/binderfs/binder
binderfs
hwbinder
vndbinder
```
可以看到有binder，标黄部分的3个分别是binder、hwbinder、vndbinder     binder是个链接


ioctl   int ioctl(int fd, ind cmd, …)；
Android自定义了自己的ioctl驱动
ProcessState.cpp中
status_t result = ioctl(fd, BINDER_VERSION, &vers);
result = ioctl(fd, BINDER_SET_MAX_THREADS, &maxThreads);

第二个参数cmd则是控制命令，如指令BINDER_SET_MAX_THREADS是“设置线程数”，最后的省略号则是各指令所需的参数，
如maxThreads表示最大线程数为 15。
指令BINDER_SET_MAX_THREADS的定义如下：
#define BINDER_SET_MAX_THREADS _IOW('b', 5, __u32)

binder_ioctl与ioctl有什么关系
因为在系统启动过程中，binder将其自身注册成了misc设备，那么在linux对这个注册的设备/dev/binder
执行系统调用ioctl的时候便会去调用它在注册的时候所传入的函数指针binder_ioctl
你调用ioctl的时候不是要传一个文件描述符吗？当这个文件描述符是打开的/dev/binder时，调用的就是binder_ioctl


goldfish 3.4
https://android.googlesource.com/kernel/goldfish/+/refs/heads/android-goldfish-3.4/drivers/staging/android/binder.c

参考gityuan
http://gityuan.com/2015/11/01/binder-driver/
http://gityuan.com/2015/11/02/binder-driver-2/


Binder驱动是Android专用的，但底层的驱动架构与Linux驱动一样。binder驱动在以misc设备进行注册，作为虚拟字符设备，没有直接操作硬件，
只是对设备内存的处理。主要是驱动设备的初始化(binder_init)，打开 (binder_open)，映射(binder_mmap)，数据操作(binder_ioctl)

系统调用
用户态的程序调用Kernel层驱动是需要陷入内核态，进行系统调用(syscall)，比如打开Binder驱动方法的调用链为： open-> __open() -> binder_open()。
open()为用户空间的方法，__open()便是系统调用中相应的处理方法，通过查找，对应调用到内核binder驱动的binder_open()方法，
至于其他的从用户态陷入内核态的流程也基本一致。



binder驱动的加载
linux的initCall机制  https://xinqiu.gitbooks.io/linux-insides-cn/content/Concepts/linux-cpu-3.html
在 Linux 内核启动中或启动后调用。实际上 initcall 机制的要点是确定内置模块和子系统初始化的正确顺序
```
early_param("debug", debug_kernel);
或者
arch_initcall(init_pit_clocksource);
device_initcall()等 

common/include/linux/init.h
#define device_initcall(fn)		__define_initcall(fn, 6)  //函数的第二个参数表示优先级，数字越小，优先级越高，带s的优先级低于不带s的优先级
```

binder.c最下面
common/drivers/android/binder.c
```
device_initcall(binder_init);
```

binder_init    
```
static int __init binder_init(void)
{
    ...
    在/sys/kernel/debug目录创建binder目录
	binder_debugfs_dir_entry_root = debugfs_create_dir("binder", NULL);
	
	const struct binder_debugfs_entry *db_entry; //5个元素的数组
    //创建文件state，stats，transactions，transaction_log，failed_transaction_log
	binder_for_each_debugfs_entry(db_entry)
		debugfs_create_file(db_entry->name,
					db_entry->mode,
					binder_debugfs_dir_entry_root,
					db_entry->data,
					db_entry->fops);

	binder_debugfs_dir_entry_proc = debugfs_create_dir("proc",
						binder_debugfs_dir_entry_root);
						
	if (!IS_ENABLED(CONFIG_ANDROID_BINDERFS) &&
	    strcmp(binder_devices_param, "") != 0) {
		/*
		* Copy the module_parameter string, because we don't want to
		* tokenize it in-place.
		 */
		// #define CONFIG_ANDROID_BINDER_DEVICES "binder,hwbinder,vndbinder" //out/android-mainline/common/include/generated/autoconf.h (3 results)
        // char *binder_devices_param = CONFIG_ANDROID_BINDER_DEVICES;
		device_names = kstrdup(binder_devices_param, GFP_KERNEL); //用于申请一段内存将形参s的内容copy到这段新申请的内存中 todo
		if (!device_names) {
			ret = -ENOMEM;
			goto err_alloc_device_names_failed;
		}

		device_tmp = device_names;
		while ((device_name = strsep(&device_tmp, ","))) {
			ret = init_binder_device(device_name);
			if (ret)
				goto err_init_binder_device_failed;
		}
	}					
    ...

	ret = init_binderfs();
	if (ret)
		goto err_init_binder_device_failed;

	return ret;
   ...
}

const struct binder_debugfs_entry binder_debugfs_entries[] = {
	{
		.name = "state",
		.mode = 0444,
		.fops = &state_fops,
		.data = NULL,
	},
	{
		.name = "stats",
		.mode = 0444,
		.fops = &stats_fops,
		.data = NULL,
	},
	{
		.name = "transactions",
		.mode = 0444,
		.fops = &transactions_fops,
		.data = NULL,
	},
	{
		.name = "transaction_log",
		.mode = 0444,
		.fops = &transaction_log_fops,
		.data = &binder_transaction_log,
	},
	{
		.name = "failed_transaction_log",
		.mode = 0444,
		.fops = &transaction_log_fops,
		.data = &binder_transaction_log_failed,
	},
	{} /* terminator */
};

static int __init init_binder_device(const char *name)
{
	int ret;
	struct binder_device *binder_device;

	binder_device = kzalloc(sizeof(*binder_device), GFP_KERNEL);
	if (!binder_device)
		return -ENOMEM;
    //binder注册虚拟字符设备所对应的file_operations
    //file_operation是把系统调用和驱动程序关联起来的关键结构，这个结构的每一个成员都对应着一个系统调用，
    //Linux系统调用通过读取file_operation中相应的函数指针，接着把控制权转交给函数，从而完成Linux设备驱动程序的工作
    //const struct file_operations binder_fops = {
    //    .owner = THIS_MODULE,
    //    .poll = binder_poll,
    //    .unlocked_ioctl = binder_ioctl,
    //    .compat_ioctl = compat_ptr_ioctl, //compat_ioctl是对32和64位的兼容，最终调用unlocked_ioctl
    //    .mmap = binder_mmap,
    //    .open = binder_open,
    //    .flush = binder_flush,
    //    .release = binder_release,
    //};
    //binder_device的miscdevice用于misc设备注册
	binder_device->miscdev.fops = &binder_fops;
	//动态分配次设备号  是否动态
	binder_device->miscdev.minor = MISC_DYNAMIC_MINOR;
	binder_device->miscdev.name = name;

	refcount_set(&binder_device->ref, 1);
	binder_device->context.binder_context_mgr_uid = INVALID_UID;
	binder_device->context.name = name;
	mutex_init(&binder_device->context.context_mgr_node_lock);
    //注册misc设备
	ret = misc_register(&binder_device->miscdev);
	if (ret < 0) {
		kfree(binder_device);
		return ret;
	}
    //将binder设备加入链表（头插法）
	hlist_add_head(&binder_device->hlist, &binder_devices);

	return ret;
}




int __init init_binderfs(void)
{
	int ret;
	const char *name;
	size_t len;

	/* Verify that the default binderfs device names are valid. */
	name = binder_devices_param;
	for (len = strcspn(name, ","); len > 0; len = strcspn(name, ",")) {
		if (len > BINDERFS_MAX_NAME)
			return -E2BIG; //名称校验
		name += len;
		if (*name == ',')
			name++;
	}

	/* Allocate new major number for binderfs. */
	ret = alloc_chrdev_region(&binderfs_dev, 0, BINDERFS_MAX_MINOR,
				  "binder"); //创建名为binder的字符设备 设备号动态分配 todo
	if (ret)
		return ret;
    //注册到内核file system
	ret = register_filesystem(&binder_fs_type);
	if (ret) {
		unregister_chrdev_region(binderfs_dev, BINDERFS_MAX_MINOR);
		return ret;
	}
	return ret;
}

//文件 type
static struct file_system_type binder_fs_type = {
	.name			= "binder",
	.init_fs_context	= binderfs_init_fs_context,
	.parameters		= binderfs_fs_parameters,
	.kill_sb		= binderfs_kill_super,
	.fs_flags		= FS_USERNS_MOUNT,
};
```
1 /sys/kernel/debug创建binder目录 并创建5个文件
2 注册binder为misc设备 文件有binder,hwbinder,vndbinder
miscellaneous 杂项各式各样的
在Linux驱动中把无法归类的五花八门的设备定义为misc设备，Linux内核所提供的misc设备有很强的包容性，各种无法归结为标准字符设备的类型都可以定义为misc设备，
譬如NVRAM，看门狗，实时时钟，字符LCD等
在Linux内核里把所有的misc设备组织在一起，构成了一个子系统(subsys)，统一进行管理。在这个子系统里的所有miscdevice类型的设备共享一个主设备号MISC_MAJOR(10)，
但次设备号不同
3 


binder_open
```
static int binder_open(struct inode *nodp, struct file *filp)
{
   //binder 进程信息
	struct binder_proc *proc, *itr;
	struct binder_device *binder_dev;
	struct binderfs_info *info;
	struct dentry *binder_binderfs_dir_entry_proc = NULL;
	bool existing_pid = false;

	...
    //用于管理binder的各种信息  GFP_KERNEL是Linux分配内存标识，允许在分配过程休眠、交换页到硬盘等，分配成功率会较高 https://hjk.life/posts/linux-kernel-memory/
	//GFP-get free pages
	proc = kzalloc(sizeof(*proc), GFP_KERNEL);
	if (proc == NULL)
		return -ENOMEM;
	//初始化自旋锁	
	spin_lock_init(&proc->inner_lock);
	spin_lock_init(&proc->outer_lock);
	get_task_struct(current->group_leader);
	proc->tsk = current->group_leader;  //将当前线程的task保存到binder进程的tsk
	proc->cred = get_cred(filp->f_cred);
	//初始化内核链表 list_head
	INIT_LIST_HEAD(&proc->todo);
	//初始化内核链表 wait_queue  //https://www.zhihu.com/column/c_1080796413692190720
	init_waitqueue_head(&proc->freeze_wait);
	//进程优先级
	if (binder_supported_policy(current->policy)) {
		proc->default_priority.sched_policy = current->policy;
		proc->default_priority.prio = current->normal_prio;
	} else {
		proc->default_priority.sched_policy = SCHED_NORMAL;
		proc->default_priority.prio = NICE_TO_PRIO(0);
	}

	/* binderfs stashes devices in i_private */
	if (is_binderfs_device(nodp)) {
		binder_dev = nodp->i_private;
		info = nodp->i_sb->s_fs_info;
		binder_binderfs_dir_entry_proc = info->proc_log_dir;
	} else {
		binder_dev = container_of(filp->private_data,
					  struct binder_device, miscdev);
	}
	refcount_inc(&binder_dev->ref);
	proc->context = &binder_dev->context;
	binder_alloc_init(&proc->alloc);

	binder_stats_created(BINDER_STAT_PROC); //binder状态为BINDER_STAT_PROC
	proc->pid = current->group_leader->pid;
	INIT_LIST_HEAD(&proc->delivered_death);//初始化已分发的死亡通知列表
	INIT_LIST_HEAD(&proc->waiting_threads);
	filp->private_data = proc; //proc保存到filp

	mutex_lock(&binder_procs_lock);
	hlist_for_each_entry(itr, &binder_procs, proc_node) {
		if (itr->pid == proc->pid) {
			existing_pid = true;
			break;
		}
	}
	//Binder驱动中通过static HLIST_HEAD(binder_procs);，创建了全局的哈希链表binder_procs，用于保存所有的binder_proc队列，
	//  每次新创建的binder_proc对象都会加入binder_procs链表中
	hlist_add_head(&proc->proc_node, &binder_procs); //将proc_node节点添加到binder_procs为表头的队列
	mutex_unlock(&binder_procs_lock);
    ...
	return 0;
}
```


binder_mmap
主要功能：首先在内核虚拟地址空间，申请一块与用户虚拟内存相同大小的内存；然后再申请1个page大小的物理内存，
再将同一块物理内存分别映射到内核虚拟地址空间和用户虚拟内存空间，从而实现了用户空间的Buffer和内核空间的Buffer同步操作的功能。
```
static const struct vm_operations_struct binder_vm_ops = {
	.open = binder_vma_open, //todo 
	.close = binder_vma_close,
	.fault = binder_vm_fault,
};
static int binder_mmap(struct file *filp, struct vm_area_struct *vma)
{
	struct binder_proc *proc = filp->private_data;

	if (proc->tsk != current->group_leader)
		return -EINVAL;
   ...
	vm_flags_mod(vma, VM_DONTCOPY | VM_MIXEDMAP, VM_MAYWRITE);
	vma->vm_ops = &binder_vm_ops;
	vma->vm_private_data = proc;
	return binder_alloc_mmap_handler(&proc->alloc, vma);
}
```
common/drivers/android/binder_alloc.c
进程虚拟内存空间与内核虚拟地址空间做映射
```
int binder_alloc_mmap_handler(struct binder_alloc *alloc,
			      struct vm_area_struct *vma)
{
	int ret;
	const char *failure_string;
	struct binder_buffer *buffer;

	if (unlikely(vma->vm_mm != alloc->mm)) {
		ret = -EINVAL;
		failure_string = "invalid vma->vm_mm";
		goto err_invalid_mm;
	}

	mutex_lock(&binder_alloc_mmap_lock);
	if (alloc->buffer_size) {
		ret = -EBUSY;
		failure_string = "already mapped";
		goto err_already_mapped;
	}
	//保证映射内存大小不超过4M
	alloc->buffer_size = min_t(unsigned long, vma->vm_end - vma->vm_start,
				   SZ_4M);
	mutex_unlock(&binder_alloc_mmap_lock);

	alloc->buffer = (void __user *)vma->vm_start; //alloc->buffer指向内核虚拟空间的地址
    //分配物理页的指针数组，数组大小为vma的等效page个数；  为数组分配内存，大小n*size，并对分配的内存清零
	alloc->pages = kcalloc(alloc->buffer_size / PAGE_SIZE,
			       sizeof(alloc->pages[0]),
			       GFP_KERNEL);
	if (alloc->pages == NULL) {
		ret = -ENOMEM;
		failure_string = "alloc page array";
		goto err_alloc_pages_failed;
	}

	buffer = kzalloc(sizeof(*buffer), GFP_KERNEL);
	if (!buffer) {
		ret = -ENOMEM;
		failure_string = "alloc buffer struct";
		goto err_alloc_buf_struct_failed;
	}

	buffer->user_data = alloc->buffer;
	list_add(&buffer->entry, &alloc->buffers);
	buffer->free = 1;
	//将此内核缓冲区加入到binder_alloc的空闲缓冲红黑树中
	binder_insert_free_buffer(alloc, buffer);
	alloc->free_async_space = alloc->buffer_size / 2;

	/* Signal binder_alloc is fully initialized */
	binder_alloc_set_vma(alloc, vma);

	return 0;
   ...
}
```
//https://juejin.cn/post/7062654742329032740
在旧版内核中，这里会调用binder_update_page_range函数分别将内核虚拟内存和进程虚拟内存与物理内存做映射，这样内核虚拟内存和进程虚拟内存也相当于间接建立了映射关系，
而在4.4.223中，这件事将会延迟到binder_ioctl后


binder_ioctl  todo ioctl的其他调用
https://juejin.cn/post/7069675794028560391
```
static long binder_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
{
	int ret;
	struct binder_proc *proc = filp->private_data;
	struct binder_thread *thread;
	//__user是一个宏，它告诉编译器不应该解除这个指针的引用（因为在当前地址空间中它是没有意义的），(void __user *)arg表示arg是一个用户空间的地址，
	不能直接进行拷贝等，要使用copy_from_user，copy_to_user等函数。 //todo
	void __user *ubuf = (void __user *)arg;

	binder_selftest_alloc(&proc->alloc);

	trace_binder_ioctl(cmd, arg);
    //睡眠直到binder_stop_on_user_error < 2==true
    //当binder因为错误而停止后，调用binder_ioctl，则会挂起进程，直到其他进程通过wake_up_interruptible来唤醒binder_user_error_wait队列，
	//并且满足binder_stop_on_user_error < 2这个条件，binder_ioctl才会继续往后运行
	ret = wait_event_interruptible(binder_user_error_wait, binder_stop_on_user_error < 2);
	if (ret)
		goto err_unlocked;
    //从binder_proc获得binder_thread  没有则新建并更新binder_proc
	thread = binder_get_thread(proc);
	if (thread == NULL) {
		ret = -ENOMEM;
		goto err;
	}

	switch (cmd) {
	case BINDER_WRITE_READ:
		...
	case BINDER_SET_MAX_THREADS: {
	...
	}
	case BINDER_SET_CONTEXT_MGR_EXT: {
		...
	}
	case BINDER_SET_CONTEXT_MGR:
		...
	case BINDER_THREAD_EXIT:
		...
	case BINDER_VERSION: {
	...
	}
	case BINDER_GET_NODE_INFO_FOR_REF: {
		....
	}
	case BINDER_GET_NODE_DEBUG_INFO: {
		....
	}
	case BINDER_FREEZE: {
	...
	case BINDER_GET_FROZEN_INFO: {
		....
	}
	case BINDER_ENABLE_ONEWAY_SPAM_DETECTION: {
	....
	}
	case BINDER_GET_EXTENDED_ERROR:
	...
	default:
		ret = -EINVAL;
		goto err;
	}
	ret = 0;
err:
	if (thread)
		thread->looper_need_return = false;
	wait_event_interruptible(binder_user_error_wait, binder_stop_on_user_error < 2);
	if (ret && ret != -EINTR)
		pr_info("%d:%d ioctl %x %lx returned %d\n", proc->pid, current->pid, cmd, arg, ret);
err_unlocked:
	trace_binder_ioctl_done(ret);
	return ret;
}
```

BINDER_WRITE_READ
```
case BINDER_WRITE_READ:
		ret = binder_ioctl_write_read(filp, arg, thread);
		if (ret)
			goto err;
		break;

struct binder_write_read {
	binder_size_t		write_size;	/* bytes to write */
	binder_size_t		write_consumed;	/* bytes consumed by driver */
	binder_uintptr_t	write_buffer;
	binder_size_t		read_size;	/* bytes to read */
	binder_size_t		read_consumed;	/* bytes consumed by driver */
	binder_uintptr_t	read_buffer;
};		
static int binder_ioctl_write_read(struct file *filp, unsigned long arg,
				struct binder_thread *thread)
{
	int ret = 0;
	struct binder_proc *proc = filp->private_data;
	void __user *ubuf = (void __user *)arg;
	struct binder_write_read bwr;
    //将用户空间ubuf拷贝至内核空间bwr
	if (copy_from_user(&bwr, ubuf, sizeof(bwr))) {
		ret = -EFAULT;
		goto out;
	}
	....
    //存在写数据
	if (bwr.write_size > 0) {
		ret = binder_thread_write(proc, thread,
					  bwr.write_buffer,
					  bwr.write_size,
					  &bwr.write_consumed);
		trace_binder_write_done(ret);
		if (ret < 0) {
			bwr.read_consumed = 0;
			if (copy_to_user(ubuf, &bwr, sizeof(bwr)))
				ret = -EFAULT;
			goto out;
		}
	}
	//存在读数据
	if (bwr.read_size > 0) {
		ret = binder_thread_read(proc, thread, bwr.read_buffer,
					 bwr.read_size,
					 &bwr.read_consumed,
					 filp->f_flags & O_NONBLOCK);
		trace_binder_read_done(ret);
		//如果todo队列中有未处理的任务，唤醒等待状态下的线程
		binder_inner_proc_lock(proc); //spin_lock(&proc->inner_lock);
		if (!binder_worklist_empty_ilocked(&proc->todo))
			binder_wakeup_proc_ilocked(proc);
		binder_inner_proc_unlock(proc);
		if (ret < 0) {
			if (copy_to_user(ubuf, &bwr, sizeof(bwr)))
				ret = -EFAULT;
			goto out;
		}
	}
	。。。
	//将内核空间修改后的bwr拷贝至用户空间ubuf  todo 拷贝回去有什么改变吗
	if (copy_to_user(ubuf, &bwr, sizeof(bwr))) {
		ret = -EFAULT;
		goto out;
	}
out:
	return ret;
}	
```
说好binder只进行一次复制的呢？其实是这样的没错，这里的copy_from_user或者copy_to_user只是复制了binder_write_read结构体，
得到了需要IPC数据的虚拟内存地址而已，真正的复制操作是在binder读写操作中进行的

binder_thread_write
```
static int binder_thread_write(struct binder_proc *proc,
			struct binder_thread *thread,
			binder_uintptr_t binder_buffer, size_t size,
			binder_size_t *consumed)
{
	uint32_t cmd;
	struct binder_context *context = proc->context;
	//需要写的数据
	void __user *buffer = (void __user *)(uintptr_t)binder_buffer;
	void __user *ptr = buffer + *consumed;
	void __user *end = buffer + size;

	while (ptr < end && thread->return_error.cmd == BR_OK) {
		int ret;
        //获得binder请求码
		if (get_user(cmd, (uint32_t __user *)ptr))
			return -EFAULT;
		ptr += sizeof(uint32_t);//移动ptr
		trace_binder_command(cmd);
		//记录binder数据信息
		if (_IOC_NR(cmd) < ARRAY_SIZE(binder_stats.bc)) {
			atomic_inc(&binder_stats.bc[_IOC_NR(cmd)]);
			atomic_inc(&proc->stats.bc[_IOC_NR(cmd)]);
			atomic_inc(&thread->stats.bc[_IOC_NR(cmd)]);
		}
		switch (cmd) {
		case BC_INCREFS:
		case BC_ACQUIRE:
		case BC_RELEASE:
		case BC_DECREFS: {
			...
		case BC_INCREFS_DONE:
		case BC_ACQUIRE_DONE: {
			...
		case BC_ATTEMPT_ACQUIRE:
		 ...
		case BC_ACQUIRE_RESULT:
			...
		case BC_FREE_BUFFER: {
			...
		case BC_TRANSACTION_SG:
		case BC_REPLY_SG: {
		....
		case BC_TRANSACTION:
		case BC_REPLY: {
			struct binder_transaction_data tr;
			if (copy_from_user(&tr, ptr, sizeof(tr)))
				return -EFAULT;
			ptr += sizeof(tr); //增长ptr
			binder_transaction(proc, thread, &tr,
					   cmd == BC_REPLY, 0);
			break;
		}
		case BC_REGISTER_LOOPER:
			....
			break;
		case BC_ENTER_LOOPER:
			...
		case BC_EXIT_LOOPER:
			....
		case BC_REQUEST_DEATH_NOTIFICATION:
		case BC_CLEAR_DEATH_NOTIFICATION: {
			....
		case BC_DEAD_BINDER_DONE: {
			...
		default:
			....
		}
		*consumed = ptr - buffer;
	}
	return 0;
}
```
binder请求码用于用户空间程序向binder驱动发送请求消息，以BC开头，被定义在enum binder_driver_command_protocol中
（include/uapi/linux/android/binder.h）
BC是binder command代表请求
BR是binder return代表响应

看一下BC_TRANSACTION/BC_REPLY
从用户空间中复制了一份binder_transaction_data到内核空间，接着就调用binder_transaction函数继续处理
binder_transaction函数的代码很长，我们可以将它分为几个部分：
1 获得目标进程/线程信息
2 将数据拷贝到目标进程所映射的内存中（此时会建立实际的映射关系）
3 将待处理的任务加入todo队列，唤醒目标线程
```
static struct binder_ref *binder_get_ref_olocked(struct binder_proc *proc,
						 u32 desc, bool need_strong_ref)
{
	struct rb_node *n = proc->refs_by_desc.rb_node;
	struct binder_ref *ref;

	while (n) {
		ref = rb_entry(n, struct binder_ref, rb_node_desc);
		if (desc < ref->data.desc) {
			n = n->rb_left;
		} else if (desc > ref->data.desc) {
			n = n->rb_right;
		} else if (need_strong_ref && !ref->data.strong) {
			binder_user_error("tried to use weak ref as strong ref\n");
			return NULL;
		} else {
			return ref;
		}
	}
	return NULL;
}


static struct binder_node *binder_get_node_refs_for_txn(
		struct binder_node *node,
		struct binder_proc **procp,
		uint32_t *error)
{
	struct binder_node *target_node = NULL;

	binder_node_inner_lock(node);
	if (node->proc) {
		target_node = node;
		//binder_node强引用计数加1
		binder_inc_node_nilocked(node, 1, 0, NULL);
		//binder_node临时引用计数加1
		binder_inc_node_tmpref_ilocked(node);
		//binder_proc临时引用计数加1
		node->proc->tmp_ref++;
		//使外部传入的proc指针指向binder_proc地址
		*procp = node->proc;
	} else
		*error = BR_DEAD_REPLY;
	binder_node_inner_unlock(node);

	return target_node;
}

static struct binder_thread *binder_get_txn_from_and_acq_inner(
		struct binder_transaction *t)
	__acquires(&t->from->proc->inner_lock)
{
	struct binder_thread *from;
    // t->from;
	from = binder_get_txn_from(t);
	if (!from) {
		__acquire(&from->proc->inner_lock);
		return NULL;
	}
	binder_inner_proc_lock(from->proc);
	if (t->from) {
		BUG_ON(from != t->from);
		return from;
	}
	binder_inner_proc_unlock(from->proc);
	__acquire(&from->proc->inner_lock);
	binder_thread_dec_tmpref(from);
	return NULL;
}

static struct binder_thread *binder_get_txn_from(
		struct binder_transaction *t)
{
	struct binder_thread *from;
	spin_lock(&t->lock);
	from = t->from;
	if (from) //引用计数增加
		atomic_inc(&from->tmp_ref);
	spin_unlock(&t->lock);
	return from;
}

static void binder_transaction(struct binder_proc *proc,
			       struct binder_thread *thread,
			       struct binder_transaction_data *tr, int reply,
			       binder_size_t extra_buffers_size)
{
	int ret;
	struct binder_transaction *t;
	struct binder_work *w;
	struct binder_work *tcomplete;
	binder_size_t buffer_offset = 0;
	binder_size_t off_start_offset, off_end_offset;
	binder_size_t off_min;
	binder_size_t sg_buf_offset, sg_buf_end_offset;
	binder_size_t user_offset = 0;
	struct binder_proc *target_proc = NULL;
	struct binder_thread *target_thread = NULL;
	struct binder_node *target_node = NULL;
	struct binder_transaction *in_reply_to = NULL;
	struct binder_transaction_log_entry *e;
	uint32_t return_error = 0;
	uint32_t return_error_param = 0;
	uint32_t return_error_line = 0;
	binder_size_t last_fixup_obj_off = 0;
	binder_size_t last_fixup_min_off = 0;
	struct binder_context *context = proc->context;
	int t_debug_id = atomic_inc_return(&binder_last_id);
	ktime_t t_start_time = ktime_get();
	char *secctx = NULL;
	u32 secctx_sz = 0;
	struct list_head sgc_head;
	struct list_head pf_head;
	const void __user *user_buffer = (const void __user *)
				(uintptr_t)tr->data.ptr.buffer;
	bool is_nested = false;
	INIT_LIST_HEAD(&sgc_head);
	INIT_LIST_HEAD(&pf_head);
	...
	binder_inner_proc_lock(proc);
	binder_set_extended_error(&thread->ee, t_debug_id, BR_OK, 0);
	binder_inner_proc_unlock(proc);

	if (reply) {
		binder_inner_proc_lock(proc);
		 //这个事务是发起事务，也就是说我们需要对这个事务做应答  todo binder事务
		in_reply_to = thread->transaction_stack;
		if (in_reply_to == NULL) {
			...error
		}
		if (in_reply_to->to_thread != thread) {
			...error
		}
		//改指向下一个需要处理的事务，即将这个事务移出链表  todo transaction_stack结构
		thread->transaction_stack = in_reply_to->to_parent;
		binder_inner_proc_unlock(proc);
		//目标线程即为需要回应的事务的发起线程
		target_thread = binder_get_txn_from_and_acq_inner(in_reply_to);
		if (target_thread == NULL) {
			...error
		}
		if (target_thread->transaction_stack != in_reply_to) {
			...//error
		}
		//通过binder_thread获得binder_proc
		target_proc = target_thread->proc;
		target_proc->tmp_ref++;
		binder_inner_proc_unlock(target_thread->proc);
	} else {
		if (tr->target.handle) {
			struct binder_ref *ref;

			/*
			 * There must already be a strong ref
			 * on this node. If so, do a strong
			 * increment on the node to ensure it
			 * stays alive until the transaction is
			 * done.
			 */
			binder_proc_lock(proc);
			//查找binder引用  从proc->refs_by_desc中查找
			ref = binder_get_ref_olocked(proc, tr->target.handle,
						     true);
			if (ref) {
			  //通过目标binder实体获取目标进程信息 binder_node和binder_proc
				target_node = binder_get_node_refs_for_txn(
						ref->node, &target_proc,
						&return_error);
			} else {
				...error
			}
			binder_proc_unlock(proc);
		} else {
		   //handle为0代表目标target是ServiceManager
			mutex_lock(&context->context_mgr_node_lock);
			//ServiceManager为binder驱动的context，所以可以直接从context中获取binder实体
			target_node = context->binder_context_mgr_node;
			if (target_node)
			  //通过目标binder实体获取目标进程信息
				target_node = binder_get_node_refs_for_txn(
						target_node, &target_proc,
						&return_error);
			else
				return_error = BR_DEAD_REPLY;
			mutex_unlock(&context->context_mgr_node_lock);
			if (target_node && target_proc->pid == proc->pid) {
				...error
			}
		}
		if (!target_node) {
			....error
		}
		e->to_node = target_node->debug_id;
		if (WARN_ON(proc == target_proc)) {
			....error
		}
		 //使用LSM进行安全检查
		if (security_binder_transaction(proc->cred,
						target_proc->cred) < 0) {
			....error
		}
		binder_inner_proc_lock(proc);

		w = list_first_entry_or_null(&thread->todo,
					     struct binder_work, entry);
		if (!(tr->flags & TF_ONE_WAY) && w &&
		    w->type == BINDER_WORK_TRANSACTION) {
			...error
		}
       //flags不带TF_ONE_WAY（即需要reply）并且当前线程存在binder事务栈
		if (!(tr->flags & TF_ONE_WAY) && thread->transaction_stack) {
			struct binder_transaction *tmp;

			tmp = thread->transaction_stack;
			if (tmp->to_thread != thread) {
				...error
			}
			//寻找一个合适的目标binder线程
			while (tmp) {
				struct binder_thread *from;

				spin_lock(&tmp->lock);
				from = tmp->from;
				if (from && from->proc == target_proc) {
					atomic_inc(&from->tmp_ref);
					target_thread = from;
					spin_unlock(&tmp->lock);
					is_nested = true;
					break;
				}
				spin_unlock(&tmp->lock);
				tmp = tmp->from_parent;
			}
		}
		binder_inner_proc_unlock(proc);
	}
}
```
BC_TRANSACTION中需要根据binder句柄来获取各种信息，
BC_REPLY只需要找到需要回应的那个事务，那个事务所在的线程和进程即为reply事务的目标线程和目标进程


数据拷贝，建立映射
common/drivers/android/binder_alloc.c
```
unsigned long
binder_alloc_copy_user_to_buffer(struct binder_alloc *alloc,
				 struct binder_buffer *buffer,
				 binder_size_t buffer_offset,
				 const void __user *from,
				 size_t bytes)
{
	if (!check_buffer(alloc, buffer, buffer_offset, bytes))
		return bytes;

	while (bytes) {
		unsigned long size;
		unsigned long ret;
		struct page *page;
		pgoff_t pgoff;
		void *kptr;

		page = binder_alloc_get_page(alloc, buffer,
					     buffer_offset, &pgoff);
		size = min_t(size_t, bytes, PAGE_SIZE - pgoff);
		kptr = kmap_local_page(page) + pgoff;
		ret = copy_from_user(kptr, from, size);
		kunmap_local(kptr);
		if (ret)
			return bytes - size + ret;
		bytes -= size;
		from += size;
		buffer_offset += size;
	}
	return 0;
}
```
```
if (target_thread)
	e->to_thread = target_thread->pid;
	e->to_proc = target_proc->pid;

	//为目标进程binder事务分配空间（后续会加到目标进程/线程的todo队列中，由目标进程/线程处理这个事务）
	t = kzalloc(sizeof(*t), GFP_KERNEL);
	if (t == NULL) {
		...error
	}
	INIT_LIST_HEAD(&t->fd_fixups);
	binder_stats_created(BINDER_STAT_TRANSACTION);
	spin_lock_init(&t->lock);

	tcomplete = kzalloc(sizeof(*tcomplete), GFP_KERNEL);
	if (tcomplete == NULL) {
		...error
	}
	binder_stats_created(BINDER_STAT_TRANSACTION_COMPLETE);

	t->debug_id = t_debug_id;
	t->start_time = t_start_time;

	....
    //设置事务发起线程
	if (!reply && !(tr->flags & TF_ONE_WAY))
		t->from = thread;
	else
		t->from = NULL;
	//设置事务处理进程和线程	
	t->from_pid = proc->pid;
	t->from_tid = thread->pid;
	t->sender_euid = task_euid(proc->tsk);
	t->to_proc = target_proc;
	t->to_thread = target_thread;
	t->code = tr->code;
	t->flags = tr->flags;
	t->is_nested = is_nested;
	//设置优先级
	if (!(t->flags & TF_ONE_WAY) &&
	    binder_supported_policy(current->policy)) {
		/* Inherit supported policies for synchronous transactions */
		t->priority.sched_policy = current->policy;
		t->priority.prio = current->normal_prio;
	} else {
		/* Otherwise, fall back to the default priority */
		t->priority = target_proc->default_priority;
	}
   //安全相关
	if (target_node && target_node->txn_security_ctx) {
		u32 secid;
		size_t added_size;

		security_cred_getsecid(proc->cred, &secid);
		ret = security_secid_to_secctx(secid, &secctx, &secctx_sz);
		if (ret) {
			...error
		}
		added_size = ALIGN(secctx_sz, sizeof(u64));
		extra_buffers_size += added_size;
		if (extra_buffers_size < added_size) {
			...error
		}
	}

	trace_binder_transaction(reply, t, target_node);
    //分配缓存，建立映射
	t->buffer = bindebinder_alloc_new_bufr_alloc_new_buf(&target_proc->alloc, tr->data_size,
		tr->offsets_size, extra_buffers_size,
		!reply && (t->flags & TF_ONE_WAY), current->tgid);
	if (IS_ERR(t->buffer)) {
		...error
	}
	if (secctx) {
		int err;
		size_t buf_offset = ALIGN(tr->data_size, sizeof(void *)) +
				    ALIGN(tr->offsets_size, sizeof(void *)) +
				    ALIGN(extra_buffers_size, sizeof(void *)) -
				    ALIGN(secctx_sz, sizeof(u64));

		t->security_ctx = (uintptr_t)t->buffer->user_data + buf_offset;
		err = binder_alloc_copy_to_buffer(&target_proc->alloc,
						  t->buffer, buf_offset,
						  secctx, secctx_sz);
		if (err) {
			t->security_ctx = 0;
			WARN_ON(1);
		}
		security_release_secctx(secctx, secctx_sz);
		secctx = NULL;
	}
	t->buffer->debug_id = t->debug_id;
	t->buffer->transaction = t;
	t->buffer->target_node = target_node;
	t->buffer->clear_on_free = !!(t->flags & TF_CLEAR_BUF);
	trace_binder_transaction_alloc_buf(t->buffer);
   //这里就是真正的一次复制
	if (binder_alloc_copy_user_to_buffer(
				&target_proc->alloc,
				t->buffer,
				ALIGN(tr->data_size, sizeof(void *)),
				(const void __user *)
					(uintptr_t)tr->data.ptr.offsets,
				tr->offsets_size)) {
		...error
	}
	
	//检查数据对齐
	if (!IS_ALIGNED(tr->offsets_size, sizeof(binder_size_t))) {
		...error
	}
	if (!IS_ALIGNED(extra_buffers_size, sizeof(u64))) {
		...error
	}
	off_start_offset = ALIGN(tr->data_size, sizeof(void *));
	buffer_offset = off_start_offset;
	off_end_offset = off_start_offset + tr->offsets_size;
	sg_buf_offset = ALIGN(off_end_offset, sizeof(void *));
	sg_buf_end_offset = sg_buf_offset + extra_buffers_size -
		ALIGN(secctx_sz, sizeof(u64));
	off_min = 0;
	
	//循环遍历每一个binder对象
	for (buffer_offset = off_start_offset; buffer_offset < off_end_offset;
	     buffer_offset += sizeof(binder_size_t)) {
		struct binder_object_header *hdr;
		size_t object_size;
		struct binder_object object;
		binder_size_t object_offset;
		binder_size_t copy_size;

		if (binder_alloc_copy_from_buffer(&target_proc->alloc,
						  &object_offset,
						  t->buffer,
						  buffer_offset,
						  sizeof(object_offset))) {
			...error
		}

		/*
		 * Copy the source user buffer up to the next object
		 * that will be processed.
		 */
		copy_size = object_offset - user_offset;
		if (copy_size && (user_offset > object_offset ||
				binder_alloc_copy_user_to_buffer(
					&target_proc->alloc,
					t->buffer, user_offset,
					user_buffer + user_offset,
					copy_size))) {
			...error
		}
		object_size = binder_get_object(target_proc, user_buffer,
				t->buffer, object_offset, &object);
		if (object_size == 0 || object_offset < off_min) {
			...error
		}
		/*
		 * Set offset to the next buffer fragment to be
		 * copied
		 */
		user_offset = object_offset + object_size;

		hdr = &object.hdr;
		off_min = object_offset + object_size;
		/需要对binder类型进行转换
        //因为在A进程中为本地binder对象，对于B进程则为远程binder对象，反之亦然
		switch (hdr->type) {
		case BINDER_TYPE_BINDER:
		case BINDER_TYPE_WEAK_BINDER: {
			struct flat_binder_object *fp;

			fp = to_flat_binder_object(hdr); //todo 转换的过程
			ret = binder_translate_binder(fp, t, thread);

			if (ret < 0 ||
			    binder_alloc_copy_to_buffer(&target_proc->alloc,
							t->buffer,
							object_offset,
							fp, sizeof(*fp))) {
				...error
			}
		} break;
		case BINDER_TYPE_HANDLE:
		case BINDER_TYPE_WEAK_HANDLE: {
			struct flat_binder_object *fp;

			fp = to_flat_binder_object(hdr);
			ret = binder_translate_handle(fp, t, thread);
			if (ret < 0 ||
			    binder_alloc_copy_to_buffer(&target_proc->alloc,
							t->buffer,
							object_offset,
							fp, sizeof(*fp))) {
				...error
			}
		} break;

		case BINDER_TYPE_FD: {
		  ...
		} break;
		case BINDER_TYPE_FDA: {
			...
		} break;
		case BINDER_TYPE_PTR: {
			....
		} break;
		default:
			...error
		}
	}
	/* Done processing objects, copy the rest of the buffer */
	if (binder_alloc_copy_user_to_buffer(
				&target_proc->alloc,
				t->buffer, user_offset,
				user_buffer + user_offset,
				tr->data_size - user_offset)) {
		...error
	}

	ret = binder_do_deferred_txn_copies(&target_proc->alloc, t->buffer,
					    &sgc_head, &pf_head);
	if (ret) {
		...error
	}
	 //设置工作类型
	if (t->buffer->oneway_spam_suspect)
		tcomplete->type = BINDER_WORK_TRANSACTION_ONEWAY_SPAM_SUSPECT;
	else
		tcomplete->type = BINDER_WORK_TRANSACTION_COMPLETE;
	//设置目标进程的事务类型	
	t->work.type = BINDER_WORK_TRANSACTION;

```
我们可以将这一部分再细分成几个部分：
1 分配缓存，建立映射
2 数据拷贝
3 binder类型转换

数据拷贝
关于数据拷贝这部分就不用多说了，物理内存已经分配好了，映射也建立了，接下来直接调用copy_from_user将数据从用户空间拷贝至映射的那块内存就可以了
binder类型转换
最后循环遍历每一个binder对象，对其中每一个binder对象类型做转换，因为在一个进程中为本地binder对象，对于另一个进程则为远程binder对象，反之亦然

分配缓存，建立映射
```
common/drivers/android/binder_alloc.c
struct binder_buffer *binder_alloc_new_buf(struct binder_alloc *alloc,
					   size_t data_size,
					   size_t offsets_size,
					   size_t extra_buffers_size,
					   int is_async,
					   int pid)
{
	struct binder_buffer *buffer;
	mutex_lock(&alloc->mutex);
	buffer = binder_alloc_new_buf_locked(alloc, data_size, offsets_size,
					     extra_buffers_size, is_async, pid);
	mutex_unlock(&alloc->mutex);
	return buffer;
}

static struct binder_buffer *binder_alloc_new_buf_locked(
				struct binder_alloc *alloc,
				size_t data_size,
				size_t offsets_size,
				size_t extra_buffers_size,
				int is_async,
				int pid)
{
	struct rb_node *n = alloc->free_buffers.rb_node;
	struct binder_buffer *buffer;
	size_t buffer_size;
	struct rb_node *best_fit = NULL;
	void __user *has_page_addr;
	void __user *end_page_addr;
	size_t size, data_offsets_size;
	int ret;

	/* Check binder_alloc is fully initialized */
	if (!binder_alloc_get_vma(alloc)) {
		...//error
	}

    //计算需要的缓冲区大小
    //这里需要将size对齐void *（32位下占用4字节，64位下占用8字节）
	data_offsets_size = ALIGN(data_size, sizeof(void *)) +
		ALIGN(offsets_size, sizeof(void *));

	if (data_offsets_size < data_size || data_offsets_size < offsets_size) {
		..//error
	}
	size = data_offsets_size + ALIGN(extra_buffers_size, sizeof(void *));
	if (size < data_offsets_size || size < extra_buffers_size) {
		..//error
	}
	if (is_async &&
	    alloc->free_async_space < size + sizeof(struct binder_buffer)) {
		..//error
	}

	/* Pad 0-size buffers so they get assigned unique addresses */
	size = max(size, sizeof(void *));
   //从binder_alloc的空闲缓冲红黑树中找到一个大小最合适的binder_buffer
	while (n) {
	   //当找到一个需求大小和缓存区大小刚好相同的空闲缓存区时
        //此时buffer就正好指向这个空闲缓存区
		buffer = rb_entry(n, struct binder_buffer, rb_node);
		BUG_ON(!buffer->free);
		buffer_size = binder_alloc_buffer_size(alloc, buffer);

		if (size < buffer_size) {
			best_fit = n;
			n = n->rb_left;
		} else if (size > buffer_size)
			n = n->rb_right;
		else {
			best_fit = n;
			break;
		}
	}
	if (best_fit == NULL) {
		...//error
	}
	//此时buffer指向的是所需求的空闲缓存区所在红黑树节点的父节点
    //需要让其指向真正需求的那个空闲缓存区
	if (n == NULL) {
		buffer = rb_entry(best_fit, struct binder_buffer, rb_node);
		buffer_size = binder_alloc_buffer_size(alloc, buffer);
	}
	....
    /计算出buffer的终点，向下对齐（不能超过可用的buffer_size）
	has_page_addr = (void __user *)
		(((uintptr_t)buffer->user_data + buffer_size) & PAGE_MASK);
	WARN_ON(n && buffer_size != size);
	//计算出实际上我们接收数据需要的空间的终点，向上映射  todo PAGE_ALIGN
	end_page_addr =
		(void __user *)PAGE_ALIGN((uintptr_t)buffer->user_data + size);
	if (end_page_addr > has_page_addr)
		end_page_addr = has_page_addr;
	//分配物理页，建立映射	
	ret = binder_update_page_range(alloc, 1, (void __user *)
		PAGE_ALIGN((uintptr_t)buffer->user_data), end_page_addr);
	if (ret)
		return ERR_PTR(ret);
    //有空余空间的话，分隔这个buffer，剩余的buffer加入到空闲缓存区红黑树中（合理利用空间）
	if (buffer_size != size) {
		struct binder_buffer *new_buffer;

		new_buffer = kzalloc(sizeof(*buffer), GFP_KERNEL);
		if (!new_buffer) {
			pr_err("%s: %d failed to alloc new buffer struct\n",
			       __func__, alloc->pid);
			goto err_alloc_buf_struct_failed;
		}
		new_buffer->user_data = (u8 __user *)buffer->user_data + size;
		list_add(&new_buffer->entry, &buffer->entry);
		new_buffer->free = 1;
		binder_insert_free_buffer(alloc, new_buffer);
	}
    //我们已经使用了这个buffer，要将其从空闲缓存区红黑树中移除  todo 红黑树操作
	rb_erase(best_fit, &alloc->free_buffers);
	//标记为非空闲
	buffer->free = 0;
	buffer->allow_user_free = 0;
	//插入到已分配缓存区红黑树中
	binder_insert_allocated_buffer_locked(alloc, buffer);
	...
	buffer->data_size = data_size;
	buffer->offsets_size = offsets_size;
	buffer->async_transaction = is_async;
	buffer->extra_buffers_size = extra_buffers_size;
	buffer->pid = pid;
	buffer->oneway_spam_suspect = false;
	 //如果是异步事件, 那么更新binder_alloc的异步事件空闲buffer
	if (is_async) {
		alloc->free_async_space -= size + sizeof(struct binder_buffer);
		....
		if (alloc->free_async_space < alloc->buffer_size / 10) {
			/*
			 * Start detecting spammers once we have less than 20%
			 * of async space left (which is less than 10% of total
			 * buffer size).
			 */
			buffer->oneway_spam_suspect = debug_low_async_space_locked(alloc, pid);
		} else {
			alloc->oneway_spam_detected = false;
		}
	}
	return buffer;
....
}

```
这个函数的整体逻辑分为三个部分：
找到可用的空闲内核缓存区，计算我们需要分配的大小
分配物理页，建立映射
初始化新分配的buffer

怎么分配物理页，建立映射
common/drivers/android/binder_alloc.c
```
static int binder_update_page_range(struct binder_alloc *alloc, int allocate,
				    void __user *start, void __user *end)
{
	void __user *page_addr;
	unsigned long user_page_addr;
	struct binder_lru_page *page;
	struct vm_area_struct *vma = NULL;
	struct mm_struct *mm = NULL;
	bool need_mm = false;
	....
	if (end <= start)
		return 0;

	trace_binder_update_page_range(alloc, allocate, start, end);

	if (allocate == 0)
		goto free_range;
   //检查是否有页框需要分配
	for (page_addr = start; page_addr < end; page_addr += PAGE_SIZE) {
		page = &alloc->pages[(page_addr - alloc->buffer) / PAGE_SIZE];
		if (!page->page_ptr) {
			need_mm = true;
			break;
		}
	}
    //指向目标用户进程的内存空间描述体
	if (need_mm && mmget_not_zero(alloc->mm))
		mm = alloc->mm;

	if (mm) {
		mmap_write_lock(mm);
		vma = alloc->vma;
	}

	if (!vma && need_mm) {
		....
		goto err_no_vma;
	}

	for (page_addr = start; page_addr < end; page_addr += PAGE_SIZE) {
		int ret;
		bool on_lru;
		size_t index;
        //指向对应页框地址，为后面赋值做准备
		index = (page_addr - alloc->buffer) / PAGE_SIZE;
		page = &alloc->pages[index];
        //page->page_ptr不为NULL说明之前已经分配并映射过了
		if (page->page_ptr) {
			trace_binder_alloc_lru_start(alloc, index);

			on_lru = list_lru_del(&binder_alloc_lru, &page->lru);
			WARN_ON(!on_lru);

			trace_binder_alloc_lru_end(alloc, index);
			continue;
		}

		if (WARN_ON(!vma))
			goto err_page_ptr_cleared;

		trace_binder_alloc_page_start(alloc, index);
		//分配一个页的物理内存
		page->page_ptr = alloc_page(GFP_KERNEL |
					    __GFP_HIGHMEM |
					    __GFP_ZERO);
		//未分配成功			    
		if (!page->page_ptr) {
			...
			goto err_alloc_page_failed;
		}
		page->alloc = alloc;
		INIT_LIST_HEAD(&page->lru);

		user_page_addr = (uintptr_t)page_addr;
		//将物理内存空间映射到目标用户进程虚拟内存空间  todo 其他内核还有map_kernel_range_noflush 将物理页和内核地址映射的逻辑
		ret = vm_insert_page(vma, user_page_addr, page[0].page_ptr);
		if (ret) {
			....
			goto err_vm_insert_page_failed;
		}

		if (index + 1 > alloc->pages_high)
			alloc->pages_high = index + 1;

		trace_binder_alloc_page_end(alloc, index);
	}
	if (mm) {
	    //释放mm_struct的读信号量
		mmap_write_unlock(mm);
		mmput(mm);
	}
	return 0;
。。。
}
```
先分配物理内存，再将这块物理内存映射到用户进程虚拟空间  前面mmap里面内核虚拟空间与用户进程虚拟空间做了映射


第三部分：加入todo队列，唤醒目标线程
```
if (reply) {
     //如果请求码为BC_REPLY
     //将tcomplete插入到事务发起binder线程的todo队列中
		binder_enqueue_thread_work(thread, tcomplete);
		binder_inner_proc_lock(target_proc);
		if (target_thread->is_dead) {
			return_error = BR_DEAD_REPLY;
			binder_inner_proc_unlock(target_proc);
			goto err_dead_proc_or_thread;
		}
		BUG_ON(t->buffer->async_transaction != 0);
		//将发起事务从目标binder线程的事务链表中移除
		binder_pop_transaction_ilocked(target_thread, in_reply_to);
		//将t->work插入到目标binder线程的todo队列中
		binder_enqueue_thread_work_ilocked(target_thread, &t->work);
		target_proc->outstanding_txns++;
		binder_inner_proc_unlock(target_proc);
		if (in_reply_to->is_nested) {
			spin_lock(&thread->prio_lock);
			thread->prio_state = BINDER_PRIO_PENDING;
			thread->prio_next = in_reply_to->saved_priority;
			spin_unlock(&thread->prio_lock);
		}
		 //唤醒目标binder线程的等待队列
		wake_up_interruptible_sync(&target_thread->wait);
		//恢复发起事务的优先级
		binder_restore_priority(thread, &in_reply_to->saved_priority);
		//释放发起事务
		binder_free_transaction(in_reply_to);
	} else if (!(t->flags & TF_ONE_WAY)) {
	    //如果请求码为BC_TRANSACTION并且不为异步操作，需要返回
		BUG_ON(t->buffer->async_transaction != 0);
		binder_inner_proc_lock(proc);
		/*
		 * Defer the TRANSACTION_COMPLETE, so we don't return to
		 * userspace immediately; this allows the target process to
		 * immediately start processing this transaction, reducing
		 * latency. We will then return the TRANSACTION_COMPLETE when
		 * the target replies (or there is an error).
		 */
		//将tcomplete插入到事务发起binder线程的todo队列中（这里会延迟执行BINDER_WORK_TRANSACTION_COMPLETE） 
		binder_enqueue_deferred_thread_work_ilocked(thread, tcomplete);
		//设置为需要回应
		t->need_reply = 1;
		//插入事务链表中
		t->from_parent = thread->transaction_stack;
		thread->transaction_stack = t;
		binder_inner_proc_unlock(proc);
		//将t->work插入目标线程的todo队列中并唤醒目标进程
		return_error = binder_proc_transaction(t,
				target_proc, target_thread);
		if (return_error) {
			binder_inner_proc_lock(proc);
			//出错后，移除该事务
			binder_pop_transaction_ilocked(thread, t);
			binder_inner_proc_unlock(proc);
			goto err_dead_proc_or_thread;
		}
	} else {
		...
		return_error = binder_proc_transaction(t, target_proc, NULL);
		/*
		 * Let the caller know when async transaction reaches a frozen
		 * process and is put in a pending queue, waiting for the target
		 * process to be unfrozen.
		 */
		if (return_error == BR_TRANSACTION_PENDING_FROZEN)
			tcomplete->type = BINDER_WORK_TRANSACTION_PENDING;
		//如果请求码为BC_TRANSACTION并且为异步操作，不需要返回
        //将tcomplete插入到事务发起binder线程的todo队列中
        binder_enqueue_thread_work(thread, tcomplete);
        //将t->work插入目标进程的某个线程（或目标进程）的todo队列中并唤醒目标进程
		binder_enqueue_thread_work(thread, tcomplete);
		if (return_error &&
		    return_error != BR_TRANSACTION_PENDING_FROZEN)
			goto err_dead_proc_or_thread;
	}
	 //减临时引用计数
	if (target_thread)
		binder_thread_dec_tmpref(target_thread);
	binder_proc_dec_tmpref(target_proc);
	if (target_node)
		binder_dec_node_tmpref(target_node);
	/*
	 * write barrier to synchronize with initialization
	 * of log entry
	 */
	smp_wmb();
	WRITE_ONCE(e->debug_id_done, t_debug_id);
	return;
   ...//错误处理
```
这一块的代码基本上格式都是一样的，都是将tcomplete插入到事务发起binder线程的todo队列中，t->work插入到目标binder线程的todo队列中，
最后唤醒目标进程


binder_thread_read
common/drivers/android/binder.c
binder读操作的函数，这个函数也是比较长，将它分成几个部分：
1 等待可用的binder_work
2 循环获取todo队列中的binder_work，并根据binder_work的type，执行一定的处理
3 处理binder_transaction以及binder_transaction_data，并将binder_transaction_data拷贝回用户空间

```
https://juejin.cn/post/7073783503791325214
binder_thread.looper取值
BINDER_LOOPER_STATE_REGISTERED：表示该binder线程是非主binder线程
BINDER_LOOPER_STATE_ENTERED：表示该binder线程是主binder线程
BINDER_LOOPER_STATE_EXITED：表示该binder线程马上就要退出了
BINDER_LOOPER_STATE_INVALID：表示该binder线程是无效的（比如原来是binder主线程，后续用户又发送了一个BC_REGISTER_LOOPER请求）
BINDER_LOOPER_STATE_WAITING：表示当前binder线程正在等待请求
BINDER_LOOPER_STATE_NEED_RETURN：表示该binder线程在处理完transaction后需要返回到用户态

static bool binder_available_for_proc_work_ilocked(struct binder_thread *thread)
{
	return !thread->transaction_stack &&
		binder_worklist_empty_ilocked(&thread->todo) && //list_empty() list非空
		(thread->looper & (BINDER_LOOPER_STATE_ENTERED |
				   BINDER_LOOPER_STATE_REGISTERED)); //这个binder线程既不是主线程，也没有被注册成binder子线程
}

static int binder_wait_for_work(struct binder_thread *thread,
				bool do_proc_work)
{
	DEFINE_WAIT(wait);//定义一个等待队列项  todo https://juejin.cn/post/7073783503791325214
	struct binder_proc *proc = thread->proc;
	int ret = 0;

	binder_inner_proc_lock(proc);
	for (;;) {
	   //准备睡眠等待
		prepare_to_wait(&thread->wait, &wait, TASK_INTERRUPTIBLE|TASK_FREEZABLE);
		 //检查确认是否有binder_work可以处理
		if (binder_has_work_ilocked(thread, do_proc_work))
			break;
		if (do_proc_work) //可以处理binder_proc.todo中的工作的话  将该binder线程加入到binder_proc中的等待线程中
			list_add(&thread->waiting_thread_node,
				 &proc->waiting_threads);
		binder_inner_proc_unlock(proc);
		schedule();
		binder_inner_proc_lock(proc);
		list_del_init(&thread->waiting_thread_node);//将该binder线程从binder_proc中的等待线程中移除
		if (signal_pending(current)) {//检查当前系统调用进程是否有信号处理
			ret = -EINTR;
			break;
		}
	}
	//结束等待
	finish_wait(&thread->wait, &wait);
	binder_inner_proc_unlock(proc);
	return ret;
}

static bool binder_has_work_ilocked(struct binder_thread *thread,
				    bool do_proc_work)
{
	return thread->process_todo ||
		thread->looper_need_return ||
		(do_proc_work &&
		 !binder_worklist_empty_ilocked(&thread->proc->todo));
}


static int binder_thread_read(struct binder_proc *proc,
			      struct binder_thread *thread,
			      binder_uintptr_t binder_buffer, size_t size,
			      binder_size_t *consumed, int non_block)
{
    //用户空间传进来的需要将数据读到的地址
    //实际上只是传输一些命令码和一个binder_transaction_data_secctx结构体
    //真正的数据已经映射到用户虚拟内存空间中了，根据binder_transaction_data中所给的地址直接读就可以了
	void __user *buffer = (void __user *)(uintptr_t)binder_buffer;
	//起始地址 = 读数据的首地址 + 已读数据大小
	void __user *ptr = buffer + *consumed;
	//结束地址 = 读数据的首地址 + 读数据的总大小
	void __user *end = buffer + size;

	int ret = 0;
	int wait_for_proc_work;

	if (*consumed == 0) {
	    //向用户空间写一个binder响应码，该响应码不做任何操作
	    //get_user和put_user 类似于copy_to_user和copy_from_user，适用于简单类型变量(char、int、long等)
		if (put_user(BR_NOOP, (uint32_t __user *)ptr))
			return -EFAULT;
		ptr += sizeof(uint32_t);
	}

retry:
	binder_inner_proc_lock(proc);
	//检查是否有可用的工作需要处理
	wait_for_proc_work = binder_available_for_proc_work_ilocked(thread);
	binder_inner_proc_unlock(proc);
    //将线程的状态置为等待中
	thread->looper |= BINDER_LOOPER_STATE_WAITING;

	trace_binder_wait_for_work(wait_for_proc_work,
				   !!thread->transaction_stack,
				   !binder_worklist_empty(proc, &thread->todo));
	//如果没有可用的工作，可以等待进程todo队列中的工作			   
	if (wait_for_proc_work) {
		if (!(thread->looper & (BINDER_LOOPER_STATE_REGISTERED |
					BINDER_LOOPER_STATE_ENTERED))) {
			...//error
			//进程进入休眠状态，等待唤醒
			wait_event_interruptible(binder_user_error_wait,
						 binder_stop_on_user_error < 2);
		}
		//恢复优先级    todo 优先级有什么用
		binder_restore_priority(thread, &proc->default_priority);
	}

	if (non_block) {
	//如果是非阻塞模式（这里似乎不会执行到）
        //线程和进程的todo队列中都没有工作
		if (!binder_has_work(thread, wait_for_proc_work))
			ret = -EAGAIN;
	} else {
	    //如果是阻塞模式
        //等待binder工作到来
		ret = binder_wait_for_work(thread, wait_for_proc_work);
	}

    //将线程的等待中状态解除
	thread->looper &= ~BINDER_LOOPER_STATE_WAITING;

	if (ret)
		return ret;
```
这一部分先检查是否有可用的binder_work待处理，如果有的话进入到下一部分，如果没有的话则需要等待


第二部分：获取工作，根据type做一定的处理
```
 //循环处理todo队列中的工作
while (1) {
		uint32_t cmd;
		struct binder_transaction_data_secctx tr;
		struct binder_transaction_data *trd = &tr.transaction_data;
		struct binder_work *w = NULL;
		struct list_head *list = NULL;
		struct binder_transaction *t = NULL;
		struct binder_thread *t_from;
		size_t trsize = sizeof(*trd);

		binder_inner_proc_lock(proc);
		//找到需要处理的todo队列
		if (!binder_worklist_empty_ilocked(&thread->todo))
			list = &thread->todo;
		else if (!binder_worklist_empty_ilocked(&proc->todo) &&
			   wait_for_proc_work)
			list = &proc->todo;
		else {
			binder_inner_proc_unlock(proc);

			/* no data added */
			if (ptr - buffer == 4 && !thread->looper_need_return) //不需要返回进行重试
            //只跳过了数据头部的命令码，没有读取任何数据
				goto retry;
			break;
		}
        //传输过来的数据大小不符合
		if (end - ptr < sizeof(tr) + 4) {
			binder_inner_proc_unlock(proc);
			break;
		}
		//从todo队列中出队一项binder_work
		w = binder_dequeue_work_head_ilocked(list);
		if (binder_worklist_empty_ilocked(&thread->todo))
			thread->process_todo = false;

		switch (w->type) {
		case BINDER_WORK_TRANSACTION: {
			binder_inner_proc_unlock(proc);
			//根据binder_work找到binder_transaction结构
			t = container_of(w, struct binder_transaction, work);
		} break;
		case BINDER_WORK_RETURN_ERROR: {
			...
		} break;
		case BINDER_WORK_TRANSACTION_COMPLETE:
		case BINDER_WORK_TRANSACTION_PENDING:
		case BINDER_WORK_TRANSACTION_ONEWAY_SPAM_SUSPECT: {
			if (proc->oneway_spam_detection_enabled &&
				   w->type == BINDER_WORK_TRANSACTION_ONEWAY_SPAM_SUSPECT)
				cmd = BR_ONEWAY_SPAM_SUSPECT;
			else if (w->type == BINDER_WORK_TRANSACTION_PENDING)
				cmd = BR_TRANSACTION_PENDING_FROZEN;
			else
				cmd = BR_TRANSACTION_COMPLETE;
			binder_inner_proc_unlock(proc);
			kfree(w);//释放  todo
			binder_stats_deleted(BINDER_STAT_TRANSACTION_COMPLETE);
			//回复给用户进程BR_TRANSACTION_COMPLETE响应码
			if (put_user(cmd, (uint32_t __user *)ptr))
				return -EFAULT;
			ptr += sizeof(uint32_t);
             //更新统计数据
			binder_stat_br(proc, thread, cmd);
			...
		} break;
		case BINDER_WORK_NODE: {
			....
		case BINDER_WORK_DEAD_BINDER:
		case BINDER_WORK_DEAD_BINDER_AND_CLEAR:
		case BINDER_WORK_CLEAR_DEATH_NOTIFICATION: {
			...
		} break;
		default:
			binder_inner_proc_unlock(proc);
			...
			break;
		}
...
}
```
这里先创建了一个binder_transaction_data_secctx结构体，后续会将它拷贝到用户空间去，然后创建了一个指针trd指向tr.transaction_data的地址，
   这样后续操作trd就相当于操作tr.transaction_data了
当进程从睡眠中唤醒，意味着有可用的binder_work了，这时候理论上来说，binder_thread和binder_proc其中总有一个todo队列不为空，
  这里优先处理binder_thread的todo队列，如果两者都为空，且还未读取过任何数据，重新goto到retry处等待
接着就是将binder_work从相应的todo队列中出队，再根据其类型执行不同的处理操作，这里我们只针对BINDER_WORK_TRANSACTION 
   和BINDER_WORK_TRANSACTION_COMPLETE这两种最重要的类型分析
当类型为BINDER_WORK_TRANSACTION时，表示是别的进程向自己发起binder请求，此时，我们根据binder_work找到对应的binder_transaction结构
当类型为BINDER_WORK_TRANSACTION_COMPLETE时，表示发起的请求BC_TRANSACTION已经完成了，此时将回复给用户空间BR_TRANSACTION_COMPLETE响应码，
   然后更新统计数据，释放资源

第三部分：处理binder_transaction，拷贝回用户空间
```
while (1) {
		uint32_t cmd;
		struct binder_transaction_data_secctx tr;
		struct binder_transaction_data *trd = &tr.transaction_data;
		struct binder_work *w = NULL;
		struct list_head *list = NULL;
		struct binder_transaction *t = NULL;
		struct binder_thread *t_from;
		size_t trsize = sizeof(*trd);
		....
		if (!t) //只有在type == BINDER_WORK_TRANSACTION的情况下，t才会被赋值
			continue;

		BUG_ON(t->buffer == NULL);
		if (t->buffer->target_node) {//binder实体不为NULL，对应着BC_TRANSACTION请求
			struct binder_node *target_node = t->buffer->target_node;
            //binder实体在用户空间中的地址
			trd->target.ptr = target_node->ptr;
			//携带的额外数据
			trd->cookie =  target_node->cookie;
			//设置优先级
			binder_transaction_priority(thread, t, target_node);
			//设置响应码
			cmd = BR_TRANSACTION;
		} else {//binder实体为NULL，对应着BC_REPLY请求
			trd->target.ptr = 0;
			trd->cookie = 0;
			cmd = BR_REPLY;
		}
		 //表示要对目标对象请求的命令代码
		trd->code = t->code;
		//事务标志，详见enum transaction_flags
		trd->flags = t->flags;
		//请求发起进程的uid
		trd->sender_euid = from_kuid(current_user_ns(), t->sender_euid);
         //获取发起请求的binder线程
		t_from = binder_get_txn_from(t);
		if (t_from) {
			struct task_struct *sender = t_from->proc->tsk;
            //设置发起请求的进程pid
			trd->sender_pid =
				task_tgid_nr_ns(sender,
						task_active_pid_ns(current));
		} else {
			trd->sender_pid = 0;
		}

		ret = binder_apply_fd_fixups(proc, t);
		if (ret) {
			struct binder_buffer *buffer = t->buffer;
			bool oneway = !!(t->flags & TF_ONE_WAY);
			int tid = t->debug_id;

			if (t_from)
				binder_thread_dec_tmpref(t_from);
			buffer->transaction = NULL;
			binder_cleanup_transaction(t, "fd fixups failed",
						   BR_FAILED_REPLY);
			binder_free_buf(proc, thread, buffer, true);
			...
			if (cmd == BR_REPLY) {
				cmd = BR_FAILED_REPLY;
				if (put_user(cmd, (uint32_t __user *)ptr))
					return -EFAULT;
				ptr += sizeof(uint32_t);
				binder_stat_br(proc, thread, cmd);
				break;
			}
			continue;
		}
		//数据大小
		trd->data_size = t->buffer->data_size;
		//偏移数组大小
		trd->offsets_size = t->buffer->offsets_size;
		//设置数据区首地址（这里通过内核空间地址和user_buffer_offset计算得出用户空间地址）
		trd->data.ptr.buffer = (uintptr_t)t->buffer->user_data;
		//偏移数组紧挨着数据区，所以它的首地址就为数据区地址加上数据大小
		trd->data.ptr.offsets = trd->data.ptr.buffer +
					ALIGN(t->buffer->data_size,
					    sizeof(void *));

		tr.secctx = t->security_ctx;
		if (t->security_ctx) {
			cmd = BR_TRANSACTION_SEC_CTX;
			trsize = sizeof(tr);
		}
		//回复给用户进程对应的响应码
		if (put_user(cmd, (uint32_t __user *)ptr)) {
			if (t_from)
				binder_thread_dec_tmpref(t_from);

			binder_cleanup_transaction(t, "put_user failed",
						   BR_FAILED_REPLY);

			return -EFAULT;
		}
		ptr += sizeof(uint32_t);
		//将binder_transaction_data拷贝至用户空间
		if (copy_to_user(ptr, &tr, trsize)) {
			if (t_from)
				binder_thread_dec_tmpref(t_from);

			binder_cleanup_transaction(t, "copy_to_user failed",
						   BR_FAILED_REPLY);

			return -EFAULT;
		}
		ptr += trsize;

		trace_binder_transaction_received(t);
		//更新数据统计
		binder_stat_br(proc, thread, cmd);
		...
        //临时引用计数减1
		if (t_from)
			binder_thread_dec_tmpref(t_from);
		t->buffer->allow_user_free = 1;//允许释放这个buffer
		if (cmd != BR_REPLY && !(t->flags & TF_ONE_WAY)) {
		   //非异步处理
			binder_inner_proc_lock(thread->proc);
			//将这个事务插入到事务栈中
			t->to_parent = thread->transaction_stack;
			t->to_thread = thread;//设置目标处理线程
			thread->transaction_stack = t;
			binder_inner_proc_unlock(thread->proc);
		} else {
			binder_free_transaction(t);
		}
		break;
	}

done:
    //更新已读数据大小
	*consumed = ptr - buffer;
	binder_inner_proc_lock(proc);
	/请求线程数为0且没有等待线程，已启动线程数小于最大线程数
    //且这个binder线程既不是主线程，也没有被注册成binder子线程
	if (proc->requested_threads == 0 &&
	    list_empty(&thread->proc->waiting_threads) &&
	    proc->requested_threads_started < proc->max_threads &&
	    (thread->looper & (BINDER_LOOPER_STATE_REGISTERED |
	     BINDER_LOOPER_STATE_ENTERED)) /* the user-space code fails to */
	     /*spawn a new thread if we leave this out */) {
	   //向用户空间发送BR_SPAWN_LOOPER响应码，创建新binder线程
		proc->requested_threads++;
		binder_inner_proc_unlock(proc);
		...
		if (put_user(BR_SPAWN_LOOPER, (uint32_t __user *)buffer))
			return -EFAULT;
		//更新统计信息	
		binder_stat_br(proc, thread, BR_SPAWN_LOOPER);
	} else
		binder_inner_proc_unlock(proc);
	return 0;
}
```
这一部分主要做的工作是，将处理事务所需要的信息（命令码、PID等）和数据（数据区首地址和偏移数组地址）准备好，拷贝到用户空间，
交给用户空间处理这个事务