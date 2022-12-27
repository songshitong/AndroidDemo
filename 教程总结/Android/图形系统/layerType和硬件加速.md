https://www.wanandroid.com/wenda/show/24990
https://blog.csdn.net/zhjali123/article/details/81367453

setLayerType(View.LAYER_TYPE_HARDWARE, null)
getLayerType()
LAYER_TYPE默认是
LAYER_TYPE_NONE
其他两种类型是LAYER_TYPE_SOFTWARE和LAYER_TYPE_HARDWARE
LAYER_TYPE_SOFTWARE
Indicates that the view has a software layer. A software layer is backed by a bitmap and causes the view 
 to be rendered using Android's software rendering pipeline, even if hardware acceleration is enabled.
Software layers have various usages:
When the application is not using hardware acceleration, a software layer is useful to apply a specific color 
   filter and/or blending mode and/or translucency to a view and all its children.
When the application is using hardware acceleration, a software layer is useful to render drawing primitives not supported 
  by the hardware accelerated pipeline. It can also be used to cache a complex view tree into a texture and 
  reduce the complexity of drawing operations. For instance, when animating a complex view tree with a translation, 
  a software layer can be used to render the view tree only once.
Software layers should be avoided when the affected view tree updates often. Every update will require to re-render 
   the software layer, which can potentially be slow (particularly when hardware acceleration is turned on 
   since the layer will have to be uploaded into a hardware texture after every update.)

LAYER_TYPE_HARDWARE
Indicates that the view has a hardware layer. A hardware layer is backed by a hardware specific texture
 (generally Frame Buffer Objects or FBO on OpenGL hardware) and causes the view to be rendered using Android's
  hardware rendering pipeline, but only if hardware acceleration is turned on for the view hierarchy. 
  When hardware acceleration is turned off, hardware layers behave exactly as software layers.
A hardware layer is useful to apply a specific color filter and/or blending mode and/or translucency 
  to a view and all its children.
A hardware layer can be used to cache a complex view tree into a texture and reduce the complexity of drawing operations. 
  For instance, when animating a complex view tree with a translation, a hardware layer can be used to 
  render the view tree only once.
A hardware layer can also be used to increase the rendering quality when rotation transformations are applied on a view.
  It can also be used to prevent potential clipping issues when applying 3D transforms on a view.


LAYER_TYPE_SOFTWARE和LAYER_TYPE_HARDWARE
不同点：一个使用软件绘制，一个使用硬件绘制
相同点：
  保存复杂的view到texture，减少复杂的绘制操作
注意：LAYER_TYPE_SOFTWARE不适用于view tree频繁更新，因为需要重新渲染software layer，在硬件加速下更严重，每次更新需要将software layer
上传到hardware texture