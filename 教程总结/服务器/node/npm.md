
常用命令
install :
```
npm install
npm install <package-name>@<version>

npm install --verbose  //显示安装的详细信息 
npm install --loglevel silly //比verbose内容更多
```
Updating packages:
```
npm update <name>
```

Running Tasks
```
{
  "scripts": {
    "watch": "webpack --watch --progress --colors --config webpack.conf.js",
    "dev": "webpack --progress --colors --config webpack.conf.js",
    "prod": "NODE_ENV=production webpack -p --config webpack.conf.js"
  }
}

$ npm run watch
$ npm run dev
$ npm run prod
```


依赖冲突  https://www.cnblogs.com/goloving/p/16631481.html
```
 Fix the upstream dependency conflict, or retry
npm ERR! this command with --force, or --legacy-peer-deps
npm ERR! to accept an incorrect (and potentially broken) dependency resolution.
```
npm install --legacy-peer-deps
或者npm install --force
关于Peer Dependencies的解释
Peer Dependencies: In package.json file, there is an object called as peerDependencies and 
it consists of all the packages that are exactly required in the project or to the person who is downloading 
and the version numbers should also be the same. That is the reason they were named as peerDependencies. 
The best example is ‘react’ which is common in every project to run similarly.