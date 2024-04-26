


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