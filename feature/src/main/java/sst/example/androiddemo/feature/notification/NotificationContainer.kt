package sst.example.androiddemo.feature.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import sst.example.androiddemo.feature.R


/**
 * The type Notification container.
 */
class NotificationContainer private constructor() {

    /**
     * The Builder.
     */
    internal var builder: NotificationCompat.Builder

    private val context: Context

    /**
     * The Notification id.
     */
    /**
     * Get pre notification id int. 获取上一个ID，新提示的回自动 ++1；
     *
     * @return the int
     */
    var preNotificationId = 1000
        internal set

    /**
     * The Nm.
     */
    internal var nm: NotificationManager

    init {
        context = Utils.getApp()
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNormalNotificationChannel(context,R.string.app_name)
        builder = NotificationCompat.Builder(context, CHANNEL_ID)
    }

    /**
     * 通知栏提示
     *
     * @param content the content
     * @return int int
     */
    fun tipNotification(content: String): Int {
        preNotificationId++
        buildNormal(content)
        val notification = builder.build()
        nm.notify(preNotificationId, notification)
        return preNotificationId
    }

    /**
     * 通知栏提示,时间
     *
     * @param content the content
     * @param time    the time
     * @return int int
     */
    fun tipNotification(content: String, time: Long): Int {
        preNotificationId++
        buildNormal(content)
        builder.setWhen(time)
        val notification = builder.build()
        nm.notify(preNotificationId, notification)
        return preNotificationId
    }

    /**
     * 通知栏提示,带有点击事件,自定义时间，悬挂式效果
     *
     * @param content the content
     * @param time    the time
     * @param intent  the intent
     * @return int int
     */
    fun tipNotification(content: String, time: Long, intent: PendingIntent): Int {
        preNotificationId++
        buildNormal(content)
        builder.setWhen(time)
        builder.setContentIntent(intent)
        if (!AppUtils.isAppForeground()) {
            //悬挂式通知,app在后台时弹出
            builder.setFullScreenIntent(intent, false)
        }
        val notification = builder.build()
        nm.notify(preNotificationId, notification)
        return preNotificationId
    }

    /**
     * 通知栏提示,带有点击事件
     *
     * @param content the content
     * @param intent  the intent
     * @return int int
     */
    fun tipNotification(content: String, intent: PendingIntent): Int {
        preNotificationId++
        buildNormal(content)
        builder.setContentIntent(intent)
        val notification = builder.build()
        nm.notify(preNotificationId, notification)
        return preNotificationId
    }

    private fun buildNormal(content: String) {
        builder.setPriority(NotificationCompat.PRIORITY_MAX)
        builder.setAutoCancel(true)
        builder.setContentTitle(context.getResources().getString(R.string.app_name))
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setContentText(content)
        builder.setTicker(content)
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
        builder.setGroup(GROUP)
        builder.setStyle(
            NotificationCompat.BigTextStyle().bigText(content).setBigContentTitle(
                context.getResources().getString(R.string.app_name)
            )
        )


    }

    /**
     * Cancle notification.
     *
     * @param notificationId the notification id
     */
    fun cancleNotification(notificationId: Int) {
        nm.cancel(notificationId)
    }

    /**
     * Cancle all.
     */
    fun cancleAll() {
        nm.cancelAll()
    }


    /**
     * @param description 描述ID
     */
    private fun createNormalNotificationChannel(context: Context,description:Int) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_NAME
            val description = context.getString(description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            nm.createNotificationChannel(channel)
        }
    }

    companion object {

        /**
         * The constant CHANNELName.
         */
        val CHANNEL_NAME = "notification_channel"

        /**
         * The constant CHANNEL_ID.
         */
        val CHANNEL_ID = "100"

        /**
         * group
         */
        val GROUP = "oldfriendspub_notification_group"

        /**
         * Gets instance.
         *
         * @return the instance
         */
        val instance = NotificationContainer()
    }
}