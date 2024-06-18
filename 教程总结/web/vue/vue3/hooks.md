
https://juejin.cn/post/7083401842733875208

官方对自定义hook定义：在 Vue 应用的概念中，“组合式函数” (Composables) 是一个利用 Vue 组合式 API 来封装和复用有状态逻辑的函数。
https://cn.vuejs.org/guide/reusability/composables.html

vue3中hook函数的一些表现
以函数形式抽离一些可复用的方法像钩子一样挂着，随时可以引入和调用，实现高内聚低耦合的目标；
1 将可复用功能抽离为外部JS文件
2 函数名/文件名以use开头，形如：useXX
3 引用时将响应式变量或者方法显式解构暴露出来如：const {nameRef，Fn} = useXX()
（在setup函数解构出自定义hooks的变量和方法）


例子
简单的加法计算，将加法抽离为自定义Hooks，并且传递响应式数据
```
import { ref, watch } from 'vue';
const useAdd= ({ num1, num2 }) =>{
  const addNum = ref(0)
  watch([num1, num2], ([num1, num2]) => {
    addFn(num1, num2)
   })
  const addFn = (num1, num2) => {
    addNum.value = num1 + num2
   }
  return {
    addNum,
    addFn
   }
}
export default useAdd
```
加法组件
```
<template>
  <div>
     num1:<input v-model.number="num1" style="width:100px" />
    <br />
     num2:<input v-model.number="num2" style="width:100px" />
  </div>
  <span>加法等于:{{ addNum }}</span>
</template>
<script setup>
import { ref } from 'vue'
import useAdd from './useAdd.js'   //引入自动hook 
const num1 = ref(2)
const num2 = ref(1)
//加法功能-自定义Hook（将响应式变量或者方法形式暴露出来）
const { addNum, addFn } = useAdd({ num1, num2 })
addFn(num1.value, num2.value)
</script>
```
Vue3自定义Hooks和Vue2时代Mixin的关系
```
Mixin不足
在 Vue 2 中，mixin 是将部分组件逻辑抽象成可重用块的主要工具。但是，他们有几个问题：
1、Mixin 很容易发生冲突：因为每个 mixin 的 property 都被合并到同一个组件中，所以为了避免 property 名冲突，你仍然需要了解其他每个特性。
2、可重用性是有限的：我们不能向 mixin 传递任何参数来改变它的逻辑，这降低了它们在抽象逻辑方面的灵活性。
```


总结：
Vue2时代Option Api ，data、methos、watch.....分开写，这种是碎片化的分散的，代码一多就容易高耦合，维护时来回切换代码是繁琐的！
Vue3时代Composition Api，通过利用各种Hooks和自定义Hooks将碎片化的响应式变量和方法按功能分块写，实现高内聚低耦合
