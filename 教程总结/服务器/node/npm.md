
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