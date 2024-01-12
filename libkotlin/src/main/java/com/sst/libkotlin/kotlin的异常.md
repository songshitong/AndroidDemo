


kotlin移除检查异常的原因
https://stackoverflow.com/questions/58639126/whats-the-idea-behind-kotlin-removing-checked-exceptions
https://stackoverflow.com/questions/613954/the-case-against-checked-exceptions

缺点： 没有编译提示异常了   
 需要关注文档以及内部实现可能抛出的异常   这要求完善的文档，但是开发时会仔细看官方文档吗？

改进：
1 关注文档与实现  及时进行异常捕获
2 作为SDK，如何避免抛出异常来实现功能，改为监听回调，让业务显示的处理？？