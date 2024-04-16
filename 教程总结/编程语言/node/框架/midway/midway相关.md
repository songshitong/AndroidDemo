


swagger https://midwayjs.org/en/docs/2.0.0/extensions/swagger
访问：http://127.0.0.1:7001/swagger-ui/index.html 拿到 swagger UI 界面
注解使用
```
@CreateApiDoc()
  .summary('get user')
  .description('This is a open api for get user')
  .build()
```

路由和控制器
一般来说，控制器常用于对用户的请求参数做一些校验，转换，调用复杂的业务逻辑，拿到相应的业务结果后进行数据组装，然后返回。
在 Midway 中，控制器 也承载了路由的能力，每个控制器可以提供多个路由，不同的路由可以执行不同的操作
```
@Controller('/app') //分组
export class HomeController {

  @Get('/home') //  地址为/app/home
  async home() {
    return 'Hello Midwayjs!';
  }

  @Post('/update')//   地址为/app/update
  async updateData() {
    return 'This is a post method'
  }
}
```

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
