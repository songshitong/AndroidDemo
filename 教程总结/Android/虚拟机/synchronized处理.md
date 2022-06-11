加锁的总结
1 如果目标Object的monitor(指向一个LockWord对象)还没有上锁(状态为KUnlocked)，则设置monitor_为一个瘦锁，其状态为kStateThinLocked，
并且保存拥有该锁的线程的Id。同时，设置上锁次数为0。 使用CAS更新

2 如果目标Object的monitor_的锁状态为kStateThinLocked(暂且不考虑上锁次数超过4096的情况，此时，monitor_依然是一个瘦锁)。
则检查调用线程和拥有该锁的线程是否为同一个。
如果是同一个线程再次上锁，则只需增加上锁次数即可。
如果是其他线程试图获取该锁，则先尽量让新的竞争者(也就是当前的调用线程)让出最多50次CPU资源(sched_yield())。如果50次后依然无法得到该锁，
  则需要将瘦锁变成一个胖锁。此时，monitor_的锁状态将变成kKFatLocked。

3 如果目标Object的monitor_的锁状态为kFatLocked，则调用对应Monitor对象的Lock函数进行抢锁。
Lock函数内部会使用比如futex或pthread_mutex_t等来实现抢锁。一旦Lock函数返回，则调用线程就获得了该锁。

4 如何给目标Object monitor_增肥呢?
如果因为B线程导致目标Object.monitor_需要增肥的话，InflateThinLocked居然
会暂停当前拥有锁的线程A的运行。然后做个增肥手术，替换目标Object.monitor_为新的胖
锁，最后再恢复线程A的运行

android 8.0
指令定义
art/runtime/dex_instruction_list.h
```
 V(0x1D, MONITOR_ENTER, "monitor-enter", k11x, kIndexNone, kContinue | kThrow | kClobber, kVerifyRegA) \
 V(0x1E, MONITOR_EXIT, "monitor-exit", k11x, kIndexNone, kContinue | kThrow | kClobber, kVerifyRegA) \
```

art对于MONITOR_ENTER和MONITOR_EXIT的处理
art/runtime/interpreter/interpreter_switch_impl.cc
```
JValue ExecuteSwitchImpl(Thread* self, const DexFile::CodeItem* code_item,
                         ShadowFrame& shadow_frame, JValue result_register,
                         bool interpret_one_instruction) {
  ...
   case Instruction::MONITOR_ENTER: {
        PREAMBLE();
        //obj是synchronized所使用的对象
        ObjPtr<mirror::Object> obj = shadow_frame.GetVRegReference(inst->VRegA_11x(inst_data));
        if (UNLIKELY(obj == nullptr)) {
          ThrowNullPointerExceptionFromInterpreter();
          HANDLE_PENDING_EXCEPTION();
        } else {
          //调用DoMonitorEnter
          DoMonitorEnter<do_assignability_check>(self, &shadow_frame, obj);
          POSSIBLY_HANDLE_PENDING_EXCEPTION(self->IsExceptionPending(), Next_1xx);
        }
        break;
      }  
   case Instruction::MONITOR_EXIT: {
        PREAMBLE();
        ObjPtr<mirror::Object> obj = shadow_frame.GetVRegReference(inst->VRegA_11x(inst_data));
        if (UNLIKELY(obj == nullptr)) {
          ThrowNullPointerExceptionFromInterpreter();
          HANDLE_PENDING_EXCEPTION();
        } else {
          //调用DoMonitorExit
          DoMonitorExit<do_assignability_check>(self, &shadow_frame, obj);
          POSSIBLY_HANDLE_PENDING_EXCEPTION(self->IsExceptionPending(), Next_1xx);
        }
        break;
      }                          
}
```

DoMonitorEnter与DoMonitorExit
关键是调用MonitorEnter和MonitorExit
art/runtime/interpreter/interpreter_common.h
```
static inline void DoMonitorEnter(Thread* self, ShadowFrame* frame, ObjPtr<mirror::Object> ref)
    NO_THREAD_SAFETY_ANALYSIS
    REQUIRES(!Roles::uninterruptible_) {
  StackHandleScope<1> hs(self);
  Handle<mirror::Object> h_ref(hs.NewHandle(ref));
  h_ref->MonitorEnter(self);
  if (kMonitorCounting && frame->GetMethod()->MustCountLocks()) {
    frame->GetLockCountData().AddMonitor(self, h_ref.Get());
  }
}

static inline void DoMonitorExit(Thread* self, ShadowFrame* frame, ObjPtr<mirror::Object> ref)
    NO_THREAD_SAFETY_ANALYSIS
    REQUIRES(!Roles::uninterruptible_) {
  StackHandleScope<1> hs(self);
  Handle<mirror::Object> h_ref(hs.NewHandle(ref));
  h_ref->MonitorExit(self);
  if (kMonitorCounting && frame->GetMethod()->MustCountLocks()) {
    frame->GetLockCountData().RemoveMonitorOrThrow(self, h_ref.Get());
  }
}
```

先看MonitorEnter相关
art/runtime/monitor.cc
```
mirror::Object* Monitor::MonitorEnter(Thread* self, mirror::Object* obj, bool trylock) {
   //FakeLock是为了符合多线程检查工具设置的，内部没有功能，返回值就是输入参数
  obj = FakeLock(obj);
  uint32_t thread_id = self->GetThreadId();
  size_t contention_count = 0;
  StackHandleScope<1> hs(self);
  Handle<mirror::Object> h_obj(hs.NewHandle(obj));
    while (true) {
    //获取obj monitor_,其内容保存在LockWord
    LockWord lock_word = h_obj->GetLockWord(false);
    switch (lock_word.GetState()) {
      //未上锁的状态
      case LockWord::kUnlocked: {
        //如果处于未上锁状态，锁上他  FromThinLockId构造一个新的LockWord
        //他是一个Thin Lock，由thread_id所代表的线程所有，锁的次数为0,锁的状态为kThinLocked
        LockWord thin_locked(LockWord::FromThinLockId(thread_id, 0, lock_word.GCState()));
        //更新obj的monitor_成员为thin_locked的LockWord 使用CAS更新
        if (h_obj->CasLockWordWeakAcquire(lock_word, thin_locked)) {
          AtraceMonitorLock(self, h_obj.Get(), false /* is_wait */);
          return h_obj.Get();  // Success!
        }
        continue;  // Go again.
      }
      case LockWord::kThinLocked: {
        //如果是已经处于上锁状态，检查拥有该锁的线程和当前调用MonitorEnter的线程是否相同
        //如果相同则是所谓的递归锁，需要增加lock count
        uint32_t owner_thread_id = lock_word.ThinLockOwner();
        if (owner_thread_id == thread_id) {
          uint32_t new_count = lock_word.ThinLockCount() + 1;
          //一个线程持有同一个锁的次数大部分情况不能超过4096
          if (LIKELY(new_count <= LockWord::kThinLockMaxCount)) {
            //创建一个新的LockWord,只更新lock count
            LockWord thin_locked(LockWord::FromThinLockId(thread_id,
                                                          new_count,
                                                          lock_word.GCState()));
            //如果使用读屏障                                              
            if (!kUseReadBarrier) {
              //将新的thin_locked对象更新到Object monitor_中
              h_obj->SetLockWord(thin_locked, false /* volatile */);
              AtraceMonitorLock(self, h_obj.Get(), false /* is_wait */);
              return h_obj.Get();  // Success!
            } else {
              //否则使用cas更新
              if (h_obj->CasLockWordWeakRelaxed(lock_word, thin_locked)) {
                AtraceMonitorLock(self, h_obj.Get(), false /* is_wait */);
                return h_obj.Get();  // Success!
              }
            }
            continue;  // Go again.
          } else {
            // 如果一个线程持有某个锁的次数超过4096,将瘦锁升级为胖锁
            InflateThinLocked(self, h_obj, lock_word, 0);
          }
        } else {
          //如果当前持有锁的线程和调用MonitorEnter的线程不是同一个
          if (trylock) {
            return nullptr;
          }
          // Contention.
          contention_count++;
          Runtime* runtime = Runtime::Current();
          //GetMaxSpinsBeforeThinLockInflation获取的是Runtime.h max_spins_before_thin_lock_inflation_
          //由编译期常量kDefaultMaxSpinsBeforeThinLockInflation = 50指定，monitor.h
          
          //monitor属于重型资源，一般情况尽量不要使用它，当出现多个线程试图获取同一个锁时，也就是下面的，ART做了如下优化
          (1)先通过sched_yield系统调用主动让出当前调用线程的执行资格。操作系统将在后续
            某个时间恢复该线程的执行。从调用sched_Yield让出CPU资源到后续某个时间又
            恢复它的执行之间存在一定的时间差，而在这个时间差里，占用该锁的线程可能会释放
            对锁的占用。所以，调用线程从schedyield返回后，通过外面的while循环会重新
            尝试获取锁。如果成功的话，就无须使用Monitor了。这种优化很好，但是最多用50
            次。因为多核CPU的存在导致虽然调用线程让出了当前CPU核的资源，但CPU的其他
            核可能会立即运行它。简单点说，就是这个时间差可能会很短，甚至接近于0。在这种
            情况下，这段代码就变成了所谓的忙等待，反而会影响系统的性能。
            
            (2)所以，当让出了50次执行资格后依然无法拿到锁，则通过InflateThinLocked
            先将着锁变成胖锁。
          if (contention_count <= runtime->GetMaxSpinsBeforeThinLockInflation()) {
           //sched_yield 调用线程可能会让出CPU资源给其他线程  
            sched_yield();
          } else {
            contention_count = 0;
            //锁升级
            InflateThinLocked(self, h_obj, lock_word, 0);
          }
        }
        continue;  // 不管是sched_yield还是转为胖锁，继续循环
      }
      case LockWord::kFatLocked: {
        //如果是胖锁，借助Monitor Lock函数(内部通过futex实现线程间同步)来获取锁 
        QuasiAtomic::ThreadFenceAcquire();
        // FatLockMonitor()从MonitorPool中获取monitor  MonitorPool::MonitorFromMonitorId(mon_id)
        Monitor* mon = lock_word.FatLockMonitor();
        if (trylock) {
          return mon->TryLock(self) ? h_obj.Get() : nullptr;
        } else {
          //如果锁被其他线程拥有，此处会等待
          mon->Lock(self);
          return h_obj.Get();  //一旦lock函数返回，锁被本线程拥有
        }
      }
      case LockWord::kHashCode:
       ...
    }
  }
}
```
构建瘦锁的LockWord
art/runtime/lock_word.h
```
  static LockWord FromThinLockId(uint32_t thread_id, uint32_t count, uint32_t gc_state) {
    ...
    return LockWord((thread_id << kThinLockOwnerShift) |
                    (count << kThinLockCountShift) |
                    (gc_state << kGCStateShift) |
                    (kStateThinOrUnlocked << kStateShift));
  }
  
  //锁的状态
  enum LockState {
    kUnlocked,    // No lock owners.
    kThinLocked,  // Single uncontended owner.
    kFatLocked,   // See associated monitor.
    kHashCode,    // Lock word contains an identity hash.
    kForwardingAddress,  // Lock word contains the forwarding address of an object.
  };
 
  //获取锁的状态
  LockState GetState() const {
    CheckReadBarrierState();
    if ((!kUseReadBarrier && UNLIKELY(value_ == 0)) ||
        (kUseReadBarrier && UNLIKELY((value_ & kGCStateMaskShiftedToggled) == 0))) {
      return kUnlocked;
    } else {
      uint32_t internal_state = (value_ >> kStateShift) & kStateMask;
      switch (internal_state) {
        case kStateThinOrUnlocked:
          return kThinLocked;
        case kStateHash:
          return kHashCode;
        case kStateForwardingAddress:
          return kForwardingAddress;
        default:
          ...
          return kFatLocked;
      }
    }
  }  
```

Obj.monitor_更新
```
art/runtime/mirror/object-inl.h
//cas更新
inline bool Object::CasLockWordWeakAcquire(LockWord old_val, LockWord new_val) {
 return CasFieldWeakAcquire32<false, false>(
 OFFSET_OF_OBJECT_MEMBER(Object, monitor_), old_val.GetValue(), new_val.GetValue());
}

//volatile更新
inline void Object::SetLockWord(LockWord new_val, bool as_volatile) {
  if (as_volatile) {
    SetField32Volatile<false, false, kVerifyFlags>(
        OFFSET_OF_OBJECT_MEMBER(Object, monitor_), new_val.GetValue());
  } else {
    SetField32<false, false, kVerifyFlags>(
        OFFSET_OF_OBJECT_MEMBER(Object, monitor_), new_val.GetValue());
  }
}

art/runtime/mirror/object-readbarrier-inl.h
inline bool Object::CasLockWordWeakRelease(LockWord old_val, LockWord new_val) {
  // Force use of non-transactional mode and do not check.
  return CasFieldWeakRelease32<false, false>(
      OFFSET_OF_OBJECT_MEMBER(Object, monitor_), old_val.GetValue(), new_val.GetValue());
}
```


瘦锁升级胖锁的过程 InflateThinLocked
art/runtime/monitor.cc
```
void Monitor::InflateThinLocked(Thread* self, Handle<mirror::Object> obj, LockWord lock_word,
                                uint32_t hash_code) {
  DCHECK_EQ(lock_word.GetState(), LockWord::kThinLocked);
  uint32_t owner_thread_id = lock_word.ThinLockOwner();
  if (owner_thread_id == self->GetThreadId()) {
    //如果是同一线程，直接Inflate()，这种情况为同一线程多次获取锁，并且次数太多，瘦锁不得不变胖
    Inflate(self, self, obj.Get(), hash_code);
  } else {
    //如果是多个线程竞争
    ThreadList* thread_list = Runtime::Current()->GetThreadList();
    //设置Thread tlsPtr_.monitor_enter_object对象为目标obj
    self->SetMonitorEnterObject(obj.Get());
    bool timed_out;
    Thread* owner;
    {
      ScopedThreadSuspension sts(self, kBlocked);
      //要求当前拥有该锁的线程暂停运行。因为我们要给他“动手术“  todo
      owner = thread_list->SuspendThreadByThreadId(owner_thread_id, false, &timed_out);
    }
    if (owner != nullptr) {
      // We succeeded in suspending the thread, check the lock's status didn't change.
      lock_word = obj->GetLockWord(true);
      //再次检查obj的monitor_,确认为kThinLocked，并且被owner_thread_id线程所持有
      //将obj的monitor_替换为增肥的LockWord对象
      if (lock_word.GetState() == LockWord::kThinLocked &&
          lock_word.ThinLockOwner() == owner_thread_id) {
        Inflate(self, owner, obj.Get(), hash_code);
      }
      //恢复原线程的运行  todo
      thread_list->Resume(owner, false);
    }
    self->SetMonitorEnterObject(nullptr);
  }
}
```
假设线程A先抢到了目标Object对应的monitor_锁(此时锁的状态为kThinLocked)，而
后续因为线程B的原因，目标Object.monitor_要变胖为KFatLocked才能实现同步。此时A还
在运行过程中(也就是线程A还在用目标Object的monitor_)，那么我们该如何给目标Object
monitor_增肥呢?
原来，如果因为B线程导致目标Object.monitor_需要增肥的话，InflateThinLocked居然
会暂停当前拥有锁的线程A的运行。然后做个增肥手术，替换目标Object.monitor_为新的胖
锁，最后再恢复线程A的运行。对A来说，这个手术虽然做得神不知鬼不觉，但依然感觉有
点吃亏。因为A此时已经是锁的拥有者，而因为其他线程要用锁居然会导致A暂停运行一段时间
inflate函数
```
void Monitor::Inflate(Thread* self, Thread* owner, mirror::Object* obj, int32_t hash_code) {
  ....
  //创建新的monitor
  Monitor* m = MonitorPool::CreateMonitor(self, owner, obj, hash_code);
  ...
  //将obj的monitor_替换为增肥的LockWord对象
  if (m->Install(self)) {
    ...
    //添加到MonitorList
    Runtime::Current()->GetMonitorList()->Add(m);
    ....
  } else {
    MonitorPool::ReleaseMonitor(self, m);
  }
}

bool Monitor::Install(Thread* self) {
  MutexLock mu(self, monitor_lock_);  // Uncontended mutex acquisition as monitor isn't yet public.
  ....
  LockWord lw(GetObject()->GetLockWord(false));
  switch (lw.GetState()) {
    case LockWord::kThinLocked: {
      CHECK_EQ(owner_->GetThreadId(), lw.ThinLockOwner());
      lock_count_ = lw.ThinLockCount();
      break;
    }
    ....
  }
  LockWord fat(this, lw.GCState());
  //使用cas更新Object.monitor_为LockWord的胖对象，锁的状态为kKFatLocked
  bool success = GetObject()->CasLockWordWeakRelease(lw, fat);
  if (success && owner_ != nullptr && lock_profiling_threshold_ != 0) {
    locking_method_ = owner_->GetCurrentMethod(&locking_dex_pc_, false);
  }
  return success;
}
```
//todo 对象头 monitor_的分布https://juejin.cn/post/6956213033806872606#heading-1

释放锁的过程
art/runtime/monitor.cc
```
bool Monitor::MonitorExit(Thread* self, mirror::Object* obj) {
  ....
  self->AssertThreadSuspensionIsAllowable();
  obj = FakeUnlock(obj);
  StackHandleScope<1> hs(self);
  Handle<mirror::Object> h_obj(hs.NewHandle(obj));
  while (true) {
    LockWord lock_word = obj->GetLockWord(true);
    switch (lock_word.GetState()) {
      case LockWord::kHashCode:
      case LockWord::kUnlocked:
        //处理monitor-exit时，锁的状态不应该为kHashCode，kUnlocked
        FailedUnlock(h_obj.Get(), self->GetThreadId(), 0u, nullptr);
        return false;  // Failure.
      case LockWord::kThinLocked: {
        //如果为瘦锁，上锁次数递减
        uint32_t thread_id = self->GetThreadId();
        uint32_t owner_thread_id = lock_word.ThinLockOwner();
        if (owner_thread_id != thread_id) {
          //没有得到锁的线程去释放锁，显然出错了
          FailedUnlock(h_obj.Get(), thread_id, owner_thread_id, nullptr);
          return false;  // Failure.
        } else {
          LockWord new_lw = LockWord::Default();
          if (lock_word.ThinLockCount() != 0) {
            //瘦锁次数-1
            uint32_t new_count = lock_word.ThinLockCount() - 1;
            new_lw = LockWord::FromThinLockId(thread_id, new_count, lock_word.GCState());
          } else {
            new_lw = LockWord::FromDefault(lock_word.GCState());
          }
          if (!kUseReadBarrier) {
            //如果使用读屏障，更新obj.monitor_为新的LockWord
            h_obj->SetLockWord(new_lw, true);
            AtraceMonitorUnlock();
            // Success!
            return true;
          } else {
            // Use CAS to preserve the read barrier state.
            if (h_obj->CasLockWordWeakRelease(lock_word, new_lw)) {
              AtraceMonitorUnlock();
              // Success!
              return true;
            }
          }
          continue;  // Go again.
        }
      }
      case LockWord::kFatLocked: {
        //胖锁，调用Unlock
        Monitor* mon = lock_word.FatLockMonitor();
        return mon->Unlock(self);
      }
      ....
    }
  }
}

bool Monitor::Unlock(Thread* self) {
  DCHECK(self != nullptr);
  uint32_t owner_thread_id = 0u;
  {
    MutexLock mu(self, monitor_lock_);
    Thread* owner = owner_;
    if (owner != nullptr) {
      owner_thread_id = owner->GetThreadId();
    }
    if (owner == self) {
      // We own the monitor, so nobody else can be in here.
      AtraceMonitorUnlock();
      if (lock_count_ == 0) {
        owner_ = nullptr;
        locking_method_ = nullptr;
        locking_dex_pc_ = 0;
        // Wake a contender.
        //唤醒操作
        monitor_contenders_.Signal(self);
      } else {
        --lock_count_;
      }
      return true;
    }
  }
  ...
  return false;
}
```
MonitorExit比较简单。但细心的读者可能会问，MonitorEnter中瘦锁会转变为胖锁，为什么在MonitorExit中却没有看到胖锁转变为瘦锁的地方〔
(也就是没有调用Monitor》deflate函数的代码)。这是因为胖锁转变为瘦锁涉及内存资源的回收，所以ART将这部分内容放到GC部分
来处理。 //todo

//todo 机器码执行模式下的处理