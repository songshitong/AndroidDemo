

https://www.runoob.com/css3/css3-flexbox.html
https://www.w3school.com.cn/css/css3_flexbox.asp
CSS3 弹性盒（ Flexible Box 或 flexbox），是一种当页面需要适应不同的屏幕大小以及设备类型时确保元素拥有恰当的行为的布局方式。
引入弹性盒布局模型的目的是提供一种更加有效的方式来对一个容器中的子元素进行排列、对齐和分配空白空间


在 Flexbox 布局模块（问世）之前，可用的布局模式有以下四种：
1 块（Block），用于网页中的部分（节）
2 行内（Inline），用于文本
3 表，用于二维表数据
4 定位，用于元素的明确位置
弹性框布局模块，可以更轻松地设计灵活的响应式布局结构，而无需使用浮动或定位

常用属性
display	规定用于 HTML 元素的盒类型。  
```
.flex-container {
  display: flex;
}
```
flex-direction	规定弹性容器内的弹性项目的方向。 column;column-reverse;row;row-reverse;
justify-content	当弹性项目没有用到主轴上的所有可用空间时，水平对齐这些项目。 center;flex-start;flex-end;space-around;space-between;
align-items	当弹性项目没有用到主轴上的所有可用空间时，垂直对齐这些项。center;flex-start;flex-end;stretch(拉伸);baseline(基线对齐);
flex-wrap	规定弹性项目是否应该换行，若一条 flex 线上没有足够的空间容纳它们。 nowrap(默认);wrap;wrap-reverse;
align-content	修改 flex-wrap 属性的行为。与 align-items 相似，但它不对齐弹性项目，而是对齐 flex 线。center;flex-start;flex-end;space-between;space-around;stretch;
flex-flow	flex-direction 和 flex-wrap 的简写属性。  flex-flow: row wrap;
order	规定弹性项目相对于同一容器内其余弹性项目的顺序。  值必须是数字，默认值是 0
flex-grow 属性规定某个 flex 项目相对于其余 flex 项目将增长多少  该值必须是数字，默认值是 0
flex-shrink 属性规定某个 flex 项目相对于其余 flex 项目将收缩多少
flex-basis 属性规定 flex 项目的初始长度。  某个div使用特定的长度，其他自适应
align-self属性规定弹性容器内所选项目的对齐方式。覆盖容器的 align-items 属性。 center；flex-start...
flex	flex-grow、flex-shrink 以及 flex-basis 属性的简写属性。


若是设置每行显示的个数，则设置子元素的宽度即可，如每行显示三个：则父元素display：flex；flex-wrap: wrap;align-content: flex-start；
子元素：flex：1；width：33.3%； ，这样每个元素占1/3，也就是每行展示三个