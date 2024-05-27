https://www.sass.hk/docs/
css转为sass
https://www.sass.hk/css2sass/

Sass    Syntactically Awesome StyleSheets
Syntactically  /sɪnˈtæktɪkli/  句法上；语法上；句法；句法特征


语法格式 (Syntax)
Sass 有两种语法格式
1 SCSS (Sassy CSS) —— 这种格式仅在 CSS3 语法的基础上进行拓展，所有 CSS3 语法在 SCSS 中都是通用的，同时加入 Sass 的特色功能。
此外，SCSS 也支持大多数 CSS hacks 写法以及浏览器前缀写法 (vendor-specific syntax)，以及早期的 IE 滤镜写法。这种格式以 .scss 作为拓展名
2 另一种也是最早的 Sass 语法格式，被称为缩进格式 (Indented Sass) 通常简称 "Sass"，是一种简化格式。它使用 “缩进” 代替 “花括号” 表示属性属于某个选择器，
 用 “换行” 代替 “分号” 分隔属性，很多人认为这样做比 SCSS 更容易阅读，书写也更快速。缩进格式也可以使用 Sass 的全部功能，
只是与 SCSS 相比个别地方采取了不同的表达方式，这种格式以 .sass 作为拓展名。



嵌套规则 (Nested Rules)
Sass 允许将一套 CSS 样式嵌套进另一套样式中，内层的样式将它外层的选择器作为父选择器
```
#main p {
  width: 97%;
  .redbox {
    color: #000000;
  }
}

编译为
#main p {
  width: 97%; }
  #main p .redbox {
    color: #000000; }
```


父选择器 & (Referencing Parent Selectors: &)
在嵌套 CSS 规则时，有时也需要直接使用嵌套外层的父选择器
编译后的 CSS 文件中 & 将被替换成嵌套外层的父选择器，如果含有多层嵌套，最外层的父选择器会一层一层向下传递
& 必须作为选择器的第一个字符，其后可以跟随后缀生成复合的选择器。当父选择器含有不合适的后缀时，Sass 将会报错
```
#main {
  color: black;
  &-sidebar { border: 1px solid; }
}
编译为
#main {
  color: black; }
  #main-sidebar {
    border: 1px solid; }
```


属性嵌套 (Nested Properties)
有些 CSS 属性遵循相同的命名空间 (namespace)，比如 font-family, font-size, font-weight 都以 font 作为属性的命名空间。
为了便于管理这样的属性，同时也为了避免了重复输入，Sass 允许将属性嵌套在命名空间中。
命名空间也可以包含自己的属性值
```
.funky {
  font: 20px/24px {
    family: fantasy;
    weight: bold;
  }
}
编译为：
.funky {
  font: 20px/24px;
    font-family: fantasy;
    font-weight: bold; }
```



变量 $ (Variables: $)
变量以美元符号开头，赋值方法与 CSS 属性的写法一样
变量支持块级作用域，嵌套规则内定义的变量只能在嵌套规则内使用（局部变量），不在嵌套规则内定义的变量则可在任何地方使用（全局变量）。
   将局部变量转换为全局变量可以添加 !global
```
#main {
  $width: 5em !global;
  width: $width;
}

#sidebar {
  width: $width;
}
编译为：
#main {
  width: 5em;
}

#sidebar {
  width: 5em;
}
```

变量定义 !default (Variable Defaults: !default)
可以在变量的结尾添加 !default 给一个未通过 !default 声明赋值的变量赋值，此时，如果变量已经被赋值，不会再被重新赋值，但是如果变量还没有被赋值，则会被赋予新的值
变量是 null 空值时将视为未被 !default 赋值
```
$content: "First content";
$content: "Second content?" !default;
$new_content: "First time reference" !default;

#main {
  content: $content;
  new-content: $new_content;
}
编译为
#main {
  content: "First content";
  new-content: "First time reference"; }
```


字符串 (Strings)
SassScript 支持 CSS 的两种字符串类型：有引号字符串 (quoted strings)，如 "Lucida Grande" 'http://sass-lang.com'；
与无引号字符串 (unquoted strings)，如 sans-serif bold，在编译 CSS 文件时不会改变其类型。只有一种情况例外，使用 #{} (interpolation) 时，
有引号字符串将被编译为无引号字符串，这样便于在 mixin 中引用选择器名：
```
@mixin firefox-message($selector) {
  body.firefox #{$selector}:before {
    content: "Hi, Firefox users!";
  }
}
@include firefox-message(".header");
编译为：
body.firefox .header:before {
  content: "Hi, Firefox users!"; }
```



数组 (Lists)
数组 (lists) 指 Sass 如何处理 CSS 中 margin: 10px 15px 0 0 或者 font-face: Helvetica, Arial, sans-serif 这样通过空格或者逗号分隔的一系列的值。
事实上，独立的值也被视为数组 —— 只包含一个值的数组。

数组本身没有太多功能，但 Sass list functions 赋予了数组更多新功能：nth 函数可以直接访问数组中的某一项；join 函数可以将多个数组连接在一起；
append 函数可以在数组中添加新值；而 @each 指令能够遍历数组中的每一项。

数组中可以包含子数组，比如 1px 2px, 5px 6px 是包含 1px 2px 与 5px 6px 两个数组的数组。如果内外两层数组使用相同的分隔方式，需要用圆括号包裹内层，
所以也可以写成 (1px 2px) (5px 6px)。变化是，之前的 1px 2px, 5px 6px 使用逗号分割了两个子数组 (comma-separated)，
而 (1px 2px) (5px 6px) 则使用空格分割(space-separated)。

当数组被编译为 CSS 时，Sass 不会添加任何圆括号（CSS 中没有这种写法），所以 (1px 2px) (5px 6px) 与 1px 2px, 5px 6px 在编译后的 CSS 文件中是完全一样的，
但是它们在 Sass 文件中却有不同的意义，前者是包含两个数组的数组，而后者是包含四个值的数组。

用 () 表示不包含任何值的空数组（在 Sass 3.3 版之后也视为空的 map）。空数组不可以直接编译成 CSS，比如编译 font-family: () Sass 将会报错。
如果数组中包含空数组或空值，编译时将被清除，比如 1px 2px () 3px 或 1px 2px null 3px。

基于逗号分隔的数组允许保留结尾的逗号，这样做的意义是强调数组的结构关系，尤其是需要声明只包含单个值的数组时。例如 (1,) 表示只包含 1 的数组，
而 (1 2 3,) 表示包含 1 2 3 这个以空格分隔的数组的数组。



运算 (Operations)
所有数据类型均支持相等运算 == 或 !=
数字运算 (Number Operations)
SassScript 支持数字的加减乘除、取整等运算 (+, -, *, /, %)，如果必要会在不同单位间转换值
关系运算 <, >, <=, >= 也可用于数字运算，相等运算 ==, != 可用于所有数据类型
```
p {
  width: 1in + 8pt;
}

编译为：
p {
  width: 1.111in; }
```

除法运算 / (Division and /)
/ 在 CSS 中通常起到分隔数字的用途，SassScript 作为 CSS 语言的拓展当然也支持这个功能，同时也赋予了 / 除法运算的功能。也就是说，
如果 / 在 SassScript 中把两个数字分隔，编译后的 CSS 文件中也是同样的作用。

以下三种情况 / 将被视为除法运算符号：
1 如果值，或值的一部分，是变量或者函数的返回值
2 如果值被圆括号包裹
3 如果值是算数表达式的一部分
```
p {
  font: 10px/8px;             // Plain CSS, no division
  $width: 1000px;
  width: $width/2;            // Uses a variable, does division
  width: round(1.5)/2;        // Uses a function, does division
  height: (500px/2);          // Uses parentheses, does division
  margin-left: 5px + 8px/2px; // Uses +, does division
}
编译为：
p {
  font: 10px/8px;
  width: 500px;
  height: 250px;
  margin-left: 9px; }
```
如果需要使用变量，同时又要确保 / 不做除法运算而是完整地编译到 CSS 文件中，只需要用 #{} 插值语句将变量包裹。
```
p {
  $font-size: 12px;
  $line-height: 30px;
  font: #{$font-size}/#{$line-height};
}
编译为：
p {
  font: 12px/30px; }
```

颜色值运算 (Color Operations)
颜色值的运算是分段计算进行的，也就是分别计算红色，绿色，以及蓝色的值
数字与颜色值之间也可以进行算数运算，同样也是分段计算的
```
p {
  color: #010203 + #040506;
  color: #010203 * 2;
}
编译为： 01 + 04 = 05   02 + 05 = 07   03 + 06 = 09
p {
  color: #050709;
  color: #020406 * 2; 
   }
```
如果颜色值包含 alpha channel（rgba 或 hsla 两种颜色值），必须拥有相等的 alpha 值才能进行运算，因为算术运算不会作用于 alpha 值
```
p {
  color: rgba(255, 0, 0, 0.75) + rgba(0, 255, 0, 0.75);
}
编译为：
p {
  color: rgba(255, 255, 0, 0.75); }  //alpha仍为0.75
```

字符串运算 (String Operations)
+ 可用于连接字符串
```
p {
  cursor: e + -resize;
}
编译为:
p {
  cursor: e-resize; }
```
如果有引号字符串（位于 + 左侧）连接无引号字符串，运算结果是有引号的，相反，无引号字符串（位于 + 左侧）连接有引号字符串，运算结果则没有引号。
```
p:before {
  content: "Foo " + Bar;
  font-family: sans- + "serif";
}
编译为
p:before {
  content: "Foo Bar";
  font-family: sans-serif; }
```

布尔运算 (Boolean Operations)
SassScript 支持布尔型的 and or 以及 not 运算

数组运算 (List Operations)
数组不支持任何运算方式，只能使用 list functions 控制。



& in SassScript
Just like when it’s used in selectors, & in SassScript refers to the current parent selector. 
It’s a comma-separated list of space-separated lists
```
.foo.bar .baz.bang, .bip.qux {
  $selector: &;
}
```


@extend
告诉 Sass 将一个选择器下的所有样式继承给另一个选择器
```
.error {
  border: 1px #f00;
  background-color: #fdd;
}
.error.intrusion {
  background-image: url("/image/hacked.png");
}
.seriousError {
  @extend .error;
  border-width: 3px;
}

编译为：
.error, .seriousError {
  border: 1px #f00;
  background-color: #fdd; }

.error.intrusion, .seriousError.intrusion {
  background-image: url("/image/hacked.png"); }

.seriousError {
  border-width: 3px; }
```




控制指令 (Control Directives)
SassScript 提供了一些基础的控制指令，比如在满足一定条件时引用样式，或者设定范围重复输出格式。控制指令是一种高级功能，日常编写过程中并不常用到，
主要与混合指令 (mixin) 配合使用，尤其是用在 Compass 等样式库中
if()
The built-in if() function allows you to branch on a condition and returns only one of two possible outcomes.
It can be used in any script context. The if function only evaluates the argument corresponding to the one that it will return 
– this allows you to refer to variables that may not be defined or to have calculations that would otherwise cause an error 
(E.g. divide by zero).

@if
当 @if 的表达式返回值不是 false 或者 null 时，条件成立，输出 {} 内的代码
@if 声明后面可以跟多个 @else if 声明，或者一个 @else 声明。如果 @if 声明失败，Sass 将逐条执行 @else if 声明，如果全部失败，最后执行 @else 声明
```
p {
  @if 1 + 1 == 2 { border: 1px solid; }
  @if 5 < 3 { border: 2px dotted; }
  @if null  { border: 3px double; }
}
编译为
p {
  border: 1px solid; }
  
  
$type: monster;
p {
  @if $type == ocean {
    color: blue;
  } @else if $type == matador {
    color: red;
  } @else if $type == monster {
    color: green;
  } @else {
    color: black;
  }
}  
编译为
p {
  color: green; }
```



@for
@for 指令可以在限制的范围内重复输出格式，每次按要求（变量的值）对输出结果做出变动。
这个指令包含两种格式：@for $var from <start> through <end>，或者 @for $var from <start> to <end>，区别在于 through 与 to 的含义：
当使用 through 时，条件范围包含 <start> 与 <end> 的值，而使用 to 时条件范围只包含 <start> 的值不包含 <end> 的值。
另外，$var 可以是任何变量，比如 $i；<start> 和 <end> 必须是整数值。
```
@for $i from 1 through 3 {
  .item-#{$i} { width: 2em * $i; }
}
编译为：
.item-1 {
  width: 2em; }
.item-2 {
  width: 4em; }
.item-3 {
  width: 6em; }
```


@each
@each 指令的格式是 $var in <list>, $var 可以是任何变量名，比如 $length 或者 $name，而 <list> 是一连串的值，也就是值列表。
@each 将变量 $var 作用于值列表中的每一个项目，然后输出结果
```
@each $animal in puma, sea-slug, egret, salamander {
  .#{$animal}-icon {
    background-image: url('/images/#{$animal}.png');
  }
}
编译为：
.puma-icon {
  background-image: url('/images/puma.png'); }
.sea-slug-icon {
  background-image: url('/images/sea-slug.png'); }
.egret-icon {
  background-image: url('/images/egret.png'); }
.salamander-icon {
  background-image: url('/images/salamander.png'); }
```
salamander  蝾螈(两栖动物，形似蜥蜴)    puma 英/ˈpjuːmə/ 彪马；美洲虎；山狮  egret /ˈiːɡrət/ 白鹭
sea-slug  海蛞蝓

Multiple Assignment
The @each directive can also use multiple variables, as in @each $var1, $var2, ... in . If is a list of lists, 
each element of the sub-lists is assigned to the respective variable.
```
@each $animal, $color, $cursor in (puma, black, default),
                                  (sea-slug, blue, pointer),
                                  (egret, white, move) {
  .#{$animal}-icon {
    background-image: url('/images/#{$animal}.png');
    border: 2px solid $color;
    cursor: $cursor;
  }
}
编译为
.puma-icon {
  background-image: url('/images/puma.png');
  border: 2px solid black;
  cursor: default; }
.sea-slug-icon {
  background-image: url('/images/sea-slug.png');
  border: 2px solid blue;
  cursor: pointer; }
.egret-icon {
  background-image: url('/images/egret.png');
  border: 2px solid white;
  cursor: move; }
```


@while
@while 指令重复输出格式直到表达式返回结果为 false。这样可以实现比 @for 更复杂的循环，只是很少会用到
```
$i: 6;
@while $i > 0 {
  .item-#{$i} { width: 2em * $i; }
  $i: $i - 2; //6 4 2 
}
编译为：
.item-6 {
  width: 12em; }

.item-4 {
  width: 8em; }

.item-2 {
  width: 4em; }
```



混合指令 (Mixin Directives)
混合指令（Mixin）用于定义可重复使用的样式，避免了使用无语意的 class，比如 .float-left。混合指令可以包含所有的 CSS 规则，
绝大部分 Sass 规则，甚至通过参数功能引入变量，输出多样化的样式。

定义混合指令 @mixin (Defining a Mixin: @mixin)
混合指令的用法是在 @mixin 后添加名称与样式，比如名为 large-text 的混合通过下面的代码定义
```
@mixin large-text {
  font: {
    family: Arial;
    size: 20px;
    weight: bold;
  }
  color: #ff0000;
}
```
引用混合样式 @include (Including a Mixin: @include)
使用 @include 指令引用混合样式，格式是在其后添加混合名称，以及需要的参数（可选）：
```
.page-title {
  @include large-text;
  padding: 4px;
  margin-top: 10px;
}

编译为
.page-title {
  font-family: Arial;
  font-size: 20px;
  font-weight: bold;
  color: #ff0000;
  padding: 4px;
  margin-top: 10px; }
```

参数 (Arguments)
参数用于给混合指令中的样式设定变量，并且赋值使用。在定义混合指令的时候，按照变量的格式，通过逗号分隔，将参数写进圆括号里。
引用指令时，按照参数的顺序，再将所赋的值对应写进括号
混合指令也可以使用给变量赋值的方法给参数设定默认值，然后，当这个指令被引用的时候，如果没有给参数赋值，则自动使用默认值
```
@mixin sexy-border($color, $width: 1in) {
  border: {
    color: $color;
    width: $width;
    style: dashed;
  }
}
p { @include sexy-border(blue, 1in); }
编译为：
p {
  border-color: blue;
  border-width: 1in;
  border-style: dashed; }
```
混合指令也可以使用关键词参数
```
p { @include sexy-border($color: blue); }
h1 { @include sexy-border($color: blue, $width: 2in); }
```

参数变量 (Variable Arguments)
有时，不能确定混合指令需要使用多少个参数，比如一个关于 box-shadow 的混合指令不能确定有多少个 'shadow' 会被用到。这时，
可以使用参数变量 … 声明（写在参数的最后方）告诉 Sass 将这些参数视为值列表处理
```
@mixin box-shadow($shadows...) {
  -moz-box-shadow: $shadows;
  -webkit-box-shadow: $shadows;
  box-shadow: $shadows;
}
.shadows {
  @include box-shadow(0px 4px 5px #666, 2px 6px 10px #999);
}
编译为
.shadowed {
  -moz-box-shadow: 0px 4px 5px #666, 2px 6px 10px #999;
  -webkit-box-shadow: 0px 4px 5px #666, 2px 6px 10px #999;
  box-shadow: 0px 4px 5px #666, 2px 6px 10px #999;
}
```


向混合样式中导入内容 (Passing Content Blocks to a Mixin)
在引用混合样式的时候，可以先将一段代码导入到混合指令中，然后再输出混合样式，额外导入的部分将出现在 @content 标志的地方
```
@mixin apply-to-ie6-only {
  * html {
    @content;
  }
}
@include apply-to-ie6-only {
  #logo {
    background-image: url(/logo.gif);
  }
}
编译为
* html #logo {
  background-image: url(/logo.gif);
}
```
为便于书写，@mixin 可以用 = 表示，而 @include 可以用 + 表示，所以上面的例子可以写成
```
=apply-to-ie6-only
  * html
    @content

+apply-to-ie6-only
  #logo
    background-image: url(/logo.gif)
```


函数指令 (Function Directives)
Sass 支持自定义函数，并能在任何属性值或 Sass script 中使用：
```
$grid-width: 40px;
$gutter-width: 10px;

@function grid-width($n) {
  @return $n * $grid-width + ($n - 1) * $gutter-width;
}

#sidebar { width: grid-width(5); }

编译为：
#sidebar {
  width: 240px; }
```