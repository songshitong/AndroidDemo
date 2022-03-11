  String str1 = new StringBuilder("计算机").append("软件").toString();
  System.out.println(str1==str1.intern());
  String str2 = new StringBuilder("ja").append("va").toString();
  System.out.println(str2==str2.intern());
  
  
   stack=3, locals=3, args_size=1
           0: new           #2                  // class java/lang/StringBuilder
           3: dup
           4: ldc           #3                  // String 计算机
           6: invokespecial #4                  // Method java/lang/StringBuilder."<init>":(Ljava/lang/String;)V
           9: ldc           #5                  // String 软件
          11: invokevirtual #6                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
          14: invokevirtual #7                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
          17: astore_1
          18: getstatic     #8                  // Field java/lang/System.out:Ljava/io/PrintStream;
          21: aload_1
          22: aload_1
          23: invokevirtual #9                  // Method java/lang/String.intern:()Ljava/lang/String;
          26: if_acmpne     33
          29: iconst_1
          30: goto          34
          33: iconst_0
          34: invokevirtual #10                 // Method java/io/PrintStream.println:(Z)V
          37: new           #2                  // class java/lang/StringBuilder
          40: dup
          41: ldc           #11                 // String ja
          43: invokespecial #4                  // Method java/lang/StringBuilder."<init>":(Ljava/lang/String;)V
          46: ldc           #12                 // String va
          48: invokevirtual #6                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
          51: invokevirtual #7                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
          54: astore_2
          55: getstatic     #8                  // Field java/lang/System.out:Ljava/io/PrintStream;
          58: aload_2
          59: aload_2
          60: invokevirtual #9                  // Method java/lang/String.intern:()Ljava/lang/String;
          63: if_acmpne     70
          66: iconst_1
          67: goto          71
          70: iconst_0
          71: invokevirtual #10                 // Method java/io/PrintStream.println:(Z)V
          74: return
       
