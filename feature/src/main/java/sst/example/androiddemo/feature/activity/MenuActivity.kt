package sst.example.androiddemo.feature.activity

import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_menu.*








//ContextMenu与OptionMenu的区别：
//1、OptionMenu对应的是activity，一个activity只能拥有一个选项菜单；
//2、ContextMenu对应的是view，每个view都可以设置上下文菜单；
//3、一般情况下ContextMenu常用语ListView或者GridView

//Sub Menu
//将功能相同的操作分组显示，他作用在OptionsMenu上，是OptionsMenu的二级菜单
class MenuActivity : AppCompatActivity() {
    private val TAG ="MenuActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(sst.example.androiddemo.feature.R.layout.activity_menu)
        Log.d(TAG,"onCreate ====")
        registerForContextMenu(showContextMenu)
        showContextMenu.setOnClickListener {
            openContextMenu(it)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        Log.d(TAG,"onCreateContextMenu ====")

        menu?.setHeaderTitle("上下文菜单")
        menu?.setHeaderIcon(sst.example.androiddemo.feature.R.drawable.ic_launcher)
        //加载上下文菜单内容
        menu?.add(1, 1, 1, "保存")
        menu?.add(1, 2, 1, "更改")
        menu?.add(1, 3, 1, "删除")
        super.onCreateContextMenu(menu, v, menuInfo)
    }
    /**
     * 创建单击事件
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            1 -> Toast.makeText(this, "点击了保存", Toast.LENGTH_SHORT).show()
            2 -> Toast.makeText(this, "点击了更改", Toast.LENGTH_SHORT).show()
            3 -> Toast.makeText(this, "点击了删除", Toast.LENGTH_SHORT).show()

            else -> {
            }
        }
        return super.onContextItemSelected(item)
    }

    //每次展示menu会调用
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG,"onPrepareOptionsMenu ====")
        return super.onPrepareOptionsMenu(menu)
    }

    //创建option菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG,"onCreateOptionsMenu ====")
        // groupId--1:分组的id;itemId--100:菜单项的id;order--1:菜单项排序用的;title--"菜单1":菜单名称;
        val item = menu?.add(1, 100, 1, "菜单项")
        item?.title = "我是一个菜单"
        // 在API>=11时，是不显示图标的
        item?.setIcon(sst.example.androiddemo.feature.R.drawable.ic_launcher)
        menu?.add(1, 101, 1, "登录")
        menu?.add(1, 102, 1, "设置")
        menu?.add(1, 103, 1, "退出")

        val fileMenu = menu?.addSubMenu("查看文件")
        val editMenu = menu?.addSubMenu("输入文件")
        //添加菜单项
        fileMenu?.add(1, 1, 1, "文件1")
        fileMenu?.add(1, 2, 1, "文件2")
        fileMenu?.add(1, 3, 1, "文件3")
        editMenu?.add(2, 1, 1, "输入1")
        editMenu?.add(2, 2, 1, "输入2")
        editMenu?.add(2, 3, 1, "输入3")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.groupId === 1) {
            when (item.itemId) {
                1 -> Toast.makeText(this, "点击了文件1", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(this, "点击了文件2", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "点击了文件3", Toast.LENGTH_SHORT).show()
                101 -> Toast.makeText(this, "你点击了登录", Toast.LENGTH_SHORT).show()
                102 -> Toast.makeText(this, "你点击了设置", Toast.LENGTH_SHORT).show()
                103 -> Toast.makeText(this, "你点击了退出", Toast.LENGTH_SHORT).show()
                else -> {
                }
            }
        } else if (item.groupId === 2) {
            when (item.itemId) {
                1 -> Toast.makeText(this, "点击了输入1", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(this, "点击了输入2", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "点击了输入3", Toast.LENGTH_SHORT).show()

                else -> {
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onStart() {
        super.onStart()
        Log.d(TAG," onStart ==== ")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG," onResume ==== ")

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG," onPause ==== ")

    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG," onStop ==== ")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG," onDestroy ==== ")

    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG," onRestart ==== ")

    }
}
