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



common/drivers/android/binder.c
binder_init   //todo 哪里调用Init方法了 https://blog.csdn.net/u014535072/article/details/111405069
```
static int __init binder_init(void)
{
    ...
    在/sys/kernel/debug目录创建binder目录
	binder_debugfs_dir_entry_root = debugfs_create_dir("binder", NULL);

	binder_for_each_debugfs_entry(db_entry)
		debugfs_create_file(db_entry->name,
					db_entry->mode,
					binder_debugfs_dir_entry_root,
					db_entry->data,
					db_entry->fops);

	binder_debugfs_dir_entry_proc = debugfs_create_dir("proc",
						binder_debugfs_dir_entry_root);
    ...

	ret = init_binderfs();
	if (ret)
		goto err_init_binder_device_failed;

	return ret;
   ...
}

//文件 type
static struct file_system_type binder_fs_type = {
	.name			= "binder",
	.init_fs_context	= binderfs_init_fs_context,
	.parameters		= binderfs_fs_parameters,
	.kill_sb		= binderfs_kill_super,
	.fs_flags		= FS_USERNS_MOUNT,
};

#define CONFIG_ANDROID_BINDER_DEVICES "binder,hwbinder,vndbinder" //out/android-mainline/common/include/generated/autoconf.h (3 results)
char *binder_devices_param = CONFIG_ANDROID_BINDER_DEVICES;
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
				  "binder");
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
```



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

	binder_debug(BINDER_DEBUG_OPEN_CLOSE, "%s: %d:%d\n", __func__,
		     current->group_leader->pid, current->pid);
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
		binder_inner_proc_lock(proc);
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
	//将内核空间修改后的bwr拷贝至用户空间ubuf
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
```