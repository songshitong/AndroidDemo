

json的序列化
```
 public JSONObject(@NonNull Map copyFrom) {
        this();
        Map<?, ?> contentsTyped = (Map<?, ?>) copyFrom;
        for (Map.Entry<?, ?> entry : contentsTyped.entrySet()) {
            //遍历hashMap，存储到nameValuePairs
            String key = (String) entry.getKey();
            if (key == null) { //key不能为null
                throw new NullPointerException("key == null");
            }
            nameValuePairs.put(key, wrap(entry.getValue()));
        }
    }
    
 @Nullable public static Object wrap(@Nullable Object o) {
        if (o == null) {
            return NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        if (o.equals(NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArray((Collection) o);
            } else if (o.getClass().isArray()) {
                return new JSONArray(o);
            }
            if (o instanceof Map) {
                return new JSONObject((Map) o);
            }
            if (o instanceof Boolean ||
                o instanceof Byte ||
                o instanceof Character ||
                o instanceof Double ||
                o instanceof Float ||
                o instanceof Integer ||
                o instanceof Long ||
                o instanceof Short ||
                o instanceof String) {
                return o;
            }
            if (o.getClass().getPackage().getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }    
```
1 value包装 null包装为JSONOBJECT.NULL对象  collection，Array包装为JSONArray，Map包装为JSONObject
以java.开头的类，转为string存储
2 遍历hashMap，存储到nameValuePairs

toString方法  不带换行或者空格
```
    @Override @NonNull public String toString() {
        try {
            JSONStringer stringer = new JSONStringer();
            writeTo(stringer);
            return stringer.toString();
        } catch (JSONException e) {
            return null;
        }
    }
    
   void writeTo(JSONStringer stringer) throws JSONException {
        stringer.object(); //构建 "{
        //写入LinkedHashMap中的key value
        for (Map.Entry<String, Object> entry : nameValuePairs.entrySet()) {
            stringer.key(entry.getKey()).value(entry.getValue());
        }
        stringer.endObject();//构建}"
    }    
```
org/json/JSONStringer.java
```
final StringBuilder out = new StringBuilder(); //存储结果
private final List<Scope> stack = new ArrayList<Scope>(); //当前写入的类型

   public JSONStringer object() throws JSONException {
        return open(Scope.EMPTY_OBJECT, "{");
    }

    JSONStringer open(Scope empty, String openBracket) throws JSONException {
        if (stack.isEmpty() && out.length() > 0) {
            throw new JSONException("Nesting problem: multiple top-level roots");
        }
        beforeValue();
        //存储Scope.EMPTY_OBJECT
        stack.add(empty);
        //存入 { 
        out.append(openBracket);
        return this;
    }

 private void beforeValue() throws JSONException {
        if (stack.isEmpty()) {
            return;
        }
        //获取最新的一个scope
        Scope context = peek();
        if (context == Scope.EMPTY_ARRAY) { // first in array
            //替换最新的scope为Scope.NONEMPTY_ARRAY
            replaceTop(Scope.NONEMPTY_ARRAY);
            newline();
        } else if (context == Scope.NONEMPTY_ARRAY) { // another in array
            //写入,
            out.append(',');
            newline();
        } else if (context == Scope.DANGLING_KEY) { // value for key
            //冒号是否带空格
            out.append(indent == null ? ":" : ": ");
            replaceTop(Scope.NONEMPTY_OBJECT);
        } else if (context != Scope.NULL) {
            throw new JSONException("Nesting problem");
        }
    } 
 
  //写入换行和空几个格  默认不写入
  private void newline() {
        if (indent == null) {
            return;
        }
        out.append("\n");
        for (int i = 0; i < stack.size(); i++) {
            out.append(indent);
        }
    }          
```

stringer.key()写入key
```
 public JSONStringer key(String name) throws JSONException {
        if (name == null) {
            throw new JSONException("Names must be non-null");
        }
        beforeKey();
        string(name); //写入key
        return this;
    }
 
     private void beforeKey() throws JSONException {
        Scope context = peek();
        if (context == Scope.NONEMPTY_OBJECT) { // first in object
            //key前面需要,
            out.append(',');
        } else if (context != Scope.EMPTY_OBJECT) { // not in an object!
            throw new JSONException("Nesting problem");
        }
        newline();
        replaceTop(Scope.DANGLING_KEY);
    }
    
 private void string(String value) {
        //写入"
        out.append("\"");
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                case '/':
                    out.append('\\').append(c);
                    break;

                case '\t':
                    out.append("\\t");
                    break;

                case '\b':
                    out.append("\\b");
                    break;

                case '\n':
                    out.append("\\n");
                    break;

                case '\r':
                    out.append("\\r");
                    break;

                case '\f':
                    out.append("\\f");
                    break;

                default:
                    if (c <= 0x1F) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        //写入"
        out.append("\"");
    }
```
"  \\  /  写为 \\"  \\\\ \\/
\t->\\t  \b->\\b  \n->\\n \r->\\r \f->\\f
0x1F以下写为\\字符04x   以上直接输出


stringer.value()写入value
```
public JSONStringer value(long value) throws JSONException {
        if (stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue(); //处理换行，冒号等
        out.append(value); //直接写入value
        return this;
    }

```

endObject()
```
 JSONStringer close(Scope empty, Scope nonempty, String closeBracket) throws JSONException {
        Scope context = peek();
        if (context != nonempty && context != empty) {
            throw new JSONException("Nesting problem");
        }
        //移除当前的scope
        stack.remove(stack.size() - 1);
        if (context == nonempty) {
            newline();
        }
        //写入 }
        out.append(closeBracket);
        return this;
    }
```
toString则是将当前out(StringBuilder)的值转为string输出了