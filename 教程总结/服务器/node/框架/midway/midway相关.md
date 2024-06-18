


swagger https://midwayjs.org/en/docs/2.0.0/extensions/swagger
访问：http://127.0.0.1:7001/swagger-ui/index.html 拿到 swagger UI 界面
注解使用
```
@CreateApiDoc()
  .summary('get user')
  .description('This is a open api for get user')
  .build()
```

koa  https://midwayjs.org/docs/extensions/koa
midway使用了koa的路由基础能力，并默认内置了 session 和 body-parser 功能
bodyParser 功能，默认会解析 Post 请求，自动识别 json 和 form 类型
koa提供扩展 Context的能力
获取到原始的 Http Server
```
koa.Framework.getServer();
```


路由和控制器
一般来说，控制器常用于对用户的请求参数做一些校验，转换，调用复杂的业务逻辑，拿到相应的业务结果后进行数据组装，然后返回。
在 Midway 中，控制器 也承载了路由的能力，每个控制器可以提供多个路由，不同的路由可以执行不同的操作
```
@Controller('/app') //分组
export class HomeController {

  @Get('/home') //  地址为/app/home
  async home(@Query('uid') uid: string) { //query参数
    return 'Hello Midwayjs!';
  }

  @Post('/update')//   地址为/app/update
  async updateData(@Body('uid') uid: string) {//body参数
    return 'This is a post method'
  }
}
```
从路径获取参数
```
//指定装饰器
 @Get('/:uid')
  async getUser(@Param('uid') uid: string): Promise<User> {
    // xxxx
  }
  
//从params获取
 @Get('/:uid')
  async getUser(): Promise<User> {
    const params = this.ctx.params;
    // {
    //   uid: '1',
    // }
  }  
```


网络请求 HttpClient
```
 const httpclient = new HttpClient({
            headers: {
              'Content-Type': 'application/json',
              'Charset': 'UTF-8',
              'Authorization': token
            },
            timeout: 2000
          });
 httpclient.request(url,{ method: 'GET', dataType:'json'})  //请求method  响应数据的格式         
```
midway版本3.2.0
httpclient对于application/x-www-form-urlencoded形式处理有问题，建议使用node自带的request
urlencoded需要将参数格式化为 "a"="value"，但是httpclient格式化为"a=value" = ""


application和Context https://www.midwayjs.org/docs/req_res_app
Application 是某一个组件中的应用对象，在不同的组件中，可能有着不同的实现。Application 对象上会包含一些统一的方法，这些方法统一来自于 IMidwayApplication 定义。
在所有被依赖注入容器管理的类中，都可以使用 @App() 装饰器来获取 当前最主要 的 Application
```
@Controller('/')
export class HomeController {
  @App()
  app: Application;
}
```
Midway 应用对外暴露的协议是组件带来的，每个组件都会暴露自己协议对应的 Application 对象。
这就意味着在一个应用中会包含多个 Application，我们默认约定，在 src/configuration.ts 中第一个引入的 Application 即为 
Main Application （主要的 Application）
app的常用属性和方法
```
this.app.getAppDir() //项目根目录路径
this.app.getBaseDir(); //基础路径，默认开发中为 src 目录，编译后为 dist 目录
this.app.getEnv();
this.app.getApplicationContext();
this.app.getConfig("xx") //获取特定 key 配置
this.app.getLogger();
this.app.setAttr('abc', {
  a: 1,
  b: 2,
});
const value = this.app.getAttr('abc');
this.app.getNamespace() //获取到当前 app 归属的组件的 框架的类型（即组件的 namespace）,例如koa
```
常见的namespace
```
常见的 namespace 如下：
Package    	Namespace
@midwayjs/web	egg
@midwayjs/koa	koa
@midwayjs/express	express
@midwayjs/grpc	gRPC
@midwayjs/ws	webSocket
@midwayjs/socketio	socketIO
@midwayjs/faas	faas
@midwayjs/kafka	kafka
@midwayjs/rabbitmq	rabbitMQ
@midwayjs/bull	bull
```

Context
Context 是一个请求级别的对象，在每一次收到用户请求时，框架会实例化一个 Context 对象，
在 Http 场景中，这个对象封装了这次用户请求的信息，或者其他获取请求参数，设置响应信息的方法，在 WebSocket，Rabbitmq 等场景中，
Context 也有各自的属性，以框架的定义为准。
在 默认的请求作用域 中，也就是说在 控制器（Controller）或者普通的服务（Service）中，我们可以使用 @Inject 来注入对应的实例
```
@Controller('/')
export class HomeController {

  @Inject()
  ctx: Context;
  ...
}
```
在拦截器或者装饰器设计的时候，由于我们无法得知用户是否写了 ctx 属性，还可以通过内置的 REQUEST_OBJ_CTX_KEY 字段来获取
```
@Controller('/')
export class HomeController {

  @Inject()
  ctx: Context;

  @Get('/')
  async home() {
    ctx.logger.info(this.ctx === this[REQUEST_OBJ_CTX_KEY]);
    // => true
  }
}
```
context的属性和常用方法
```
//通过context获取service
const userService = await this.ctx.requestContext.getAsync(UserService);
this.ctx.logger
this.ctx.startTime
//当前请求的临时数据
this.ctx.setAttr('abc', {
  a: 1,
  b: 2,
});
const value = this.ctx.getAttr('abc');
this.ctx.getApp()
```

ORM框架 简化数据库的使用
midway支持多种orm框架以及数据库的使用
TypeORM https://www.midwayjs.org/docs/extensions/orm
配置使用的数据库
```
export default {
  typeorm: {
    dataSource: {
      default: {
        type: 'mysql', //当前使用的数据库为MySql
        host: '*******',
        port: 3306,
        username: '*******',
        password: '*******',
        database: undefined,
        synchronize: false,     // 如果第一次使用，不存在表，有同步的需求可以写 true，注意会丢数据
        logging: false,
        ...
      }
    }
  },
}
```
定义实体模型
```
// entity/photo.entity.ts
import { Entity, Column } from 'typeorm';

@Entity()
export class Photo {
  @Column()
  id: number;

  @Column()
  name: string;
  ...
}
```
查询数据
```
@Provide()
export class PhotoService {

  @InjectEntityModel(Photo)
  photoModel: Repository<Photo>;

  // find
  async findPhotos() {
    // find first
    let firstPhoto = await this.photoModel.findOne({
      where: {
        id: 1
      }
    });
    console.log("First photo from the db: ", firstPhoto);
    ....
  }
}
```



https://www.midwayjs.org/docs/middleware
Web 中间件(web middleware)
Web 中间件是在控制器调用 之前 和 之后（部分）调用的函数。 中间件函数可以访问请求和响应对象
```
client  --http request-> web中间件  ---> controller
        <-http response- web中间件  <---
```
不同的上层 Web 框架中间件形式不同，Midway 标准的中间件基于 洋葱圈模型。而 Express 则是传统的队列模型。
Koa 和 EggJs 可以在 控制器前后都被执行，在 Express 中，中间件 只能在控制器之前 调用

示例：打印控制器（Controller）执行的时间
```
@Middleware()
export class ReportMiddleware implements IMiddleware<Context, NextFunction> {

  resolve() {
    return async (ctx: Context, next: NextFunction) => {
      // 控制器前执行的逻辑
      const startTime = Date.now();
      // 执行下一个 Web 中间件，最后执行到控制器
      // 这里可以拿到下一个中间件或者控制器的返回值
      const result = await next();
      // 控制器之后执行的逻辑
      console.log(Date.now() - startTime);
      // 返回给上一个中间件的结果
      return result;
    };
  }

  static getName(): string {
    return 'report';
  }
  
   ignore(ctx: Context): boolean {
    // 下面的路由将忽略此中间件
    return ctx.path === '/'
      || ctx.path === '/api/auth'
      || ctx.path === '/api/login';
  }

 match(ctx: Context): boolean {
    // 下面的匹配到的路由会执行此中间件  match与ignore只能配置一个
    if (ctx.path === '/api/index') {
      return true;
    }
  }

}
```
await next() 则代表了下一个要执行的逻辑，这里一般代表控制器执行，在执行的前后，我们可以进行一些打印和赋值操作，这也是洋葱圈模型最大的优势。

根据应用到的位置，分为两种：
1、全局中间件，所有的路由都会执行的中间件，比如 cookie、session、统一返回数据结构 等等
```
@Configuration({
  imports: [koa]
  // ...
})
export class MainConfiguration {
  ...
  async onReady() {
    this.app.useMiddleware(ReportMiddleware); //配置数组：this.app.useMiddleware([ReportMiddleware1, ReportMiddleware2])
  }
}
```
2、路由中间件，单个/部分路由会执行的中间件，比如某个路由的前置校验，数据处理等等
给controller配置或者给单个GET，POST请求配置



服务和注入  https://www.midwayjs.org/docs/service
在业务中，只有控制器（Controller）的代码是不够的，一般来说会有一些业务逻辑被抽象到一个特定的逻辑单元中，我们一般称为服务（Service）
提供这个抽象有以下几个好处：
保持 Controller 中的逻辑更加简洁。
保持业务逻辑的独立性，抽象出来的 Service 可以被多个 Controller 重复调用。
将逻辑和展现分离，更容易编写测试用例
创建服务
```
@Provide()
export class UserService {

  async getUser(id: number) {
    return {
      id,
      name: 'Harry',
      age: 18,
    };
  }
}

@Provide 装饰器的作用：
1、这个 Class，被依赖注入容器托管，会自动被实例化（new）
2、这个 Class，可以被其他在容器中的 Class 注入
```
使用服务
```
@Controller('/api/user')
export class APIController {
  @Inject()
  userService: UserService;

  @Get('/')
  async getUser(@Query('id') uid) {
    const user = await this.userService.getUser(uid);
    return {success: true, message: 'OK', data: user};
  }
}
@Inject 装饰器，作用为：
1、在依赖注入容器中，找到对应的属性名，并赋值为对应的实例化对象
```


异常处理
Midway 提供了一个内置的异常处理器，负责处理应用程序中所有未处理的异常。当您的应用程序代码抛出一个异常处理时，该处理器就会捕获该异常，然后等待用户处理。
异常处理器的执行位置处于中间件之后，所以它能拦截所有的中间件和业务抛出的错误。
```
---请求(request)------->middle ware  --请求-->controller
response<--Error Filter<--response-- middle ware <--响应--
```
异常处理器
内置的异常处理器用于标准的请求响应场景，它可以捕获所有请求中抛出的错误。
通过 @Catch 装饰器我们可以定义某一类异常的处理程序，我们可以轻松的捕获某一类型的错误，做出处理，也可以捕获全局的错误，返回统一的格式。
同时，框架也提供了一些默认的 Http 错误，放在 httpError 这个对象下。
比如捕获抛出的 InternalServerErrorError 错误
```
@Catch(httpError.InternalServerErrorError)
export class InternalServerErrorFilter {
  async catch(err: MidwayHttpError, ctx: Context) {

    // ...
    return 'got 500 error, ' + err.message;
  }
}

//使用:
export class MainConfiguration {

  @App()
  app: koa.Application;

  async onReady() {
    this.app.useFilter([InternalServerErrorFilter]);
  }
}
```

管道：
管道是参数装饰器的内部机制，可以在参数装饰器逻辑之后执行一些自定义代码，一般用于以下的场景：
1、数据的校验
2、参数的转换
@Body 装饰器已经被自动注册了 ValidatePipe ，如果 UserDTO 是一个已经经过 @Rule 装饰器修饰的 DTO，会自动校验并转换。
```
export class HomeController {

  @Post('/')
  async updateUser(@Body() user: UserDTO ) {
    // ...
  }
}
```



守卫
守卫会根据运行时出现的某些条件（例如权限，角色，访问控制列表等）来确定给定的请求是否由路由处理程序处理。
普通的应用程序中，一般会在中间件中处理这些逻辑，但是中间件的逻辑过于通用，同时也无法很优雅的去和路由方法进行结合，为此我们在中间件之后，
    进入路由方法之前设计了守卫，可以方便的进行方法鉴权等处理。
守卫会在中间件 之后，路由方法 之前 执行。
编写一个守卫，用于角色鉴权。
```
@Guard()
export class AuthGuard implements IGuard<Context> {
  async canActivate(context: Context, supplierClz, methodName: string): Promise<boolean> {
    // 从类元数据上获取角色信息
    const roleNameList = getPropertyMetadata<string[]>(ROLE_META_KEY, supplierClz, methodName);
    if (roleNameList && roleNameList.length && context.user.role) {
      // 假设中间件已经拿到了用户角色信息，保存到了 context.user.role 中
      // 直接判断是否包含该角色
      return roleNameList.includes(context.user.role);
    }

    return false;
  }
}
```
守卫使用，@UseGuard(AuthGuard)可以在controller,单个路由
全局使用在MainConfiguration中:
```
async onReady() {
  this.app.useGuard([AuthGuard, Auth2Guard]);
}
```


JWT https://www.midwayjs.org/docs/extensions/jwt
JSON Web Token (JWT)是一个开放标准(RFC 7519)，它定义了一种紧凑的、自包含的方式，用于作为JSON对象在各方之间安全地传输信息。
该信息可以被验证和信任，因为它是数字签名的。
Midway 提供了 jwt 组件，简单提供了一些 jwt 相关的 API，可以基于它做独立的鉴权和校验
```
import { JwtService } from '@midwayjs/jwt'; //jwt组件提供的service

@Middleware()
export class JwtMiddleware {
  @Inject()
  jwtService: JwtService;

  resolve() {
    return async (ctx: Context, next: NextFunction) => {
      // 判断下有没有校验信息
      if (!ctx.headers['authorization']) {
        throw new httpError.UnauthorizedError();
      }
      // 从 header 上获取校验信息
      const parts = ctx.get('authorization').trim().split(' ');

      if (parts.length !== 2) {
        throw new httpError.UnauthorizedError();
      }

      const [scheme, token] = parts;

      if (/^Bearer$/i.test(scheme)) {
        try {
          //jwt.verify方法验证token是否有效
          await this.jwtService.verify(token, {
            complete: true,
          });
        } catch (error) {
          //token过期 生成新的token
          const newToken = getToken(user);
          //将新token放入Authorization中返回给前端
          ctx.set('Authorization', newToken);
        }
        await next();
      }
    };
  }
```


跨域  https://www.midwayjs.org/docs/extensions/cross_domain
什么是 CORS
```
Access to fetch at 'http://127.0.0.1:7002/' from origin 'http://127.0.0.1:7001' has been blocked by CORS policy:
 No 'Access-Control-Allow-Origin' header is present on the requested resource. If an opaque response serves your needs,
  set the request's mode to 'no-cors' to fetch the resource with CORS disabled.
```
出于安全性，浏览器限制脚本内发起的跨源 HTTP 请求。例如，XMLHttpRequest 和 Fetch API 遵循同源策略。这意味着使用这些 API 的 Web 应用程序
只能从加载应用程序的同一个域请求 HTTP 资源，除非响应报文包含了正确 CORS 响应头。

CORS  "跨域资源共享"（Cross-origin resource sharing）

CORS 机制允许 Web 应用服务器进行跨源访问控制，从而使跨源数据传输得以安全进行。现代浏览器支持在 API 容器中（例如 XMLHttpRequest 或 Fetch）使用 CORS，
以降低跨源 HTTP 请求所带来的风险。
可以参考 https://www.ruanyifeng.com/blog/2016/04/cors.html
未使用 credentials //凭证，资格，证明，证书
```
export default {
  // ...
  cors: {
    origin: '*',
  },
}
```
使用 credentials
```
//客户端fetch
fetch(url, {
  credentials: "include",
});

//服务端配置
export default {
  // ...
  cors: {
    credentials: true,
  },
}
```
限制 origin 来源
```
export default {
  // ...
  cors: {
    origin: 'http://127.0.0.1:7001',
    credentials: true,
  },
}
```
完整配置
```
export const cors = {
  // 允许跨域的方法，【默认值】为 GET,HEAD,PUT,POST,DELETE,PATCH
  allowMethods: string |string[];
  // 设置 Access-Control-Allow-Origin 的值，【默认值】会获取请求头上的 origin
  // 也可以配置为一个回调方法，传入的参数为 request，需要返回 origin 值
  // 例如：http://test.midwayjs.org
  // 如果设置了 credentials，则 origin 不能设置为 *
  origin: string|Function;
  // 设置 Access-Control-Allow-Headers 的值，【默认值】会获取请求头上的 Access-Control-Request-Headers
  allowHeaders: string |string[];
  // 设置 Access-Control-Expose-Headers 的值
  exposeHeaders: string |string[];
  // 设置 Access-Control-Allow-Credentials，【默认值】false
   // 也可以配置为一个回调方法，传入的参数为 request，返回值为 true 或 false
  credentials: boolean|Function;
  // 是否在执行报错的时候，把跨域的 header 信息写入到 error 对的 headers 属性中，【默认值】false
  keepHeadersOnError: boolean;
  // 设置 Access-Control-Max-Age
  maxAge: number;
}
```


生命周期相关  https://www.midwayjs.org/docs/lifecycle
在通常情况下，我们希望在应用启动的时候做一些初始化、或者其他一些预处理的事情，比如创建数据库连接、预生成一些配置，而不是在请求响应时去处理
框架提供了这些生命周期函数供开发人员处理：
1  配置文件加载，我们可以在这里去修改配置（onConfigLoad）
2  依赖注入容器准备完毕，可以在这个阶段做大部分的事情（onReady）
3  服务启动完成，可以拿到 server（onServerReady）
4  应用即将关闭，在这里清理资源（onStop）
```
@Configuration()
export class MainConfiguration implements ILifeCycle {

  async onConfigLoad(): Promise<void> {
    // 直接返回数据，会自动合并到配置中
    return {
      test: 1
    }
  }
}
```


定时任务
本地机器运行 cron  https://www.midwayjs.org/docs/extensions/cron
跨机器的任务队列 bull https://www.midwayjs.org/docs/extensions/bull
任务队列
队列是一种强大的设计模式，可帮助您应对常见的应用程序扩展和性能挑战。队列可以帮助您解决的一些问题。
示例如下：
平滑处理峰值。可以在任意时间启动资源密集型任务，然后将这些任务添加到队列中，而不是同步执行。让任务进程以受控方式从队列中提取任务。
   也可以轻松添加新的队列消费者以扩展后端任务处理。
分解可能会阻塞 Node.js 事件循环的单一任务。比如用户请求需要像音频转码这样的 CPU 密集型工作，就可以将此任务委托给其他进程，
   从而释放面向用户的进程以保持响应。
提供跨各种服务的可靠通信渠道。例如，您可以在一个进程或服务中排队任务（作业），并在另一个进程或服务中使用它们。在任何流程或服务的作业生命周期中完成、
  错误或其他状态更改时，您都可以收到通知（通过监听状态事件）。当队列生产者或消费者失败时，它们的状态被保留，并且当节点重新启动时任务处理可以自动重新启动。
midwayjs/task模块从v3.6.0开始废弃，推荐使用midwayjs/cron
```
// src/job/sync.job.ts
import { Job, IJob } from '@midwayjs/cron';
import { FORMAT } from '@midwayjs/core';

@Job({
  cronTime: FORMAT.CRONTAB.EVERY_PER_30_MINUTE, //执行周期
  start: true, //是否自动启动任务
  runOnInit:true,//是否在初始化就执行一次
})
export class DataSyncCheckerJob implements IJob {
  async onTick() {
    // ...
  }
  async onComplete() {
    // 记录一些数据等等，用处不是很大
  }
}
```
cron规则
工具：https://crontab.guru/
https://en.wikipedia.org/wiki/Cron
The cron command-line utility is a job scheduler on Unix-like operating systems.
基本规则
```
# ┌───────────── minute (0–59)
# │ ┌───────────── hour (0–23)
# │ │ ┌───────────── day of the month (1–31)
# │ │ │ ┌───────────── month (1–12)
# │ │ │ │ ┌───────────── day of the week (0–6) (Sunday to Saturday;
# │ │ │ │ │                                   7 is also Sunday on some systems)
# │ │ │ │ │
# │ │ │ │ │
# * * * * * <command to execute>
```

https://midwayjs.org/en/docs/2.0.0/extensions/cache
缓存用户信息，token等到本地机器
```
cacheManager.set(key, value, { ttl: 1000 }); // ttl的单位为秒 
cacheManager.set(key, value, { ttl: null }); //禁止过期
cacheManager.get(key); //undifined
cacheManager.del(key);
cacheManager.reset(); //全部清空
```