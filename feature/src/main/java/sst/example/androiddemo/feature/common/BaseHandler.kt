package sst.example.androiddemo.feature.common


import android.app.Activity
import android.os.Handler
import android.os.Message
import androidx.fragment.app.Fragment

import java.lang.ref.WeakReference


/**
 * handler包装类
 * 外部使用
 *  1 私有静态类继承本类
 *  private static class MyHandler extends BaseHandler
 *  2 实例化子类
 *  MyHandler handler = new MyHandler(this);
 */

//todo 优化成lifeCycle的  destory时将消息移除mHandler.removeCallbacksAndMessages(null)
abstract class BaseHandler : Handler {

    protected var activityWeakReference: WeakReference<Activity>? = null
    protected var fragmentWeakReference: WeakReference<Fragment>? = null

    private constructor() {}//构造私有化,让调用者必须传递一个Activity 或者 Fragment的实例

    constructor(activity: Activity) {
        this.activityWeakReference = WeakReference(activity)
    }

    constructor(fragment: Fragment) {
        this.fragmentWeakReference = WeakReference<Fragment>(fragment)
    }

    override fun handleMessage(msg: Message) {
        if(!checkActivity()){
            childHandleMessage(msg)
        }else if(!checkFragment()){
            childHandleMessage(msg)
        }
    }

    private fun checkFragment() =
        fragmentWeakReference == null || fragmentWeakReference!!.get() == null || fragmentWeakReference!!.get()!!.isRemoving()

    //activity是否调用finish()
    private fun checkActivity() =
        activityWeakReference == null || activityWeakReference!!.get() == null || activityWeakReference!!.get()!!.isFinishing

    /**
     * 抽象方法用户实现,用来处理具体的业务逻辑
     *
     * @param msg
     * @param what [What]
     */
    abstract fun childHandleMessage(msg: Message)


    fun getActivity():Activity?{
        return activityWeakReference!!.get()
    }

    fun getFragment():Fragment?{
        return fragmentWeakReference!!.get()
    }
}