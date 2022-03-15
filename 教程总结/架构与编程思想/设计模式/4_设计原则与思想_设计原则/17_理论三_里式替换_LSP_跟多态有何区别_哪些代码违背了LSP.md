在上两节课中，我们学习了 SOLID 原则中的单一职责原则和开闭原则，这两个原则都比较重要，想要灵活应用也比较难，
需要你在实践中多加练习、多加体会。
今天，我们再来学习 SOLID 中的“L”对应的原则：里式替换原则。

整体上来讲，这个设计原则是比较简单、容易理解和掌握的。今天我主要通过几个反例，带你看看，哪些代码是违反里式替换原则的？
我们该如何将它们改造成满足里式替换原则？除此之外，这条原则从定义上看起来，跟我们之前讲过的“多态”有点类似。
所以，我今天也会讲一下，它跟多态的区别。
话不多说，让我们正式开始今天的学习吧！

如何理解“里式替换原则”？
里式替换原则的英文翻译是：Liskov Substitution Principle，缩写为 LSP。
这个原则最早是在 1986 年由 Barbara Liskov 提出，他是这么描述这条原则的：
If S is a subtype of T, then objects of type T may be replaced with objects of type S, without breaking the program。
在 1996 年，Robert Martin 在他的 SOLID 原则中，重新描述了这个原则，英文原话是这样的：Functions that use pointers of 
  references to base classes must be able to use objects of derived classes without knowing it。

我们综合两者的描述，将这条原则用中文描述出来，是这样的：子类对象（object of subtype/derived class）能够替换程序（program）中
  父类对象（object of base/parent class）出现的任何地方，并且保证原来程序的逻辑行为（behavior）不变及正确性不被破坏。