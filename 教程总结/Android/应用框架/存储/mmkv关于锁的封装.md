
1
ScopedLock 类似于C++11的标准，构建时加锁，在锁释放时解锁
https://github.com/Tencent/MMKV/blob/272e5aa1fb57b4ee8cc0ff37e70b7885d99832a7/Core/ScopedLock.hpp
```
 explicit ScopedLock(T *oLock) : m_lock(oLock) {
        lock();
    }

    ~ScopedLock() {
        unlock();
        m_lock = nullptr;
    }
```

2
ThreadLock  对于线程锁的封装，类似面向对象，减少操作
https://github.com/Tencent/MMKV/blob/272e5aa1fb57b4ee8cc0ff37e70b7885d99832a7/Core/ThreadLock.cpp
```
ThreadLock::ThreadLock() {
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&m_lock, &attr);
    pthread_mutexattr_destroy(&attr);
}
ThreadLock::~ThreadLock() {
    pthread_mutex_destroy(&m_lock);
}

void ThreadLock::lock() {
    auto ret = pthread_mutex_lock(&m_lock);
    if (ret != 0) {
        MMKVError("fail to lock %p, ret=%d, errno=%s", &m_lock, ret, strerror(errno));
    }
}

void ThreadLock::unlock() {
    auto ret = pthread_mutex_unlock(&m_lock);
    if (ret != 0) {
        MMKVError("fail to unlock %p, ret=%d, errno=%s", &m_lock, ret, strerror(errno));
    }
}

void ThreadLock::ThreadOnce(ThreadOnceToken_t *onceToken, void (*callback)()) {
    pthread_once(onceToken, callback);
}
```

3 文件锁  封装后支持递归锁和锁的升级/降级
https://github.com/Tencent/MMKV/blob/272e5aa1fb57b4ee8cc0ff37e70b7885d99832a7/Core/InterProcessLock.h
https://blog.csdn.net/lin20044140410/article/details/104485708
https://github.com/Tencent/MMKV/wiki/android_ipc
递归锁
意思是如果一个进程/线程已经拥有了锁，那么后续的加锁操作不会导致卡死，并且解锁也不会导致外层的锁被解掉。
对于文件锁来说，前者是满足的，后者则不然。因为文件锁是状态锁，没有计数器，无论加了多少次锁，一个解锁操作就全解掉。只要用到子函数，就非常需要递归锁。

锁升级/降级
锁升级是指将已经持有的共享锁，升级为互斥锁，亦即将读锁升级为写锁；锁降级则是反过来。文件锁支持锁升级，但是容易死锁：假如 A、B 进程都持有了读锁，
现在都想升级到写锁，就会陷入相互等待的困境，发生死锁。另外，由于文件锁不支持递归锁，也导致了锁降级无法进行，一降就降到没有锁。
为了解决这两个难题，需要对文件锁进行封装，增加读锁、写锁计数器。处理逻辑如下表：

读锁计数器	写锁计数器	加读锁	加写锁	解读锁	解写锁
        0     	0	    加读锁	加写锁	-	    -
        0      	1	    +1	    +1	     -	    解写锁
        0	    N	    +1	    +1	    -	    -1
        1	    0	    +1	    解读锁再加写锁	解读锁	-
        1	    1	    +1	    +1	    -1	    加读锁
        1	    N	    +1	    +1	    -1	    -1
        N	    0	    +1	解读锁再加写锁	-1	    -
        N	    1	    +1	    +1	    -1	    加读锁
        N	    N	    +1	    +1	    -1	    -1
需要注意的地方有两点：
加写锁时，如果当前已经持有读锁，那么先尝试加写锁，try_lock 失败说明其他进程持有了读锁，我们需要先将自己的读锁释放掉，再进行加写锁操作，以避免死锁的发生。
A 有读锁 获取写锁    B 有读锁，获取写锁
解写锁时，假如之前曾经持有读锁，那么我们不能直接释放掉写锁，这样会导致读锁也解了。我们应该加一个读锁，将锁降级。
https://github.com/Tencent/MMKV/blob/272e5aa1fb57b4ee8cc0ff37e70b7885d99832a7/Core/InterProcessLock.h
```
class FileLock {
    MMKVFileHandle_t m_fd;
    size_t m_sharedLockCount; //记录锁的次数，用于实现升级和降级
    size_t m_exclusiveLockCount;
    
     bool doLock(LockType lockType, bool wait, bool *tryAgain = nullptr); //第三个有默认值
     bool lock(LockType lockType);
     bool try_lock(LockType lockType, bool *tryAgain);
     bool unlock(LockType lockType);
}

class InterProcessLock {
    FileLock *m_fileLock;
    LockType m_lockType; //锁的类型
    bool m_enable; //控制是否开启

    void lock() { //都调用的是FileLock
        if (m_enable) {
            m_fileLock->lock(m_lockType);
        }
    }

    bool try_lock(bool *tryAgain = nullptr) {
        if (m_enable) {
            return m_fileLock->try_lock(m_lockType, tryAgain);
        }
        return false;
    }

    void unlock() {
        if (m_enable) {
            m_fileLock->unlock(m_lockType);
        }
    }
 }
```
实现文件
https://github.com/Tencent/MMKV/blob/272e5aa1fb57b4ee8cc0ff37e70b7885d99832a7/Core/InterProcessLock.cpp
```
static int32_t LockType2FlockType(LockType lockType) { //锁类型转换
    switch (lockType) {
        case SharedLockType:
            return LOCK_SH; //共享锁
        case ExclusiveLockType:
            return LOCK_EX; //排他锁
    }
    return LOCK_EX;
}

bool FileLock::lock(LockType lockType) {
    return doLock(lockType, true); //不等待
}

bool FileLock::try_lock(LockType lockType, bool *tryAgain) {
    return doLock(lockType, false, tryAgain);
}

bool FileLock::doLock(LockType lockType, bool wait, bool *tryAgain) {
    if (!isFileLockValid()) { //fd是否>0
        return false;
    }
    bool unLockFirstIfNeeded = false;

    if (lockType == SharedLockType) {
        //获取读锁
        //已经存在读锁了，增加计数器，直接退出    计数器是递归锁
        //已经存在写锁了，不加读锁，会导致降级
        if (m_sharedLockCount > 0 || m_exclusiveLockCount > 0) {
            m_sharedLockCount++;
            return true;
        }
    } else {
        //获取写锁
        //已经存在写锁了，计数器加+，退出
        if (m_exclusiveLockCount > 0) {
            m_exclusiveLockCount++;
            return true;
        }
        //已经存在读锁了，需要先解读锁
        if (m_sharedLockCount > 0) {
            unLockFirstIfNeeded = true;
        }
    }
    //第一次获取 读锁/写锁  或者存在读锁但要获取写锁
    auto ret = platformLock(lockType, wait, unLockFirstIfNeeded, tryAgain);
    if (ret) {
        if (lockType == SharedLockType) {
            m_sharedLockCount++;
        } else {
            m_exclusiveLockCount++;
        }
    }
    return ret;
}

platformLock在android是ashmemLock 其他是flock

android锁的实现
https://github.com/Tencent/MMKV/blob/master/Core/InterProcessLock_Android.cpp
static short LockType2FlockType(LockType lockType) {
    switch (lockType) {
        case SharedLockType:
            return F_RDLCK; //读锁
        case ExclusiveLockType:
            return F_WRLCK; //写锁
    }
}
//使用fcntl实现
bool FileLock::ashmemLock(LockType lockType, bool wait, bool unLockFirstIfNeeded, bool *tryAgain) {
    m_lockInfo.l_type = LockType2FlockType(lockType);
    if (unLockFirstIfNeeded) {
        //尝试获取该类型锁
        auto ret = fcntl(m_fd, F_SETLK, &m_lockInfo);
        if (ret == 0) {
            return true;
        }
        //获取锁失败，先解锁
        auto type = m_lockInfo.l_type;
        m_lockInfo.l_type = F_UNLCK;
        ret = fcntl(m_fd, F_SETLK, &m_lockInfo);
        if (ret != 0) {
            MMKVError("fail to try unlock first fd=%d, ret=%d, error:%s", m_fd, ret, strerror(errno));
        }
        m_lockInfo.l_type = type;
    }

    int cmd = wait ? F_SETLKW : F_SETLK;
    auto ret = fcntl(m_fd, cmd, &m_lockInfo);
    if (ret != 0) { //加锁失败
        if (tryAgain) {
            *tryAgain = (errno == EAGAIN);
        }
        if (wait) { //阻塞的情况仍然失败，打印异常
            MMKVError("fail to lock fd=%d, ret=%d, error:%s", m_fd, ret, strerror(errno));
        }
        //恢复以前的锁 
        if (unLockFirstIfNeeded) {
            m_lockInfo.l_type = LockType2FlockType(SharedLockType);
            ret = fcntl(m_fd, cmd, &m_lockInfo);
            if (ret != 0) {
                // let's hope this never happen
                MMKVError("fail to recover shared-lock fd=%d, ret=%d, error:%s", m_fd, ret, strerror(errno));
            }
        }
        return false;
    } else {
        return true;
    }
}
```


解锁
https://github.com/Tencent/MMKV/blob/master/Core/InterProcessLock.cpp
```
bool FileLock::unlock(LockType lockType) {
    if (!isFileLockValid()) {
        return false;
    }
    bool unlockToSharedLock = false;

    if (lockType == SharedLockType) {
       //解读锁，计数已经为0，返回
        if (m_sharedLockCount == 0) {
            return false;
        }
        //存在读的递归锁，写锁，计数器-1，返回
        if (m_sharedLockCount > 1 || m_exclusiveLockCount > 0) {
            m_sharedLockCount--;
            return true;
        }
    } else {
        //解写锁，计数已经为0，返回
        if (m_exclusiveLockCount == 0) {
            return false;
        }
        //写锁存在递归锁，计数-1，返回
        if (m_exclusiveLockCount > 1) {
            m_exclusiveLockCount--;
            return true;
        }
        //解写锁前，存在读锁  = 降级为读锁，写锁计数+1
        if (m_sharedLockCount > 0) {
            unlockToSharedLock = true;
        }
    }
  
    auto ret = platformUnLock(unlockToSharedLock);
    if (ret) {
        //解锁成功，对应的计数-1
        if (lockType == SharedLockType) {
            m_sharedLockCount--;
        } else {
            m_exclusiveLockCount--;
        }
    }
    return ret;
}

android解锁
https://github.com/Tencent/MMKV/blob/master/Core/InterProcessLock_Android.cpp
bool FileLock::ashmemUnLock(bool unlockToSharedLock) { //解锁还是降级为读锁
    m_lockInfo.l_type = static_cast<short>(unlockToSharedLock ? F_RDLCK : F_UNLCK);
    auto ret = fcntl(m_fd, F_SETLK, &m_lockInfo);
    if (ret != 0) {
        MMKVError("fail to unlock fd=%d, ret=%d, error:%s", m_fd, ret, strerror(errno));
        return false;
    } else {
        return true;
    }
}
```