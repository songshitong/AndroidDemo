
https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick

What is the Event Loop?
The event loop is what allows Node.js to perform non-blocking I/O operations — despite the fact that 
JavaScript is single-threaded — by offloading operations to the system kernel whenever possible.
despite 尽管，任凭
```
   ┌───────────────────────────┐
┌─>│           timers          │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │     pending callbacks     │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │       idle, prepare       │
│  └─────────────┬─────────────┘      ┌───────────────┐
│  ┌─────────────┴─────────────┐      │   incoming:   │
│  │           poll            │<─────┤  connections, │
│  └─────────────┬─────────────┘      │   data, etc.  │
│  ┌─────────────┴─────────────┐      └───────────────┘
│  │           check           │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
└──┤      close callbacks      │
   └───────────────────────────┘
```
timers: this phase executes callbacks scheduled by setTimeout() and setInterval().
pending callbacks: executes I/O callbacks deferred to the next loop iteration. such as types of TCP errors
idle, prepare: only used internally.
poll: retrieve new I/O events; execute I/O related callbacks (almost all with the exception of close callbacks, 
   the ones scheduled by timers, and setImmediate()); node will block here when appropriate.
   The poll phase has two main functions:
      1 Calculating how long it should block and poll for I/O
      2 Processing events in the poll queue.
check: setImmediate() callbacks are invoked here.
close callbacks: some close callbacks, e.g. socket.on('close', ...).


测试setTimeout和setImmediate
```
setImmediate(() => {
 console.log('immediate');
});

setTimeout(() => {
 console.log('timeout');
}, 0);
```
结果：
```
immediate
timeout
```
颠倒一下
```
setTimeout(() => {
    console.log('timeout');
  }, 0);
  
  setImmediate(() => {
    console.log('immediate');
  });
```
结果：
```
timeout
immediate
```



Why use process.nextTick()?
There are two main reasons:
1 Allow users to handle errors, cleanup any then unneeded resources, or perhaps try the request again before the event loop continues.
2 At times it's necessary to allow a callback to run after the call stack has unwound but before the event loop continues.


EventEmitter
https://nodejs.org/en/learn/asynchronous-work/the-nodejs-event-emitter
```
const EventEmitter = require('node:events');
const eventEmitter = new EventEmitter();

eventEmitter.on('start', (start, end) => {
  console.log(`started from ${start} to ${end}`);
});
eventEmitter.emit('start', 1, 100);
```