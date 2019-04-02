package sst.example.androiddemo.feature.Animation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_view_group.*
import sst.example.androiddemo.feature.R
import sst.example.androiddemo.feature.R.layout.activity_view_group
 class ViewGroupActivity : AppCompatActivity() {
    val list = ArrayList<String>()
    lateinit var mContext: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_view_group)
        mContext = this
        list.add("tom111111111")
        list.add("tom22222222")
        list.add("tom333333333")
        list.add("tom444444")
        list.add("tom5555")
        list.add("tom66666666")
        list.add("tom777777")
        list.add("tom8888")
        list.add("tom99")
        list.add("tom1010")
        list.add("tom11")
        list.add("tom122222222222")
        list.add("tom133333333")
        list.add("tom1444444444")
        list.add("tom155555")

//      xml  LayoutAnimation可以用在任何ViewGroup上
        viewGroupRecyView.layoutManager = LinearLayoutManager(mContext)
        viewGroupRecyView.adapter = Adapter()

        //代码设置
        val layoutAnimationController =  AnimationUtils.loadLayoutAnimation(mContext,R.anim.layout_animation_fall_down)
        viewGroupRecyView.layoutAnimation = layoutAnimationController

        //下次更新布局启动动画
        viewGroupRecyView.scheduleLayoutAnimation()
        //启动动画
        viewGroupRecyView.startLayoutAnimation()

        viewGroupRecyView.postDelayed({
            //使用 gridLayoutAnimation 必须自定义recyclerview 不然报错 ！！！！！
            viewGroupRecyView.layoutManager = GridLayoutManager(mContext,3)
            viewGroupRecyView.layoutAnimation = AnimationUtils.loadLayoutAnimation(mContext,R.anim.grid_layout_animation_from_bottom)
            viewGroupRecyView.startLayoutAnimation()
        },3000L)

    }

    inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(mContext).inflate(R.layout.item_string,null)
            return MyViewHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            val textView = p0.itemView.findViewById<TextView>(R.id.item_string_tv)
            textView.text = list.get(p1)
        }

        inner class MyViewHolder(view:View) : RecyclerView.ViewHolder(view) {

        }

    }
}
