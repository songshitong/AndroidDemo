
//todo 这块没看懂，需要手写一下

https://blog.yorek.xyz/android/3rd-library/hprof-shrink/
1
HPROF文件在解决Java内存泄露以及OOM问题上提供了非常大的帮助，但是HPROF文件是非常大的，基本与堆内存占用呈一次线性关系。

所以HPROF文件在上传到服务器时，一般需要经过裁剪、压缩等工作。比如一个 100MB 的文件裁剪后一般只剩下 30MB 左右，使用 7zip 压缩最后小于 10MB，
增加了文件上传的成功率

裁剪分为两大流派：
1 dump之后，对文件进行读取并裁剪的流派：比如Shark、微信的Matrix等
2 dump时直接对数据进行实时裁剪，需要hook数据的写入过程：比如美团的Probe、快手的KOOM等

下面是原始的HPROF经过各种裁剪方案，最后压缩后的文件大小。
       原始大小	裁剪后	zip后	备注
Shark	154MB	154MB	6M
Matrix	154MB	26M	    7M
KOOM	154MB	17M    	3M	裁剪后的文件需要还原
可以看到，HPROF文件的裁剪、压缩过程在上报之前还是非常有必要的。




HPROF文件格式
Android中的HPROF文件基于Java，但是比Java多了一些TAG，内容相较而言更加丰富。

HPROF文件总体由header和若干个record组成，每个record第一个字节TAG表示了该record的类型。

record 我们主要了解一下这些类型：

STRING(0x01)
字符串池，每一条记录包含字符串ID以及字符串文本
LOAD CLASS(0x02)
已经加载过的类，每条记录包含类的序号id（从1开始自增）、类对象的ID、堆栈序号、类名的string ID
HEAP DUMP(0x0c) & HEAP DUMP SEGMENT(0x1c)
两者都是堆信息，格式也都相同，处理时一般一并处理了。里面含有多个子TAG，每个子TAG第一个字节表示其类型


HEAP DUMP、HEAP DUMP SEGMENT里面包含了多个子TAG，这里举出我们需要关心的一些子TAG：
CLASS DUMP(0x20)
 表示了该class里面的字段、superclass等信息
INSTANCE DUMP(0x21)
 表示了该类的实例信息，这块信息里面记录了实例以及引用信息，是我们 一定要保留的内容
OBJECT ARRAY DUMP(0x22)
 顾名思义，就是对象数组的信息
PRIMITIVE ARRAY DUMP(0x23)
 基本类型数组信息，这是我们需要 裁剪掉的的内容。这部分内容占比非常大，且对于我们分析内存泄露引用链没有作用，但是对于分析OOM还有有帮助的。
HEAP DUMP INFO(0xfe)
Android特有的TAG，表明了这块内存空间是位于App、Image还是Zygote Space。在KOOM中，会根据的Space的类型，进行INSTANCE DUMP、OBJECT ARRAY DUMP的裁剪，
所以KOOM的裁剪率更高。

jdk hprof文件格式可以参考：Binary Dump Format (format=b)
https://hg.openjdk.org/jdk8/jdk8/jdk/raw-file/tip/src/share/demo/jvmti/hprof/manual.html#mozTocId848088


Android hprof文件格式没有找到直观的，只能从源码中进行推断：
HprofTag  tag
https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:art/runtime/hprof/hprof.cc
```
enum HprofTag {
  HPROF_TAG_STRING = 0x01,
  HPROF_TAG_LOAD_CLASS = 0x02,
  HPROF_TAG_UNLOAD_CLASS = 0x03,
  HPROF_TAG_STACK_FRAME = 0x04,
  HPROF_TAG_STACK_TRACE = 0x05,
  HPROF_TAG_ALLOC_SITES = 0x06,
  HPROF_TAG_HEAP_SUMMARY = 0x07,
  HPROF_TAG_START_THREAD = 0x0A,
  HPROF_TAG_END_THREAD = 0x0B,
  HPROF_TAG_HEAP_DUMP = 0x0C,
  HPROF_TAG_HEAP_DUMP_SEGMENT = 0x1C,
  HPROF_TAG_HEAP_DUMP_END = 0x2C,
  HPROF_TAG_CPU_SAMPLES = 0x0D,
  HPROF_TAG_CONTROL_SETTINGS = 0x0E,
};
...
```
ProcessHeap 处理过程
https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:art/runtime/hprof/hprof.cc;bpv=1;bpt=1;l=507?gsn=ProcessHeap&gs=kythe%3A%2F%2Fandroid.googlesource.com%2Fplatform%2Fsuperproject%3Flang%3Dc%252B%252B%3Fpath%3Dart%2Fruntime%2Fhprof%2Fhprof.cc%23MVm3ci-wBCgQyEbyiabRtMoeEum9pXdflYNedCOTiV8
```
  void ProcessHeap(bool header_first)
      REQUIRES(Locks::mutator_lock_) {
    // Reset current heap and object count.
    current_heap_ = HPROF_HEAP_DEFAULT;
    objects_in_segment_ = 0;
....
      ProcessHeader(true);
      ProcessBody();
    ...
  }
```

header格式如下，共31byte：
         格式版本号	                                                          0x00	identifier大小	timestamp
占byte数	  18 byte	                                                          1 byte	4 byte	          8 byte
16进制示例 4A 41 56 41 20 50 52 4F 46 49 4C 45 20 31 2E 30 2E 33	              00	00 00 00 04	     00 00 01 81 A3 25 DD 52
含义	     JAVA PROFILE 1.0.3		                                                      4	             1656299576658

每条record可以分为公共的部分以及body，公共的部分为9byte：
             TAG	    相较于header里面时间戳的时间	body长度{n}	   body
占byte数      1 byte     4 byte	                    4 byte	      n byte
16进制示例	01	        00 00 00 00	               00 00 00 10	  00 40 05 9D 24 24 64 65 6C 65 67 61 74 65 5F 30
含义	       TAG值为1	    time为0                 	body有16字节	   表示ID为0x0040059d，文本为$$delegate_0的字符串
在上面record的例子中，由于TAG为01，所以body的解析按照STRING来。
STRING由4byte的ID以及后面的字符串组成，ID的byte数由header中的identifier大小决定，所以取4个byte为0x0040059d。
剩下的16-4=12个byte（24 24 64 65 6C 65 67 61 74 65 5F 30）解码成UTF8即为$$delegate_0。

其他类型的Record，我们按照固有格式，也能对应解析出来。


下面以Matrix和快手KOOM方案为例，来了解一下具体的两种裁剪方案，看看两者的异同
Matrix裁剪方案
Matrix裁剪方案代表的是典型的先dump后裁剪的流派，该流派中规中矩，没有native hook这种黑科技，兼容性较好。但是DUMP过程可能会比较久，
会对用户体验影响比较大，而且也容易引发二次崩溃。

该方案源码可见HprofBufferShrinker.java
matrix-resource-canary\matrix-resource-canary-android\src\main\java\com\tencent\matrix\resource\hproflib\HprofBufferShrinker.java
Matrix裁剪时，首先利用HprofReader来解析hprof文件，然后分别调用HprofInfoCollectVisitor、HprofKeptBufferCollectVisitor、
HprofBufferShrinkVisitor这三个Visitor来完成hprof的裁剪流程，最后通过HprofWriter重写hprof。这是一个典型的访问者模式2了：
```
 public void shrink(File hprofIn, File hprofOut) throws IOException {
        FileInputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(hprofIn);
            os = new BufferedOutputStream(new FileOutputStream(hprofOut));
            final HprofReader reader = new HprofReader(new BufferedInputStream(is));
            reader.accept(new HprofInfoCollectVisitor());
            // Reset.
            is.getChannel().position(0);
            reader.accept(new HprofKeptBufferCollectVisitor());
            // Reset.
            is.getChannel().position(0);
            reader.accept(new HprofBufferShrinkVisitor(new HprofWriter(os)));
        } ...
    }
```


HprofInfoCollectVisitor
首先看看HprofInfoCollectVisitor。顾名思义，该类主要起收集信息的作用：
访问到header时：记录identifier的byte数
matrix-resource-canary\matrix-resource-canary-android\src\main\java\com\tencent\matrix\resource\hproflib\HprofBufferShrinker.java
```
 @Override
        public void visitHeader(String text, int idSize, long timestamp) {
            mIdSize = idSize;
            mNullBufferId = ID.createNullID(idSize);
        }
```
访问String时：保存Bitmap类及其mBuffer、mRecycled字段的字符串id，保存String类及其value字段的字符串id
```
 @Override
        public void visitStringRecord(ID id, String text, int timestamp, long length) {
            if (mBitmapClassNameStringId == null && "android.graphics.Bitmap".equals(text)) {
                mBitmapClassNameStringId = id;
            } else if (mMBufferFieldNameStringId == null && "mBuffer".equals(text)) {
                mMBufferFieldNameStringId = id;
            } else if (mMRecycledFieldNameStringId == null && "mRecycled".equals(text)) {
                mMRecycledFieldNameStringId = id;
            } else if (mStringClassNameStringId == null && "java.lang.String".equals(text)) {
                mStringClassNameStringId = id;
            } else if (mValueFieldNameStringId == null && "value".equals(text)) {
                mValueFieldNameStringId = id;
            }
        }
```
访问LOAD CLASS时，根据字符串id匹配并保存Bitmap类、String类的id
```
 @Override
        public void visitLoadClassRecord(int serialNumber, ID classObjectId, int stackTraceSerial, ID classNameStringId, int timestamp, long length) {
            if (mBmpClassId == null && mBitmapClassNameStringId != null && mBitmapClassNameStringId.equals(classNameStringId)) {
                mBmpClassId = classObjectId;
            } else if (mStringClassId == null && mStringClassNameStringId != null && mStringClassNameStringId.equals(classNameStringId)) {
                mStringClassId = classObjectId;
            }
        }
```

访问HEAP DUMP、HEAP DUMP SEGMENT的CLASS DUMP时：根据两个类的id进行匹配，保存Bitmap、String类里面的instance fields（可以理解为字段）
```
 @Override
        public HprofHeapDumpVisitor visitHeapDumpRecord(int tag, int timestamp, long length) {
            return new HprofHeapDumpVisitor(null) {
                @Override
                public void visitHeapDumpClass(ID id, int stackSerialNumber, ID superClassId, ID classLoaderId, int instanceSize, Field[] staticFields, Field[] instanceFields) {
                    if (mBmpClassInstanceFields == null && mBmpClassId != null && mBmpClassId.equals(id)) {
                        mBmpClassInstanceFields = instanceFields;
                    } else if (mStringClassInstanceFields == null && mStringClassId != null && mStringClassId.equals(id)) {
                        mStringClassInstanceFields = instanceFields;
                    }
                }
            };
        }
```
该类收集的信息主要是Bitmap.mBuffer和String.value，这两个字段都是基本类型数组，后面是可以剔除的。
当然，这里注意一下Bitmap.mBuffer在Android 8.0及以后就不在Java Heap中了



HprofKeptBufferCollectVisitor
HprofKeptBufferCollectVisitor保存了Bitmap的buffer id数据、String的value id数据，以及基本类型数据的id -> 值之间的映射关系：
访问到子TAG INSTANCE DUMP时：根据之前访问CLASS DUMP时保存的字段信息，解析出感兴趣的值。
 若是Bitmap对象，且mRecycled不为true，则保存bufferId到mBmpBufferIds 
 若是String对象，保存valueId到mStringValueIds
com/tencent/matrix/resource/hproflib/HprofBufferShrinker.java
```
  @Override
        public HprofHeapDumpVisitor visitHeapDumpRecord(int tag, int timestamp, long length) {
            return new HprofHeapDumpVisitor(null) {

                @Override
                public void visitHeapDumpInstance(ID id, int stackId, ID typeId, byte[] instanceData) {
                    try {
                        if (mBmpClassId != null && mBmpClassId.equals(typeId)) {
                            ID bufferId = null;
                            Boolean isRecycled = null;
                            final ByteArrayInputStream bais = new ByteArrayInputStream(instanceData);
                            for (Field field : mBmpClassInstanceFields) {
                                final ID fieldNameStringId = field.nameId;
                                final Type fieldType = Type.getType(field.typeId);
                                ....
                                if (mMBufferFieldNameStringId.equals(fieldNameStringId)) {
                                    bufferId = (ID) IOUtil.readValue(bais, fieldType, mIdSize);
                                } else if (mMRecycledFieldNameStringId.equals(fieldNameStringId)) {
                                    isRecycled = (Boolean) IOUtil.readValue(bais, fieldType, mIdSize);
                                } else if (bufferId == null || isRecycled == null) {
                                    IOUtil.skipValue(bais, fieldType, mIdSize);
                                } else {
                                    break;
                                }
                            }
                            bais.close();
                            final boolean reguardAsNotRecycledBmp = (isRecycled == null || !isRecycled);
                            //保存isRecycled为false的
                            if (bufferId != null && reguardAsNotRecycledBmp && !bufferId.equals(mNullBufferId)) {
                                mBmpBufferIds.add(bufferId);
                            }
                        } else if (mStringClassId != null && mStringClassId.equals(typeId)) {
                            ID strValueId = null;
                            final ByteArrayInputStream bais = new ByteArrayInputStream(instanceData);
                            for (Field field : mStringClassInstanceFields) {
                                final ID fieldNameStringId = field.nameId;
                                final Type fieldType = Type.getType(field.typeId);
                                ...
                                if (mValueFieldNameStringId.equals(fieldNameStringId)) {
                                    strValueId = (ID) IOUtil.readValue(bais, fieldType, mIdSize);
                                } else if (strValueId == null) {
                                    IOUtil.skipValue(bais, fieldType, mIdSize);
                                } else {
                                    break;
                                }
                            }
                            bais.close();
                            if (strValueId != null && !strValueId.equals(mNullBufferId)) {
                                mStringValueIds.add(strValueId);
                            }
                        }
                    } ...
                }              
            };
        }
```
访问到子TAG PRIMITIVE ARRAY DUMP时，保存数组对象id与对应的byte[] elements到mBufferIdToElementDataMap中备用
```
@Override
public void visitHeapDumpPrimitiveArray(int tag, ID id, int stackId, int numElements, int typeId, byte[] elements) {
    // Map<ID, byte[]>
    mBufferIdToElementDataMap.put(id, elements);
}
```

hprof文件解析结束时，根据基本数据类型数组id在不在mBmpBufferIds中过滤mBufferIdToElementDataMap，这样留下来的都是Bitmap里面的buffer数据了。
然后将剩下的数据做md5，根据md5判断Bitmap像素数据是否有重复，若有重复，保存 重复id -> 重复id 和 此次id -> 重复id 
这两组kv关系到mBmpBufferIdToDeduplicatedIdMap中。
```
 @Override
        public void visitEnd() {
            final Set<Map.Entry<ID, byte[]>> idDataSet = mBufferIdToElementDataMap.entrySet();
            final Map<String, ID> duplicateBufferFilterMap = new HashMap<>();
            for (Map.Entry<ID, byte[]> idDataPair : idDataSet) {
                final ID bufferId = idDataPair.getKey();
                final byte[] elementData = idDataPair.getValue();
                if (!mBmpBufferIds.contains(bufferId)) {
                    // Discard non-bitmap buffer.
                    continue;
                }
                final String buffMd5 = DigestUtil.getMD5String(elementData);
                final ID mergedBufferId = duplicateBufferFilterMap.get(buffMd5);
                if (mergedBufferId == null) {
                    duplicateBufferFilterMap.put(buffMd5, bufferId);
                } else {
                    mBmpBufferIdToDeduplicatedIdMap.put(mergedBufferId, mergedBufferId);
                    mBmpBufferIdToDeduplicatedIdMap.put(bufferId, mergedBufferId);
                }
            }
            // Save memory cost.
            mBufferIdToElementDataMap.clear();
        }
```
这一步操作的结果保存在mStringValueIds、mBmpBufferIdToDeduplicatedIdMap中。前者表示字符串value的id集合，后者用来将Bitmap的buffer进行去重处理


HprofBufferShrinkVisitor
HprofBufferShrinkVisitor毫无疑问是真正进行裁剪的步骤了。

对于需要进行裁剪的数据，可以直接return处理，这样文件重新写入的时候这部分数据就不会进行写入了，这与ASM中的操作一样，两者也都是访问者模式的设计风格。

在访问子TAG INSTANCE DUMP时：若是Bitmap对象，解析出bufferId后看看是不是有可以重用的数据（mBmpBufferIdToDeduplicatedIdMap），若有则替换。
所以Matrix并没有完全剔除Bitmap里面的buffer数据。而且一般来说，裁剪时INSTANCE DUMP可以完全忽略，这里Matrix为了剔除重复的buffer数据，
才处理了这部分数据。
```
 @Override
        public HprofHeapDumpVisitor visitHeapDumpRecord(int tag, int timestamp, long length) {
            return new HprofHeapDumpVisitor(super.visitHeapDumpRecord(tag, timestamp, length)) {
                @Override
                public void visitHeapDumpInstance(ID id, int stackId, ID typeId, byte[] instanceData) {
                    try {
                        if (typeId.equals(mBmpClassId)) {
                            ID bufferId = null;
                            int bufferIdPos = 0;
                            final ByteArrayInputStream bais = new ByteArrayInputStream(instanceData);
                            for (Field field : mBmpClassInstanceFields) {
                                final ID fieldNameStringId = field.nameId;
                                final Type fieldType = Type.getType(field.typeId);
                                if (fieldType == null) {
                                    throw new IllegalStateException("visit instance failed, lost type def of typeId: " + field.typeId);
                                }
                                if (mMBufferFieldNameStringId.equals(fieldNameStringId)) {
                                    bufferId = (ID) IOUtil.readValue(bais, fieldType, mIdSize);
                                    break;
                                } else {
                                    //跳过非bitmap.mBuffer相关的
                                    bufferIdPos += IOUtil.skipValue(bais, fieldType, mIdSize);
                                }
                            }
                            if (bufferId != null) {
                                final ID deduplicatedId = mBmpBufferIdToDeduplicatedIdMap.get(bufferId);
                                if (deduplicatedId != null && !bufferId.equals(deduplicatedId) && !bufferId.equals(mNullBufferId)) {
                                    //将deduplicatedId的数据保存到instanceData
                                    modifyIdInBuffer(instanceData, bufferIdPos, deduplicatedId);
                                }
                            }
                        }
                    }...
                }

                private void modifyIdInBuffer(byte[] buf, int off, ID newId) {
                    final ByteBuffer bBuf = ByteBuffer.wrap(buf);
                    bBuf.position(off);
                    bBuf.put(newId.getBytes());
                }
```

在访问到子TAG PRIMITIVE ARRAY DUMP时，只保留重复的Bitmap bufferId所对应的数据以及String的value数据。
```
@Override
    public void visitHeapDumpPrimitiveArray(int tag, ID id, int stackId, int numElements, int typeId, byte[] elements) {
        final ID deduplicatedID = mBmpBufferIdToDeduplicatedIdMap.get(id);
        // Discard non-bitmap or duplicated bitmap buffer but keep reference key.
        // 为null的情况：不是buffer数据；或者是独一份的buffer数据
        // ID不相等的情况：buffer A与buffer B md5一致，但保留起来的是A，这里id却为B，因此B应该要被替换为A，B的数据要被删除
        if (deduplicatedID == null || !id.equals(deduplicatedID)) {
            if (!mStringValueIds.contains(id)) {
                return;
            }
        }
        super.visitHeapDumpPrimitiveArray(tag, id, stackId, numElements, typeId, elements);
    }
```
Matrix方案裁剪hprof文件时，裁剪的是HEAP_DUMP、HEAP_DUMP_SEGMENT里面的PRIMITIVE_ARRAY_DUMP段。该方案仅仅会保存字符串的数据以及重复的那一份Bitmap的buffer数据，
其他基本类型数组会被剔除。



KOOM裁剪方案 todo
hook了数据的io过程，在写入时对数据流进行裁剪，一步到位。且快手的KOOM在DUMP时采取了fork的形式，利用了Copy-On-Write(COW)机制，对主进程的影响更小。
KOOM方案仅仅针对HEAP DUMP、HEAP DUMP SEGMENT进行处理。没有Matrix那种保留Bitmap buffer以及String value的意思。
KOOM在dump时会传入文件路径，在文件open的hook回调中根据文件路径进行匹配，匹配成功之后记录下文件的fd。在内容写入时匹配fd，
 这样就可以精准拿到hprof写入时的内容了。