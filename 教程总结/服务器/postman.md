
https://juejin.cn/post/7109426363677261837
 配置域名替换
 1 environment->配置Global   variable是要使用的参数，例如为url；value是不同环境的域名
 2 使用：在接口中使用variable替换  {{url}}/login
 3 切换环境，右上角，send按钮上面有环境切换

参数替换  例如不同环境token不同，同一个环境中多个api的token是一样的
1 environment->配置global   选择上面建立的环境，url下面新建参数即可
2 使用：Authorization: {{token}}


postman中使用js脚本   例如参数时间为随机数，或者需要通过一定计算获得
1 pre-request script中定义脚本
```
function getToken() {
  return 'Basic ' + Buffer.from('user:pwd', 'utf8').toString('base64');
}
var token = getToken();
console.log(token)
pm.globals.set("token", token); //设置为全局参数
```
2 请求中使用参数{{token}}
