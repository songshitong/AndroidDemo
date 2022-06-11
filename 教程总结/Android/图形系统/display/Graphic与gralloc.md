
/frameworks/native/libs/ui/GraphicBuffer.cpp
```
GraphicBuffer::GraphicBuffer()
      : BASE(), mOwner(ownData), mBufferMapper(GraphicBufferMapper::get()),
        mInitCheck(NO_ERROR), mId(getUniqueId()), mGenerationNumber(0)
  {
      //宽高
      width  =
      height =
      stride =
      //格式,可能是yuv,也可能是其他例如rgb
      format =
      usage_deprecated = 0;
      usage  = 0;
      layerCount = 0;
      handle = NULL;
  }
```