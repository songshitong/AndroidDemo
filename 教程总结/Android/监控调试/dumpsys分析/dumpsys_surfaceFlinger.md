
adb shell dumpsys SurfaceFlinger
https://mp.weixin.qq.com/s/tTcUtZeEJgptf8-CqdQUmA
微信首页，打开+号显示的悬浮菜单
```
Display 4630946829234430593 HWC layers:
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 Layer name
           Z |  Window Type |  Layer Class |  Comp Type |  Transform |   Disp Frame (LTRB) |          Source Crop (LTRB) |     Frame Rate (Explicit) (Seamlessness) [Focused]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 com.tencent.mm/com.tencent.mm.ui.LauncherUI#0
  rel      0 |            1 |            0 |     DEVICE |          0 |    0    0 1080 2400 |    0.0    0.0 1080.0 2400.0 |                                              [ ]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 PopupWindow:a83ff89#0
  rel      0 |         1000 |            0 |     DEVICE |          0 |  599  226 1058  852 |    0.0    0.0  459.0  626.0 |                                              [*]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 com.miui.securitycenter/.FloatingWindow#0
  rel      0 |         2003 |            0 |     DEVICE |          0 |    0  552   60  752 |    0.0    0.0   60.0  200.0 |                                              [ ]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 StatusBar#0
  rel      0 |         2000 |            0 |     DEVICE |          0 |    0    0 1080   96 |    0.0    0.0 1080.0   96.0 |                                              [ ]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 MiuiPaperContrastOverlay#0
  rel      0 |            0 |            0 |     DEVICE |          0 |    0    0 1080 2400 |    0.0    0.0 1080.0 2400.0 |                                              [ ]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 RoundCornerTop#0
  rel      0 |         2024 |            0 |     DEVICE |          0 |    0    0 1080  198 |    0.0    0.0 1080.0  198.0 |                                              [ ]
---------------------------------------------------------------------------------------------------------------------------------------------------------------
 RoundCornerBottom#0
  rel      0 |         2024 |            0 |     DEVICE |          0 |    0 2202 1080 2400 |    0.0    0.0 1080.0  198.0 |                                              [ ]
--------------------------------------------------------------------------------------------------------------------------------
```
com.tencent.mm/com.tencent.mm.ui.LauncherUI#0是微信的主窗口，并且铺满了整个屏幕(0,0,1080,2400)。

PopupWindow:7020633#0是弹起的 PopupWindow，它是一个独立的窗口（Surface），屏幕坐标范围是(599  226 1058  852)。

StatusBar#0 表示系统状态栏，由系统进程负责绘制，屏幕坐标范围是(0    0 1080   96)，即此状态栏高96像素。

//这个日志没有
NavigationBar#0 表示系统导航栏，由系统进程负责绘制，屏幕坐标范围是(0,2214,1080,2340)，即此导航栏高126像素。

最后两个窗口也是系统窗口，RoundCornerTop#0，RoundCornerBottom#0 据说是圆角

上述所有图层的合成类型都是Device，即HWC硬件模块负责合成这些Layer。

SurfaceFlinger会合成上述所有图层（Layer），并送显到内嵌的Display 4630946829234430593。


https://blog.csdn.net/xiaodanpeng/article/details/51852193
dumpsys SurfaceFlinger解析   不同的版本信息不一致，具体参考源码
android8.0
信息来自dumpAllLocked
frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
```
void SurfaceFlinger::dumpAllLocked(const Vector<String16>& args, size_t& index,
        String8& result) const
{
    ....
    /*
     * Dump library configuration.
     */

    colorizer.bold(result);
    result.append("Build configuration:");
    colorizer.reset(result);
     ....
    result.append("\n");

    result.append("\nWide-Color information:\n");
    dumpWideColorInfo(result);

    colorizer.bold(result);
    result.append("Sync configuration: ");
    colorizer.reset(result);
    result.append(SyncFeatures::getInstance().toString());
    result.append("\n");

    const auto& activeConfig = mHwc->getActiveConfig(HWC_DISPLAY_PRIMARY);

    colorizer.bold(result);
    result.append("DispSync configuration: ");
    colorizer.reset(result);
    result.appendFormat("app phase %" PRId64 " ns, sf phase %" PRId64 " ns, "
            "present offset %" PRId64 " ns (refresh %" PRId64 " ns)",
        vsyncPhaseOffsetNs, sfVsyncPhaseOffsetNs,
        dispSyncPresentTimeOffset, activeConfig->getVsyncPeriod());
    result.append("\n");

    // Dump static screen stats
    result.append("\n");
    dumpStaticScreenStats(result);
    result.append("\n");

    dumpBufferingStats(result);

    /*
     * Dump the visible layer list
     */
    colorizer.bold(result);
    result.appendFormat("Visible layers (count = %zu)\n", mNumLayers);
    colorizer.reset(result);
    mCurrentState.traverseInZOrder([&](Layer* layer) {
        layer->dump(result, colorizer);
    });

    /*
     * Dump Display state
     */

    colorizer.bold(result);
    result.appendFormat("Displays (%zu entries)\n", mDisplays.size());
    colorizer.reset(result);
    for (size_t dpy=0 ; dpy<mDisplays.size() ; dpy++) {
        const sp<const DisplayDevice>& hw(mDisplays[dpy]);
        hw->dump(result);
    }

    /*
     * Dump SurfaceFlinger global state
     */

    colorizer.bold(result);
    result.append("SurfaceFlinger global state:\n");
    colorizer.reset(result);

    HWComposer& hwc(getHwComposer());
    sp<const DisplayDevice> hw(getDefaultDisplayDeviceLocked());

    colorizer.bold(result);
    result.appendFormat("EGL implementation : %s\n",
            eglQueryStringImplementationANDROID(mEGLDisplay, EGL_VERSION));
    colorizer.reset(result);
    result.appendFormat("%s\n",
            eglQueryStringImplementationANDROID(mEGLDisplay, EGL_EXTENSIONS));

    mRenderEngine->dump(result);

    hw->undefinedRegion.dump(result, "undefinedRegion");
    result.appendFormat("  orientation=%d, isDisplayOn=%d\n",
            hw->getOrientation(), hw->isDisplayOn());
    result.appendFormat(
            "  last eglSwapBuffers() time: %f us\n"
            "  last transaction time     : %f us\n"
            "  transaction-flags         : %08x\n"
            "  refresh-rate              : %f fps\n"
            "  x-dpi                     : %f\n"
            "  y-dpi                     : %f\n"
            "  gpu_to_cpu_unsupported    : %d\n"
            ,
            mLastSwapBufferTime/1000.0,
            mLastTransactionTime/1000.0,
            mTransactionFlags,
            1e9 / activeConfig->getVsyncPeriod(),
            activeConfig->getDpiX(),
            activeConfig->getDpiY(),
            !mGpuToCpuSupported);

    result.appendFormat("  eglSwapBuffers time: %f us\n",
            inSwapBuffersDuration/1000.0);

    result.appendFormat("  transaction time: %f us\n",
            inTransactionDuration/1000.0);

    /*
     * VSYNC state
     */
    mEventThread->dump(result);
    result.append("\n");

    /*
     * HWC layer minidump
     */
    for (size_t d = 0; d < mDisplays.size(); d++) {
        const sp<const DisplayDevice>& displayDevice(mDisplays[d]);
        int32_t hwcId = displayDevice->getHwcDisplayId();
        if (hwcId == DisplayDevice::DISPLAY_ID_INVALID) {
            continue;
        }

        result.appendFormat("Display %d HWC layers:\n", hwcId);
        Layer::miniDumpHeader(result);
        mCurrentState.traverseInZOrder([&](Layer* layer) {
            layer->miniDump(result, hwcId);
        });
        result.append("\n");
    }

    /*
     * Dump HWComposer state
     */
    colorizer.bold(result);
    result.append("h/w composer state:\n");
    colorizer.reset(result);
    bool hwcDisabled = mDebugDisableHWC || mDebugRegion;
    result.appendFormat("  h/w composer %s\n",
            hwcDisabled ? "disabled" : "enabled");
    hwc.dump(result);

    /*
     * Dump gralloc state
     */
    const GraphicBufferAllocator& alloc(GraphicBufferAllocator::get());
    alloc.dump(result);
}
```
一般第一行是 Build configuration: [sf] [libui] [libgui]  一般是全局的宏
如果一下特殊宏打开,第一行log会打印出来:
FRAMEBUFFER_FORCE_FORMAT,HAS_CONTEXT_PRIORITY,NEVER_DEFAULT_TO_ASYNC_MODE,TARGET_DISABLE_TRIPLE_BUFFERING,DONT_USE_FENCE_SYNC

Wide-Color information: 屏幕颜色信息  当前是SRGB
Display 4630946829234430593 color modes:
Current color mode: ColorMode::SRGB (7)

Sync机制
一般是frameworks/native/libs/gui/SyncFeatures.cpp  SyncFeatures::SyncFeatures()
Sync configuration: [using: EGL_ANDROID_native_fence_sync EGL_KHR_wait_sync]

DispSync参数
DispSync configuration: app phase 0 ns, sf phase 0 ns, present offset 0 ns (refresh 16666667 ns)

layer的dump
Visible layers (count = 9)  count的值来源于layersSortedByZ中layer的数量
接下来就进入各个layer的dump   很长的一段
```
Composition layers
* Layer 0xb400007752bd0000 (com.tencent.mm/com.tencent.mm.ui.LauncherUI#0)  
```
0xb400007752bd0000指向当前layer对象的值,括号中是当前layer的名称,id是创建layer时产生的序列号


Display状态
Displays (1 entries)
+ DisplayDevice{4630946829234430593, internal, primary, ""}


SurfaceFlinger global state

EGL implementation
EGL implementation : 1.5 Android META-EGL

Region undefinedRegion (this=0xb400007772c24558, count=1)
[  0,   0, 1080, 2400]

eglSwapBuffers time
transaction time  0.000000 us


HWC layer  HWC的layer
Display 4630946829234430593 HWC layers:
Layer name |Window Type |Layer Class |Comp Type |Transform | Disp Frame (LTRB) |Source Crop (LTRB) |Frame Rate (Explicit) (Seamlessness) [Focused]

HWComposer state
h/w composer state:
h/w composer enabled

gralloc state