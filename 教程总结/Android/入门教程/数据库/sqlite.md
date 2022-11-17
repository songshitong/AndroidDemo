
数据库配置 名称/路径(name包含路径可以指定数据库位置)，版本号
```
class MySqlHelper(context: Context,name:String) : SQLiteOpenHelper(context,name,null,1) {
  override fun onCreate(db: SQLiteDatabase?) {
    val table = "create table if not exists Record( id Integer primary key AUTOINCREMENT,name text(50))"
    db?.execSQL(table)
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
  }
}
```

插入一条数据
```
     val dbPath = getMyDatabasePath().path+File.separator+"AiBadge.db"
    val db = MySqlHelper(this,dbPath).writableDatabase
    val values = ContentValues()
    values.put("name","name1")
    db.insert("Record",null,values)
```
查询数据  表名，查询的列，是否执行selection，是否执行groupBy,是否having,是否排序orderBy，是否限制条数limit
```
 val cursor = db.query("Record",null,null,null,null,null,null,null)
    while (cursor.moveToNext()){
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      println(" record is $id $name")
    }
    cursor.close()
```
