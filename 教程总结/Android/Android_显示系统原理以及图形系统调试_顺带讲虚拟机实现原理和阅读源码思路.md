
虚拟框架微信视频号    android 8.1
问题:太极APP打开显示黑屏
要知道surfaceflinger与bufferqueue在进程共享内存 由gralloc分配 
APP通过egl的open gl 拓展绘制到graphic buffer的显存中
fence机制用来同步 hwcompser

需要case by case定位 猜测APP画出来就是黑的,surfaceflinger某些图层没绘制出来,送显的时候有问题?
APP不掉用opengl,surface,在一个稳定的Android环境,几乎可以肯定不是APP的问题

排查 graphic buffer对象  非常重要
eglCreateClientImage使gb在cpu和gpu之间共享

将gb存为一张图片,确定是否正常,调试整个图形系统挺麻烦的,图像是一个二维数组,根本没法看
gb没有规定图像格式,可能是yuv,rgb等

突破点:android的截屏是如何将gb存成一张图呢 搜索screencap  
/frameworks/base/cmds/screencap/screencap.cpp
screencap的命令的源码
```
 static void usage(const char* pname)
 {
     fprintf(stderr,
             "usage: %s [-hp] [-d display-id] [FILENAME]\n"
             "   -h: this message\n"
             "   -p: save the file as a png.\n"
             "   -d: specify the display id to capture, default %d.\n"
             "If FILENAME ends with .png it will be saved as a png.\n"
             "If FILENAME is not given, the results will be printed to stdout.\n",
             pname, DEFAULT_DISPLAY_ID
     );
 }
```
命令太通用了,搜出来一大堆,怎么整
可以搜索命令的帮助文档   可以搜索specify the physical display ID to capture
进入adb shell
screencap -help    //不要只使用screencap命令,有一堆乱码
```
usage: screencap [-hp] [-d display-id] [FILENAME]
   -h: this message
   -p: save the file as a png.
   -d: specify the physical display ID to capture (default: 4630946829234430593)
       see "dumpsys SurfaceFlinger --display-id" for valid display IDs.
If FILENAME ends with .png it will be saved as a png.
If FILENAME is not given, the results will be printed to stdout
```
问题 命令执行在Android12,输出的文档是12的,搜索的源码是Android8.0 可能搜不到
在Android8.0尝试命令 
搜索*save the file as a png.*   帮助文档一般是独一无二的

/frameworks/base/cmds/screencap/screencap.cpp
从main函数开始,前面是解析参数,跳过   一开始不要关注细节
```
int main(int argc, char** argv)
  {
   ...
 //初始binder  重要代码  
ProcessState::self()->setThreadPoolMaxThreadCount(0);
ProcessState::self()->startThreadPool();
... 
//拿到displayID
sp<IBinder> display = SurfaceComposerClient::getBuiltInDisplay(displayId);
...
 //根据英文 更新屏幕代码截图
 status_t result = screenshot.update(display, Rect(),
              0 /* reqWidth */, 0 /* reqHeight */,
              INT32_MIN, INT32_MAX, /* all layers */
              false, captureOrientation);
   if (result == NO_ERROR) {
          //拿到像素  还有一些配置   base是一个指针,代表一块内存
          base = screenshot.getPixels();
          w = screenshot.getWidth();
          h = screenshot.getHeight();
          s = screenshot.getStride();
          f = screenshot.getFormat();
          d = screenshot.getDataSpace();
          size = screenshot.getSize();
      }    
   
   if (base != NULL) {
          if (png) {
              const SkImageInfo info =
                  SkImageInfo::Make(w, h, flinger2skia(f), kPremul_SkAlphaType,
                      dataSpaceToColorSpace(d));
              //思考base是一块内存,为什么skia可以将SkImageInfo存为png,此时需要知道screenshot.getPixels的内容,查找graphicbuffer        
              SkPixmap pixmap(info, base, s * bytesPerPixel(f));
              struct FDWStream final : public SkWStream {
                size_t fBytesWritten = 0;
                int fFd;
                FDWStream(int f) : fFd(f) {}
                size_t bytesWritten() const override { return fBytesWritten; }
                bool write(const void* buffer, size_t size) override {
                  fBytesWritten += size;
                  return size == 0 || ::write(fFd, buffer, size) > 0;
                }
              } fdStream(fd);
              (void)SkEncodeImage(&fdStream, pixmap, SkEncodedImageFormat::kPNG, 100);
              if (fn != NULL) {
                  notifyMediaScanner(fn);
              }   
          } else {
             //不是png直接写入文件   为什么内存可以直接写入文件,写入的格式是什么呢,jpg,yuv
              uint32_t c = dataSpaceToInt(d);
              write(fd, &w, 4);
              write(fd, &h, 4);
              write(fd, &f, 4);
              write(fd, &c, 4);
              size_t Bpp = bytesPerPixel(f);
              for (size_t y=0 ; y<h ; y++) {
                  write(fd, base, w*Bpp);
                  base = (void *)((char *)base + s*Bpp);
              }
          }          
}
```
开始看screenshot.update  点击ScreenshotClient
/frameworks/native/libs/gui/SurfaceComposerClient.cpp
```
status_t ScreenshotClient::update(const sp<IBinder>& display,
          Rect sourceCrop, uint32_t reqWidth, uint32_t reqHeight,
          int32_t minLayerZ, int32_t maxLayerZ,
          bool useIdentityTransform, uint32_t rotation) {   
    sp<ISurfaceComposer> s(ComposerService::getComposerService());
      if (s == NULL) return NO_INIT;
      //初始化 CpuConsumer
      sp<CpuConsumer> cpuConsumer = getCpuConsumer();
  
      if (mHaveBuffer) {
          //如果有一块内存了,解锁了
          mCpuConsumer->unlockBuffer(mBuffer);
          memset(&mBuffer, 0, sizeof(mBuffer));
          mHaveBuffer = false;
      }
      //截屏
      status_t err = s->captureScreen(display, mProducer, sourceCrop,
              reqWidth, reqHeight, minLayerZ, maxLayerZ, useIdentityTransform,
              static_cast<ISurfaceComposer::Rotation>(rotation));
  
      if (err == NO_ERROR) {
          //如果没有错误,将数据buffer放入lockNextBuffer
          err = mCpuConsumer->lockNextBuffer(&mBuffer);
          if (err == NO_ERROR) {
              mHaveBuffer = true;
          }
      }
      return err;
  }
}
```
看一下getPixels
```
void const* ScreenshotClient::getPixels() const {
      return mBuffer.data;
  }
```
可知像素数据是一个指针,是谁在更新mBuffer呢
lockNextBuffer   为什么不是captureScreen,仅仅是个截图,没做数据修改
/frameworks/native/libs/gui/CpuConsumer.cpp
```
status_t CpuConsumer::lockNextBuffer(LockedBuffer *nativeBuffer) {
  //判断什么玩意 不管
  //加个锁什么的,不管
  ...
  //出现了重要的GraphicBuffer  拿到像素格式
   PixelFormat format = mSlots[slot].mGraphicBuffer->getPixelFormat();
   if (isPossiblyYUV(format)) {
      //yuv格式
          if (b.mFence.get()) {  //fence可能是一个同步内存的文件描述符
              err = mSlots[slot].mGraphicBuffer->lockAsyncYCbCr(
                  GraphicBuffer::USAGE_SW_READ_OFTEN,
                  b.mCrop,
                  &ycbcr,
                  b.mFence->dup());
          } else {
              err = mSlots[slot].mGraphicBuffer->lockYCbCr(
                  GraphicBuffer::USAGE_SW_READ_OFTEN,
                  b.mCrop,
                  &ycbcr);
          }
          if (err == OK) {
              //重要的来了  将ycbcr.y存入到bufferPointer
              bufferPointer = ycbcr.y;
              flexFormat = HAL_PIXEL_FORMAT_YCbCr_420_888;
              if (format != HAL_PIXEL_FORMAT_YCbCr_420_888) {
                  CC_LOGV("locking buffer of format %#x as flex YUV", format);
              }
          } else if (format == HAL_PIXEL_FORMAT_YCbCr_420_888) {
              CC_LOGE("Unable to lock YCbCr buffer for CPU reading: %s (%d)",
                      strerror(-err), err);
              return err;
          }
      }
      ...
      //非yuv的处理
      ...
      //对传进来的nativeBuffer的属性各种赋值,重要的是data与bufferPointer
      //所以图像数据就是bufferPointer,由lockAsyncYCbCr生成
      nativeBuffer->data   =
              reinterpret_cast<uint8_t*>(bufferPointer);
}
```
怎么把gb存为一张图片就有结果了 bufferPointer是对应的数据
如果是yuv 调用mGraphicBuffer->lockAsyncYCbCr
不是yuv 调用mGraphicBuffer->lockAsync
生成图片 抄screencap的代码
如果是png  缺点,需要增加skia的依赖,更加复杂
```
 const SkImageInfo info =
                  SkImageInfo::Make(w, h, flinger2skia(f), kPremul_SkAlphaType,
                      dataSpaceToColorSpace(d));
              SkPixmap pixmap(info, base, s * bytesPerPixel(f));
              struct FDWStream final : public SkWStream {
                size_t fBytesWritten = 0;
                int fFd;
                FDWStream(int f) : fFd(f) {}
                size_t bytesWritten() const override { return fBytesWritten; }
                bool write(const void* buffer, size_t size) override {
                  fBytesWritten += size;
                  return size == 0 || ::write(fFd, buffer, size) > 0;
                }
              } fdStream(fd);
              (void)SkEncodeImage(&fdStream, pixmap, SkEncodedImageFormat::kPNG, 100);
              if (fn != NULL) {
                  notifyMediaScanner(fn);
              }
```
如果不是png
```
 uint32_t c = dataSpaceToInt(d);
              write(fd, &w, 4);
              write(fd, &h, 4);
              write(fd, &f, 4);
              write(fd, &c, 4);
              size_t Bpp = bytesPerPixel(f);
              for (size_t y=0 ; y<h ; y++) {
                  write(fd, base, w*Bpp);
                  base = (void *)((char *)base + s*Bpp);
              }
```
自己的问题一般是在绘制图层或者合成图层存在问题
surfaceflinger代码很复杂  新手可以自己跟一下,锻炼跟代码能力,老手对framework比较熟悉,直接google,看网上的流程,甄别靠谱的
  顺着别人的逻辑,成功的概率会高一点
图像绘制在Layer  一个layer是一个图层
想要的图层,正好Layer有个dump方法,android系统开发者也经常遇到surfaceflinger问题,需要dump
/frameworks/native/services/surfaceflinger/Layer.cpp
```
void Layer::dump(String8& result, Colorizer& colorizer) const
  {
      const Layer::State& s(getDrawingState());
  
      colorizer.colorize(result, Colorizer::GREEN);
       //名字
      result.appendFormat(
              "+ %s %p (%s)\n",
              getTypeId(), this, getName().string());
      colorizer.reset(result);
  
      s.activeTransparentRegion.dump(result, "transparentRegion");
      visibleRegion.dump(result, "visibleRegion");
      surfaceDamageRegion.dump(result, "surfaceDamageRegion");
      sp<Client> client(mClientRef.promote());
      PixelFormat pf = PIXEL_FORMAT_UNKNOWN;
      const sp<GraphicBuffer>& buffer(getActiveBuffer());
      if (buffer != NULL) {
          pf = buffer->getPixelFormat();
      }
      //区域,像素格式,颜色空间
      result.appendFormat(            "      "
              "layerStack=%4d, z=%9d, pos=(%g,%g), size=(%4d,%4d), "
              "crop=(%4d,%4d,%4d,%4d), finalCrop=(%4d,%4d,%4d,%4d), "
              "isOpaque=%1d, invalidate=%1d, "
              "dataspace=%s, pixelformat=%s "
  #ifdef USE_HWC2
              "alpha=%.3f, flags=0x%08x, tr=[%.2f, %.2f][%.2f, %.2f]\n"
  #else
              "alpha=0x%02x, flags=0x%08x, tr=[%.2f, %.2f][%.2f, %.2f]\n"
  #endif
              "      client=%p\n",
              getLayerStack(), s.z,
              s.active.transform.tx(), s.active.transform.ty(),
              s.active.w, s.active.h,
              s.crop.left, s.crop.top,
              s.crop.right, s.crop.bottom,
              s.finalCrop.left, s.finalCrop.top,
              s.finalCrop.right, s.finalCrop.bottom,
              isOpaque(s), contentDirty,
              dataspaceDetails(getDataSpace()).c_str(), decodePixelFormat(pf).c_str(),
              s.alpha, s.flags,
              s.active.transform[0][0], s.active.transform[0][1],
              s.active.transform[1][0], s.active.transform[1][1],
              client.get());
      //拿到图层的gb
      sp<const GraphicBuffer> buf0(mActiveBuffer);
      uint32_t w0=0, h0=0, s0=0, f0=0;
      //gb的信息
      if (buf0 != 0) {
          w0 = buf0->getWidth();
          h0 = buf0->getHeight();
          s0 = buf0->getStride();
          f0 = buf0->format;
      }
      result.appendFormat(
              "      "
              "format=%2d, activeBuffer=[%4ux%4u:%4u,%3X],"
              " queued-frames=%d, mRefreshPending=%d\n",
              mFormat, w0, h0, s0,f0,
              mQueuedFrames, mRefreshPending);
  
      if (mSurfaceFlingerConsumer != 0) {
          mSurfaceFlingerConsumer->dumpState(result, "            ");
      }
  }
```
自己在这加一行 dumpBuffer 将gb转为图片  仿照screencap

图像的合成在FramebufferSurface   送显的时候图层都合成好了    todo FramebufferSurface
/frameworks/native/services/surfaceflinger/DisplayHardware/FramebufferSurface.cpp
```
 void FramebufferSurface::onFrameAvailable(const BufferItem& /* item */) {
      sp<GraphicBuffer> buf;
      sp<Fence> acquireFence;
      status_t err = nextBuffer(buf, acquireFence);
      if (err != NO_ERROR) {
          ALOGE("error latching nnext FramebufferSurface buffer: %s (%d)",
                  strerror(-err), err);
          return;
      }
      //新增代码,将gb dump为图片
      #if DUMP_BUFFER   
        dumpBuffer(buf);
      //将gb送入显存
      err = mHwc.fbPost(mDisplayType, acquireFence, buf);
      if (err != NO_ERROR) {
          ALOGE("error posting framebuffer: %d", err);
      }
  }
```
看一下绘制  打开绘制的dump
layer dump的命令
dumpsys surfaceflinger   每调用一次输出一次dump
存在了 /data/user/0/io.twoyi/rootfs/data/dump
将rgb图片 pull出来
screencap image format android  google一下图片格式 查看stackoverflow
https://stackoverflow.com/questions/22034959/what-format-does-adb-screencap-sdcard-screenshot-raw-produce-without-p-f
此时要搜索,自己看代码纯纯浪费时间,只有网上搜不到的时候,自己需要看,源码读多了最重要的是节省时间
```
//4个高,4个宽  与写代码的write(fd, &w, 4)一致  这是android4.4的
4 bytes as uint32 - width
4 bytes as uint32 - height
4 bytes as uint32 - pixel format
(width * heigth * bytespp) bytes as byte array - image data, where bytespp is bytes per pixels and depend on pixel format. Usually bytespp is 4.
... //下面是例子
d0 02 00 00 - width - uint32 0x000002d0 = 720
00 05 00 00 - height - uint32 0x00000500 = 1280
01 00 00 00 - pixel format - uint32 0x00000001 = 1 = PixelFormat.RGBA_8888 => bytespp = 4 => RGBA
1e 1e 1e ff - first pixel data - R = 0x1e; G = 0x1e; B = 0x1e; A = 0xff;  //rgba
```
另一个答案
```
# skip header info  
dd if=screenshot.raw of=screenshot.rgba skip=12 bs=1  //跳过前面12个
# convert rgba to png  //转换rgba为png
convert -size 720x1280 -depth 8 screenshot.rgba screenshot.png
```
上面dump的时候可以不要write(fd, &w, 4),直接输出rgb数据
找一个能查看rgba的软件  YUVViewer
RGB format custom自己设置rgba  打开alpha channel 选择RGBA还是ARGB alpha通道的位置不同
查看后发现第一个图层是背景对的,第二个是launcher是对的,第三个是下面的导航栏是对的,第四个是状态栏

所以surfaceflinger对opengl指令的绘制是没问题的  如果是比较麻烦,opengl不好调试

安卓上运行安卓,虚拟化技术lxc proot   lxc需要root unbox用的是lxc  
  proot理论可以实现,但是基于ptrace,功能太强大,APP一般会检测,然后自己闪退,不让跑
  需要先了解双开的技术 VirtualApp  全都是hook
    对Android四大组件的代理
    对Android系统服务的拦截和接管
    对Android文件系统的重定向   让程序认为自己运行在根目录
  另外技术unikernel/libos/LKL/UML
    用户空间的程序可以随便升级,内核空间不行  运行在用户空间的内核


看一下合成
打开合成的dump
运行sysdump
绘制的图层分辨率都不一样,合成的图层都一样,最终为屏幕的大小
打开后发现,图层合成有问题  
后面的直播结束了,没有对应的定位