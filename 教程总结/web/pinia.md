https://pinia.vuejs.org/zh/introduction.html


pinia
一个拥有组合式 API 的 Vue 状态管理库
同时支持Vue 2 和 Vue 3

对比 Vuex
Pinia 起源于一次探索 Vuex 下一个迭代的实验，因此结合了 Vuex 5 核心团队讨论中的许多想法。最后，我们意识到 Pinia 已经实现了我们在 Vuex 5 中想要的大部分功能，
所以决定将其作为新的推荐方案来代替 Vuex。
与 Vuex 相比，Pinia 不仅提供了一个更简单的 API，也提供了符合组合式 API 风格的 API，最重要的是，搭配 TypeScript 一起使用时有非常可靠的类型推断支持

官方示例：
todos.js
```
import { defineStore } from 'pinia'

//defineStore 创建一个 useStore 函数，检索 store 实例   todos是store的唯一id
export const useTodos = defineStore('todos', {
  state: () => ({
    /** @type {{ text: string, id: number, isFinished: boolean }[]} */
    todos: [],
    /** @type {'all' | 'finished' | 'unfinished'} */
    filter: 'all',
    // type will be automatically inferred to number
    nextId: 0,
  }),
  getters: {
    finishedTodos(state) {
      // autocompletion! ✨
      return state.todos.filter((todo) => todo.isFinished)
    },
    unfinishedTodos(state) {
      return state.todos.filter((todo) => !todo.isFinished)
    },
    /**
     * @returns {{ text: string, id: number, isFinished: boolean }[]}
     */
    filteredTodos(state) {
      if (this.filter === 'finished') {
        // call other getters with autocompletion ✨
        return this.finishedTodos
      } else if (this.filter === 'unfinished') {
        return this.unfinishedTodos
      }
      return this.todos
    },
  },
  actions: { //定义行为
    // any amount of arguments, return a promise or not
    addTodo(text) {
      // you can directly mutate the state
      this.todos.push({ text, id: this.nextId++, isFinished: false })
    },
  },
})
```
APP.vue
```
<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useTodos } from './todos.js'

const todosStore = useTodos()

//storeToRefs
//创建一个引用对象，包含 store 的所有 state、 getter 和 plugin 添加的 state 属性。 类似于 toRefs()，但专门为 Pinia store 设计， 
//所以 method 和非响应式属性会被完全忽略。
const { filter, filteredTodos } = storeToRefs(todosStore)

const newTodoText = ref('')

function addTodo() {
  if (!newTodoText.value) {
    return
  }

  todosStore.addTodo(newTodoText.value)
  newTodoText.value = ''
}
</script>

<template>
  <label><input v-model="filter" type="radio" value="all"> All</label> //选项过滤
  <label><input v-model="filter" type="radio" value="finished"> Finished</label>
  <label><input v-model="filter" type="radio" value="unfinished"> Unfinished</label>
  <hr>
  <ul>
    <li v-for="todo in filteredTodos" :key="todo.id">  //展示过滤后的todo
      <input v-model="todo.isFinished" type="checkbox">
      {{ todo.text }}
    </li>
  </ul>
  <label>
    New Todo:
    <input v-model="newTodoText" type="text" @keypress.enter="addTodo">
  </label>
  <button :disabled="!newTodoText" @click="addTodo">Add</button> //添加新的todo
</template>
```