
https://cnblogs.com/zdsdididi/p/17653695.html
https://cn.vuejs.org/guide/built-ins/keep-alive.html
在 vue 中,默认情况下，一个组件实例在被替换掉后会被销毁。这会导致它丢失其中所有已变化的状态——当这个组件再一次被显示时，会创建一个只带有初始状态的新实例。
但是 vue 提供了 keep-alive 组件,它可以将一个动态组件包装起来从而实现组件切换时候保留其状态。

一般放在App.vue，
```
<template>
  <router-view v-slot="{ Component }">
    <keep-alive>
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>
```
router-view 组件的插槽传递了一个带有当前组件的组件名 Component 的对象,然后用 keep-alive 包裹一个动态组件


结合路由控制
```
//路由配置keepalive
//route/index.ts
const routes: RouteRecordRaw[] = [
  {
    path: "/aa",
    name: "a",
    meta: {
      keepAlive: true,
    },
    component: () => import(/* webpackChunkName: "A" */ "../views/a.vue"),
  },
  ...
];

//根据keepalive控制
<template>
  <router-view v-slot="{ Component }">
    <keep-alive>
      <component v-if="$route.meta.keepAlive" :is="Component" />
    </keep-alive>
    <component v-if="!$route.meta.keepAlive" :is="Component" />
  </router-view>
</template>
```
使用include控制
```
//将需要添加到keepalive的路由进行缓存到数组  可以使用pinia/vuex

<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="['routeA','routeB']"> //缓存的路由数组
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>
```