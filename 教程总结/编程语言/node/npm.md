
常用命令
install :
```
npm install
npm install <package-name>@<version>
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