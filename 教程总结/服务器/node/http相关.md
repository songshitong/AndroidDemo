

https://juejin.cn/post/6983238709097267236
Http模块，主要的应用是两部分:
http.createServer 担当web服务器
http.createClient，担当客户端，实现爬虫之类的工作

服务器相关
```
const http = require('http')

const hostname = '127.0.0.1'
const port = 3000

const server = http.createServer((req, res) => {
  res.statusCode = 200
  res.setHeader('Content-Type', 'text/plain') //writeHead，200表示页面正常，text/plain表示是文字。
  res.end('Hello World\n') // end 完成写入
})

server.listen(port, hostname, () => {
  console.log(`服务器运行在 http://${hostname}:${port}`)
})

server.timeout = 1000 //设置超时为1秒
// 停止服务端接收新的连接
server.close([callback])
```
req对象
request.url, 客户端请求的url地址
request.headers, 客户端请求的http header
request.method, 获取请求的方式，一般有几个选项，POST,GET和DELETE等，服务器可以根据客户端的不同请求方法进行不同的处理。
request.httpVersion, http的版本
request.trailers, 存放附加的一些http头信息
request.socket, 用于监听客户端请求的socket对象

res对象
response.writeHead(statusCode, [reasonPhrase], [headers])
response.statusCode, html页面状态值
response.header, 返回的http header，可以是字符串，也可以是对象
response.setTimeout(msecs, callback), 设置http超时返回的时间，一旦超过了设定时间，连接就会被丢弃
response.statusCode, 设置返回的网页状态码
response.setHeader(name, value), 设置http协议头
response.headersSent, 判断是否设置了http的头
response.write(chunk, [encoding]), 返回的网页数据，[encoding] 默认是 utf-8
response.end([data], [encoding]), 响应结束



客户端请求接口
```
const http = require('http')
let options = {
    hostname: 'www.example.com',
    port: 80,
    path: '/',
    method: 'GET'
}

const req = http.request(options, (res) => {
    console.log(`STATUS: ${res.statusCode}`) //返回状态码
    console.log(`HEADERS: ${JSON.stringify(res.headers, null, 4)}`) // 返回头部
    res.setEncoding('utf8') // 设置编码
    res.on('data', (chunk) => { //监听 'data' 事件
        console.log(`主体: ${chunk}`)
    })

})
req.end() // end方法结束请求
```


url解析与参数格式化
url解析
```
querystring.parse('foo=bar&baz=qux&baz=quux&corge')
// returns
{ foo: 'bar', baz: ['qux', 'quux'], corge: '' }
```
对象转为字符串
```
querystring.stringify({name: 'whitemu', sex: [ 'man', 'women' ] });

// returns
'name=whitemu&sex=man&sex=women'
```