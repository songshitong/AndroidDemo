总结：
volatile
jit模式
读  volatile  Atomic.LoadSequentiallyConsistent-》std::memory_order_seq_cst  顺序一致性，禁止重排
    非volatile  Atomic.LoadJavaData ->std::memory_order_relaxed  松散模型，编译器可以重排优化
写  volatile Atomic.StoreSequentiallyConsistent->std::memory_order_seq_cst
    非volatile Atomic.StoreJavaData ->std::memory_order_relaxed
Aot模式的处理
读    mov读到寄存器   kLoadAny(LoadLoad | LoadStore)    防止后面的普通读和普通写重排到前面
写    kAnyStore(LoadStore | StoreStore)   mov写入内存(读到内存)  kAnyAny(StoreLoad)
//x86只会发生store-load的重排，所以对volatile的处理，在x86平台只有写完时插入lock的内存屏障   防止store重排到其他load的后面


多线程读写数据时候三种会导致不可预知行为的错误
第一种错误是未同步化的数据访问（Unsynchronized Data Access）
```
if(val>=0) f(val); //如果变量val的值大于0，则直接以val为参数调用f
else f(-val); //如果变量val的值小于0，则以-val为参数调用f
//也就是说，我们希望f函数的入参为正数或0，而不能为负数
```
上述代码如果运行在多线程环境下很可能会出现问题。因为val的值可能在if或else判断之后、f调用之前被其他线程修改。
第二种错误是写至半途的数据（Half-Written Data），比如下面的例子。
```
long long x = 0; //x变量，long long型，初值为0
//线程A写入x，比如
x = -1; //设置x的值为-1
//线程B读取x，比如
std::cout << x; //打印输出x的值
```
最后一行打印什么呢
·0：在这种情况下，线程B先执行完，而A还没有来得及给x赋值。
·-1：在这种情况下，线程A先执行完，x被设置为-1。然后线程B再打印x。
·其他值：出现这种情况的原因是高级语言里即使简单的一条赋值语句都可能转换为多条汇编指令。
比如，假如long long是128位的，而汇编指令一次最多操作32位长的数据，那么，x=-1这条语句将对应四条汇编指令。
如果线程A在执行这四条汇编指令过程中，线程B打印了x的值，那么打印的结果就无法确定了。

第三个
最后一种错误是重排序的语句（Reorded Statement）
```
//两个变量
int data;
bool readyFlag = false;
//线程A执行下面的语句
data = 42;
readyFlag = true; //设置为true，表示data 已经准备就绪
//线程B执行下面的语句
while(!readyFlag){} //循环等待readFlag，直到数据准备好
foo(data); //上面的循环一旦退出，就表明data已经准备好了
```
在上面的代码中，程序员的代码逻辑和顺序并没有错。
·线程A先设置data的值为42，然后设置readyFlag为true。
·线程B先用while循环等待readyFlag为true。按程序员的设计初衷，只要while循环退出，调用foo时，data的值一定是线程A中设置的42。
程序员虽然没写错代码逻辑，但编译器却可能做一些让人意想不到的优化。根据C++的规则，编译器只要保证在单一线程里代码逻辑的正确即可。
仔细观察线程A，虽然代码中data的赋值在前，readyFlag的赋值在后，但这两个语句毫无关系。所以，编译器很有可能重新排列这两行代码的顺序，
比如readyFlag赋值先执行，data赋值后执行。如此，程序运行的结果就和程序员的初衷完全不同了。


强顺序和弱顺序
假设有指令1、指令2，依次排列，一直到指令5。
如果CPU按照指令书写的顺序（指令1、指令2、指令3、指令4、指令5）来执行的话，则称之为强顺序（strong ordered）。
但如果指令1、2、3和指令4、5之间无关联（比如它们使用了不同的内存地址、寄存器），则CPU很可能会打乱顺序来执行这5条指令。这
种情况称之为弱顺序（weak ordered）。
强顺序和弱顺序和具体的CPU架构有关。
对弱顺序模型的CPU而言，如果需保证指令执行顺序的话，需要添加一个内存栅（英文为memory barrier或memory fence）。内存栅
的目的是保证栅之前的指令都执行完后，才能执行栅后面的指令。在x86平台上，内存栅相关的指令为fence等

在C++11中，atomic问题可通过std atomic模板类提供的一些方法来解决，而order问题则可用到std atomic里定义的内存模型来帮助我们解决
atomic问题也可以借助mutex来解决。 不过，从更抽象的层次来考虑，mutex是用来同步代码逻辑的，而atomic是用来同步数据操作的。
  用mutex来同步数据操作，有些杀鸡用牛刀的感觉
C++11一共定义了六种内存顺序类  https://en.cppreference.com/w/cpp/atomic/memory_order。
·memory_order_seq_cst：seq cst是sequential consistent的缩写，意为顺序一致性。它是内存顺序要求中最严格的。
使用它的话就能防止代码重排的问题。它是atomic store与load函数的默认取值。所以，上面这段代码就不会再有order的问题。  
  缺点：在某些CPU平台下会影响性能，可以使用memory_order_acquire，memory_order_release优化
·memory_order_relaxed：松散模型。这种模型不对内存order有什么限制，编译器会根据目标CPU的情况做优化。
·memory_order_acquire：使用它的线程中，后续的所有读操作必须在本条原子操作后执行。
·memory_order_release：使用它的线程中，之前的所有写操作必须在本条原子操作前执行完。
·memory_order_acq_rel：同时包含上面acquire和release的要求。
·memory_order_consume：使用它的线程中，后续有关的原子操作必须在本原子操作完成后执行。
```
int data = 0;
atomic<bool> readyFlag{false};
//线程A执行下面的语句
data= 42;
//设置为true，第二个参数memory_order_seq_cst是C++11中
定义的内存顺序中的一种
readyFlag.store(true,std::memory_order_seq_cst)
//线程B执行下面的语句
//读取readyFlag的值
while(!readyFlag.load(std::memory_order_seq_cst)){}
foo(data); //上面的循环一旦退出，就表明data已经准备好了
```


android8.0
volatile的变量  java字节码
```
volatile int anInt;
    descriptor: I
    flags: (0x0040) ACC_VOLATILE
```
变量的读写，java字节码getfield putfield

Java层中读写成员变量的代码将生成iget和iput等相关的Java指令   
art/runtime/dex_instruction_list.h
```
  V(0x52, IGET, "iget", k22c, kIndexFieldRef, kContinue | kThrow | kLoad | kRegCFieldOrConstant, kVerifyRegA | kVerifyRegB | kVerifyRegCField) \
  V(0x59, IPUT, "iput", k22c, kIndexFieldRef, kContinue | kThrow | kStore | kRegCFieldOrConstant, kVerifyRegA | kVerifyRegB | kVerifyRegCField) \
```
todo 如何调用到Object::GetField和SetField


Object::GetField和SetField
art/runtime/mirror/object.h
```
 template<typename kSize, bool kIsVolatile>
  ALWAYS_INLINE kSize GetField(MemberOffset field_offset)
  
 template<typename kSize, bool kIsVolatile>
  ALWAYS_INLINE void SetField(MemberOffset field_offset, kSize new_value) 
```
GetField和SetField为模板参数
·kSize表示目标成员变量在虚拟机中对应的数据类型。比如，Java层中一个int类型的成员变量对应的kSize就是int32_t。
·kIsVolatile表示成员变量是否为volatile修饰

getField的逻辑
```
  template<typename kSize, bool kIsVolatile>
  ALWAYS_INLINE kSize GetField(MemberOffset field_offset)
      REQUIRES_SHARED(Locks::mutator_lock_) {
    //field_offset表示目标成员变量位于在对象的内存的什么位置。也就是说，下面的raw_addr内存地址里存储的就是目标成员变量。 
    const uint8_t* raw_addr = reinterpret_cast<const uint8_t*>(this) + field_offset.Int32Value();
    const kSize* addr = reinterpret_cast<const kSize*>(raw_addr);
    if (kIsVolatile) {
      //如果该成员变量为volatile，则先将addr转换成Atomic<kSize>类型，比如，Atmoic<int32_t>。Atmoic是std atomic的派生类。
        //然后调用它的LoadSequentiallyConsistent函数
      return reinterpret_cast<const Atomic<kSize>*>(addr)->LoadSequentiallyConsistent();
    } else {
      //如果不是volatile类型，则调用Atmoic的LoadJavaData函数
      return reinterpret_cast<const Atomic<kSize>*>(addr)->LoadJavaData();
    }
  }
```
最终我们会借助std atomic来实现volatile变量的读写操作

art/runtime/atomic.h
```
   //读取数据，非volatile
  T LoadJavaData() const {
    return this->load(std::memory_order_relaxed);
  }
//读取volatile型成员变量
  T LoadSequentiallyConsistent() const {
    return this->load(std::memory_order_seq_cst);
  }
  //写入非volatile型成员变量时
  void StoreJavaData(T desired) {
    this->store(desired, std::memory_order_relaxed);
  }
  //写入volatile型成员变量时使用
  void StoreSequentiallyConsistent(T desired) {
    this->store(desired, std::memory_order_seq_cst);
  }
```

setField的逻辑
```
  template<typename kSize, bool kIsVolatile>
  ALWAYS_INLINE void SetField(MemberOffset field_offset, kSize new_value)
      REQUIRES_SHARED(Locks::mutator_lock_) {
    uint8_t* raw_addr = reinterpret_cast<uint8_t*>(this) + field_offset.Int32Value();
    kSize* addr = reinterpret_cast<kSize*>(raw_addr);
    if (kIsVolatile) {
      reinterpret_cast<Atomic<kSize>*>(addr)->StoreSequentiallyConsistent(new_value);
    } else {
      reinterpret_cast<Atomic<kSize>*>(addr)->StoreJavaData(new_value);
    }
  }
```




机器码执行模式的处理
在机器码执行模式下，iget或iput指令会先编译成对应的汇编指令。根据上文对强弱顺序模型的介绍可知，如果成员变量是volatile修饰的话，
x86平台上只要添加一条对应的内存栅指令即可实现内存顺序一致的要求。
由于处理过程类似，本节仅展示x86平台上编译iget指令的核心函数InstructionCode-GeneratorX86
HandleFieldGet，其代码如下所示。
art/compiler/optimizing/code_generator_x86.cc
```
void InstructionCodeGeneratorX86::HandleFieldGet(HInstruction* instruction,
                                                 const FieldInfo& field_info) {
  DCHECK(instruction->IsInstanceFieldGet() || instruction->IsStaticFieldGet());

  LocationSummary* locations = instruction->GetLocations();
  Location base_loc = locations->InAt(0);
  Register base = base_loc.AsRegister<Register>();
  Location out = locations->Out();
  //判断是否为volatile修饰
  bool is_volatile = field_info.IsVolatile();
  //成员变量的类型
  Primitive::Type field_type = field_info.GetFieldType();
  //成员变量位于对象所处的内存的位置
  uint32_t offset = field_info.GetFieldOffset().Uint32Value();

  switch (field_type) {
    case Primitive::kPrimBoolean: {
    /*生成一条movzxb指令，从对象（位置由base决定）指定位置（由offset决定）读取目标成员变量的值到寄存器中。
    movzxb是x86 mov指令系列中的一条。笔者不拟对具体指令的作用展开介绍。读者把它当作mov即可。 */
      __ movzxb(out.AsRegister<Register>(), Address(base, offset));
      break;
    }
   ...//其他基础数据类型，生成对应的mov指令
    case Primitive::kPrimNot: {
      //成员变量的类型是引用类型
      // /* HeapReference<Object> */ out = *(base + offset)
      if (kEmitCompilerReadBarrier && kUseBakerReadBarrier) {
        // Note that a potential implicit null check is handled in this
        // CodeGeneratorX86::GenerateFieldLoadWithBakerReadBarrier call.
        codegen_->GenerateFieldLoadWithBakerReadBarrier(
            instruction, out, base, offset, /* needs_null_check */ true);
        if (is_volatile) {
          codegen_->GenerateMemoryBarrier(MemBarrierKind::kLoadAny);
        }
      } else {
        //生成mov指令
        __ movl(out.AsRegister<Register>(), Address(base, offset));
        codegen_->MaybeRecordImplicitNullCheck(instruction);
        if (is_volatile) {
          //如果是volatile类型，根据CPU的特性，有可能生成fence指令，或者使用对应的//lock指令。
          //笔者不拟讨论CPU架构中关于内存栅的细节。
          //生成kLoadAny类型的内存屏障
          codegen_->GenerateMemoryBarrier(MemBarrierKind::kLoadAny);
        }
        // If read barriers are enabled, emit read barriers other than
        // Baker's using a slow path (and also unpoison the loaded
        // reference, if heap poisoning is enabled).
        codegen_->MaybeGenerateReadBarrierSlow(instruction, out, out, base_loc, offset);
      }
      break;
    }
   ....

  if (is_volatile) {
    if (field_type == Primitive::kPrimNot) {
      // Memory barriers, in the case of references, are also handled
      // in the previous switch statement.
    } else {
      //对于其他非引用型的成员变量，如果是volatile修饰的话，也需要生成内存栅指令
      codegen_->GenerateMemoryBarrier(MemBarrierKind::kLoadAny);
    }
  }
}

void InstructionCodeGeneratorX86::HandleFieldSet(HInstruction* instruction,
                                                 const FieldInfo& field_info,
                                                 bool value_can_be_null) {
  ...
  LocationSummary* locations = instruction->GetLocations();
  Register base = locations->InAt(0).AsRegister<Register>();
  Location value = locations->InAt(1);
  bool is_volatile = field_info.IsVolatile();
  Primitive::Type field_type = field_info.GetFieldType();
  uint32_t offset = field_info.GetFieldOffset().Uint32Value();
  //是否需要写屏障
  bool needs_write_barrier =
      CodeGenerator::StoreNeedsWriteBarrier(field_type, instruction->InputAt(1));

  if (is_volatile) {
    //写前加入kAnyStore
    codegen_->GenerateMemoryBarrier(MemBarrierKind::kAnyStore);
  }

  bool maybe_record_implicit_null_check_done = false;

  switch (field_type) {
    case Primitive::kPrimBoolean:
    case Primitive::kPrimByte: {
      //从寄存器写入内存
      __ movb(Address(base, offset), value.AsRegister<ByteRegister>());
      break;
    }
    ...
    case Primitive::kPrimInt:
    case Primitive::kPrimNot: {
      if (kPoisonHeapReferences && needs_write_barrier) {
        // Note that in the case where `value` is a null reference,
        // we do not enter this block, as the reference does not
        // need poisoning.
        DCHECK_EQ(field_type, Primitive::kPrimNot);
        Register temp = locations->GetTemp(0).AsRegister<Register>();
        __ movl(temp, value.AsRegister<Register>());
        __ PoisonHeapReference(temp);
        __ movl(Address(base, offset), temp);
      } else if (value.IsConstant()) {
        int32_t v = CodeGenerator::GetInt32ValueOf(value.GetConstant());
        __ movl(Address(base, offset), Immediate(v));
      } else {
        DCHECK(value.IsRegister()) << value;
        __ movl(Address(base, offset), value.AsRegister<Register>());
      }
      break;
    }
    ...
  }

  if (!maybe_record_implicit_null_check_done) {
    codegen_->MaybeRecordImplicitNullCheck(instruction);
  }

  if (needs_write_barrier) {
    Register temp = locations->GetTemp(0).AsRegister<Register>();
    Register card = locations->GetTemp(1).AsRegister<Register>();
    codegen_->MarkGCCard(temp, card, base, value.AsRegister<Register>(), value_can_be_null);
  }

  if (is_volatile) {
    //写入KAnyAny的内存屏障
    codegen_->GenerateMemoryBarrier(MemBarrierKind::kAnyAny);
  }
}

```
读    mov读到寄存器   kLoadAny(LoadLoad | LoadStore)    防止后面的普通读和普通写重排到前面
写    kAnyStore(LoadStore | StoreStore)   mov写入内存(读到内存)  kAnyAny(StoreLoad)
//x86只会发生store-load的重排，所以对volatile的处理，在x86平台只有写完时插入lock的内存屏障   防止store重排到其他load的后面

art/compiler/utils/x86/assembler_x86.cc
```
void X86Assembler::movzxb(Register dst, ByteRegister src) {
  AssemblerBuffer::EnsureCapacity ensured(&buffer_);
  EmitUint8(0x0F);
  EmitUint8(0xB6);
  EmitRegisterOperand(dst, src);
}
```



内存屏障
内存屏障的种类
art/compiler/optimizing/nodes.h
```
 * @details We define the combined barrier types that are actually required
 * by the Java Memory Model, rather than using exactly the terminology from
 * the JSR-133 cookbook.  These should, in many cases, be replaced by acquire/release
 * primitives.  Note that the JSR-133 cookbook generally does not deal with
 * store atomicity issues, and the recipes there are not always entirely sufficient.
 * The current recipe is as follows:
 * -# Use AnyStore ~= (LoadStore | StoreStore) ~= release barrier before volatile store.
 * -# Use AnyAny barrier after volatile store.  (StoreLoad is as expensive.)
 * -# Use LoadAny barrier ~= (LoadLoad | LoadStore) ~= acquire barrier after each volatile load.
 * -# Use StoreStore barrier after all stores but before return from any constructor whose
 *    class has final fields.
 * -# Use NTStoreStore to order non-temporal stores with respect to all later
 *    store-to-memory instructions.  Only generated together with non-temporal stores.
 */
enum MemBarrierKind {
  kAnyStore,
  kLoadAny,
  kStoreStore,
  kAnyAny,
  kNTStoreStore,
  kLastBarrierKind = kNTStoreStore
};
```
art/compiler/optimizing/code_generator_x86.cc
```
void CodeGeneratorX86::GenerateMemoryBarrier(MemBarrierKind kind) {
  /*
   * According to the JSR-133 Cookbook, for x86 only StoreLoad/AnyAny barriers need memory fence.
   * All other barriers (LoadAny, AnyStore, StoreStore) are nops due to the x86 memory model.
   * For those cases, all we need to ensure is that there is a scheduling barrier in place.
   */
  switch (kind) {
    case MemBarrierKind::kAnyAny: {
      MemoryFence();
      break;
    }
    case MemBarrierKind::kAnyStore:
    case MemBarrierKind::kLoadAny:
    case MemBarrierKind::kStoreStore: {
      // nop
      break;
    }
    case MemBarrierKind::kNTStoreStore:
      // Non-Temporal Store/Store needs an explicit fence.
      MemoryFence(/* non-temporal */ true);
      break;
  }
}
```
MemoryFence在x86的实现
art/compiler/optimizing/code_generator_x86.h
```
  void MemoryFence(bool non_temporal = false) {
    if (!non_temporal) {
      assembler_.lock()->addl(Address(ESP, 0), Immediate(0));
    } else {
      assembler_.mfence();
    }
  }
```
art/compiler/utils/x86/assembler_x86.cc
```
X86Assembler* X86Assembler::lock() {
  AssemblerBuffer::EnsureCapacity ensured(&buffer_);
  EmitUint8(0xF0);
  return this;
}
void X86Assembler::mfence() {
  AssemblerBuffer::EnsureCapacity ensured(&buffer_);
  EmitUint8(0x0F);
  EmitUint8(0xAE);
  EmitUint8(0xF0);
}
```