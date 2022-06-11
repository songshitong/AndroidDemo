https://zhuanlan.zhihu.com/p/62813895   android9.0
弄明白BufferQueue，不仅可以增强对Android系统的了解，还可以弄明白/排查相关的问题，如为什么Mediacodec调用dequeueBuffer老是返回-1？
为什么普通View的draw方法直接绘制内容即可，SurfaceView在draw完毕后还需要unlockCanvasAndPost？

BufferQueue内部运作方式
BufferQueue是Android显示系统的核心，它的设计哲学是生产者-消费者模型，只要往BufferQueue中填充数据，则认为是生产者，
只要从BufferQueue中获取数据，则认为是消费者。有时候同一个类，在不同的场景下既可能是生产者也有可能是消费者。如SurfaceFlinger，
在合成并显示UI内容时，UI元素作为生产者生产内容，SurfaceFlinger作为消费者消费这些内容。而在截屏时，
SurfaceFlinger又作为生产者将当前合成显示的UI内容填充到另一个BufferQueue，截屏应用此时作为消费者从BufferQueue中获取数据并生产截图
以下是Android官网对其的介绍：
android_渲染机制_BufferQueue的数据流动.awebp

以下是常见的BufferQueue使用步骤：
1 初始化一个BufferQueue
2 图形数据的生产者通过BufferQueue申请一块GraphicBuffer，对应图中的dequeueBuffer方法
3 申请到GraphicBuffer后，获取GraphicBuffer，通过函数requestBuffer获取
4 获取到GraphicBuffer后，通过各种形式往GraphicBuffer中填充图形数据后，然后将GraphicBuffer入队到BufferQueue中，
  对应上图中的queueBuffer方法
5 在新的GraphicBuffer入队BufferQueue时，BufferQueue会通过回调通知图形数据的消费者，有新的图形数据被生产出来了
6 然后消费者从BufferQueue中出队一个GraphicBuffer，对应图中的acquireBuffer方法
7 待消费者消费完图形数据后，将空的GraphicBuffer还给BufferQueue以便重复利用，此时对应上图中的releaseBuffer方法
8 此时BufferQueue再通过回调通知图形数据的生产者有空的GraphicBuffer了，图形数据的生产者又可以从BufferQueue中获取一个空的GraphicBuffer来填充数据
9 一直循环2-8步骤，这样就有条不紊的完成了图形数据的生产-消费

当然图形数据的生产者可以不用等待BufferQueue的回调再生产数据，而是一直生产数据然后入队到BufferQueue，直到BufferQueue满为止。
图形数据的消费者也可以不用等BufferQueue的回调通知，每次都从BufferQueue中尝试获取数据，获取失败则尝试，只是这样效率比较低，
需要不断的轮训BufferQueue（因为BufferQueue有同步阻塞和非同步阻塞两种机种，在非同步阻塞机制下获取数据失败不会阻塞该线程直到有数据才唤醒该线程，
而是直接返回-1）

同时使用BufferQueue的生产者和消费者往往处在不同的进程，BufferQueue内部使用共享内存和Binder在不同的进程传递数据，减少数据拷贝提高效率。
//todo 共享内存和binder在哪

和BufferQueue有关的几个类分别是：
1 BufferBufferCore：BufferQueue的实际实现
frameworks/native/libs/gui/include/gui/BufferQueueCore.h
```
class BufferQueueCore : public virtual RefBase {
    friend class BufferQueueProducer;
    friend class BufferQueueConsumer;
 }
```
2 BufferSlot：用来存储GraphicBuffer
frameworks/native/libs/gui/include/gui/BufferSlot.h
```
struct BufferSlot {
 sp<GraphicBuffer> mGraphicBuffer;
}
```  
3 BufferState：表示GraphicBuffer的状态
  frameworks/native/libs/gui/include/gui/BufferSlot.h
4 IGraphicBufferProducer：BufferQueue的生产者接口，实现类是BufferQueueProducer
frameworks/native/libs/gui/include/gui/BufferQueueProducer.h
```
class BufferQueueProducer : public BnGraphicBufferProducer,
                            private IBinder::DeathRecipient {
  virtual status_t dequeueBuffer(int* outSlot, sp<Fence>* outFence, uint32_t width,
                                   uint32_t height, PixelFormat format, uint64_t usage,
                                   uint64_t* outBufferAge,
                                   FrameEventHistoryDelta* outTimestamps) override;
  virtual status_t queueBuffer(int slot,
            const QueueBufferInput& input, QueueBufferOutput* output);                                                             
}
```
5 IGraphicBufferConsumer：BufferQueue的消费者接口，实现类是BufferQueueConsumer
frameworks/native/libs/gui/include/gui/BufferQueueConsumer.h
```
class BufferQueueConsumer : public BnGraphicBufferConsumer {
  virtual status_t acquireBuffer(BufferItem* outBuffer,
            nsecs_t expectedPresent, uint64_t maxFrameNumber = 0) override;
  virtual status_t releaseBuffer(int slot, uint64_t frameNumber,
            const sp<Fence>& releaseFence, EGLDisplay display,
            EGLSyncKHR fence);          
}
```
6 GraphicBuffer：表示一个Buffer，可以填充图像数据
7 ANativeWindow_Buffer：GraphicBuffer的父类
9 ConsumerBase：实现了ConsumerListener接口，在数据入队列时会被调用到，用来通知消费者

BufferQueue中用BufferSlot来存储GraphicBuffer，使用数组来存储一系列BufferSlot，数组默认大小为64。
GraphicBuffer用BufferState来表示其状态
frameworks/native/libs/gui/include/gui/BufferSlot.h
FREE: 该 Buffer 没有被 producer/consumer 所使用，其所有权属于 BufferQueue
DEQUEUED: 该 Buffer 被 producer 获取了，其所有权属于 producer
QUEUED: 该 Buffer 被 producer 填充了数据且入队列到 BufferQueue 了，其所有权属于 BufferQueue
ACQUIRED: 该 Buffer 被 consumer 获取了，该Buffer的所有权属于 consumer
SHARED: 该 Buffer 处于 shared buffer 模式
  SHARED状态是一个特殊的状态，SHARED的Buffer并不参与前面所说的状态迁移。它说明Buffer被用与共享Buffer模式。除了FREE状态，
  它可以是其他的任何状态。它可以被多次dequeued, queued, 或者 acquired。这中共享Buffer的模式，主要用于VR等低延迟要求的场合。

GraphicBuffer 状态的变化过程: FREE -> dequeueBuffer() -> DEQUEUED -> queueBuffer() -> QUEUED -> acquireBuffer() ->
   ACQUIRED -> releaseBuffer() -> FREE

在Android中，由于BufferQueue的生产者-消费者往往处于不同的进程，GraphicBuffer内部是需要通过共享内存来连接生成者-消费者进程的，
每次创建GraphicBuffer，即意味着需要创建共享内存，效率较低
BufferQueue中用BufferState来表示GraphicBuffer的状态则解决了这个问题。每个GraphicBuffer都有当前的状态，通过维护GraphicBuffer的状态，
完成GraphicBuffer的复用

BufferQueue创建
在surface的创建流程创建了BufferQueue，Surface之创建流程及软硬件绘制.md
createBufferQueue
frameworks/native/libs/gui/BufferQueue.cpp
```
void BufferQueue::createBufferQueue(sp<IGraphicBufferProducer>* outProducer,
        sp<IGraphicBufferConsumer>* outConsumer,
        bool consumerIsSurfaceFlinger) {
    //创建BufferQueueCore
    sp<BufferQueueCore> core(new BufferQueueCore());
    //创建producer，consumer并付给外部变量   producer和consumer的构造器并没有特殊处理
    sp<IGraphicBufferProducer> producer(new BufferQueueProducer(core, consumerIsSurfaceFlinger));
    sp<IGraphicBufferConsumer> consumer(new BufferQueueConsumer(core));
    *outProducer = producer;
    *outConsumer = consumer;
}
```
BufferQueue的创建就是创建BufferQueueCore，BufferQueueProducer，BufferQueueConsumer


BufferQueueCore内部数据结构
frameworks/native/libs/gui/include/gui/BufferQueueCore.h
```
BufferQueueDefs::SlotsType mSlots：用数组存放的Slot，数组默认大小为BufferQueueDefs::NUM_BUFFER_SLOTS,具体是64，代表所有的Slot
std::set<int> mFreeSlots：当前所有的状态为FREE的Slot，这些Slot没有关联上具体的GraphicBuffer，后续用的时候还需要关联上GraphicBuffer
std::list<int> mFreeBuffers：当前所有的状态为FREE的Slot，这些Slot已经关联上具体的GraphicBuffer，可以直接使用
Fifo mQueue：一个先进先出队列，保存了生产者生产的数据，类型为typedef Vector<BufferItem> Fifo;

bool mAsyncMode; //非阻塞模式
bool mSharedBufferMode; 共享模式及slot
int mSharedBufferSlot
```
frameworks/native/libs/gui/BufferQueueCore.cpp
```
BufferQueueCore::BufferQueueCore() :
 mMutex(),
 mIsAbandoned(false),
 mConsumerControlledByApp(false),
 ....
{
    int numStartingBuffers = getMaxBufferCountLocked();
    //在BufferQueueCore初始化时，由于此时队列中没有入队任何数据
    //此时mFreeSlots应该包含所有的Slot，元素大小和mSlots一致，初始化代码如下：
    for (int s = 0; s < numStartingBuffers; s++) {
        mFreeSlots.insert(s);
    }
    //NUM_BUFFER_SLOTS = 64;
    for (int s = numStartingBuffers; s < BufferQueueDefs::NUM_BUFFER_SLOTS;
            s++) {
        mUnusedSlots.push_front(s);
    }
}
```
BufferQueue的工作模式，BufferQueue可以工作在几个模式：
同步模式 Synchronous-like mode
非同步模式 Non-blocking mode
舍弃模式 Discard mode
共享Buffer模式 shared buffer mode
https://www.jianshu.com/p/6d83dea3652b   todo   进程通信呢？？

以下是Buffer的入队/出队操作和BufferState的状态扭转的过程，这里只介绍非同步阻塞模式
生产者dequeueBuffer
当生产者可以生产图形数据时，首先向BufferQueue中申请一块GraphicBuffer。调用函数是BufferQueueProducer.dequeueBuffer，
如果当前BufferQueue中有可用的GraphicBuffer，则返回其对用的索引，如果不存在，则返回-1，代码在BufferQueueProducer，流程如下：
frameworks/native/libs/gui/BufferQueueProducer.cpp
```
status_t BufferQueueProducer::dequeueBuffer(int* outSlot, sp<android::Fence>* outFence,
                                            uint32_t width, uint32_t height, PixelFormat format,
                                            uint64_t usage, uint64_t* outBufferAge,
                                            FrameEventHistoryDelta* outTimestamps) {
 //1. 寻找可用的Slot，可用指Buffer状态为FREE                                           
 int found = BufferItem::INVALID_BUFFER_SLOT;
    while (found == BufferItem::INVALID_BUFFER_SLOT) {
        status_t status = waitForFreeSlotThenRelock(FreeSlotCaller::Dequeue,
                &found);
        if (status != NO_ERROR) { //出现错误返回
            return status;
        }
       ...
    }
 
 ...
 //2.找到可用的Slot，将Buffer状态设置为DEQUEUED，由于步骤1找到的Slot状态为FREE,因此这一步完成了FREE到DEQUEUED的状态切换
  *outSlot = found;
  ATRACE_BUFFER_INDEX(found);
  attachedByConsumer = mSlots[found].mNeedsReallocation;
  mSlots[found].mNeedsReallocation = false;
  mSlots[found].mBufferState.dequeue();                                            
 ...
 //3. 找到的Slot如果需要申请GraphicBuffer，则申请GraphicBuffer，这里采用了懒加载机制，如果内存没有申请，申请内存放在生产者来处理
 if (returnFlags & BUFFER_NEEDS_REALLOCATION) {
        sp<GraphicBuffer> graphicBuffer = new GraphicBuffer(
                width, height, format, BQ_LAYER_COUNT, usage,
                {mConsumerName.string(), mConsumerName.size()});
        status_t error = graphicBuffer->initCheck();
        { // Autolock scope
            Mutex::Autolock lock(mCore->mMutex);
            if (error == NO_ERROR && !mCore->mIsAbandoned) {
                graphicBuffer->setGenerationNumber(mCore->mGenerationNumber);
                mSlots[*outSlot].mGraphicBuffer = graphicBuffer;
            }
           ...
        } // Autolock scope
    }
   ...                                            
}
```
关键在于寻找可用Slot，waitForFreeSlotThenRelock的流程如下：
```
status_t BufferQueueProducer::waitForFreeSlotThenRelock(FreeSlotCaller caller,
        int* found) const {
  ...      
 //1. mQueue 是否太多       
 const int maxBufferCount = mCore->getMaxBufferCountLocked();
bool tooManyBuffers = mCore->mQueue.size()
                    > static_cast<size_t>(maxBufferCount);
if (tooManyBuffers) {
    BQ_LOGV("%s: queue size is %zu, waiting", callerString,
            mCore->mQueue.size());
} else {
   // 2. SharedBufferMode模式开启，先查找mSharedBuffer中是否有可用的
    // If in shared buffer mode and a shared buffer exists, always
    // return it.
    if (mCore->mSharedBufferMode && mCore->mSharedBufferSlot !=
            BufferQueueCore::INVALID_BUFFER_SLOT) {
        *found = mCore->mSharedBufferSlot;
    } else {
        if (caller == FreeSlotCaller::Dequeue) {
            // If we're calling this from dequeue, prefer free buffers
            //查找mFreeBuffers中是否有可用的
            int slot = getFreeBufferLocked();
            if (slot != BufferQueueCore::INVALID_BUFFER_SLOT) {
                *found = slot;
            } else if (mCore->mAllowAllocation) {
               //再查找mFreeSlots中是否有可用的，初始化时会填充满这个列表，因此第一次调用一定不会为空。
                //同时用这个列表中的元素需要关联上GraphicBuffer才可以直接使用，关联的过程由外层函数来实现new GraphicBuffer
                *found = getFreeSlotLocked();
            }
        } else {
            // If we're calling this from attach, prefer free slots
            int slot = getFreeSlotLocked();
            if (slot != BufferQueueCore::INVALID_BUFFER_SLOT) {
                *found = slot;
            } else {
                *found = getFreeBufferLocked();
            }
        }
    }
}    

//4. 如果找不到可用的Slot或者Buffer太多（同步阻塞模式下），则可能需要等
 tryAgain = (*found == BufferQueueCore::INVALID_BUFFER_SLOT) ||  tooManyBuffers;
    if (tryAgain) {
        // Return an error if we're in non-blocking mode (producer and
        // consumer are controlled by the application).
        // However, the consumer is allowed to briefly acquire an extra
        // buffer (which could cause us to have to wait here), which is
        // okay, since it is only used to implement an atomic acquire +
        // release (e.g., in GLConsumer::updateTexImage())
        if ((mCore->mDequeueBufferCannotBlock || mCore->mAsyncMode) &&
                (acquiredCount <= mCore->mMaxAcquiredBufferCount)) {
            return WOULD_BLOCK;
        }
        if (mDequeueTimeout >= 0) {
            status_t result = mCore->mDequeueCondition.waitRelative(
                    mCore->mMutex, mDequeueTimeout);
            if (result == TIMED_OUT) {
                return result;
            }
        } else {
            mCore->mDequeueCondition.wait(mCore->mMutex);
        }
    }
...   
 }
 
 int BufferQueueProducer::getFreeBufferLocked() const {
    if (mCore->mFreeBuffers.empty()) {
        return BufferQueueCore::INVALID_BUFFER_SLOT;
    }
    int slot = mCore->mFreeBuffers.front();
    mCore->mFreeBuffers.pop_front();
    return slot;
}

 int BufferQueueProducer::getFreeSlotLocked() const {
    if (mCore->mFreeSlots.empty()) {
        return BufferQueueCore::INVALID_BUFFER_SLOT;
    }
    int slot = *(mCore->mFreeSlots.begin());
    mCore->mFreeSlots.erase(slot);
    return slot;
}
```
waitForFreeSlotThenRelock函数会尝试寻找一个可用的Slot，可用的Slot状态一定是FREE（因为是从两个FREE状态的列表中获取的），
然后dequeueBuffer将状态改变为DEQUEUED，即完成了状态的扭转。

waitForFreeSlotThenRelock返回可用的Slot分为三种：
1 mSharedBufferMode开启，从mSharedBufferSlot中获取到的
2 从mFreeBuffers中获取到的，mFreeBuffers中的元素关联了GraphicBuffer，直接可用
3 从mFreeSlots中获取到的，没有关联上GraphicBuffer，因此需要申请GraphicBuffer并和Slot关联上，
  通过new GraphicBuffer申请一个GraphicBuffer，然后赋值给Slot的mGraphicBuffer完成关联
//todo mSharedBufferMode
小结dequeueBuffer：尝试找到一个Slot，并完成Slot与GraphicBuffer的关联（如果需要），然后将Slot的状态由FREE扭转成DEQUEUED。
  返回Slot在BufferQueueCore中mSlots对应的索引


生产者requestBuffer
dequeueBuffer函数获取到了可用Slot的索引后，通过requestBuffer获取到对应的GraphicBuffer。流程如下：
frameworks/native/libs/gui/BufferQueueProducer.cpp
```
status_t BufferQueueProducer::requestBuffer(int slot, sp<GraphicBuffer>* buf) {
    Mutex::Autolock lock(mCore->mMutex);
     ...
     // 1. 判断slot参数是否合法
    if (slot < 0 || slot >= BufferQueueDefs::NUM_BUFFER_SLOTS) {
        BQ_LOGE("requestBuffer: slot index %d out of range [0, %d)",
                slot, BufferQueueDefs::NUM_BUFFER_SLOTS);
        return BAD_VALUE;
    } else if (!mSlots[slot].mBufferState.isDequeued()) {
        BQ_LOGE("requestBuffer: slot %d is not owned by the producer "
                "(state = %s)", slot, mSlots[slot].mBufferState.string());
        return BAD_VALUE;
    }
    //2. 将mRequestBufferCalled置为true
    mSlots[slot].mRequestBufferCalled = true;
    *buf = mSlots[slot].mGraphicBuffer;
    return NO_ERROR;
}
```
这一步不是必须的。业务层可以直接通过Slot的索引获取到对应的GraphicBuffer。



生产者queueBuffer
上文dequeueBuffer获取到一个Slot后，就可以在Slot对应的GraphicBuffer上完成图像数据的生产了，可以是View的主线程Draw过程，
也可以是SurfaceView的子线程绘制过程，甚至可以是MediaCodec的解码过程。

填充完图像数据后，需要将Slot入队BufferQueueCore（数据写完了，可以传给生产者-消费者队列，让消费者来消费了），
   入队调用queueBuffer函数。queueBuffer的流程如下：
frameworks/native/libs/gui/BufferQueueProducer.cpp
```
status_t BufferQueueProducer::queueBuffer(int slot,
        const QueueBufferInput &input, QueueBufferOutput *output) {
        ...
     // 1. 先判断传入的Slot是否合法    
      if (slot < 0 || slot >= BufferQueueDefs::NUM_BUFFER_SLOTS) {
            BQ_LOGE("queueBuffer: slot index %d out of range [0, %d)",
                    slot, BufferQueueDefs::NUM_BUFFER_SLOTS);
            return BAD_VALUE;
        } else if (!mSlots[slot].mBufferState.isDequeued()) {
            BQ_LOGE("queueBuffer: slot %d is not owned by the producer "
                    "(state = %s)", slot, mSlots[slot].mBufferState.string());
            return BAD_VALUE;
        } else if (!mSlots[slot].mRequestBufferCalled) {
            BQ_LOGE("queueBuffer: slot %d was queued without requesting "
                    "a buffer", slot);
            return BAD_VALUE;
        }

        // If shared buffer mode has just been enabled, cache the slot of the
        // first buffer that is queued and mark it as the shared buffer.
        //SharedBufferMode开启后，缓存这个slot并标记为mShared
        if (mCore->mSharedBufferMode && mCore->mSharedBufferSlot ==
                BufferQueueCore::INVALID_BUFFER_SLOT) {
            mCore->mSharedBufferSlot = slot;
            mSlots[slot].mBufferState.mShared = true;
        } 
     ...
     //2. 将Buffer状态扭转成QUEUED，此步完成了Buffer的状态由DEQUEUED到QUEUED的过程
      mSlots[slot].mFence = acquireFence;
      mSlots[slot].mBufferState.queue();   
      ++mCore->mFrameCounter;
      currentFrameNumber = mCore->mFrameCounter;
      mSlots[slot].mFrameNumber = currentFrameNumber; 
      item.mSlot = slot;  
      
      //3. 入队mQueue
       if (mCore->mQueue.empty()) {
            // When the queue is empty, we can ignore mDequeueBufferCannotBlock
            // and simply queue this buffer
            mCore->mQueue.push_back(item);
            frameAvailableListener = mCore->mConsumerListener;
        }  
   
   ...     
   // 4. 回调frameAvailableListener，告知消费者有数据入队了
  if (frameAvailableListener != NULL) {
            frameAvailableListener->onFrameAvailable(item);
        } else if (frameReplacedListener != NULL) {
            frameReplacedListener->onFrameReplaced(item);
   }
   ...                      
}
```
从上面的注释可以看到，queueBuffer的主要步骤如下：
1 将Buffer状态扭转成QUEUED，此步完成了Buffer的状态由DEQUEUED到QUEUED的过程
2 将Buffer入队到BufferQueueCore的mQueue队列中
3 回调frameAvailableListener，告知消费者有数据入队，可以来消费数据了，frameAvailableListener是消费者注册的回调

小结queueBuffer：将Slot的状态扭转成QUEUED，并添加到mQueue中，最后通知消费者有数据入队。



消费者acquireBuffer
在消费者接收到onFrameAvailable回调时或者消费者主动想要消费数据，调用acquireBuffer尝试向BufferQueueCore获取一个数据以供消费。
消费者的代码在BufferQueueConsumer中，acquireBuffer流程如下：
frameworks/native/libs/gui/BufferQueueConsumer.cpp
```
status_t BufferQueueConsumer::acquireBuffer(BufferItem* outBuffer,
        nsecs_t expectedPresent, uint64_t maxFrameNumber) {
    // In asynchronous mode the list is guaranteed to be one buffer deep,
        // while in synchronous mode we use the oldest buffer.
    //1. 如果队列为空并且非sharedBuffer，则直接返回    
    if (mCore->mQueue.empty() && !sharedBufferAvailable) {
        return NO_BUFFER_AVAILABLE;
    }    
    //2. 取出mQueue队列的第一个元素
    BufferQueueCore::Fifo::iterator front(mCore->mQueue.begin());
    
    //3. 处理expectedPresent的情况，这种情况可能会连续丢几个Slot的“显示”时间小于expectedPresent的情况,
    //这种情况下这些Slot已经是“过时”的，直接走下文的releaseBuffer消费流程,代码比较长，忽略了
    ...
   if (sharedBufferAvailable && mCore->mQueue.empty()) {
        // make sure the buffer has finished allocating before acquiring it
        mCore->waitWhileAllocatingLocked();

        slot = mCore->mSharedBufferSlot;

        // Recreate the BufferItem for the shared buffer from the data that
        // was cached when it was last queued.
        outBuffer->mGraphicBuffer = mSlots[slot].mGraphicBuffer;
        outBuffer->mFence = Fence::NO_FENCE;
        outBuffer->mFenceTime = FenceTime::NO_FENCE;
        outBuffer->mCrop = mCore->mSharedBufferCache.crop;
        outBuffer->mTransform = mCore->mSharedBufferCache.transform &
                ~static_cast<uint32_t>(
                NATIVE_WINDOW_TRANSFORM_INVERSE_DISPLAY);
        outBuffer->mScalingMode = mCore->mSharedBufferCache.scalingMode;
        outBuffer->mDataSpace = mCore->mSharedBufferCache.dataspace;
        outBuffer->mFrameNumber = mCore->mFrameCounter;
        outBuffer->mSlot = slot;
        outBuffer->mAcquireCalled = mSlots[slot].mAcquireCalled;
        outBuffer->mTransformToDisplayInverse =
                (mCore->mSharedBufferCache.transform &
                NATIVE_WINDOW_TRANSFORM_INVERSE_DISPLAY) != 0;
        outBuffer->mSurfaceDamage = Region::INVALID_REGION;
        outBuffer->mQueuedBuffer = false;
        outBuffer->mIsStale = false;
        outBuffer->mAutoRefresh = mCore->mSharedBufferMode &&
                mCore->mAutoRefresh;
    } else {
        //非shared模式
        slot = front->mSlot;
        *outBuffer = *front;
    }
    ...
    //接第2步，取出front后移除
    mCore->mQueue.erase(front);
    
    //4. 更新Slot的状态为ACQUIRED
   if (!outBuffer->mIsStale) {
        mSlots[slot].mAcquireCalled = true;
        // Don't decrease the queue count if the BufferItem wasn't
        // previously in the queue. This happens in shared buffer mode when
        // the queue is empty and the BufferItem is created above.
        if (mCore->mQueue.empty()) {
            mSlots[slot].mBufferState.acquireNotInQueue();
        } else {
            mSlots[slot].mBufferState.acquire();
        }
        mSlots[slot].mFence = Fence::NO_FENCE;
    }
   ... 
}
```
从上面的注释可以看到，acquireBuffer的主要步骤如下：
1 从mQueue队列中取出并移除一个元素
2 改变Slot对应的状态为ACQUIRED
3 如果有丢帧逻辑，回调告知生产者有数据被消费，生产者可以准备生产数据了

小结acquireBuffer：将Slot的状态扭转成ACQUIRED，并从mQueue中移除，最后通知生产者有数据出队。



消费者releaseBuffer
消费者获取到Slot后开始消费数据（典型的消费如SurfaceFlinger的UI合成），消费完毕后，需要告知BufferQueueCore这个Slot被消费者消费完毕了，
可以给生产者重新生产数据，releaseBuffer流程如下：
frameworks/native/libs/gui/BufferQueueConsumer.cpp
```
status_t BufferQueueConsumer::releaseBuffer(int slot, uint64_t frameNumber,
        const sp<Fence>& releaseFence, EGLDisplay eglDisplay,
        EGLSyncKHR eglFence) {
     //1. 检查Slot是否合法   
     if (slot < 0 || slot >= BufferQueueDefs::NUM_BUFFER_SLOTS ||
            releaseFence == NULL) {
        BQ_LOGE("releaseBuffer: slot %d out of range or fence %p NULL", slot,
                releaseFence.get());
        return BAD_VALUE;
    } 
    ...
    //2. 容错处理：如果要处理的Slot存在于mQueue中，那么说明这个Slot的来源不合法，并不是从acquireBuffer获取的Slot，拒绝处理
    // If the frame number has changed because the buffer has been reallocated,
        // we can ignore this releaseBuffer for the old buffer.
        // Ignore this for the shared buffer where the frame number can easily
        // get out of sync due to the buffer being queued and acquired at the
        // same time.
        if (frameNumber != mSlots[slot].mFrameNumber &&
                !mSlots[slot].mBufferState.isShared()) {
            return STALE_BUFFER_SLOT;
        }
    ...
    // 3. 将Slot的状态扭转为FREE，之前是ACQUIRED，并将该Slot添加到BufferQueueCore的mFreeBuffers列表中
     mSlots[slot].mEglDisplay = eglDisplay;
        mSlots[slot].mEglFence = eglFence;
        mSlots[slot].mFence = releaseFence;
        //状态变为free
        mSlots[slot].mBufferState.release();
        // After leaving shared buffer mode, the shared buffer will
        // still be around. Mark it as no longer shared if this
        // operation causes it to be free.
        if (!mCore->mSharedBufferMode && mSlots[slot].mBufferState.isFree()) {
            mSlots[slot].mBufferState.mShared = false;
        }
        // Don't put the shared buffer on the free list.
        //shared buffer不添加到mFreeBuffers
        if (!mSlots[slot].mBufferState.isShared()) {
            mCore->mActiveBuffers.erase(slot);
            mCore->mFreeBuffers.push_back(slot);
        }
    ...
    
    // 4. 回调生产者，有数据被消费了
      // Call back without lock held
    if (listener != NULL) {
        listener->onBufferReleased();
    }  
 }
```
从上面的注释可以看到，releaseBuffer的主要步骤如下：
1 将Slot的状态扭转为FREE
2 将被消费的Slot添加到mFreeBuffers供后续的生产者dequeueBuffer使用
3 回调告知生产者有数据被消费，生产者可以准备生产数据了
小结releaseBuffer：将Slot的状态扭转成FREE，并添加到BufferQueueCore mFreeBuffers队列中，最后通知生产者有数据出队。