


Object.assign
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/assign
The Object.assign() static method copies all enumerable own properties from one or more source objects to a target object.
It returns the modified target object
```
const target = { a: 1, b: 2 };
const source = { b: 4, c: 5 };

const returnedTarget = Object.assign(target, source);

console.log(target);
// Expected output: Object { a: 1, b: 4, c: 5 }

console.log(returnedTarget === target);
// Expected output: true
```

Object.getOwnPropertyNames()
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/getOwnPropertyNames
The Object.getOwnPropertyNames() static method returns an array of all properties (including non-enumerable 
properties except for those which use Symbol) found directly in a given object.
```
console.log(Object.getOwnPropertyNames(window));
```
结果：  数组长度有一千多。。。
```
[
    "0",
    "Object",
    "Function",
    "Array",
    "Number",
    "parseFloat",
    "parseInt",
    "Infinity",
    "NaN",
   ....
]
```

对象遍历
```
for(let i in obj){
   obj[i]
}

Object.keys(obj).forEach(function(key){

})

Object.getOwnPropertyNames(obj).forEach(function(key){
    console.log(key,obj[key]);
});

Reflect.ownKeys(obj).forEach(function(key){
console.log(key,obj[key]);
});
```


https://www.volcengine.com/theme/4782730-T-7-1
数组转对象
```
type ArraytoObject<T> = {
  [K in keyof T]: T[K] extends { key: infer K2; value: infer V } ? Record<K2, V> : never;
}[keyof T] & Record<string, unknown>;

function arrayToObject<T extends { key: string; value: any }>(arr: T[]): ArraytoObject<T> {
  const obj: any = {};
  arr.forEach(item => {
    obj[item.key] = item.value;
  });
  return obj;
}

const arr = [{ key: "name", value: "Tom" }, { key: "age", value: 18 }];
const obj = arrayToObject(arr);
console.log(obj); // {name: "Tom", age: 18}
```