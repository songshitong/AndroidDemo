

https://nodejs.org/en/learn/manipulating-files/nodejs-file-stats
文件状态
```
const fs = require('node:fs');
fs.stat('/Users/joe/test.txt', (err, stats) => {
  if (err) {
    console.error(err);
  }
  // we have access to the file stats in `stats`
  stats.isFile(); // true
  stats.isDirectory(); // false
  stats.isSymbolicLink(); // false
  stats.size; // 1024000 //= 1MB
});
```
支持同步模式statSync
支持promise-based
```
const fs = require('node:fs/promises');
async function example() {
  try {
    const stats = await fs.stat('/Users/joe/test.txt');
    ...
  } catch (err) {
    console.log(err);
  }
}
example();
```


https://nodejs.org/en/learn/manipulating-files/nodejs-file-paths
path处理
```
const path = require('node:path');
const notes = '/users/joe/notes.txt';
path.dirname(notes); // /users/joe
path.basename(notes); // notes.txt
path.extname(notes); // .txt

const name = 'joe';
path.join('/', 'users', name, 'notes.txt'); // '/users/joe/notes.txt'

path.resolve('joe.txt'); // '/Users/joe/joe.txt' if run from my home folder

path.normalize('/users/joe/..//test.txt'); // '/users/test.txt'
```

folder处理
https://nodejs.org/en/learn/manipulating-files/working-with-folders-in-nodejs
create a new folder
fs.mkdir() or fs.mkdirSync() or fsPromises.mkdir()
```
const fs = require('node:fs');
const folderName = '/Users/joe/test';
try {
  if (!fs.existsSync(folderName)) {
    fs.mkdirSync(folderName);
  }
} catch (err) {
  console.error(err);
}
```
read dir
```
const fs = require('node:fs');
const isFile = fileName => {
  return fs.lstatSync(fileName).isFile();
};
fs.readdirSync(folderPath)
  .map(fileName => {
    return path.join(folderPath, fileName);
  })
  .filter(isFile);
```
rename dir
```
const fs = require('node:fs');
fs.rename('/Users/joe', '/Users/roger', err => {
  if (err) {
    console.error(err);
  }
  // done
});
```
remove dir
```
const fs = require('node:fs');
fs.rm(dir, { recursive: true, force: true }, err => {
  if (err) {
    throw err;
  }
  console.log(`${dir} is deleted!`);
});
```


https://nodejs.org/en/learn/manipulating-files/working-with-file-descriptors-in-nodejs
fd
```
const fs = require('node:fs');
fs.open('/Users/joe/test.txt', 'r', (err, fd) => {
  // fd is our file descriptor
});
```
打开模式：
```
Flag	Description	                                    File gets created if it doesn't exist
r+	This flag opens the file for reading and writing	   ❌
w+	This flag opens the file for reading and writing and it also positions the stream at the beginning of the file	 ✅
a	This flag opens the file for writing and it also positions the stream at the end of the file	 ✅
a+	This flag opens the file for reading and writing and it also positions the stream at the end of the file	 ✅
```


https://nodejs.org/en/learn/manipulating-files/reading-files-with-nodejs
read file
```
const fs = require('node:fs');
fs.readFile('/Users/joe/test.txt', 'utf8', (err, data) => {
  if (err) {
    console.error(err);
    return;
  }
  console.log(data);
});
```
支持同步，promise   fs.readFileSync() and fsPromises.readFile()(require('node:fs/promises'))


https://nodejs.org/en/learn/manipulating-files/writing-files-with-nodejs
write file
```
const fs = require('node:fs');
const content = 'Some content!';
fs.writeFile('/Users/joe/test.txt', content, err => {
  if (err) {
    console.error(err);
  } else {
    // file written successfully
  }
});
```
支持同步fs.writeFileSync  支持promise
默认的write会覆盖内容，可以指定模式
```
fs.writeFile('/Users/joe/test.txt', content, { flag: 'a+' }, err => {});
```
flag类型
```
Flag	Description	                 File gets created if it doesn't exist
r+	This flag opens the file for reading and writing	❌
w+	This flag opens the file for reading and writing and it also positions the stream at the beginning of the file	✅
a	This flag opens the file for writing and it also positions the stream at the end of the file	✅
a+	This flag opens the file for reading and writing and it also positions the stream at the end of the file	✅
```

appendFile
```
const fs = require('node:fs');
const content = 'Some content!';
fs.appendFile('file.log', content, err => {
  if (err) {
    console.error(err);
  } else {
    // done!
  }
});
```