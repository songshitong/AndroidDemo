https://segmentfault.com/a/1190000002972420


kibana在ELK阵营中用来查询展示数据
elasticsearch构建在Lucene之上，过滤器语法和Lucene相同

team: xx.front  AND  project: xx-android  AND user_id:18284
team: xx.front  AND  project: xx-android  AND custom_msg:文件删除

增加基本 AND level:"ERROR"

常用语法
字符串匹配
字符串精确匹配。 举例1- Project: atom
字符串包含匹配。 举例2- QueryString: _appId
Q: 为什么上面两个例子语法一模一样，第一个是精确匹配，第二个是包含匹配？
A: 因为Project字段存储时不分词，QueryString字段分词.所有字符串查询都遵循这一原则，后面不做特殊说明
匹配 xxkeywordxx 不进行分词
custom_msg : "\""+停止+"\""   "*停止*"
对于特殊字符的处理：custom_msg : user\/login

OR 语法
同一字段多值查询。 举例3- Level: (ERROR OR FATAL)
不同字段查询或运算。 举例4- Project: atom OR Level: FATAL
例3: 匹配Level字段值为ERROR或FATAL的文档
例4: 匹配Project字段值为atom或Level字段值为FATAL的文档

AND 语法
与运算。 举例5- Project: atom AND Level: ERROR
分词字段多值包含。 举例6- QueryString: (_appId AND ip)
例5: 匹配Project值为atom并且Level字段值为ERROR或FATAL的文档
例6: 匹配QueryString值包含_appId并且包含ip的文档

数值匹配
精确数值匹配。 举例7- DealerId: 62669
开闭区间匹配。 举例8- QTime: [500 TO 1000}
*放在TO后面表示正无穷。 举例9- QTime: [500 TO *}
*放在TO前面表示负无穷。 举例10- QTime: {* TO 100}
例7: 匹配DealerId=62669的文档
例8: 匹配QTime >= 500 并且 QTime < 1000 的文档
例9: 匹配QTime >= 500的文档
例10: 匹配QTime < 100的文档

NOT 语法
非运算。 举例11- Project: atom AND NOT Level: INFO
例11: 匹配Project值为atom并且Level字段值不为INFO的文档