
编辑器
https://github.com/marktext/marktext
https://github.com/benweet/stackedit


http://xianbai.me/learn-md/article/syntax/links.html  
换行：敲个空格，再按回车（两行之间有一个空行，没有空行直接使用一个空格）   或者tab+回车
如果需要在段落内加入换行（\<br>）：
可以在前一行的末尾加入至少两个空格<br>然后换行写其它的文字  //可以使用md编辑器查看效果

段落 标题   command+num num代表几级标题  3级标题略微大于内容    ###代表3级标题
使用 # 号可表示 1-6 级标题，一级标题对应一个 # 号，二级标题对应两个 # 号

多级列表
先输入-后再按空格键，再按tab键
1
- 1.1
    - 1.1.1
    - 1.1.2
- 2.1
多级列表：每深入一级，句首多加个Tab键
1. Part A
    1. Section One
        1. Example 1
        2. Example 2
    2. Section Two
    3. Section Three
2. Part B
    * Section One
    * Section Two
    + Section Three
    - Section Four
3. Part C


文字对齐   使用内置HTML <center>hhhh</center>  <p align="left">哈哈哈</p>  <p align="right">诶嘿</p>
          html 3级标题 <h3>标题<h3>
          

文章内跳转
   html方式  1定义锚点 <a id="head"/>     2 定义跳转点 <a href="#head">`点这里从头再读一遍`</a>
   markdown语法
   1 生成目录树 想要给文档生成目录树，只需要在文档中增加[TOC]，目录树就会根据文档中的h1~h6标题自动生成了，[TOC]独占一行
   2 [名称](#id) id是锚点的ID 也可以使用html定义锚点，使用markdown增加跳转链接

文字折叠
<details>
<summary>Title</summary>
<pre><code>
content!!!
</code></pre>
</details>


代码函数
`printf()`函数
代码段  
```
Student a;
```

寻找帮助写Markdown的脚本或工具
开源：markText