ECDH 密钥交换协议

• DH 密钥交换协议使用椭圆曲线后的变种，称为 Elliptic Curve Diffie–Hellman key Exchange，缩写为 ECDH，优点是比 DH 计算速度快、
   同等安全条件下密钥更短
• ECC（Elliptic Curve Cryptography）：椭圆曲线密码学
• 魏尔斯特拉斯椭圆函数（Weierstrass‘s elliptic functions）：y 2 =x 3 +ax+b



ECDH 的步骤

• 步骤
  1. Alice 选定大整数 Ka 作为私钥
  2. 基于选定曲线及曲线上的共享 P 点，Alice 计算出 Qa=Ka.P
  3. Alice 将 Qa、选定曲线、共享 P 点传递点 Bob
  4. Bob 选定大整数 Kb 作为私钥，将计算了 Qb=Kb.P，并将 Qb 传递给 Alice
  5. Alice 生成密钥 Qb.Ka = (X, Y)，其中 X 为对称加密的密钥
  6. Bob 生成密钥 Qa.Kb = (X, Y)，其中 X 为对称加密的密钥
• Qb.Ka = Ka.(Kb.P) = Ka.Kb.P = Kb.(Ka.P) = Qa.Kb    椭圆曲线的结合律
     

TLS1.2  www.sina.com.cn
  TLS 中  Client Hello  Cipher Suites(16 suites)安全套件
  Server Hello  
      Cipher Suite: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 (0xc02f)   安全套件选择ECDHE
  11	0.021010	60.28.216.241	10.24.61.65	TLSv1.2	1400		Certificate, Server Key Exchange, Server Hello Done
      EC Diffie-Hellman Server Params
      Curve Type: named_curve (0x03)
      Named Curve: x25519 (0x001d)     选择的椭圆曲线x25519  基点和参数定下来了
      Pubkey Length: 32
      Pubkey: b6b50a013c9ba067b3eca26c30e5a4a0e04b9a3abe47cf405ad68d9f22943344   发给client的公钥
      Signature Algorithm: rsa_pss_rsae_sha256 (0x0804)
      Signature Length: 256
      Signature: 8f158d42803ac0318cab835ac94072271b87270427c63a2a33b3291a777732d0b4d1730d…

13	0.027425	10.24.61.65	60.28.216.241	TLSv1.2	147		Client Key Exchange, Change Cipher Spec, Finished
      EC Diffie-Hellman Client Params
      Pubkey Length: 32
      Pubkey: 940a767a764234156a6613c8bdab68c59a84238e8b3df780d71f07b6eafe5828  发给sever的公钥




X25519 曲线   TLS1.3
 • 椭圆曲线变种：Montgomery curve 蒙哥马利曲线
  G点是个固定的值





龍少²⁰¹⁹
这些数学家也太厉害了，这么复杂的曲线也能想到用于密码学。
作者回复: 密码学依赖数学的进步^_^