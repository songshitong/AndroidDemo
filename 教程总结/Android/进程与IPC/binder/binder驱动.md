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
		in_reply_to = thread->transaction_stack;
		if (in_reply_to == NULL) {
			...error
		}
		if (in_reply_to->to_thread != thread) {
			...error
		}
		thread->transaction_stack = in_reply_to->to_parent;
		binder_inner_proc_unlock(proc);
		target_thread = binder_get_txn_from_and_acq_inner(in_reply_to);
		if (target_thread == NULL) {
			...error
		}
		if (target_thread->transaction_stack != in_reply_to) {
			...//error
		}
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
			ref = binder_get_ref_olocked(proc, tr->target.handle,
						     true);
			if (ref) {
				target_node = binder_get_node_refs_for_txn(
						ref->node, &target_proc,
						&return_error);
			} else {
				...error
			}
			binder_proc_unlock(proc);
		} else {
			mutex_lock(&context->context_mgr_node_lock);
			target_node = context->binder_context_mgr_node;
			if (target_node)
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

		if (!(tr->flags & TF_ONE_WAY) && thread->transaction_stack) {
			struct binder_transaction *tmp;

			tmp = thread->transaction_stack;
			if (tmp->to_thread != thread) {
				...error
			}
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
	if (target_thread)
		e->to_thread = target_thread->pid;
	e->to_proc = target_proc->pid;

	/* TODO: reuse incoming transaction for reply */
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

	if (!reply && !(tr->flags & TF_ONE_WAY))
		t->from = thread;
	else
		t->from = NULL;
	t->from_pid = proc->pid;
	t->from_tid = thread->pid;
	t->sender_euid = task_euid(proc->tsk);
	t->to_proc = target_proc;
	t->to_thread = target_thread;
	t->code = tr->code;
	t->flags = tr->flags;
	t->is_nested = is_nested;
	if (!(t->flags & TF_ONE_WAY) &&
	    binder_supported_policy(current->policy)) {
		/* Inherit supported policies for synchronous transactions */
		t->priority.sched_policy = current->policy;
		t->priority.prio = current->normal_prio;
	} else {
		/* Otherwise, fall back to the default priority */
		t->priority = target_proc->default_priority;
	}

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

	t->buffer = binder_alloc_new_buf(&target_proc->alloc, tr->data_size,
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

	if (binder_alloc_copy_user_to_buffer(
				&target_proc->alloc,
				t->buffer,
				ALIGN(tr->data_size, sizeof(void *)),
				(const void __user *)
					(uintptr_t)tr->data.ptr.offsets,
				tr->offsets_size)) {
		...error
	}
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
		switch (hdr->type) {
		case BINDER_TYPE_BINDER:
		case BINDER_TYPE_WEAK_BINDER: {
			struct flat_binder_object *fp;

			fp = to_flat_binder_object(hdr);
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
			struct binder_fd_object *fp = to_binder_fd_object(hdr);
			binder_size_t fd_offset = object_offset +
				(uintptr_t)&fp->fd - (uintptr_t)fp;
			int ret = binder_translate_fd(fp->fd, fd_offset, t,
						      thread, in_reply_to);

			fp->pad_binder = 0;
			if (ret < 0 ||
			    binder_alloc_copy_to_buffer(&target_proc->alloc,
							t->buffer,
							object_offset,
							fp, sizeof(*fp))) {
				...error
			}
		} break;
		case BINDER_TYPE_FDA: {
			struct binder_object ptr_object;
			binder_size_t parent_offset;
			struct binder_object user_object;
			size_t user_parent_size;
			struct binder_fd_array_object *fda =
				to_binder_fd_array_object(hdr);
			size_t num_valid = (buffer_offset - off_start_offset) /
						sizeof(binder_size_t);
			struct binder_buffer_object *parent =
				binder_validate_ptr(target_proc, t->buffer,
						    &ptr_object, fda->parent,
						    off_start_offset,
						    &parent_offset,
						    num_valid);
			if (!parent) {
				...error
			}
			if (!binder_validate_fixup(target_proc, t->buffer,
						   off_start_offset,
						   parent_offset,
						   fda->parent_offset,
						   last_fixup_obj_off,
						   last_fixup_min_off)) {
				...error
			}
			/*
			 * We need to read the user version of the parent
			 * object to get the original user offset
			 */
			user_parent_size =
				binder_get_object(proc, user_buffer, t->buffer,
						  parent_offset, &user_object);
			if (user_parent_size != sizeof(user_object.bbo)) {
				...error
			}
			ret = binder_translate_fd_array(&pf_head, fda,
							user_buffer, parent,
							&user_object.bbo, t,
							thread, in_reply_to);
			if (!ret)
				ret = binder_alloc_copy_to_buffer(&target_proc->alloc,
								  t->buffer,
								  object_offset,
								  fda, sizeof(*fda));
			if (ret) {
				...error
			}
			last_fixup_obj_off = parent_offset;
			last_fixup_min_off =
				fda->parent_offset + sizeof(u32) * fda->num_fds;
		} break;
		case BINDER_TYPE_PTR: {
			struct binder_buffer_object *bp =
				to_binder_buffer_object(hdr);
			size_t buf_left = sg_buf_end_offset - sg_buf_offset;
			size_t num_valid;

			if (bp->length > buf_left) {
				..error
			}
			ret = binder_defer_copy(&sgc_head, sg_buf_offset,
				(const void __user *)(uintptr_t)bp->buffer,
				bp->length);
			if (ret) {
				...error
			}
			/* Fixup buffer pointer to target proc address space */
			bp->buffer = (uintptr_t)
				t->buffer->user_data + sg_buf_offset;
			sg_buf_offset += ALIGN(bp->length, sizeof(u64));

			num_valid = (buffer_offset - off_start_offset) /
					sizeof(binder_size_t);
			ret = binder_fixup_parent(&pf_head, t,
						  thread, bp,
						  off_start_offset,
						  num_valid,
						  last_fixup_obj_off,
						  last_fixup_min_off);
			if (ret < 0 ||
			    binder_alloc_copy_to_buffer(&target_proc->alloc,
							t->buffer,
							object_offset,
							bp, sizeof(*bp))) {
				...error
			}
			last_fixup_obj_off = object_offset;
			last_fixup_min_off = 0;
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
	if (t->buffer->oneway_spam_suspect)
		tcomplete->type = BINDER_WORK_TRANSACTION_ONEWAY_SPAM_SUSPECT;
	else
		tcomplete->type = BINDER_WORK_TRANSACTION_COMPLETE;
	t->work.type = BINDER_WORK_TRANSACTION;

	if (reply) {
		binder_enqueue_thread_work(thread, tcomplete);
		binder_inner_proc_lock(target_proc);
		if (target_thread->is_dead) {
			return_error = BR_DEAD_REPLY;
			binder_inner_proc_unlock(target_proc);
			goto err_dead_proc_or_thread;
		}
		BUG_ON(t->buffer->async_transaction != 0);
		binder_pop_transaction_ilocked(target_thread, in_reply_to);
		binder_enqueue_thread_work_ilocked(target_thread, &t->work);
		target_proc->outstanding_txns++;
		binder_inner_proc_unlock(target_proc);
		if (in_reply_to->is_nested) {
			spin_lock(&thread->prio_lock);
			thread->prio_state = BINDER_PRIO_PENDING;
			thread->prio_next = in_reply_to->saved_priority;
			spin_unlock(&thread->prio_lock);
		}
		wake_up_interruptible_sync(&target_thread->wait);
		binder_restore_priority(thread, &in_reply_to->saved_priority);
		binder_free_transaction(in_reply_to);
	} else if (!(t->flags & TF_ONE_WAY)) {
		BUG_ON(t->buffer->async_transaction != 0);
		binder_inner_proc_lock(proc);
		/*
		 * Defer the TRANSACTION_COMPLETE, so we don't return to
		 * userspace immediately; this allows the target process to
		 * immediately start processing this transaction, reducing
		 * latency. We will then return the TRANSACTION_COMPLETE when
		 * the target replies (or there is an error).
		 */
		binder_enqueue_deferred_thread_work_ilocked(thread, tcomplete);
		t->need_reply = 1;
		t->from_parent = thread->transaction_stack;
		thread->transaction_stack = t;
		binder_inner_proc_unlock(proc);
		return_error = binder_proc_transaction(t,
				target_proc, target_thread);
		if (return_error) {
			binder_inner_proc_lock(proc);
			binder_pop_transaction_ilocked(thread, t);
			binder_inner_proc_unlock(proc);
			goto err_dead_proc_or_thread;
		}
	} else {
		BUG_ON(target_node == NULL);
		BUG_ON(t->buffer->async_transaction != 1);
		return_error = binder_proc_transaction(t, target_proc, NULL);
		/*
		 * Let the caller know when async transaction reaches a frozen
		 * process and is put in a pending queue, waiting for the target
		 * process to be unfrozen.
		 */
		if (return_error == BR_TRANSACTION_PENDING_FROZEN)
			tcomplete->type = BINDER_WORK_TRANSACTION_PENDING;
		binder_enqueue_thread_work(thread, tcomplete);
		if (return_error &&
		    return_error != BR_TRANSACTION_PENDING_FROZEN)
			goto err_dead_proc_or_thread;
	}
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

err_dead_proc_or_thread:
	binder_txn_error("%d:%d dead process or thread\n",
		thread->pid, proc->pid);
	return_error_line = __LINE__;
	binder_dequeue_work(proc, tcomplete);
err_translate_failed:
err_bad_object_type:
err_bad_offset:
err_bad_parent:
err_copy_data_failed:
	binder_cleanup_deferred_txn_lists(&sgc_head, &pf_head);
	binder_free_txn_fixups(t);
	trace_binder_transaction_failed_buffer_release(t->buffer);
	binder_transaction_buffer_release(target_proc, NULL, t->buffer,
					  buffer_offset, true);
	if (target_node)
		binder_dec_node_tmpref(target_node);
	target_node = NULL;
	t->buffer->transaction = NULL;
	binder_alloc_free_buf(&target_proc->alloc, t->buffer);
err_binder_alloc_buf_failed:
err_bad_extra_size:
	if (secctx)
		security_release_secctx(secctx, secctx_sz);
err_get_secctx_failed:
	kfree(tcomplete);
	binder_stats_deleted(BINDER_STAT_TRANSACTION_COMPLETE);
err_alloc_tcomplete_failed:
	if (trace_binder_txn_latency_free_enabled())
		binder_txn_latency_free(t);
	kfree(t);
	binder_stats_deleted(BINDER_STAT_TRANSACTION);
err_alloc_t_failed:
err_bad_todo_list:
err_bad_call_stack:
err_empty_call_stack:
err_dead_binder:
err_invalid_target_handle:
	if (target_node) {
		binder_dec_node(target_node, 1, 0);
		binder_dec_node_tmpref(target_node);
	}

	...

	if (target_thread)
		binder_thread_dec_tmpref(target_thread);
	if (target_proc)
		binder_proc_dec_tmpref(target_proc);

	{
		struct binder_transaction_log_entry *fe;

		e->return_error = return_error;
		e->return_error_param = return_error_param;
		e->return_error_line = return_error_line;
		fe = binder_transaction_log_add(&binder_transaction_log_failed);
		*fe = *e;
		/*
		 * write barrier to synchronize with initialization
		 * of log entry
		 */
		smp_wmb();
		WRITE_ONCE(e->debug_id_done, t_debug_id);
		WRITE_ONCE(fe->debug_id_done, t_debug_id);
	}

	BUG_ON(thread->return_error.cmd != BR_OK);
	if (in_reply_to) {
		binder_restore_priority(thread, &in_reply_to->saved_priority);
		binder_set_txn_from_error(in_reply_to, t_debug_id,
				return_error, return_error_param);
		thread->return_error.cmd = BR_TRANSACTION_COMPLETE;
		binder_enqueue_thread_work(thread, &thread->return_error.work);
		binder_send_failed_reply(in_reply_to, return_error);
	} else {
		binder_inner_proc_lock(proc);
		binder_set_extended_error(&thread->ee, t_debug_id,
				return_error, return_error_param);
		binder_inner_proc_unlock(proc);
		thread->return_error.cmd = return_error;
		binder_enqueue_thread_work(thread, &thread->return_error.work);
	}
}

```