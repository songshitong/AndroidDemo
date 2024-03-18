

概述
死亡通知是为了让Bp端(客户端进程)进能知晓Bn端(服务端进程)的生死情况，当Bn端进程死亡后能通知到Bp端。
定义：AppDeathRecipient是继承IBinder::DeathRecipient类，主要需要实现其binderDied()来进行死亡通告。
注册：binder->linkToDeath(AppDeathRecipient)是为了将AppDeathRecipient死亡通知注册到Binder上。
Bp端只需要覆写binderDied()方法，实现一些后尾清除类的工作，则在Bn端死掉后，会回调binderDied()进行相应处理。

```
frameworks/base/core/java/android/os/Binder.java
public class Binder implements IBinder {
  public void linkToDeath(@NonNull DeathRecipient recipient, int flags) {
    }
    
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return true;
    }
}

frameworks/base/core/java/android/os/BinderProxy.java
public final class BinderProxy implements IBinder {
    public native void linkToDeath(DeathRecipient recipient, int flags)
            throws RemoteException;

    public native boolean unlinkToDeath(DeathRecipient recipient, int flags);
}
```


https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```
 private boolean attachApplicationLocked(@NonNull IApplicationThread thread,
            int pid, int callingUid, long startSeq) {
    ...
    final String processName = app.processName;
    AppDeathRecipient adr = new AppDeathRecipient(
            app, pid, thread);
    thread.asBinder().linkToDeath(adr, 0);
    app.setDeathRecipient(adr);
   ....        
 }
 
 private final class AppDeathRecipient implements IBinder.DeathRecipient {
        ...
        @Override
        public void binderDied() {          
            synchronized(ActivityManagerService.this) {
                appDiedLocked(mApp, mPid, mAppThread, true, null);
            }
        }
    }
```


BinderProxy调用linkToDeath()方法是一个native方法
frameworks/base/core/jni/android_util_Binder.cpp
```
static const JNINativeMethod gBinderProxyMethods[] = {
   ...
    {"linkToDeath",         "(Landroid/os/IBinder$DeathRecipient;I)V", (void*)android_os_BinderProxy_linkToDeath},
    {"unlinkToDeath",       "(Landroid/os/IBinder$DeathRecipient;I)Z", (void*)android_os_BinderProxy_unlinkToDeath},
    ....
};

static void android_os_BinderProxy_linkToDeath(JNIEnv* env, jobject obj,
        jobject recipient, jint flags) // throws RemoteException
{
    ...
    BinderProxyNativeData *nd = getBPNativeData(env, obj);
    //获取BinderProxy.mObject成员变量值, 即BpBinder对象  todo怎么得来的
    IBinder* target = nd->mObject.get();
    ....

    if (!target->localBinder()) {  //只有Binder代理对象才会进入该分支
        //获取DeathRecipientList: 其成员变量mList记录该BinderProxy的JavaDeathRecipient列表信息
        DeathRecipientList* list = nd->mOrgue.get();
        
        //创建JavaDeathRecipient对象
        sp<JavaDeathRecipient> jdr = new JavaDeathRecipient(env, recipient, list);
        //建立死亡通知
        status_t err = target->linkToDeath(jdr, NULL, flags);
        if (err != NO_ERROR) {
            // Failure adding the death recipient, so clear its reference
            // now.
            jdr->clearReference();    //添加死亡通告失败, 则从DeathRecipientList移除引用
            signalExceptionForError(env, obj, err, true /*canThrowRemoteException*/);
        }
    }
}

class JavaDeathRecipient : public IBinder::DeathRecipient
{
public:
    JavaDeathRecipient(JNIEnv* env, jobject object, const sp<DeathRecipientList>& list)
        : mVM(jnienv_to_javavm(env)), mObject(env->NewGlobalRef(object)),
          mObjectWeak(NULL), mList(list)
    {
        //将当前对象sp添加到列表DeathRecipientList
        list->add(this);

        gNumDeathRefsCreated.fetch_add(1, std::memory_order_relaxed); //增加计数
        gcIfManyNewRefs(env);//触发GC
    }
}
```

BpBinder.linkToDeath
frameworks/native/libs/binder/BpBinder.cpp
```
status_t BpBinder::linkToDeath(
    const sp<DeathRecipient>& recipient, void* cookie, uint32_t flags)
{
    if (isRpcBinder()) return UNKNOWN_TRANSACTION;

    Obituary ob;
    ob.recipient = recipient; //JavaDeathRecipient
    ob.cookie = cookie; //null
    ob.flags = flags; //0

    LOG_ALWAYS_FATAL_IF(recipient == nullptr,
                        "linkToDeath(): recipient must be non-NULL");

    {
        AutoMutex _l(mLock);

        if (!mObitsSent) {  //没有执行过sendObituary，则进入该方法
            if (!mObituaries) { //第一次执行该方法
                mObituaries = new Vector<Obituary>;
                if (!mObituaries) {
                    return NO_MEMORY;
                }
                ...
                getWeakRefs()->incWeak(this);
                IPCThreadState* self = IPCThreadState::self();
                self->requestDeathNotification(binderHandle(), this); //写入BC_REQUEST_DEATH_NOTIFICATION命令
                self->flushCommands();
            }
            ssize_t res = mObituaries->add(ob);
            return res >= (ssize_t)NO_ERROR ? (status_t)NO_ERROR : res;
        }
    }
    return DEAD_OBJECT;
}
```
Obituary  讣告；讣闻   生死簿；死亡讣告；讣文

requestDeathNotification
frameworks/native/libs/binder/IPCThreadState.cpp
```
status_t IPCThreadState::requestDeathNotification(int32_t handle, BpBinder* proxy)
{
    mOut.writeInt32(BC_REQUEST_DEATH_NOTIFICATION);
    mOut.writeInt32((int32_t)handle);
    mOut.writePointer((uintptr_t)proxy);
    return NO_ERROR;
}
```
flushCommands就是把命令向驱动发出，此处参数为false，则不会阻塞等待读。 向Kernel层的binder driver发送BC_REQUEST_DEATH_NOTIFICATION命令，
经过ioctl执行到 binder_ioctl_write_read()方法。


```
common/drivers/android/binder_internal.h
struct binder_ref_death {
	struct binder_work work;
	binder_uintptr_t cookie;
};

https://cs.android.com/android/kernel/superproject/+/common-android-mainline:common/drivers/android/binder.c
static int binder_thread_write(struct binder_proc *proc,
			struct binder_thread *thread,
			binder_uintptr_t binder_buffer, size_t size,
			binder_size_t *consumed)
{
  ...
  	case BC_REQUEST_DEATH_NOTIFICATION:  //注册死亡通知
		case BC_CLEAR_DEATH_NOTIFICATION: {
			uint32_t target;
			binder_uintptr_t cookie;
			struct binder_ref *ref;
			struct binder_ref_death *death = NULL;

			if (get_user(target, (uint32_t __user *)ptr)) //获取target
				return -EFAULT;
			ptr += sizeof(uint32_t);
			if (get_user(cookie, (binder_uintptr_t __user *)ptr)) //获取BpBinder
				return -EFAULT;
			ptr += sizeof(binder_uintptr_t);
			if (cmd == BC_REQUEST_DEATH_NOTIFICATION) {
				... //创建death
				death = kzalloc(sizeof(*death), GFP_KERNEL);
				if (death == NULL) {
					...//创建失败处理
					break;
				}
			}
			...
			binder_proc_lock(proc);
			ref = binder_get_ref_olocked(proc, target, false);
			if (ref == NULL) {
				...//error
			}

			...

			binder_node_lock(ref->node);
			if (cmd == BC_REQUEST_DEATH_NOTIFICATION) {
				if (ref->death) {//native Bp可注册多个，但Kernel只允许注册一个死亡通知
					...
					binder_node_unlock(ref->node);
					binder_proc_unlock(proc);
					kfree(death);
					break;
				}
				binder_stats_created(BINDER_STAT_DEATH); //状态
				INIT_LIST_HEAD(&death->work.entry); //初始化list
				death->cookie = cookie; //BpBinder指针
				ref->death = death;
				if (ref->node->proc == NULL) { //当目标binder服务所在进程已死,则直接发送死亡通知。这是非常规情况
					ref->death->work.type = BINDER_WORK_DEAD_BINDER;
					binder_inner_proc_lock(proc);
					//添加到todo队列
					binder_enqueue_work_ilocked(
						&ref->death->work, &proc->todo);
					binder_wakeup_proc_ilocked(proc);
					binder_inner_proc_unlock(proc);
				}
			} else {
				...
			}
			binder_node_unlock(ref->node);   //todo binder驱动处理流程
			binder_proc_unlock(proc);
		} break;	
   ...
}
```


死亡通知的触发
当Binder服务所在进程死亡后，会释放进程相关的资源，Binder也是一种资源。 binder_open打开binder驱动/dev/binder，这是字符设备，
获取文件描述符。在进程结束的时候会有一个关闭文件系统的过程中会调用驱动close方法，该方法相对应的是release()方法。当binder的fd被释放后，
此处调用相应的方法是binder_release().

但并不是每个close系统调用都会触发调用release()方法. 只有真正释放设备数据结构才调用release(),内核维持一个文件结构被使用多少次的计数，
即便是应用程序没有明显地关闭它打开的文件也适用: 内核在进程exit()时会释放所有内存和关闭相应的文件资源, 通过使用close系统调用最终也会release binder.
frameworks/native/libs/binder/ProcessState.cpp
```
ProcessState::~ProcessState()
{
    if (mDriverFD >= 0) {
        if (mVMStart != MAP_FAILED) {
            munmap(mVMStart, BINDER_VM_SIZE);
        }
        close(mDriverFD);
    }
    mDriverFD = -1;
}
```
common/drivers/android/binder.c
```
const struct file_operations binder_fops = {
	.owner = THIS_MODULE,
	.poll = binder_poll,
	.unlocked_ioctl = binder_ioctl,
	.compat_ioctl = compat_ptr_ioctl,
	.mmap = binder_mmap,
	.open = binder_open,
	.flush = binder_flush,
	.release = binder_release,   //fd关闭时触发
};

static int binder_release(struct inode *nodp, struct file *filp)
{
	struct binder_proc *proc = filp->private_data;
	debugfs_remove(proc->debugfs_entry);
	if (proc->binderfs_entry) { //移除debugfs  
		binderfs_remove_file(proc->binderfs_entry);
		proc->binderfs_entry = NULL;
	}
	binder_defer_work(proc, BINDER_DEFERRED_RELEASE);

	return 0;
}

static DECLARE_WORK(binder_deferred_work, binder_deferred_func);

static void
binder_defer_work(struct binder_proc *proc, enum binder_deferred_state defer)
{
	mutex_lock(&binder_deferred_lock);
	proc->deferred_work |= defer;
	if (hlist_unhashed(&proc->deferred_work_node)) {
		hlist_add_head(&proc->deferred_work_node,
				&binder_deferred_list); //保存到binder_deferred_list
		schedule_work(&binder_deferred_work);//向工作队列添加binder_deferred_work
	}
	mutex_unlock(&binder_deferred_lock);
}
```
binder_deferred_func
```
static void binder_deferred_func(struct work_struct *work)
{
	struct binder_proc *proc;
	int defer;
	do {
		mutex_lock(&binder_deferred_lock);
		if (!hlist_empty(&binder_deferred_list)) {
			proc = hlist_entry(binder_deferred_list.first,
					struct binder_proc, deferred_work_node);
			hlist_del_init(&proc->deferred_work_node);
			defer = proc->deferred_work;
			proc->deferred_work = 0;
		} else {
			proc = NULL;
			defer = 0;
		}
		mutex_unlock(&binder_deferred_lock);

		if (defer & BINDER_DEFERRED_FLUSH)
			binder_deferred_flush(proc);  //binder_flush最终调用的是

		if (defer & BINDER_DEFERRED_RELEASE)
			binder_deferred_release(proc); /* frees proc */ //inder_release最终调用的是binder_deferred_release
	} while (proc);
}
```
binder_deferred_release
```
static void binder_deferred_release(struct binder_proc *proc)
{
	struct binder_context *context = proc->context;
	struct rb_node *n;
	int threads, nodes, incoming_refs, outgoing_refs, active_transactions;

	mutex_lock(&binder_procs_lock);
	hlist_del(&proc->proc_node);  //删除proc_node节点
	mutex_unlock(&binder_procs_lock);

	mutex_lock(&context->context_mgr_node_lock);
	if (context->binder_context_mgr_node &&
	    context->binder_context_mgr_node->proc == proc) {
	...
		context->binder_context_mgr_node = NULL;
	}
	mutex_unlock(&context->context_mgr_node_lock);
	binder_inner_proc_lock(proc);
	...
	proc->tmp_ref++;

	proc->is_dead = true;
	proc->is_frozen = false;
	proc->sync_recv = false;
	proc->async_recv = false;
	threads = 0;
	active_transactions = 0;
	//释放binder_thread
	while ((n = rb_first(&proc->threads))) {
		struct binder_thread *thread;

		thread = rb_entry(n, struct binder_thread, rb_node);
		binder_inner_proc_unlock(proc);
		threads++;
		active_transactions += binder_thread_release(proc, thread);
		binder_inner_proc_lock(proc);
	}

	nodes = 0;
	incoming_refs = 0;
	//释放binder_node 
	while ((n = rb_first(&proc->nodes))) {
		struct binder_node *node;

		node = rb_entry(n, struct binder_node, rb_node);
		nodes++;
		...
		binder_inc_node_tmpref_ilocked(node);
		rb_erase(&node->rb_node, &proc->nodes);
		binder_inner_proc_unlock(proc);
		incoming_refs = binder_node_release(node, incoming_refs);
		binder_inner_proc_lock(proc);
	}
	binder_inner_proc_unlock(proc);

	outgoing_refs = 0;
	binder_proc_lock(proc);
	//释放binder_ref 
	while ((n = rb_first(&proc->refs_by_desc))) {
		struct binder_ref *ref;

		ref = rb_entry(n, struct binder_ref, rb_node_desc);
		outgoing_refs++;
		binder_cleanup_ref_olocked(ref);
		binder_proc_unlock(proc);
		binder_free_ref(ref);
		binder_proc_lock(proc);
	}
	binder_proc_unlock(proc);

	binder_release_work(proc, &proc->todo);
	binder_release_work(proc, &proc->delivered_death);
	...
	binder_proc_dec_tmpref(proc);
}
```