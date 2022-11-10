
https://developer.android.com/training/data-storage/room#kts

数据库历史,可以查看哪些变更了
```
 defaultConfig {
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
```

注意：
1 room 不允许在主线程访问



数据库
```
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    //拿到各种dao
}

数据库构建
val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()
```


实体类注解
```
@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?
)
```

定义DAO  查询，插入，删除
```
@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<User>

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
           "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): User

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: User):Long
    
    @Update()
    fun update(var user:User)

    @Delete //删除一条返回1，删除多条返回count
    fun delete(user: User): Int
    //使用部分属性删除
    @Delete(entity = XGTTPaiTaskRecord::class)
    fun delete(name: String)
    
    //使用query代替删除
    @Query("DELETE FROM users WHERE user_id = :userId")
    fun deleteByUserId( userId:Long);
}
```




数据库升级
自动升级  https://developer.android.com/training/data-storage/room/migrating-db-versions
```
@Database(
  version = 2,
  entities = [User::class],
  autoMigrations = [
    AutoMigration (from = 1, to = 2)
  ]
)
abstract class AppDatabase : RoomDatabase() {
  ...
}
```
自动升级对于下面需要手动指定
删除或重命名表
删除或重命名列
```
@DeleteTable
@RenameTable
@DeleteColumn
@RenameColumn


@Database(
  version = 2,
  entities = [User::class],
  autoMigrations = [
    AutoMigration (
      from = 1,
      to = 2,
      spec = AppDatabase.MyAutoMigration::class
    )
  ]
)
abstract class AppDatabase : RoomDatabase() {
  @RenameTable(fromTableName = "User", toTableName = "AppUser")
  class MyAutoMigration : AutoMigrationSpec
  ...
}
```

手动迁移
说明：手动迁移和自动迁移都指定了，使用手动迁移
适用于：
1 将一个表拆分为两个
```
val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("CREATE TABLE `Fruit` (`id` INTEGER, `name` TEXT, " +
      "PRIMARY KEY(`id`))")
  }
}

Room.databaseBuilder(applicationContext, MyDb::class.java, "database-name")
  .addMigrations(MIGRATION_1_2).build()
```