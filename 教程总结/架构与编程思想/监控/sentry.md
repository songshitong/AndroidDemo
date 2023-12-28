
sentry 默认关闭dump  设置——>隐私
Store Native Crash Reports
Store native crash reports such as Minidumps for improved processing and download in issue details


https://juejin.cn/post/6844904143090352135
sentry是一个基于Django构建的现代化的实时事件日志监控、记录和聚合平台,主要用于如何快速的发现故障。支持几乎所有主流开发语言和平台,
并提供了现代化UI,它专门用于监视错误和提取执行适当的事后操作所需的所有信息,而无需使用标准用户反馈循环的任何麻烦。官方提供了多个语言的SDK.
让开发者第一时间获悉错误信息,并方便的整合进自己和团队的工作流中
//可以将不同的bug指定开发者

issues
查看anr  查询不同线程的状态，以及dump信息

Performance Monitoring
Apdex(Application Performance Index)
是由Apdex联盟开发的用于评估应用性能的工业标准。Apdex标准从用户的角度出发，将对应用响应时间的表现，转为用户对于应用性能的可量化范围为0-1的满意度评价
主要指标
T: Threshold for the target response time.
Satisfactory: Users are satisfied using the app when their page load times are less than or equal to T.
//可容忍 Tolerable: Users consider the app tolerable to use when their page load times are greater than T and less than or equal to 4T.
//懊恼 Frustrated: Users are frustrated with the app when their page load times are greater than 4T.
Apdex: (Number of Satisfactory Requests + (Number of Tolerable Requests/2)) / (Number of Total Requests)

Failure Rate 网络请求失败率
根据http返回码判断

Throughput (Total, TPM, TPS) 吞吐量
average transactions per minute (TPM)
average transactions per second (TPS)

Latency
Average Transaction Duration
P50 Threshold
P50 阈值表示 50% 的事务持续时间大于阈值。这也是中位数。例如，如果 P50 阈值设置为 10 毫秒，则 50% 的事务超过该阈值，耗时超过 10 毫秒
P75 Threshold
P95 Threshold
P99 Threshold

Frequency 主要有四个维度
count
count unique values (for a given field)
average requests (transactions) per second
average requests (transactions) per minute

User Misery
用户加权的性能指标，用于评估应用程序性能的相对大小
User Misery 突出显示对用户影响最大的事务。


Session Replay
类似视频形式，查看用户问题发生时进行了哪些操作  页面是简版的布局，可能是为了保护用户信息，同时，方便实现页面动作