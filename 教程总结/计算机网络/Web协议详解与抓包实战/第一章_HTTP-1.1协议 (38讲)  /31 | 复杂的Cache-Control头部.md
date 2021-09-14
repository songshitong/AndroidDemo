Cache-Control 头部

• Cache-Control = 1#cache-directive
   • cache-directive = token [ "=" ( token / quoted-string ) ]
     • delta-seconds = 1*DIGIT   秒数
       • RFC 规范中的要求是，至少能支持到 2147483648 (2^31)
• 请求中的头部：max-age、max-stale、min-fresh  值后面要跟=数字
    no-cache、no-store、no-transform、only-if-cached  值后面不跟=，直接是token
• 响应中的头部： max-age、s-maxage 值后面要跟=数字
     must-revalidate 、proxyrevalidate 、no-cache、no-store、no-transform、public、private 值后面不跟=，直接是token
     响应中的no-cache,private两种用法都可以

  max-age=315360000
  

Cache-Control 头部在请求中的值
• max-age：告诉服务器，客户端不会接受 Age 超出 max-age 秒的缓存    超过后代理服务器去找源服务器的原始响应
• max-stale：告诉服务器，即使缓存不再新鲜，但陈旧秒数没有超出 max-stale 时，客户端仍打算使用。若 max-stale 后没有值，
   则表示无论过期多久客户端都可使用
• min-fresh：告诉服务器，Age 至少经过 min-fresh 秒后缓存才可使用   不超过min-fresh使用源服务器的响应
• no-cache：告诉服务器，不能直接使用已有缓存作为响应返回，除非带着缓存条件到上游服务端得到 304 验证返回码才可使用现有缓存
• no-store：告诉各代理服务器不要对该请求的响应缓存（实际有不少不遵守该规定的代理服务器）
• no-transform：告诉代理服务器不要修改消息包体的内容   有的服务器会修改HTML内容
• only-if-cached：告诉服务器仅能返回缓存的响应，否则若没有缓存则返回 504 错误码  


Cache-Control 头部在响应中的值
• must-revalidate：告诉客户端一旦缓存过期，必须向服务器验证后才可使用
• proxy-revalidate：与 must-revalidate 类似，但它仅对代理服务器的共享缓存有效
• no-cache：告诉客户端不能直接使用缓存的响应，使用前必须在源服务器验证得到 304 返回码。如果 no-cache 后指定头部，则若
   客户端的后续请求及响应中不含有这些头则可直接使用缓存      百度首页使用no-cache
• max-age：告诉客户端缓存 Age 超出 max-age 秒后则缓存过期   与请求中的使用不同
• s-maxage：与 max-age 相似，但仅针对共享缓存，且优先级高于 max-age 和 Expires
• public：表示无论私有缓存或者共享缓存，皆可将该响应缓存
• private：表示该响应不能被代理服务器作为共享缓存使用。若 private 后指定头 部，则在告诉代理服务器不能缓存指定的头部，
   但可缓存其他部分
• no-store：告诉所有下游节点不能对响应进行缓存
• no-transform：告诉代理服务器不能修改消息包体的内容


magicnum
min-fresh的介绍是不是有问题？它的意思不是说缓存在age<max-age-min-fresh才可用吗？而您说的是age至少经过min-fresh秒后缓存才可用
作者回复: 是的，谢谢指正。
min-fresh是在请求中使用的，通俗的讲它的含义表示：如果你有缓存，那么当前缓存不能过期，而且至少要经过min-fresh秒都不能过期的话，才能给我用。
RFC定义请参考：https://tools.ietf.org/html/rfc7234#section-5.2.1.3


15757172538
老师，这么多种方式，应用在什么场景下合适？
作者回复: 当缓存proxy不在你的控制范围内，但业务又强加各种过期时间要求时，才会用到他们。通常max-age就够用了

小樱桃
请教一下老师有没有快速记忆这些的方法，还有组合使用的效果，看完感觉过两天会忘😂
作者回复: 最常用的主要是no-cache、no-store、max-age，其他不常用的记住它们打算从哪些方面、试图影响代理还是浏览器的缓存，用到有这个意识再针对性的去查即可。


ray
老师好，
请问response有带max-age头部指定浏览器缓存时间，但是response没有回传age头部的情况下。
浏览器会怎么缓存这么response呢？

谢谢老师的解答^^
作者回复: 基于收到的时间以及max-age来判断过期与否