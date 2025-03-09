todo
https://www.jianshu.com/p/e636f4f8487b
https://www.jianshu.com/p/b87fee2f7a23

XmlParse有例子


android 11
frameworks/base/core/java/com/android/internal/util/XmlUtils.java
```
   public static final HashMap<String, ?> readMapXml(InputStream in)
    throws XmlPullParserException, java.io.IOException
    {
        XmlPullParser   parser = Xml.newPullParser();
        parser.setInput(in, StandardCharsets.UTF_8.name());
        return (HashMap<String, ?>) readValueXml(parser, new String[1]);
    }
   public static final void writeMapXml(Map val, OutputStream out)
            throws XmlPullParserException, java.io.IOException {
        XmlSerializer serializer = new FastXmlSerializer();
        serializer.setOutput(out, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeMapXml(val, null, serializer);
        serializer.endDocument();
    }
```
android 12
TypedXmlPullParser  {@link XmlPullParser}的专门化，它添加了显式方法来支持原语数据类型的一致和高效转换。
```
  public static final HashMap<String, ?> readMapXml(InputStream in)
            throws XmlPullParserException, java.io.IOException {
        TypedXmlPullParser parser = Xml.newFastPullParser();
        parser.setInput(in, StandardCharsets.UTF_8.name());
        return (HashMap<String, ?>) readValueXml(parser, new String[1]);
    }
  public static final void writeMapXml(Map val, OutputStream out)
            throws XmlPullParserException, java.io.IOException {
        TypedXmlSerializer serializer = Xml.newFastSerializer();
        serializer.setOutput(out, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeMapXml(val, null, serializer);
        serializer.endDocument();
    }

```