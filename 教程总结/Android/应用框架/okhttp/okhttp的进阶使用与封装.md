
https://blog.csdn.net/ucxiii/article/details/52447945
okhttp异常： java.lang.IllegalStateException: closed
okhttp的responseBody是一个流，string()只能调用一次然后流就关闭了 再次使用就会出错，可能被其他拦截器关闭

https://github.com/hongyangAndroid/okhttputils