
https://www.jianshu.com/p/f4837fd9b3b4
什么是Protobuf
protobuf 是google开源的一个序列化框架，类似xml，json。但是它存储的方式是二进制
总长度-> key的长度 -> key的内容 -> value的长度 -> value的内容 -> key的长度 -> key的内容 -> value的长度 -> value的内容 ....
(int:4字节 最大能存2的32次方)

ProtoBuf存储结构是总长度->key的长度->key的内容->value的长度->value的内容...
总长度我们可以用int来存储，也就是4个字节进行存储
key的长度实际就是字符串的长度(我们定义key只能是字符串)

PortoBuf写入方式
一个字节有8位，我们将后7位用来保存数据，第一位用来判断是否还有字节，如果没有则为0，如果有则为1
如何判断当前是否还有字节？
因为我们只保存后7位字节，而7位字节全是1的是7F，所以当我们的数大于7F则表示我们还有字节  //16进制
  //没有数据0000 0000->0111 1111=7F   还有数据1000 0000=80

当我们的数据大于7F,如何存储？
我们以5201314数据进行分析,首先将5201314转成字节码0100 1111 0101 1101 1010 0010   //16进制4F5DA2
1、当前数据大于7F,我们先取最低的七位，也就是010 0010，第一位补1，则数据是1010 0010写入文件
2、5201314右移动7F，左边不足补0，原数据则变成000 0000 1001 1110 1011 1011(大于7F)  //16进制9EBB
3、取出最低七位,1011 1011写入文件。     //16进制BB
4、5201314右移动7F，左边不足补0，原数据则变成000 0000 0000 0001 0011 1101
5、取出最低七位,1011 1101写入文件。原数据则变成000 0000 0000 0000 0000 0010    //1011 1101的16进制BD
6、这时候数据小于07F，则直接将0000 0010写入文件，结束  //16进制=2
上述步骤之后拿到数据
```
1、1010 0010
2、1011 1011
3、1011 1101
4、0000 0010
```

既然已经将数据存储了，那如何取出数据？
1、我们将0000 0010拼接到1011 1101之前，因为1011 1101中之后后七位是有效数据，所以第一位需要去掉首位，此时的原数据就是0000 0010 0011 1101
  //(1)011 1101->011 1101 注意是去掉首位，因为之前的是补位的      拼接0000 0010后是0000 0010 011 1101         
2、依次推论，将上面拼好的数据放到1011 1011之前，得到数据0000 0010 011 1101 011 1011
3、再将上面拼好的数据放到1010 0010之前，得到数据0 0000 0100 1111 0101 1101 1 010 0010
4、去除无效位数0，也就还原了原来的数据0100 1111 0101 1101 1010 0010


代码实现
上面写入方式了解之后，看起来还是挺简单，但是代码怎么写呢？

1、我们现在写入一个int的数据，怎么获取它的大小?
7F的字节码是0111 1111，也就是说第一位是1就代表需要两个字节来存。因此我们可以让我们当前的value&(0xFFFFFFFF<<7)，判断是否等于0，
如果等于0则表示需要一个字节就可以    
//总共64位   0xFFFFFFFF&x，结果每一位是否为1取决于x   0xFFFFFFFF<<7&x，结果后6位肯定为0,前面的结果取决于x 
判断110需要几个字节
```
    1111 1111 1111 1111 1111 1111 1000 0000            (0xffffffff<<7)
&   0000 0000 0000 0000 0000 0000 0110 1110            (110)
=   0000 0000 0000 0000 0000 0000 0000 0000            0
```

假设我们的value现在是150，因为值已经大于0x7F(也就是上述不成立)，这时候我们需要将value&(0xFFFFFFFF<<14),如果等于0则表示需要2个字节
```
    1111 1111 1111 1111 1100 0000 0000 0000            (0xffffffff<<14)
&   0000 0000 0000 0000 0000 0000 1001 0110            (150)
=   0000 0000 0000 0000 0000 0000 1000 0000            0
```
以此推论，最终我们可以写出如下代码
```
int32_t ProtoBuf::computeInt32Size(int32_t value) {
    //0xffffffff 表示 uint 最大值
    //<< 7 则低7位变成0 与上value
    //如果value只要7位就够了则=0,编码只需要一个字节，否则进入其他判断
    if ((value & (0xffffffff << 7)) == 0) {
        return 1;
    } else if ((value & (0xffffffff << 14)) == 0) {
        return 2;
    } else if ((value & (0xffffffff << 21)) == 0) {
        return 3;
    } else if ((value & (0xffffffff << 28)) == 0) {
        return 4;
    }
    return 5;
}
```
2、我们现在存一个key和value的数据，应该怎么计算它的大小
key的长度 -> key的内容 -> value的长度 -> value的内容
首先key的长度其实也就是     //算出key类型的长度
```
int32_t keyLength = key.length();
```
然后保存key的长度+key内容的长度:    //ProtoBuf::computeInt32Size(keyLength)是算出key类型的内容最大占几个字节
```
 int32_t size = keyLength + ProtoBuf::computeInt32Size(keyLength);
```
value的长度+value内容的长度

所以获取key+value大小的完整代码
```
int32_t ProtoBuf::computeItemSize(std::string key, ProtoBuf *value) {
    int32_t keyLength = key.length();
    // 保存key的长度与key数据需要的字节
    int32_t size = keyLength + ProtoBuf::computeInt32Size(keyLength);
    // 加上保存value的长度与value数据需要的字节
    size += value->length() + ProtoBuf::computeInt32Size(value->length());
    return size;
}
```


如何写入数据
我们上面分析了写入方式，那么我们现在直接假设写入的key数据的长度是字符串110，因为110小于0x7F所以直接写入，则直接写入即可
```
 if (value <= 0x7f) {
            writeByte(value);
            return;
 }
void ProtoBuf::writeByte(int8_t value) {
    if (m_position == m_size) {
        //满啦，出错啦
        return;
    }
    //将byte放入数组
    m_buf[m_position++] = value;
}
```

如果key数据的长度是字符串150，因为此时大于0x7f，将150转成字符串 1001 1000 ，首先记录低七位
```
(value & 0x7F)
```
将第一位的数据变成1,再移除低7位
```
writeByte((value & 0x7F) | 0x80);
//7位已经写完了，处理更高位的数据
value >>= 7;
```
原理如下
```
         0111 1111            (0x7F)
&        1001 1000             (150)
=        0001 1000              //记录低7位
|        1000 0000             (0X80)
=        1001 1000             //高位补1,代表还有数据
```
此时key的长度已经全部写完，那key的内容怎么写呢，其实也很简单，直接将key的内容拷贝到数组就可以了
```
  memcpy(m_buf + m_position, data->getBuf(), numberOfBytes);
```
因此写入string数据的完整代码可以写成如下
```
void ProtoBuf::writeByte(int8_t value) {
    if (m_position == m_size) {
        //满啦，出错啦
        return;
    }
    //将byte放入数组
    m_buf[m_position++] = value;
}

void ProtoBuf::writeRawInt(int32_t value) {
    while (true) {
        //每次处理7位数据，如果写入的数据 <= 0x7f（7位都是1）那么使用7位就可以表示了
        if (value <= 0x7f) {
            writeByte(value);
            return;
        } else {
            //大于7位，则先记录低7位，并且将最高位置为1
            //1、& 0x7F 获得低7位数据
            //2、| 0x80 让最高位变成1，表示超过1个字节记录整个数据
            writeByte((value & 0x7F) | 0x80);
            //7位已经写完了，处理更高位的数据
            value >>= 7;
        }
    }
}
void ProtoBuf::writeString(std::string value) {
    size_t numberOfBytes = value.size();
    writeRawInt(numberOfBytes);
    //key的内容拷贝
    memcpy(m_buf + m_position, value.data(), numberOfBytes);
    m_position += numberOfBytes;
}
```

如何读取数据？
如果当前的最高位，也就是第一位是0，则表示是一个字节，直接返回就可以
```
    if ((tmp >> 7) == 0) {
        return tmp;
    }
```
如果最高位1代表还有数据，我们首先读取低7位的数据
```
 int32_t result = tmp & 0x7f;
```
再读取一个字节，将后面的读取到字节左移7位拼接到上一个数据的低7位
```
int32_t ProtoBuf::readInt() {
    uint8_t tmp = readByte();
    //最高1位为0  这个字节是一个有效int。
    if ((tmp >> 7) == 0) {
        return tmp;
    }
    //获得低7位数据
    int32_t result = tmp & 0x7f;
    int32_t i = 1;
    do {
        //再读一个字节
        tmp = readByte();
        if (tmp < 0x80) {
            //读取后一个字节左移7位再拼上前一个数据的低7位
            result |= tmp << (7 * i);
        } else {
            result |= (tmp & 0x7f) << (7 * i);
        }
        i++;
    } while (tmp >= 0x80);
    return result;
}
int8_t ProtoBuf::readByte() {
    if (m_position == m_size) {
        return 0;
    }
    return m_buf[m_position++];
}
```
完整代码
https://github.com/Peakmain/Video_Audio/blob/master/app/src/main/cpp/src/mmkv/ProtoBuf.cpp

页、页框、页表
基本概念
CPU执行一个进程的时候，都会访问内存
但是并不是直接访问物理内存地址，而是通过虚拟地址访问物理内存地址
页：将进程分配的虚拟地址空间划分成的块，对应的大小叫做页面的大小
页框：将物理地址划分的块
页表：记录每一对页和页框的映射关系
页面大小是4k，或者4k的整数倍

