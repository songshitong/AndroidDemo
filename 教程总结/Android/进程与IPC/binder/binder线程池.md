
线程池相关
http://gityuan.com/2016/10/29/binder-thread-pool/




android-9.0.0_r60
Binder线程创建
其实无论是system_server进程，还是app进程，都是在进程fork完成后，便会在新进程中执行onZygoteInit()的过程中，启动binder线程池
Java层进程的创建都是通过Process.start()方法，向Zygote进程发出创建进程的socket消息，Zygote收到消息后会调用Zygote.forkAndSpecialize()来fork出新进程，
在新进程中会调用到RuntimeInit.nativeZygoteInit方法，该方法经过jni映射，最终会调用到app_main.cpp中的onZygoteInit

frameworks/base/cmds/app_process/app_main.cpp
```
 virtual void onZygoteInit() 
    {
        sp<ProcessState> proc = ProcessState::self();
        ALOGV("App process: starting thread pool.\n");
        proc->startThreadPool();
    }
```
ProcessState::self()是单例模式，主要工作是调用open()打开/dev/binder驱动设备，再利用mmap()映射内核的地址空间，
将Binder驱动的fd赋值ProcessState对象中的变量mDriverFD，用于交互操作。startThreadPool()是创建一个新的binder线程，不断进行talkWithDriver()

PS.startThreadPool  创建binder主线程
frameworks/native/libs/binder/ProcessState.cpp
```
void ProcessState::startThreadPool()
{
    AutoMutex _l(mLock);
    if (!mThreadPoolStarted) {
        mThreadPoolStarted = true;
        spawnPooledThread(true);
    }
}
```
通过变量mThreadPoolStarted来保证每个应用进程只允许启动一个binder线程池, 且本次创建的是binder主线程(isMain=true). 
其余binder线程池中的线程都是由Binder驱动来控制创建的。
```
void ProcessState::spawnPooledThread(bool isMain)
{
    if (mThreadPoolStarted) {
        String8 name = makeBinderThreadName();
        ALOGV("Spawning new pooled thread, name=%s\n", name.string());
        sp<Thread> t = new PoolThread(isMain);
        t->run(name.string());
    }
}

String8 ProcessState::makeBinderThreadName() {
    int32_t s = android_atomic_add(1, &mThreadPoolSeq);
    pid_t pid = getpid();
    String8 name;
    name.appendFormat("Binder:%d_%X", pid, s);
    return name;
}
```
只有通过spawnPooledThread方法来创建的线程才符合这个格式，对于直接将当前线程通过joinThreadPool加入线程池的线程名则不符合这个命名规则。 
Android N中Binder命令已改为Binder:<pid>_x格式, 则对于分析问题很有帮忙,通过binder名称的pid字段可以快速定位该binder线程所属的进程p.
低版本binder线程名称Binder_x

PoolThread.run
```
class PoolThread : public Thread
{
public:
    explicit PoolThread(bool isMain)
        : mIsMain(isMain)
    {
    }
    
protected:
    virtual bool threadLoop()
    {
        IPCThreadState::self()->joinThreadPool(mIsMain);
        return false;
    }
    
    const bool mIsMain;
};
```
从函数名看起来是创建线程池，其实就只是创建一个线程，该PoolThread继承Thread类。t->run()方法最终调用 PoolThread的threadLoop()方法


frameworks/native/libs/binder/IPCThreadState.cpp
```
void IPCThreadState::joinThreadPool(bool isMain)
{
   ...
   //创建Binder线程  写入Parcel数据mOut，用于与binder驱动交互
    mOut.writeInt32(isMain ? BC_ENTER_LOOPER : BC_REGISTER_LOOPER);

    status_t result;
    do {
        processPendingDerefs();//清除队列的引用
        // now get the next command to be processed, waiting if necessary
        result = getAndExecuteCommand();//处理下一条指令

        if (result < NO_ERROR && result != TIMED_OUT && result != -ECONNREFUSED && result != -EBADF) {
            ....
            abort();
        }
        // Let this thread exit the thread pool if it is no longer
        // needed and it is not the main process thread.
        if(result == TIMED_OUT && !isMain) {
            break;//非主线程出现timeout则线程退出
        }
    } while (result != -ECONNREFUSED && result != -EBADF);

    ....
    mOut.writeInt32(BC_EXIT_LOOPER);// 线程退出循环
    talkWithDriver(false);//false代表bwr数据的read_buffer为空
}
```
对于isMain=true的情况下， command为BC_ENTER_LOOPER，代表的是Binder主线程，不会退出的线程；
对于isMain=false的情况下，command为BC_REGISTER_LOOPER，表示是由binder驱动创建的线程。

processPendingDerefs
```
// When we've cleared the incoming command queue, process any pending derefs
void IPCThreadState::processPendingDerefs()
{
    if (mIn.dataPosition() >= mIn.dataSize()) {
        /*
         * The decWeak()/decStrong() calls may cause a destructor to run,
         * which in turn could have initiated an outgoing transaction,
         * which in turn could cause us to add to the pending refs
         * vectors; so instead of simply iterating, loop until they're empty.
         *
         * We do this in an outer loop, because calling decStrong()
         * may result in something being added to mPendingWeakDerefs,
         * which could be delayed until the next incoming command
         * from the driver if we don't process it now.
         */
        while (mPendingWeakDerefs.size() > 0 || mPendingStrongDerefs.size() > 0) {
            while (mPendingWeakDerefs.size() > 0) {
                RefBase::weakref_type* refs = mPendingWeakDerefs[0];
                mPendingWeakDerefs.removeAt(0);
                refs->decWeak(mProcess.get());//弱引用减一
            }

            if (mPendingStrongDerefs.size() > 0) {
                // We don't use while() here because we don't want to re-order
                // strong and weak decs at all; if this decStrong() causes both a
                // decWeak() and a decStrong() to be queued, we want to process
                // the decWeak() first.
                BBinder* obj = mPendingStrongDerefs[0];
                mPendingStrongDerefs.removeAt(0);
                obj->decStrong(mProcess.get()); //强引用减一
            }
        }
    }
}
```

getAndExecuteCommand
```
status_t IPCThreadState::getAndExecuteCommand()
{
    status_t result;
    int32_t cmd;

    result = talkWithDriver();//与binder进行交互
    if (result >= NO_ERROR) {
        size_t IN = mIn.dataAvail();
        if (IN < sizeof(int32_t)) return result;
        cmd = mIn.readInt32();
        ...

        pthread_mutex_lock(&mProcess->mThreadCountLock);
        mProcess->mExecutingThreadsCount++;
        if (mProcess->mExecutingThreadsCount >= mProcess->mMaxThreads &&
                mProcess->mStarvationStartTimeMs == 0) {
            mProcess->mStarvationStartTimeMs = uptimeMillis();
        }
        pthread_mutex_unlock(&mProcess->mThreadCountLock);

        result = executeCommand(cmd); //执行Binder响应码

        pthread_mutex_lock(&mProcess->mThreadCountLock);
        mProcess->mExecutingThreadsCount--;
        if (mProcess->mExecutingThreadsCount < mProcess->mMaxThreads &&
                mProcess->mStarvationStartTimeMs != 0) {
            int64_t starvationTimeMs = uptimeMillis() - mProcess->mStarvationStartTimeMs;
            if (starvationTimeMs > 100) {
                ...
            }
            mProcess->mStarvationStartTimeMs = 0;
        }
        pthread_cond_broadcast(&mProcess->mThreadCountDecrement);
        pthread_mutex_unlock(&mProcess->mThreadCountLock);
    }

    return result;
}
```
talkWithDriver
```
Parcel    mIn;
Parcel     mOut;

//此时线程刚创建mOut有数据，mIn还没有数据。doReceive默认值为false
status_t IPCThreadState::talkWithDriver(bool doReceive)
{
    if (mProcess->mDriverFD <= 0) {
        return -EBADF;
    }

    binder_write_read bwr;  //创建binder读写数据 写入read write  
    

    // Is the read buffer empty?
    const bool needRead = mIn.dataPosition() >= mIn.dataSize();

    // We don't want to write anything if we are still reading
    // from data left in the input buffer and the caller
    // has requested to read the next data.
    const size_t outAvail = (!doReceive || needRead) ? mOut.dataSize() : 0;

    bwr.write_size = outAvail;
    bwr.write_buffer = (uintptr_t)mOut.data();

    // This is what we'll read.
    if (doReceive && needRead) {
        bwr.read_size = mIn.dataCapacity();
        bwr.read_buffer = (uintptr_t)mIn.data();
    } else {
        bwr.read_size = 0;
        bwr.read_buffer = 0;
    }

    ....

    // Return immediately if there is nothing to do.  // 当同时没有输入和输出数据则直接返回
    if ((bwr.write_size == 0) && (bwr.read_size == 0)) return NO_ERROR;

    bwr.write_consumed = 0;
    bwr.read_consumed = 0;
    status_t err;
    do {
       ... //ioctl执行binder读写操作，经过syscall，进入Binder驱动。调用Binder_ioctl
        if (ioctl(mProcess->mDriverFD, BINDER_WRITE_READ, &bwr) >= 0)
            err = NO_ERROR;
     ...
    } while (err == -EINTR);

   ....

    if (err >= NO_ERROR) {
        if (bwr.write_consumed > 0) {
            if (bwr.write_consumed < mOut.dataSize())
                mOut.remove(0, bwr.write_consumed);
            else {
                mOut.setDataSize(0);
                processPostWriteDerefs();
            }
        }
        if (bwr.read_consumed > 0) {
            mIn.setDataSize(bwr.read_consumed);
            mIn.setDataPosition(0);
        }
        ...
        return NO_ERROR;
    }

    return err;
}
```
在这里调用的isMain=true，也就是向mOut例如写入的便是BC_ENTER_LOOPER. 经过talkWithDriver()与binder驱动交互

binder_thread_write()处理BC_ENTER_LOOPER的过程
common/drivers/android/binder.c    android-common-line
```
static int binder_thread_write(struct binder_proc *proc,
			struct binder_thread *thread,
			binder_uintptr_t binder_buffer, size_t size,
			binder_size_t *consumed)
{
   ...
   case BC_REGISTER_LOOPER:
			....
        binder_inner_proc_lock(proc);
        if (thread->looper & BINDER_LOOPER_STATE_ENTERED) {
           //出错原因：线程调用完BC_ENTER_LOOPER，不能执行该分支
            thread->looper |= BINDER_LOOPER_STATE_INVALID;
            binder_user_error("%d:%d ERROR: BC_REGISTER_LOOPER called after BC_ENTER_LOOPER\n",
                proc->pid, thread->pid);
        } else if (proc->requested_threads == 0) {
             //出错原因：没有请求就创建线程
            thread->looper |= BINDER_LOOPER_STATE_INVALID;
            binder_user_error("%d:%d ERROR: BC_REGISTER_LOOPER called without request\n",
                proc->pid, thread->pid);
        } else {
            proc->requested_threads--;
            proc->requested_threads_started++;
        }
        thread->looper |= BINDER_LOOPER_STATE_REGISTERED;
        binder_inner_proc_unlock(proc);
        break;
    case BC_ENTER_LOOPER:
    ....
    if (thread->looper & BINDER_LOOPER_STATE_REGISTERED) {
        //出错原因：线程调用完BC_REGISTER_LOOPER，不能立刻执行该分支
        thread->looper |= BINDER_LOOPER_STATE_INVALID;
        binder_user_error("%d:%d ERROR: BC_ENTER_LOOPER called after BC_REGISTER_LOOPER\n",
            proc->pid, thread->pid);
    }
    thread->looper |= BINDER_LOOPER_STATE_ENTERED; //创建Binder主线程
    break;
   ...
}
```
处理完BC_ENTER_LOOPER命令后，一般情况下成功设置thread->looper |= BINDER_LOOPER_STATE_ENTERED。那么binder线程的创建是在什么时候呢？ 
那就当该线程有事务需要处理的时候，进入binder_thread_read()过程
```
static int binder_thread_read(struct binder_proc *proc,
			      struct binder_thread *thread,
			      binder_uintptr_t binder_buffer, size_t size,
			      binder_size_t *consumed, int non_block)
{

  ...
  case BINDER_WORK_CLEAR_DEATH_NOTIFICATION: {
		  ...
			if (cmd == BR_DEAD_BINDER)
				goto done; /* DEAD_BINDER notifications can cause transactions */
				//Binder驱动向client端发送死亡通知，则进入done
		} break;
	...	

done:
	*consumed = ptr - buffer;
	binder_inner_proc_lock(proc);
	//创建线程的条件
	if (proc->requested_threads == 0 &&
	    list_empty(&thread->proc->waiting_threads) &&
	    proc->requested_threads_started < proc->max_threads &&
	    (thread->looper & (BINDER_LOOPER_STATE_REGISTERED |
	     BINDER_LOOPER_STATE_ENTERED)) /* the user-space code fails to */
	     /*spawn a new thread if we leave this out */) {
		proc->requested_threads++;
		binder_inner_proc_unlock(proc);
		...
		// 生成BR_SPAWN_LOOPER命令，用于创建新的线程	     
		if (put_user(BR_SPAWN_LOOPER, (uint32_t __user *)buffer))
			return -EFAULT;
		binder_stat_br(proc, thread, BR_SPAWN_LOOPER);
	} else
		binder_inner_proc_unlock(proc);
	return 0;	
}
```
进入done的条件
1 当Binder驱动向client端发送死亡通知的情况
2 当类型为BINDER_WORK_TRANSACTION(即收到命令是BC_TRANSACTION或BC_REPLY)的情况；
进入done后，任何一个Binder线程当同时满足以下条件，则会生成用于创建新线程的BR_SPAWN_LOOPER命令：
1 当前进程中没有请求创建binder线程，即requested_threads = 0；
2 当前进程没有空闲可用的binder线程，即waiting_threads = 0；（线程进入休眠状态的个数就是空闲线程数）
3 当前进程已启动线程个数小于最大上限(默认15)；
4 当前线程已接收到BC_ENTER_LOOPER或者BC_REGISTER_LOOPER命令，即当前处于BINDER_LOOPER_STATE_REGISTERED或者BINDER_LOOPER_STATE_ENTERED状态。
  上面已设置状态为BINDER_LOOPER_STATE_ENTERED，显然这条件是满足的。

talkWithDriver收到事务之后, 便进入IPC.executeCommand()
frameworks/native/libs/binder/IPCThreadState.cpp
```
status_t IPCThreadState::executeCommand(int32_t cmd)
{
    BBinder* obj;
    RefBase::weakref_type* refs;
    status_t result = NO_ERROR;

    switch ((uint32_t)cmd) {
     ...
      case BR_SPAWN_LOOPER:
        /创建新的binder线程 
        mProcess->spawnPooledThread(false);
        break;
     ...   
    }
   ... 
}
```
Binder主线程的创建是在其所在进程创建的过程一起创建的，后面再创建的普通binder线程是由spawnPooledThread(false)方法所创建的


默认地，每个进程的binder线程池的线程个数上限为15，该上限不统计通过BC_ENTER_LOOPER命令创建的binder主线程,只计算BC_REGISTER_LOOPER命令创建的线程。
某个进程的主线程执行如下方法，那么该进程可创建的binder线程个数上限是多少呢？
```
ProcessState::self()->setThreadPoolMaxThreadCount(6);  // 6个线程
ProcessState::self()->startThreadPool();   // 1个线程
IPCThread::self()->joinThreadPool();   // 1个线程
```
首先线程池的binder线程个数上限为6个，通过startThreadPool()创建的主线程不算在最大线程上限，最后一句是将当前线程成为binder线程，
  所以说可创建的binder线程个数上限为8 

总结：
Binder设计架构中，只有第一个Binder主线程(也就是Binder_1线程)是由应用程序主动创建，Binder线程池的普通线程都是由Binder驱动根据IPC通信需求创建

Binder的transaction有3种类型：
1 call: 发起进程的线程不一定是在Binder线程， 大多數情況下，接收者只指向进程，并不确定会有哪个线程来处理，所以不指定线程；
2 reply: 发起者一定是binder线程，并且接收者线程便是上次call时的发起线程(该线程不一定是binder线程，可以是任意线程)。
3 async: 与call类型差不多，唯一不同的是async是oneway方式不需要回复，发起进程的线程不一定是在Binder线程， 接收者只指向进程，
 并不确定会有哪个线程来处理，所以不指定线程。

Binder系统中可分为3类binder线程：
1 Binder主线程：进程创建过程会调用startThreadPool()过程中再进入spawnPooledThread(true)，来创建Binder主线程。
  编号从1开始，也就是意味着binder主线程名为Binder:<pid>_1，并且主线程是不会退出的。
2 Binder普通线程：是由Binder Driver来根据是否有空闲的binder线程来决定是否创建binder线程，回调spawnPooledThread(false) ，
  isMain=false，该线程名格式为Binder:<pid>_x。
3 Binder其他线程：其他线程是指并没有调用spawnPooledThread方法，而是直接调用IPC.joinThreadPool()，将当前线程直接加入binder线程队列。
 例如： mediaserver和servicemanager的主线程都是binder线程，但system_server的主线程并非binder线程。