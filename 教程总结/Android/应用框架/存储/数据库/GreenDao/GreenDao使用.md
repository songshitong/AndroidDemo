

https://github.com/greenrobot/greenDAO
https://www.jianshu.com/p/53083f782ea2

GreenDao的优缺点？
1 高性能
2 易于使用的强大API，涵盖关系和连接；
3 最小的内存消耗;
4 小库大小（<100KB）以保持较低的构建时间并避免65k方法限制;
5 数据库加密：greenDAO支持SQLCipher，以确保用户的数据安全;

版本
implementation 'org.greenrobot:greendao:3.2.2'
数据库配置
```
greendao {
    schemaVersion 1 //数据库版本号
    daoPackage 'com.aserbao.aserbaosandroid.functions.database.greenDao.db'
// 设置DaoMaster、DaoSession、Dao 包名
    targetGenDir 'src/main/java'//设置DaoMaster、DaoSession、Dao目录,请注意，这里路径用/不要用.
    generateTests false //设置为true以自动生成单元测试。
    targetGenDirTests 'src/main/java' //应存储生成的单元测试的基本目录。默认为 src / androidTest / java。
}
```


核心类
DaoMaster:：DaoMaster保存数据库对象（SQLiteDatabase）并管理特定模式的DAO类（而不是对象）。它有静态方法来创建表或删除它们。它的内部类OpenHelper和DevOpenHelper是SQLiteOpenHelper实现，它们在SQLite数据库中创建模式。
DaoSession：管理特定模式的所有可用DAO对象，您可以使用其中一个getter方法获取该对象。DaoSession还提供了一些通用的持久性方法，如实体的插入，加载，更新，刷新和删除。
XXXDao：数据访问对象（DAO）持久存在并查询实体。对于每个实体，greenDAO生成DAO。它具有比DaoSession更多的持久性方法，例如：count，loadAll和insertInTx。
Entities ：可持久化对象。通常, 实体对象代表一个数据库行使用标准 Java 属性(如一个POJO 或 JavaBean )



创建存储对象实体类
```
@Entity
public class Student {
    @Id(autoincrement = true)
    Long id;
    @Unique
    int studentNo;//学号
    String name;//姓名
    ....
    }
```
注解相关
@Entity注解
schema：如果你有多个架构，你可以告诉GreenDao当前属于哪个架构。
active：标记一个实体处于活跃状态，活动实体有更新、删除和刷新方法。
nameInDb：在数据中使用的别名，默认使用的是实体的类名。
indexes：标记如果DAO应该创建数据库表(默认为true)，如果您有多个实体映射到一个表，或者表的创建是在greenDAO之外进行的，那么将其设置为false。
createInDb：标记创建数据库表。
generateGettersSetters：如果缺少，是否应生成属性的getter和setter方法


@Id
@Id注解选择 long / Long属性作为实体ID。在数据库方面，它是主键。参数autoincrement = true 表示自增，id不给赋值或者为赋值为null即可
（这里需要注意，如果要实现自增，id必须是Long)。

@Property
允许您定义属性映射到的非默认列名。如果不存在，GreenDAO将以SQL-ish方式使用字段名称（大写，下划线而不是camel情况，例如 name将成为 NAME）。
注意：您当前只能使用内联常量来指定列名。
```
 @Property (nameInDb="name") //设置了，数据库中的表格属性名为"name",如果不设置，数据库中表格属性名为"NAME"
 String name;
```
@NotNull ：设置数据库表当前列不能为空 。

@Transient ：添加次标记之后不会生成数据库表的列。标记要从持久性中排除的属性。将它们用于临时状态等。或者，您也可以使用Java中的transient关键字

索引注解
@Index：使用@Index作为一个属性来创建一个索引，通过name设置索引别名，也可以通过unique给索引添加约束。
@Unique：向索引添加UNIQUE约束，强制所有值都是唯一的
```
@Index(unique = true)
String name;
```
约定name为唯一值，向数据库中通过insert方法继续添加已存在的name数据，会抛异常：
android.database.sqlite.SQLiteConstraintException: UNIQUE constraint failed: STUDENT.name (Sqlite code 2067), (OS error - 2:No such file or directory)
若使用insertOrReplace()方法添加数据，当前数据库中不会有重复的数据，但是重复的这条数据的id会被修改！若项目中有用到id字段进行排序的话，这一点需要特别注意


多表关联
关系注解
@ToOne：定义与另一个实体（一个实体对象）的关系
@ToMany：定义与多个实体对象的关系


GreenDao初始化  创建DaoSession
```
 /**
     * 初始化GreenDao,直接在Application中进行初始化操作
     */
    private void initGreenDao() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "aserbao.db");
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }
    
    private DaoSession daoSession;
    public DaoSession getDaoSession() {
        return daoSession;
    }
```


新增数据
```
 @Override
    public void insertData(Thing s) {
     DaoSession daoSession = ((AserbaoApplication) getApplication()).getDaoSession();
     Student student = new Student();
     ....
     daoSession.insert(student);
    }
```
insertOrReplace()数据存在则替换，数据不存在则插入
```
 daoSession.insertOrReplace(student);//插入或替换
```


删
删除有两种方式：delete()和deleteAll()；分别表示删除单个和删除所有。
```
 daoSession.delete(s);
 daoSession.deleteAll(Student.class);
```


改
通过update来进行修改：
```
daoSession.update(s);
```



查
查询的方法有：
loadAll()：查询所有数据。
queryRaw()：根据条件查询。
queryBuilder() : 方便查询的创建
```
List<Student> students = daoSession.loadAll(Student.class);
daoSession.queryRaw(Student.class, " where id = ?", s);
```
QueryBuilder的使用
编写SQL可能很困难并且容易出现错误，这些错误仅在运行时才会被注意到。该QueryBuilder的类可以让你建立你的实体，而不SQL自定义查询，并有助于在编译时已检测错误。

QueryBuilder的常见方法：
where(WhereCondition cond, WhereCondition... condMore): 查询条件，参数为查询的条件！
or(WhereCondition cond1, WhereCondition cond2, WhereCondition... condMore): 嵌套条件或者，用法同or。
and(WhereCondition cond1, WhereCondition cond2, WhereCondition... condMore): 嵌套条件且，用法同and。
join(Property sourceProperty, Class<J> destinationEntityClass):多表查询
输出结果有四种方式，选择其中一种最适合的即可，list()返回值是List,而其他三种返回值均实现Closeable,需要注意的不使用数据时游标的关闭操作：
list （）所有实体都加载到内存中。结果通常是一个没有魔法的 ArrayList。最容易使用。
listLazy （）实体按需加载到内存中。首次访问列表中的元素后，将加载并缓存该元素以供将来使用。必须关闭。
listLazyUncached （）实体的“虚拟”列表：对列表元素的任何访问都会导致从数据库加载其数据。必须关闭。
listIterator （）让我们通过按需加载数据（懒惰）来迭代结果。数据未缓存。必须关闭。
orderAsc() 按某个属性升序排；
orderDesc() 按某个属性降序排；

```
daoSession.queryBuilder(Student.class).list()
//查询所有名为"一"，然后按名字排升序
daoSession.queryBuilder(Student.class).qb.where(StudentDao.Properties.Name.eq("一")).orderAsc(StudentDao.Properties.Name).list()
//删除所有id大于5
daoSession.queryBuilder(Student.class).where(StudentDao.Properties.Id.gt(5)).where.buildDelete().executeDeleteWithoutDetachingEntities();
```


多次执行查找
使用QueryBuilder构建查询后，可以重用 Query对象以便稍后执行查询。这比始终创建新的Query对象更有效。如果查询参数没有更改，
您可以再次调用list / unique方法。可以通过setParameter方法来修改条件参数值：
```
 public List queryListByMoreTime(){
        DaoSession daoSession = ((AserbaoApplication) getApplication()).getDaoSession();
        QueryBuilder<Student> qb = daoSession.queryBuilder(Student.class);

        //搜索条件为Id值大于1，即结果为[2,3,4,5,6,7,8,9,10,11];
        // offset(2)表示往后偏移2个，结果为[4,5,6,7,8,9,10,11,12,13];
        Query<Student> query = qb.where(StudentDao.Properties.Id.gt(1)).limit(10).offset(2).build();
        List<Student> list = query.list();
        
        //通过SetParameter来修改上面的查询条件，比如我们将上面条件修改取10条Id值大于5，往后偏移两位的数据，方法如下！
        query.setParameter(0,5);
        List<Student> list1 = query.list();
        return list1;
    }
```



在多个线程中使用QueryBuilder
如果在多个线程中使用查询，则必须调用 forCurrentThread （）以获取当前线程的Query实例。Query的对象实例绑定到构建查询的拥有线程。

这使您可以安全地在Query对象上设置参数，而其他线程不会干扰。如果其他线程尝试在查询上设置参数或执行绑定到另一个线程的查询，则会抛出异常。像这样，您不需要同步语句。实际上，您应该避免锁定，因为如果并发事务使用相同的Query对象，这可能会导致死锁。

每次调用forCurrentThread （）时， 参数都会在使用其构建器构建查询时设置为初始参数。




数据库的升级
1 创建临时表TMP_,复制原来的数据库到临时表中；
2 删除之前的原表；
3 创建新表；
4 将临时表中的数据复制到新表中，最后将TMP_表删除掉；
修改Application中的DaoMaster的创建
```
        MyDaoMaster helper = new MyDaoMaster(this, "aserbaos.db");
//      DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "aserbao.db");
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
```
MyDaoMaster代码:
```
public class MyDaoMaster extends OpenHelper {
    private static final String TAG = "MyDaoMaster";
    public MyDaoMaster(Context context, String name) {
        super(context, name);
    }

    public MyDaoMaster(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
        MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener() {
            @Override
            public void onCreateAllTables(Database db, boolean ifNotExists) {
                DaoMaster.createAllTables(db, ifNotExists);
            }
            @Override
            public void onDropAllTables(Database db, boolean ifExists) {
                DaoMaster.dropAllTables(db, ifExists);
            }
        },ThingDao.class);
        Log.e(TAG, "onUpgrade: " + oldVersion + " newVersion = " + newVersion);
    }
}
```
MigrationHelper 代码：
```
public final class MigrationHelper {

    public static boolean DEBUG = false;
    private static String TAG = "MigrationHelper";
    private static final String SQLITE_MASTER = "sqlite_master";
    private static final String SQLITE_TEMP_MASTER = "sqlite_temp_master";

    private static WeakReference<ReCreateAllTableListener> weakListener;

    public interface ReCreateAllTableListener{
        void onCreateAllTables(Database db, boolean ifNotExists);
        void onDropAllTables(Database db, boolean ifExists);
    }

    public static void migrate(SQLiteDatabase db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        printLog("【The Old Database Version】" + db.getVersion());
        Database database = new StandardDatabase(db);
        migrate(database, daoClasses);
    }

    public static void migrate(SQLiteDatabase db, ReCreateAllTableListener listener, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        weakListener = new WeakReference<>(listener);
        migrate(db, daoClasses);
    }

    public static void migrate(Database database, ReCreateAllTableListener listener, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        weakListener = new WeakReference<>(listener);
        migrate(database, daoClasses);
    }

    public static void migrate(Database database, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        printLog("【Generate temp table】start");
        generateTempTables(database, daoClasses);
        printLog("【Generate temp table】complete");

        ReCreateAllTableListener listener = null;
        if (weakListener != null) {
            listener = weakListener.get();
        }

        if (listener != null) {
            listener.onDropAllTables(database, true);
            printLog("【Drop all table by listener】");
            listener.onCreateAllTables(database, false);
            printLog("【Create all table by listener】");
        } else {
            dropAllTables(database, true, daoClasses);
            createAllTables(database, false, daoClasses);
        }
        printLog("【Restore data】start");
        restoreData(database, daoClasses);
        printLog("【Restore data】complete");
    }

    private static void generateTempTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (int i = 0; i < daoClasses.length; i++) {
            String tempTableName = null;

            DaoConfig daoConfig = new DaoConfig(db, daoClasses[i]);
            String tableName = daoConfig.tablename;
            if (!isTableExists(db, false, tableName)) {
                printLog("【New Table】" + tableName);
                continue;
            }
            try {
                tempTableName = daoConfig.tablename.concat("_TEMP");
                StringBuilder dropTableStringBuilder = new StringBuilder();
                dropTableStringBuilder.append("DROP TABLE IF EXISTS ").append(tempTableName).append(";");
                db.execSQL(dropTableStringBuilder.toString());

                StringBuilder insertTableStringBuilder = new StringBuilder();
                insertTableStringBuilder.append("CREATE TEMPORARY TABLE ").append(tempTableName);
                insertTableStringBuilder.append(" AS SELECT * FROM ").append(tableName).append(";");
                db.execSQL(insertTableStringBuilder.toString());
                printLog("【Table】" + tableName +"\n ---Columns-->"+getColumnsStr(daoConfig));
                printLog("【Generate temp table】" + tempTableName);
            } catch (SQLException e) {
                Log.e(TAG, "【Failed to generate temp table】" + tempTableName, e);
            }
        }
    }

    private static boolean isTableExists(Database db, boolean isTemp, String tableName) {
        if (db == null || TextUtils.isEmpty(tableName)) {
            return false;
        }
        String dbName = isTemp ? SQLITE_TEMP_MASTER : SQLITE_MASTER;
        String sql = "SELECT COUNT(*) FROM " + dbName + " WHERE type = ? AND name = ?";
        Cursor cursor=null;
        int count = 0;
        try {
            cursor = db.rawQuery(sql, new String[]{"table", tableName});
            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }
            count = cursor.getInt(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return count > 0;
    }


    private static String getColumnsStr(DaoConfig daoConfig) {
        if (daoConfig == null) {
            return "no columns";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < daoConfig.allColumns.length; i++) {
            builder.append(daoConfig.allColumns[i]);
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }


    private static void dropAllTables(Database db, boolean ifExists, @NonNull Class<? extends AbstractDao<?, ?>>... daoClasses) {
        reflectMethod(db, "dropTable", ifExists, daoClasses);
        printLog("【Drop all table by reflect】");
    }

    private static void createAllTables(Database db, boolean ifNotExists, @NonNull Class<? extends AbstractDao<?, ?>>... daoClasses) {
        reflectMethod(db, "createTable", ifNotExists, daoClasses);
        printLog("【Create all table by reflect】");
    }

    /**
     * dao class already define the sql exec method, so just invoke it
     */
    private static void reflectMethod(Database db, String methodName, boolean isExists, @NonNull Class<? extends AbstractDao<?, ?>>... daoClasses) {
        if (daoClasses.length < 1) {
            return;
        }
        try {
            for (Class cls : daoClasses) {
                Method method = cls.getDeclaredMethod(methodName, Database.class, boolean.class);
                method.invoke(null, db, isExists);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void restoreData(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (int i = 0; i < daoClasses.length; i++) {
            DaoConfig daoConfig = new DaoConfig(db, daoClasses[i]);
            String tableName = daoConfig.tablename;
            String tempTableName = daoConfig.tablename.concat("_TEMP");

            if (!isTableExists(db, true, tempTableName)) {
                continue;
            }

            try {
                // get all columns from tempTable, take careful to use the columns list
                List<TableInfo> newTableInfos = TableInfo.getTableInfo(db, tableName);
                List<TableInfo> tempTableInfos = TableInfo.getTableInfo(db, tempTableName);
                ArrayList<String> selectColumns = new ArrayList<>(newTableInfos.size());
                ArrayList<String> intoColumns = new ArrayList<>(newTableInfos.size());
                for (TableInfo tableInfo : tempTableInfos) {
                    if (newTableInfos.contains(tableInfo)) {
                        String column = '`' + tableInfo.name + '`';
                        intoColumns.add(column);
                        selectColumns.add(column);
                    }
                }
                // NOT NULL columns list
                for (TableInfo tableInfo : newTableInfos) {
                    if (tableInfo.notnull && !tempTableInfos.contains(tableInfo)) {
                        String column = '`' + tableInfo.name + '`';
                        intoColumns.add(column);

                        String value;
                        if (tableInfo.dfltValue != null) {
                            value = "'" + tableInfo.dfltValue + "' AS ";
                        } else {
                            value = "'' AS ";
                        }
                        selectColumns.add(value + column);
                    }
                }

                if (intoColumns.size() != 0) {
                    StringBuilder insertTableStringBuilder = new StringBuilder();
                    insertTableStringBuilder.append("REPLACE INTO ").append(tableName).append(" (");
                    insertTableStringBuilder.append(TextUtils.join(",", intoColumns));
                    insertTableStringBuilder.append(") SELECT ");
                    insertTableStringBuilder.append(TextUtils.join(",", selectColumns));
                    insertTableStringBuilder.append(" FROM ").append(tempTableName).append(";");
                    db.execSQL(insertTableStringBuilder.toString());
                    printLog("【Restore data】 to " + tableName);
                }
                StringBuilder dropTableStringBuilder = new StringBuilder();
                dropTableStringBuilder.append("DROP TABLE ").append(tempTableName);
                db.execSQL(dropTableStringBuilder.toString());
                printLog("【Drop temp table】" + tempTableName);
            } catch (SQLException e) {
                Log.e(TAG, "【Failed to restore data from temp table 】" + tempTableName, e);
            }
        }
    }

    private static List<String> getColumns(Database db, String tableName) {
        List<String> columns = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 0", null);
            if (null != cursor && cursor.getColumnCount() > 0) {
                columns = Arrays.asList(cursor.getColumnNames());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            if (null == columns)
                columns = new ArrayList<>();
        }
        return columns;
    }

    private static void printLog(String info){
        if(DEBUG){
            Log.d(TAG, info);
        }
    }

    private static class TableInfo {
        int cid;
        String name;
        String type;
        boolean notnull;
        String dfltValue;
        boolean pk;

        @Override
        public boolean equals(Object o) {
            return this == o
                    || o != null
                    && getClass() == o.getClass()
                    && name.equals(((TableInfo) o).name);
        }

        @Override
        public String toString() {
            return "TableInfo{" +
                    "cid=" + cid +
                    ", name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", notnull=" + notnull +
                    ", dfltValue='" + dfltValue + '\'' +
                    ", pk=" + pk +
                    '}';
        }

        private static List<TableInfo> getTableInfo(Database db, String tableName) {
            String sql = "PRAGMA table_info(" + tableName + ")";
            printLog(sql);
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor == null)
                return new ArrayList<>();
            TableInfo tableInfo;
            List<TableInfo> tableInfos = new ArrayList<>();
            while (cursor.moveToNext()) {
                tableInfo = new TableInfo();
                tableInfo.cid = cursor.getInt(0);
                tableInfo.name = cursor.getString(1);
                tableInfo.type = cursor.getString(2);
                tableInfo.notnull = cursor.getInt(3) == 1;
                tableInfo.dfltValue = cursor.getString(4);
                tableInfo.pk = cursor.getInt(5) == 1;
                tableInfos.add(tableInfo);
                // printLog(tableName + "：" + tableInfo);
            }
            cursor.close();
            return tableInfos;
        }
    }
}
```

