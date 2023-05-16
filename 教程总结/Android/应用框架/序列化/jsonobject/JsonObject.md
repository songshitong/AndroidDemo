android sdk 32  java版本应该是11
```
1 反序列化
val jsonObject = JSONObject(data)
val id = jsonObject.getInt("id")
2 序列化
使用put
JSONObject obj = new JSONObject();
  obj.put("name", "John");
  //调用toString()方法可直接将其内容打印出来
  System.out.println(obj.toString());
使用HashMap
Map<String, Object> data = new HashMap<String, Object>();
  data.put("name", "John");
  JSONObject obj = new JSONObject(data);
  System.out.println(obj.toString());  
```

org\json\JSONObject.java
```
//解析的结果存储在LinkedHashMap
private final LinkedHashMap<String, Object> nameValuePairs;
    public JSONObject(@NonNull String json) throws JSONException {
        this(new JSONTokener(json));
    }
    
org/json/JSONTokener.java
    public JSONTokener(String in) {
        if (in != null && in.startsWith("\ufeff")) {
            in = in.substring(1);
        }
        this.in = in;
    } 

  public JSONObject(@NonNull JSONTokener readFrom) throws JSONException {
        Object object = readFrom.nextValue();
        //读取string，如果不是JsonObject抛出异常  例如解析为JSONArray或String，抛出异常
        if (object instanceof JSONObject) {
            this.nameValuePairs = ((JSONObject) object).nameValuePairs;
        } else {
            throw JSON.typeMismatch(object, "JSONObject");
        }
    }       
```
https://blog.csdn.net/chenmozhe22/article/details/89472790
ufeff相关
首行出现的”\ufeff“叫BOM(“ByteOrder Mark”)用来声明该文件的编码信息.
”utf-8“ 是以字节为编码单元,它的字节顺序在所有系统中都是一样的,没有字节序问题,因此它不需要BOM,所以当用"utf-8"编码方式读取带有BOM的文件时,
  它会把BOM当做是文件内容来处理
“uft-8-sig"中sig全拼为 signature 也就是"带有签名的utf-8”, 因此"utf-8-sig"读取带有BOM的"utf-8文件时"会把BOM单独处理,
  与文本内容隔离开,也是我们期望的结果.


nextValue解析
```
 public Object nextValue() throws JSONException {
        int c = nextCleanInternal();
        switch (c) {
            case -1:
                throw syntaxError("End of input");

            case '{':
                return readObject();

            case '[':
                return readArray();

            case '\'':
            case '"':
                return nextString((char) c);

            default:
                pos--;
                return readLiteral();
        }
    }
    

 private int nextCleanInternal() throws JSONException {
        while (pos < in.length()) {
            int c = in.charAt(pos++);
            switch (c) {
                //跳过 \t 空格 \n \r
                case '\t':
                case ' ':
                case '\n':
                case '\r':
                    continue;

                case '/':
                    if (pos == in.length()) {
                        return c;
                    }
                    char peek = in.charAt(pos);
                    switch (peek) {
                        case '*':
                            //跳过 /*
                            pos++;
                            int commentEnd = in.indexOf("*/", pos);
                            if (commentEnd == -1) {
                                throw syntaxError("Unterminated comment");
                            }
                            pos = commentEnd + 2;
                            continue;

                        case '/':
                            // 跳过//
                            pos++;
                            skipToEndOfLine();
                            continue;

                        default:
                            return c;
                    }

                case '#':
                    //跳过 #
                    skipToEndOfLine();
                    continue;

                default:
                    return c;
            }
        }
        return -1;
    }    
```
1 跳过#，空格，\t  \n \r /* // 等
2 匹配{  读取为object
  匹配[  读取为array
  匹配'或" 读取为String 
  可能为null,数字，布尔等

readObject
```
private JSONObject readObject() throws JSONException {
        JSONObject result = new JSONObject();
        int first = nextCleanInternal();
        if (first == '}') {
            //空对象，返回
            return result;
        } else if (first != -1) {
           //下一个有值，回退
            pos--;
        }

        while (true) {
            Object name = nextValue();
            //读取值，如果不是string抛出异常，key必须为string
            if (!(name instanceof String)) {
                if (name == null) {
                    throw syntaxError("Names cannot be null");
                } else {
                    throw syntaxError("Names must be strings, but " + name
                            + " is of type " + name.getClass().getName());
                }
            }

            int separator = nextCleanInternal();
            //分隔符不存在，抛出异常
            if (separator != ':' && separator != '=') {
                throw syntaxError("Expected ':' after " + name);
            }
            //遇到> 跳过
            if (pos < in.length() && in.charAt(pos) == '>') {
                pos++;
            }
            //将key和读取的下一个value存储到nameValuePairs
            result.put((String) name, nextValue());

            //读取下一对
            switch (nextCleanInternal()) {
                case '}': //读取完成，返回JsonObject
                    return result;
                case ';':
                case ',':
                    continue;
                default:
                    throw syntaxError("Unterminated object");
            }
        }
    }
```

获取JsonObject后，json解析已经完成，结果存储在LinkedHashMap中
jsonObject.getInt("id")
```
    public int getInt(@NonNull String name) throws JSONException {
        Object object = get(name); //nameValuePairs获取存储的结果        Object result = nameValuePairs.get(name);
        //将long或者double转为Int
        Integer result = JSON.toInteger(object);
        if (result == null) { //int结果为null
            throw JSON.typeMismatch(name, object, "int");
        }
        return result;
    }
```

读取String
nextString
```
//quote 为'或"
public String nextString(char quote) throws JSONException {
        StringBuilder builder = null;
        int start = pos;

        while (pos < in.length()) {
            int c = in.charAt(pos++);
            if (c == quote) {
                //再一次遇到'或"，文字读取结束
                if (builder == null) {
                    // a new string avoids leaking memory
                    return new String(in.substring(start, pos - 1));
                } else {
                    builder.append(in, start, pos - 1);
                    return builder.toString();
                }
            }
            //文字中遇到转义字符
            if (c == '\\') {
                if (pos == in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append(in, start, pos - 1);
                builder.append(readEscapeCharacter());
                start = pos;
            }
        }
        throw syntaxError("Unterminated string");
    }
    
 
  private char readEscapeCharacter() throws JSONException {
        char escaped = in.charAt(pos++);
        switch (escaped) {
            //unicode转换
            case 'u':
                if (pos + 4 > in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                String hex = in.substring(pos, pos + 4);
                pos += 4;
                try {
                    return (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException nfe) {
                    throw syntaxError("Invalid escape sequence: " + hex);
                }
            case 't':
                return '\t';
            case 'b':
                return '\b';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 'f':
                return '\f';
            case '\'':
            case '"':
            case '\\':
            default:
                return escaped;
        }
    }
```
字符串读取主要是匹配''或""  对于unicode需要转码
对于 //t->\t   //b->\b //n->\n  //r->\n //f->\f 等  //后面为\ " \\原样返回

readArray
```
   private JSONArray readArray() throws JSONException {
        JSONArray result = new JSONArray();
        //是否以,]结尾
        boolean hasTrailingSeparator = false;
        while (true) {
            switch (nextCleanInternal()) {
                case -1:
                    throw syntaxError("Unterminated array");
                case ']':
                    //数组结束了，返回结果
                    if (hasTrailingSeparator) {
                        result.put(null);
                    }
                    return result;
                case ',':
                case ';':
                    //只有分割符，没有值
                    result.put(null);
                    hasTrailingSeparator = true;
                    continue;
                default:
                    pos--;
            }
            //读取数组中的值 存储在JsonArray的List<Object>
            result.put(nextValue());

            switch (nextCleanInternal()) {
                case ']': //数组读取完成，返回
                    return result;
                case ',':
                case ';':
                    hasTrailingSeparator = true;
                    continue;
                default:
                    throw syntaxError("Unterminated array");
            }
        }
    }
```

其他值的读取readLiteral
```
 private Object readLiteral() throws JSONException {
        //读取到 \r \n {}[]/\\:,=;# \t\f 或者结尾返回 
        String literal = nextToInternal("{}[]/\\:,=;# \t\f");

        if (literal.length() == 0) {
            throw syntaxError("Expected literal value");
        } else if ("null".equalsIgnoreCase(literal)) {
            //json中null字符
            return JSONObject.NULL;
        } else if ("true".equalsIgnoreCase(literal)) {
           // true
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(literal)) {
           //false
            return Boolean.FALSE;
        }

        //不包含.
        if (literal.indexOf('.') == -1) {
            int base = 10;
            String number = literal;
            if (number.startsWith("0x") || number.startsWith("0X")) {
                number = number.substring(2);
                base = 16;
            } else if (number.startsWith("0") && number.length() > 1) {
                number = number.substring(1);
                base = 8;
            }
            //解析为16进制，10进制，或者8进制的long
            try {
                long longValue = Long.parseLong(number, base);
                if (longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
                    return (int) longValue;
                } else {
                    return longValue;
                }
            } catch (NumberFormatException e) {
                //超过long的最大值，按照string返回
            }
        }

        //包含.  尝试解析为double类型
        try {
            return Double.valueOf(literal);
        } catch (NumberFormatException ignored) {
        }

        /* ... finally give up. We have an unquoted string */
        return new String(literal); // a new string avoids leaking memory
    }
```
1 解析json的 null,true,false,Long,double  
2 以上都不是转为string返回



