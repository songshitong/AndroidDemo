
https://zhuanlan.zhihu.com/p/46664887
DiskLruCache 遵从 LRU 算法，当缓存数据达到设定的极限值时将会后台自动按照 LRU 算法移除缓存直到满足存下新的缓存不超过极限值
    一条缓存记录一次只能有一个 editor ，如果值不可编辑将会返回一个空值
    使用LinkedHashMap<String, Entry> lruEntries实现LRU
DiskLruCache 的数据是缓存在文件系统的某一目录中的，这个目录必须是唯一对应某一条缓存的，缓存可能会重写和删除目录中的文件。
  多个进程同一时间使用同一个缓存目录会出错。
存储文件目录名image_manager_disk_cache，默认大小250M
后台线程glide-disk-lru-cache-thread，超过250M后按LRU淘汰最少使用的，journal文件超过2000条也会重建该文件
  触发时机：
remove(String key)  写入一条REMOVE，删除LinkedHashMap中的entry，开始触发后台清理
setMaxSize(long maxSize)   
get(String key)   新增一条read记录，判断是否journal超出，开始触发后台清理
提交修改commit()   如果成功，写入一条CLEAN记录,否则，写入一条REMOVE记录，判断是否journal超出，开始触发后台清理
```
 public synchronized void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
    executorService.submit(cleanupCallable);
  }
```


DiskLruCache的使用
打开缓存
```
/**
* @param directory    缓存目录
* @param appVersion   当前应用程序的版本号
* @param valueCount   同一个key可以对应多少个缓存文件，基本都是1
* @param maxSize      最多可以缓存多少字节的数据
* @throws IOException 如果读写缓存失败会抛出IO异常
  */
  public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize) throws IOException
```
写入缓存
```
// key会成为缓存文件的文件名，并且必须要和URL是一一对应的，而URL可能包含特殊字符，不能用作文件名，
// 所以对URL进行MD5编码，编码后的字符串是唯一的，并且只会包含0-F字符，符合文件命名规则
String key = generateKey(url);
DiskLruCache.Editor editor = mDiskLruCache.edit(key);
// 通过Editor获取到os是指向缓存文件的输出流，然后把想存的东西写入
File file = editor.getFile(0);
// ...流操作  writer会自动创建文件，这个版本的DiskLruCache没有创建缓存文件，依靠外部的writer实现
writer.write(file)
// 写完缓存后，调用commit()，来提交缓存；调用abort()，放弃写入的缓存
editor.commit();
// editor.abort();
```
读取缓存
```
DiskLruCache.Value snapshot = mDiskLruCache.get(key);
// 通过snapshot获取到输入流，然后对流进行操作
File is = snapshot.getFile(0);
// ...文件操作
```


journal文件解读
journal文件是DiskLruCache的一个日志文件，程序对每个文件的操作记录都存放在这个文件中
```
libcore.io.DiskLruCache
1
100
1

DIRTY 335c4c6028171cfddfbaae1a9c313c52
CLEAN 335c4c6028171cfddfbaae1a9c313c52 2342
REMOVE 335c4c6028171cfddfbaae1a9c313c52
DIRTY 1ab96a171faeeee38496d8b330771a7a
CLEAN 1ab96a171faeeee38496d8b330771a7a 1600
READ 335c4c6028171cfddfbaae1a9c313c52
READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
```
journal文件头
第一行：固定字符串libcore.io.DiskLruCache
第二行：DiskLruCache的版本号，这个值恒为1。
第三行：应用程序的版本号。每当版本号改变，缓存路径下存储的所有数据都会被清空，因为DiskLruCache认为应用更新，所有的数据都应重新获取。
第四行：指每个key对应几个文件，一般为1个
第五行：空行

journal文件内容     key一般是外部传入的，可以是url的md5，缓存文件的名字
DIRTY：第六行以DIRTY前缀开始，后面跟着缓存文件的key，表示一个entry正在被写入。   edit()方法
CLEAN：当写入成功，就会写入一条CLEAN记录，后面的数字记录文件的长度，如果一个key可以对应多个文件，那么就会有多个数字   
   调用edit()之后进行commit()
REMOVE：表示写入失败，或者调用remove(key)方法的时候都会写入一条REMOVE记录
READ：表示一次读取记录

NOTE：当journal文件记录的操作次数达到2000时，就会触发重构journal的事件，来保证journal文件的大小始终在一个合理的范围内。

journal文件与lruEntries的关系
//lruEntries中存储是非REMOVE(在readJournal)或者是编辑中的(edit)的  这样在大小达到max时，根据journal生成lruEntries可以进行LRU淘汰了
  所以淘汰的是很久没有进行READ读或DIRTY，CLEAN编辑操作的文件


先看一个DiskLruCache的结构
```
public final class DiskLruCache implements Closeable {
  //journal文件相关
  static final String JOURNAL_FILE = "journal";
  static final String JOURNAL_FILE_TEMP = "journal.tmp";
  static final String JOURNAL_FILE_BACKUP = "journal.bkp";
  static final String MAGIC = "libcore.io.DiskLruCache";
  static final String VERSION_1 = "1";
  
  private static final String CLEAN = "CLEAN";
  private static final String DIRTY = "DIRTY";
  private static final String REMOVE = "REMOVE";
  private static final String READ = "READ";
  //利用LinkedHashMap实现LRU  key为string，value为entry  true代表按访问顺序排序，每次访问元素后该元素将移至链表的尾部
  private final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap<String, Entry>(0, 0.75f, true);
  
  //建立一个线程池后台线程  名字为glide-disk-lru-cache-thread 空闲线程最多存活60秒
  final ThreadPoolExecutor executorService =
      new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
          new DiskLruCacheThreadFactory());
  //executorService执行的清理工作        
  private final Callable<Void> cleanupCallable = new Callable<Void>() {
    public Void call() throws Exception {
      synchronized (DiskLruCache.this) {
        if (journalWriter == null) {
          return null; // Closed.
        }
        //校验大小，超出后淘汰最老的
        trimToSize();
        if (journalRebuildRequired()) {
          //重建journal
          rebuildJournal();
          //多余的行
          redundantOpCount = 0;
        }
      }
      return null;
    }
  };
}
```
redundant  [rɪˈdʌndənt]  冗余的;多余的;被裁减的;不需要的
文件操作使用Editor，LRU更新基于entry
```
 //编辑器
 public final class Editor {
    private final Entry entry;
    private final boolean[] written;
    private boolean committed;

    private Editor(Entry entry) {
      this.entry = entry;
      this.written = (entry.readable) ? null : new boolean[valueCount];
    }

    //获取一个读取流
   // index缓存文件索引，一个key可能对应多个文件，当对应一个文件时，只要传0
    private InputStream newInputStream(int index) throws IOException {
      synchronized (DiskLruCache.this) {
        if (entry.currentEditor != this) {
          throw new IllegalStateException();
        }
        if (!entry.readable) {
          return null;
        }
        try {
          //文件名为文件为[key].[index].tmp
          return new FileInputStream(entry.getCleanFile(index));
        } catch (FileNotFoundException e) {
          return null;
        }
      }
    }


    public String getString(int index) throws IOException {
      InputStream in = newInputStream(index);
      return in != null ? inputStreamToString(in) : null;
    }

    public File getFile(int index) throws IOException {
      synchronized (DiskLruCache.this) {
        。。。
        if (!entry.readable) {
            written[index] = true;
        }
        File dirtyFile = entry.getDirtyFile(index);
        directory.mkdirs();
        return dirtyFile;
      }
    }

    /** Sets the value at {@code index} to {@code value}. */
    public void set(int index, String value) throws IOException {
      Writer writer = null;
      try {
        OutputStream os = new FileOutputStream(getFile(index));
        writer = new OutputStreamWriter(os, Util.UTF_8);
        writer.write(value);
      } finally {
        Util.closeQuietly(writer);
      }
    }

    /**
     * Commits this edit so it is visible to readers.  This releases the
     * edit lock so another edit may be started on the same key.
     */
    public void commit() throws IOException {
      // The object using this Editor must catch and handle any errors
      // during the write. If there is an error and they call commit
      // anyway, we will assume whatever they managed to write was valid.
      // Normally they should call abort.
      completeEdit(this, true);
      committed = true;
    }

    /**
     * Aborts this edit. This releases the edit lock so another edit may be
     * started on the same key.
     */
    public void abort() throws IOException {
      completeEdit(this, false);
    }

    public void abortUnlessCommitted() {
      if (!committed) {
        try {
          abort();
        } catch (IOException ignored) {
        }
      }
    }
  }

  //实现LRU的条目
  private final class Entry {
    private final String key;

    /** Lengths of this entry's files. */
    private final long[] lengths;

    /** Memoized File objects for this entry to avoid char[] allocations. */
    File[] cleanFiles;
    File[] dirtyFiles;

    /** True if this entry has ever been published. */
    private boolean readable;

    /** The ongoing edit or null if this entry is not being edited. */
    private Editor currentEditor;

    /** The sequence number of the most recently committed edit to this entry. */
    private long sequenceNumber;

    private Entry(String key) {
      this.key = key;
      this.lengths = new long[valueCount];
      cleanFiles = new File[valueCount];
      dirtyFiles = new File[valueCount];
      //entry初始化时，会同步初始化cleanFiles，dirtyFiles
      //文件为[key].[index].tmp
      StringBuilder fileBuilder = new StringBuilder(key).append('.');
      
      int truncateTo = fileBuilder.length();
      for (int i = 0; i < valueCount; i++) {
          fileBuilder.append(i);
          cleanFiles[i] = new File(directory, fileBuilder.toString());
          fileBuilder.append(".tmp");
          dirtyFiles[i] = new File(directory, fileBuilder.toString());
          fileBuilder.setLength(truncateTo);
      }
    }

    public String getLengths() throws IOException {
      StringBuilder result = new StringBuilder();
      for (long size : lengths) {
        result.append(' ').append(size);
      }
      return result.toString();
    }

    /** Set lengths using decimal numbers like "10123". */
    private void setLengths(String[] strings) throws IOException {
      if (strings.length != valueCount) {
        throw invalidLengths(strings);
      }

      try {
        for (int i = 0; i < strings.length; i++) {
          lengths[i] = Long.parseLong(strings[i]);
        }
      } catch (NumberFormatException e) {
        throw invalidLengths(strings);
      }
    }

    private IOException invalidLengths(String[] strings) throws IOException {
      throw new IOException("unexpected journal line: " + java.util.Arrays.toString(strings));
    }

    public File getCleanFile(int i) {
      return cleanFiles[i];
    }

    public File getDirtyFile(int i) {
      return dirtyFiles[i];
    }
  }
```

获取DiskLruCache
DiskLruCache#open
```
 public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
      throws IOException {
    ...

    // 检查journal.bkp文件是否存在(journal的备份文件)
    File backupFile = new File(directory, JOURNAL_FILE_BACKUP);
    if (backupFile.exists()) {
      File journalFile = new File(directory, JOURNAL_FILE);
       // 如果journal文件存在，则删除journal.bkp备份文件
      if (journalFile.exists()) {
        backupFile.delete();
      } else {
        // journal文件不存在，将bkp文件重命名为journal文件
        renameTo(backupFile, journalFile, false);
      }
    }

    // Prefer to pick up where we left off.
    DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
    
    // 判断journal文件是否存在
    if (cache.journalFile.exists()) {
      try {
        // 读取journal文件
        cache.readJournal();
        cache.processJournal();
        return cache;
      } catch (IOException journalIsCorrupt) {
        ....
        cache.delete();
      }
    }

    // journal文件不存在，则创建缓存目录，重新构造DiskLruCache
    directory.mkdirs();
    cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
    // 重新创建journal文件
    cache.rebuildJournal();
    return cache;
  }
```

看一下rebuildJournal
```
private synchronized void rebuildJournal() throws IOException {
 if (journalWriter != null) {
      closeWriter(journalWriter);
    }
    // 创建journal.tmp文件
    Writer writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(journalFileTmp), Util.US_ASCII));
    try {
       // 写入文件头(5行)
      writer.write(MAGIC);
      writer.write("\n");
      writer.write(VERSION_1);
      writer.write("\n");
      writer.write(Integer.toString(appVersion));
      writer.write("\n");
      writer.write(Integer.toString(valueCount));
      writer.write("\n");
      writer.write("\n");
      // 遍历lruEntries   重建后只有dirty和clean记录
      for (Entry entry : lruEntries.values()) {
        if (entry.currentEditor != null) {
          writer.write(DIRTY + ' ' + entry.key + '\n');
        } else {
          writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
        }
      }
    } finally {
      closeWriter(writer);
    }

    if (journalFile.exists()) {
      //将journal重命名为jorunal.bkp
      renameTo(journalFile, journalFileBackup, true);
    }
    // 将journal.tmp文件重命名为journal文件
    renameTo(journalFileTmp, journalFile, false);
    //删除jorunal.bkp也就是老的jornal文件
    journalFileBackup.delete();

    journalWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(journalFile, true), Util.US_ASCII));
}
```

看一下readJournal
```
private void readJournal() throws IOException {
    StrictLineReader reader = new StrictLineReader(new FileInputStream(journalFile), Util.US_ASCII);
    try {
      //读取文件头
      String magic = reader.readLine();
      String version = reader.readLine();
      String appVersionString = reader.readLine();
      String valueCountString = reader.readLine();
      String blank = reader.readLine();
      // 校验journal文件头
      if (!MAGIC.equals(magic)
          || !VERSION_1.equals(version)
          || !Integer.toString(appVersion).equals(appVersionString)
          || !Integer.toString(valueCount).equals(valueCountString)
          || !"".equals(blank)) {
        throw new IOException("unexpected journal header: [" + magic + ", " + version + ", "
            + valueCountString + ", " + blank + "]");
      }

      int lineCount = 0;
      while (true) {
        try {
          //逐行读取
          readJournalLine(reader.readLine());
          lineCount++;
        } catch (EOFException endOfJournal) {
          break;
        }
      }
      //多余的行  在readJournalLine中REMOVE操作会将entry移除，其他的都会生成entry,此时的数量是需要移除的
      redundantOpCount = lineCount - lruEntries.size();

      // If we ended on a truncated line, rebuild the journal before appending to it.
      if (reader.hasUnterminatedLine()) {
        //如果文件被截断了，重建
        rebuildJournal();
      } else {
        journalWriter = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(journalFile, true), Util.US_ASCII));
      }
    } finally {
      Util.closeQuietly(reader);
    }
  }

  //读取jornal内容行
  private void readJournalLine(String line) throws IOException {
    int firstSpace = line.indexOf(' ');
    if (firstSpace == -1) {
      throw new IOException("unexpected journal line: " + line);
    }

    int keyBegin = firstSpace + 1;
    int secondSpace = line.indexOf(' ', keyBegin);
    final String key;
    if (secondSpace == -1) {
      // 获取缓存key
      key = line.substring(keyBegin);
      // 如果是REMOVE记录，则调用lruEntries.remove(key)
      if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
        lruEntries.remove(key);
        return;
      }
    } else {
      key = line.substring(keyBegin, secondSpace);
    }
    // 如果该key没有加入到lruEntries，则创建并加入  加入到map
    Entry entry = lruEntries.get(key);
    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    }
    
    if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
      // 处理CLEAN记录
      String[] parts = line.substring(secondSpace + 1).split(" ");
      entry.readable = true;
      entry.currentEditor = null;
      entry.setLengths(parts);
    } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
      // 处理DIRTY记录
      entry.currentEditor = new Editor(entry);
    } else if (secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ)) {
      //READ记录被lruEntries.get()处理完了
    } else {
      throw new IOException("unexpected journal line: " + line);
    }
  }
```
redundant  [rɪˈdʌndənt] 冗余的;多余的;被裁减的;不需要的
truncated  [trʌŋˈkeɪtɪd] 截短，缩短，删节(尤指掐头或去尾)


processJournal()
```
 private void processJournal() throws IOException {
    deleteIfExists(journalFileTmp);
    for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
      Entry entry = i.next();
      if (entry.currentEditor == null) {
        for (int t = 0; t < valueCount; t++) {
          // 统计所有可用cache占据的容量
          size += entry.lengths[t];
        }
      } else {
        // 删除非法DIRTY状态的entry,并删除对应的文件
        entry.currentEditor = null;
        for (int t = 0; t < valueCount; t++) {
          deleteIfExists(entry.getCleanFile(t));
          deleteIfExists(entry.getDirtyFile(t));
        }
        i.remove();
      }
    }
  }
```

写入缓存
DiskLruCache#edit
```
public Editor edit(String key) throws IOException {
    //ANY_SEQUENCE_NUMBER -1
    return edit(key, ANY_SEQUENCE_NUMBER);
  }

  private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
    checkNotClosed();
    // 获取entry
    Entry entry = lruEntries.get(key);
    if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
        || entry.sequenceNumber != expectedSequenceNumber)) {
      return null; // Value is stale.
    }
    if (entry == null) {
      //创建新的entry然后加入lruEntries
      entry = new Entry(key);
      lruEntries.put(key, entry);
    } else if (entry.currentEditor != null) {
      // 该entry正在被编辑
      return null; // Another edit is in progress.
    }
    // 创建Editor并赋值给entry.currentEditor
    Editor editor = new Editor(entry);
    entry.currentEditor = editor;
     
    //写入一条DIRTY 
    // Flush the journal before creating files to prevent file leaks.
    journalWriter.append(DIRTY);
    journalWriter.append(' ');
    journalWriter.append(key);
    journalWriter.append('\n');
    flushWriter(journalWriter);
    return editor;
  }
```
Editor#newInputStream  返回文件为[key].[index].tmp的流

Editor#commit
```
   public void commit() throws IOException {
      completeEdit(this, true);
      committed = true;
    }
```
DiskLruCache
```
 private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
    Entry entry = editor.entry;
    if (entry.currentEditor != editor) {
      throw new IllegalStateException();
    }

    // If this edit is creating the entry for the first time, every index must have a value.
    // 判断是否需要写入成功，且是第一次写入
    if (success && !entry.readable) {
      for (int i = 0; i < valueCount; i++) {
        if (!editor.written[i]) {
          editor.abort();
          throw new IllegalStateException("Newly created entry didn't create value for index " + i);
        }
        if (!entry.getDirtyFile(i).exists()) {
          editor.abort();
          return;
        }
      }
    }

    for (int i = 0; i < valueCount; i++) {
      File dirty = entry.getDirtyFile(i);
      if (success) {
        if (dirty.exists()) {
          File clean = entry.getCleanFile(i);
          // 将dirtyFile重命名为cleanFile，更新size的大小
          dirty.renameTo(clean);
          long oldLength = entry.lengths[i];
          long newLength = clean.length();
          entry.lengths[i] = newLength;
          size = size - oldLength + newLength;
        }
      } else {
        deleteIfExists(dirty);
      }
    }

    redundantOpCount++;
    entry.currentEditor = null;
    if (entry.readable | success) {
      // 如果成功，写入一条CLEAN记录
      entry.readable = true;
      journalWriter.append(CLEAN);
      journalWriter.append(' ');
      journalWriter.append(entry.key);
      journalWriter.append(entry.getLengths());
      journalWriter.append('\n');

      if (success) {
        entry.sequenceNumber = nextSequenceNumber++;
      }
    } else {
      // 否则，写入一条REMOVE记录
      lruEntries.remove(entry.key);
      journalWriter.append(REMOVE);
      journalWriter.append(' ');
      journalWriter.append(entry.key);
      journalWriter.append('\n');
    }
    //刷新缓冲，写入文件
    flushWriter(journalWriter);

    if (size > maxSize || journalRebuildRequired()) {
      executorService.submit(cleanupCallable);
    }
  }
  
  private boolean journalRebuildRequired() {
    //多余的操作大于2000
    final int redundantOpCompactThreshold = 2000;
    return redundantOpCount >= redundantOpCompactThreshold //
        && redundantOpCount >= lruEntries.size();
  }
```

读取缓存
DiskLruCache#get
```
public synchronized Value get(String key) throws IOException {
    checkNotClosed();
    Entry entry = lruEntries.get(key);
    if (entry == null) {
      return null;
    }

    if (!entry.readable) {
      return null;
    }

    for (File file : entry.cleanFiles) {
        // A file must have been deleted manually!
        if (!file.exists()) {
            return null;
        }
    }

    redundantOpCount++;
    // 往journal文件写入一条READ记录
    journalWriter.append(READ);
    journalWriter.append(' ');
    journalWriter.append(key);
    journalWriter.append('\n');
    if (journalRebuildRequired()) {
      executorService.submit(cleanupCallable);
    }
    // entry封装成value并返回
    return new Value(key, entry.sequenceNumber, entry.cleanFiles, entry.lengths);
  }
```
value可以看作为entry的快照
```
  public final class Value {
    private final String key;
    private final long sequenceNumber;
    private final long[] lengths;
    private final File[] files;

      private Value(String key, long sequenceNumber, File[] files, long[] lengths) {
      this.key = key;
      this.sequenceNumber = sequenceNumber;
      this.files = files;
      this.lengths = lengths;
    }

    public Editor edit() throws IOException {
      return DiskLruCache.this.edit(key, sequenceNumber);
    }

    public File getFile(int index) {
        return files[index];
    }

    /** Returns the string value for {@code index}. */
    public String getString(int index) throws IOException {
      InputStream is = new FileInputStream(files[index]);
      return inputStreamToString(is);
    }

    /** Returns the byte length of the value for {@code index}. */
    public long getLength(int index) {
      return lengths[index];
    }
  }
```


删除操作
```
public synchronized boolean remove(String key) throws IOException {
    checkNotClosed();
    Entry entry = lruEntries.get(key);
    //entry不存在或正在编辑
    if (entry == null || entry.currentEditor != null) {
      return false;
    }
    for (int i = 0; i < valueCount; i++) {
      File file = entry.getCleanFile(i);
      //删除文件
      if (file.exists() && !file.delete()) {
        throw new IOException("failed to delete " + file);
      }
      //记录当前大小
      size -= entry.lengths[i];
      entry.lengths[i] = 0;
    }
    //写入一条REMOVE
    redundantOpCount++;
    journalWriter.append(REMOVE);
    journalWriter.append(' ');
    journalWriter.append(key);
    journalWriter.append('\n');
    //从 lruEntries中移除
    lruEntries.remove(key);

    if (journalRebuildRequired()) {
      executorService.submit(cleanupCallable);
    }

    return true;
  }
```

清理工作中的trimToSize
```
  private void trimToSize() throws IOException {
    //超出大小
    while (size > maxSize) {
      //lruEntries按访问顺序排序，每次访问元素后该元素将移至链表的尾部  从头部开始淘汰，则是淘汰最老的
      Map.Entry<String, Entry> toEvict = lruEntries.entrySet().iterator().next();
      //删除文件与对应的记录   文件名即md5的key
      remove(toEvict.getKey());
    }
  }
```


Glide中对DiskLruCache的使用
DiskCache 磁盘缓存的接口
```
public interface DiskCache {

  interface Factory {
    //默认250M
    int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;
    
    DiskCache build();
  }

  interface Writer {
    //把data写到file
    boolean write(@NonNull File file);
  }

  File get(Key key);

  void put(Key key, Writer writer);

  void delete(Key key);

  void clear();
}
```
DiskCache
空的实现DiskCacheAdapter，真正实现LRU的DiskLruCacheWrapper
DiskLruCacheWrapper是对DiskLruCache的包装，实现文件LRU的是DiskLruCache   DiskLruCache查看DiskLruCache.md
```
public class DiskLruCacheWrapper implements DiskCache {
  private DiskLruCache diskLruCache;
  private synchronized DiskLruCache getDiskCache() throws IOException {
    if (diskLruCache == null) {
      diskLruCache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize);
    }
    return diskLruCache;
  }
  
  public File get(Key key) {
    String safeKey = safeKeyGenerator.getSafeKey(key);
    。。。。
    File result = null;
    try {
      final DiskLruCache.Value value = getDiskCache().get(safeKey);
      if (value != null) {
        result = value.getFile(0);
      }
    } catch (IOException e) {
      。。。
    }
    return result;
  }

  @Override
  public void put(Key key, Writer writer) {
    String safeKey = safeKeyGenerator.getSafeKey(key);
    writeLocker.acquire(safeKey);
    try {
     。。。。
      try {
        
        DiskLruCache diskCache = getDiskCache();
        Value current = diskCache.get(safeKey);
        if (current != null) {
          return;
        }

        DiskLruCache.Editor editor = diskCache.edit(safeKey);
        。。。
        try {
          File file = editor.getFile(0);
          if (writer.write(file)) {
            editor.commit();
          }
        } finally {
          editor.abortUnlessCommitted();
        }
      } catch (IOException e) {
        。。。
      }
    } finally {
      writeLocker.release(safeKey);
    }
  }
}
```
DiskCache的默认工厂为InternalCacheDiskCacheFactory.java
默认的目录名为image_manager_disk_cache
默认大小为250M