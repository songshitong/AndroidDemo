package com.example.opencv

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.ObjectId
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
  companion object{
    const val TAG = "MainActivity"
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    if(OpenCVLoader.initDebug()){
      Log.d(TAG,"open cv init")
    }
    findViewById<View>(R.id.colorConvertBtn).setOnClickListener {
      startActivity(Intent(this,ColorConvertActivity::class.java))
    }

    findViewById<View>(R.id.textRecognitionBtn).setOnClickListener {
      startActivity(Intent(this,TextRecognizeActivity::class.java))
    }
    findViewById<View>(R.id.drawBtn).setOnClickListener {
      startActivity(Intent(this,DrawActivity::class.java))
    }
    findViewById<View>(R.id.imgSplitBtn).setOnClickListener {
      startActivity(Intent(this,ImgSplitActivity::class.java))
    }
    findViewById<View>(R.id.templateBtn).setOnClickListener {
      startActivity(Intent(this,TemplateMatchActivity::class.java))
    }
    findViewById<View>(R.id.ContoursBtn).setOnClickListener {
      startActivity(Intent(this,ContoursActivity::class.java))
    }
    findViewById<View>(R.id.imgFilterBtn).setOnClickListener {
      startActivity(Intent(this,PictureFilterActivity::class.java))
    }

    val config = RealmConfiguration.create(schema = setOf(Item::class))
    val realm: Realm = Realm.open(config)
    //添加
    realm.writeBlocking {
      copyToRealm(Item().apply {
        summary = "Do the laundry"
        isComplete = false
      })
    }
    val items: RealmResults<Item> = realm.query<Item>().find()
    Log.d("realm add","items $items")
    //变更
    realm.writeBlocking {
      findLatest(items.first())?.isComplete = true
    }
    Log.d("realm update ","items ${realm.query<Item>().find()}")
    //删除
    realm.writeBlocking {
      //Frozen objects cannot be deleted. They must be converted to live objects first by using `MutableRealm/DynamicMutableRealm.findLatest(frozenObject)`
      //找到数据的最新版本，然后删除
      findLatest(items.last())?.let { delete(it) }
    }
    Log.d("realm delete","items ${realm.query<Item>().find()}")

    // frozen object
    //frozen objects that can be passed between threads safely
    //frozen objects don't automatically update when data changes in your realm
  }
}

//不支持data class
class Item() : RealmObject {
  @PrimaryKey
  var _id: ObjectId = ObjectId.create()
  var isComplete: Boolean = false
  var summary: String = ""
  var owner_id: String = ""
  constructor(ownerId: String = "") : this() {
    owner_id = ownerId
  }

  override fun toString(): String {
    return "Item(_id=$_id, isComplete=$isComplete, summary='$summary', owner_id='$owner_id')"
  }
}