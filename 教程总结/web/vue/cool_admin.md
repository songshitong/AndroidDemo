

https://cool-js.com/admin/vue/service.html#%E8%AF%B4%E6%98%8E
关于service网络请求的说明
1 它不再需要前端开发者去创建与接口对应的文件，而是通过 EPS 的方法自动生成
2 service 的结构是根据 接口路径 转化成对应的层级

eps
开发环境下，系统通过服务端/admin/base/open/eps 获取所有的接口及实体。然后调用本地服务 /__cool_eps 生成 build/cool/dist/eps.d.ts 
   和 build/cool/dist/eps.json 两个文件。
/admin/base/open/eps是cool服务端提供的所有接口信息
eps.d.ts 用于 service 的类型提示    
路径也可能是build\cool\temp\eps.d.ts
```
declare namespace Eps {
    //实体类
	interface BaseSysDepartmentEntity {
		id?: number;
		name?: string;
		...
	}
	//接口方法及权限
	interface TaskInfo {
		delete(data?: any): Promise<any>;
		update(data?: any): Promise<any>;
		start(data?: any): Promise<any>;
		...
		permission: {
			delete: string;
			update: string;
			start: string;
			once: string;
			stop: string;
			info: string;
			page: string;
			log: string;
			add: string;
			list: string;
		};
		request: Service["request"];
	}
	
	//service请求提供的方法
	type Service = {
		request(options: {
			url: string;
			method?: "POST" | "GET" | string;
			data?: any;
			params?: any;
			proxy?: boolean;
			[key: string]: any;
		}): Promise<any>;
		test: Test;
		chat: { message: ChatMessage; session: ChatSession };
	    ....
		space: { info: SpaceInfo; type: SpaceType };
		task: { info: TaskInfo };
	};
}		
```
eps.json 用于生成 service 数据（build 时请确保该文件存在且最新）   
//build\cool\temp\eps.json
```
[["/admin/base/comm","",[["/personUpdate","post"],["/uploadMode","get"],["/permmenu","get"],["/person","get"]....
```
例如接口的路径是
/admin/demo/test/page
调用方式
```
service.demo.test.page().then((res) => {
	console.log(res);
});
```

接口不生成对应的方法，可能出现的问题
1 path路径包含级，后端生成url的最好只定义一级@GET("/info")
2 在admin下面的文件才会自动生成  例如publish/admin/app路径下面，自动生成的接口是
3 server提供了app/base/comm/eps的接口，但是前端并未获取接口信息，也就是只有admin下面的自动生成，app没有
app/base/comm/eps是收集app目录下的接口地址
```
interface PublishApp{ 
  info():Promise<any>
}
```


页面路由
路由是动态添加的，路由菜单会保存在server，同时根据权限决定是否展示  
https://cool-js.com/admin/vue/router.html
代码实现参考
src\cool\router\index.ts
src\modules\base\store\menu.ts
