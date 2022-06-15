
https://exoplayer.dev/hello-world.html 参考官网最好
https://www.jianshu.com/p/6e466e112877
ExoPlayer实现的通用组件是：
MediaSource：媒体资源，用于定义要播放的媒体，加载媒体，以及从哪里加载媒体。简单的说，MediaSource就是代表我们要播放的媒体文件，
   可以是本地资源，可以是网络资源。MediaSource在播放开始的时候，通过ExoPlayer.prepare方法注入。
Renderer：渲染器，用于渲染媒体文件。当创建播放器的时候，Renderers被注入。
TrackSelector：轨道选择器，用于选择MediaSource提供的轨道（tracks），供每个可用的渲染器使用。
LoadControl：用于控制MediaSource何时缓冲更多的媒体资源以及缓冲多少媒体资源。LoadControl在创建播放器的时候被注入。


ExoPlayer库提供了在普通使用场景下上述组件的默认实现。ExoPlayer可以使用这些默认的组件，也可以使用自定义组件。
   例如可以注入一个自定义的LoadControl用来改变播放器的缓存策略，或者可以注入一个自定义渲染器以使用Android本身不支持的视频解码器。
