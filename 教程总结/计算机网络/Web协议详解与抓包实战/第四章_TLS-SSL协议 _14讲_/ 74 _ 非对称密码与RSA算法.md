非对称密码用来传递秘钥

非对称密码
• 每个参与方都有一对密钥
• 公钥
  • 向对方公开
• 私钥
  • 仅自己使用


非对称加解密的过程   
• 加密      怎么拿到公钥1 pki基础设施，从第三方拿到的   2 建立连接通过握手传递 TLS的握手
  • 使用对方的公钥加密消息
• 解密
  • 使用自己的私钥解密消息


RSA 算法
• 1977 年由罗纳德·李维斯特（Ron Rivest）、阿迪·萨莫尔（Adi Shamir）和伦纳德·阿德曼 （Leonard Adleman）一起提出，因此名为 RSA 算法

RSA 算法中公私钥的产生
明文的长度要小于n
RSA 算法的安全性来源于对一个大数做因数分解非常的困难

RSA 算法加解密流程
算法用了大量的乘法，相对于对称加密，性能很慢
mod  取余数，取模




LearnAndTry
数学在计算机中的最好应用。
作者回复: 是的