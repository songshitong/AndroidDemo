package sst.example.androiddemo.feature.video

import VideoHandle.EpEditor
import VideoHandle.EpVideo
import VideoHandle.OnEditorListener
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.UriUtils
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXGameVideoFileObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_video_parser.*
import sst.example.androiddemo.feature.R
import sst.example.androiddemo.feature.common.BaseHandler
import sst.example.androiddemo.feature.util.BitmapUtils
import sst.example.androiddemo.feature.util.MyUtils
import sst.example.androiddemo.feature.util.TimeUtil
import java.io.File


class VideoParserActivity : AppCompatActivity() {
    val TAG = "VideoParserActivity"
    val appId = "wx57dae74a35c31b6a"
    var inDirectory :String? = ""
    var inPath = ""
    var outDirectory = ""
    var outPath = ""
    var clipStart: Float = 0f
    var clipEnd: Float = 0f
    //videoview 的preparlistener 在拉起系统播放，返回后会出发
    var isVideoViewInit: Boolean = false

    //微信参数
    private var api: IWXAPI? = null
    private var mTargetScene = SendMessageToWX.Req.WXSceneSession


    inner class MyHandler : BaseHandler {
        constructor(activity: Activity) : super(activity)

        override fun childHandleMessage(msg: Message) {
            if (videoView.currentPosition >= clipEnd.toInt()) {
                //手动停止
                pauseVideoView()
                return
            }
            if (showIv.visibility == View.VISIBLE && videoView.currentPosition > 0) {
                //隐藏第一帧
                showIv.visibility = View.GONE
            }
            //判断裁剪区域
            if (videoView.currentPosition >= clipEnd) {
                videoView.pause()
                videoView.seekTo(clipStart.toInt())
                return
            }
            frameRcy.post {
                clipProgressBar.setShowProgress(true)
                clipProgressBar.setProgress(videoView.currentPosition.toFloat())
            }
            //progre以接近16ms的流畅度刷新
            sendEmptyMessageDelayed(0, 17)
        }
    }

    val handler = MyHandler(this)


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_parser)
//        val videoFile: File = getFileStreamPath("samplevideo_5mb.mp4")
//                Log.d(TAG, "file path " + videoFile.absolutePath)

        startActivityForResult(MyUtils.getAlbumVideoIntent(), 1000)
        inDirectory = getExternalFilesDir("/video")!!.path

        outDirectory = getExternalFilesDir("/frame")!!.path
        outPath = outDirectory + "/image-%03d.jpg"
        Log.d(TAG, "inPath " + inPath)

        MyUtils.initDirectory(inDirectory)
        MyUtils.initDirectory(outDirectory)
        FileUtils.deleteFilesInDir(outDirectory)
        frameRcy.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false) as RecyclerView.LayoutManager?
        videoView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (stopIv.visibility == View.VISIBLE) {
                    startVideoView()
                } else if (stopIv.visibility == View.GONE) {
                    pauseVideoView()
                }
            }
            false
        }

        clipVideo.setOnClickListener {
            if (clipEnd - clipStart == 0f) {
                Toast.makeText(this, "截取视频长度不能为0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val epVideo = EpVideo(inPath)
            Log.d(TAG, "clip video start " + clipStart / 1000 + " duration " + (clipEnd - clipStart) / 1000)
            epVideo.clip(clipStart / 1000, (clipEnd - clipStart) / 1000)

            val outPath = inDirectory + "/out.mp4"
            clipVideo(epVideo, outPath)
        }

        clipProgressBar.setClipProgressBarListener(object : ClipProgressBar.ClipProgressBarListener {
            override fun clipProgressStartChange(clipStart: Float, clipEnd: Float) {
                this@VideoParserActivity.clipStart = clipStart
                this@VideoParserActivity.clipEnd = clipEnd
                pauseVideoView()
                videoView.seekTo(clipStart.toInt())
                progressCurrent.text = TimeUtil.getMsFromMilSeconds((clipEnd - clipStart).toInt())
            }

            override fun clipProgressEndChange(clipEnd: Float, clipStart: Float) {
                this@VideoParserActivity.clipStart = clipStart
                this@VideoParserActivity.clipEnd = clipEnd
                pauseVideoView()
                videoView.seekTo(clipEnd.toInt())
                progressCurrent.text = TimeUtil.getMsFromMilSeconds((clipEnd - clipStart).toInt())
            }
        })

        //微信设置
        api = WXAPIFactory.createWXAPI(this, appId, false)

        shareVideo.setOnClickListener {
            val outPath = inDirectory + "/out.mp4"
            shareLocalVideo(outPath)
        }
    }

    private fun startVideoView() {
        //判断裁剪区域
//        if(videoView.currentPosition<=clipStart){
//            videoView.seekTo(clipStart.toInt())
//        }
        //移动右侧滑块后，播放重置到clipstart
        //todo seekTo 不准确问题，会寻找最近的关键帧
        videoView.seekTo(clipStart.toInt())
        Log.d(TAG, "play start " + clipStart)
        videoView.start()
        //隐藏图标
        stopIv.visibility = View.GONE
        handler.sendEmptyMessage(0)
    }

    private fun pauseVideoView() {
        stopIv.visibility = View.VISIBLE
        videoView.pause()
        handler.removeCallbacksAndMessages(null)
    }

    //视频裁剪 todo 加入FFmpeg后 裁剪失败
    private fun clipVideo(epVideo: EpVideo, outPath: String) {
        EpEditor.exec(epVideo, EpEditor.OutputOption(outPath), object : OnEditorListener {
            override fun onSuccess() {
                videoView.post {
                    Toast.makeText(this@VideoParserActivity, "编辑完成:$outPath", Toast.LENGTH_SHORT).show()
                }
                startActivity(MyUtils.getSysVideoPlayer(UriUtils.file2Uri(File(outPath))))
            }

            override fun onFailure() {
                Toast.makeText(this@VideoParserActivity, "编辑失败", Toast.LENGTH_SHORT).show()
            }

            override fun onProgress(v: Float) {
                Log.d(TAG, "clip progress " + v)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == 1000) {//图片选择完成
                val uri = data!!.data
                Log.d(TAG, data.toString())
                if (uri == null) {
                    Log.w(TAG, "图片选择完成" + "uri is null ")
                }
                videoView.setVideoURI(uri)
                videoView.setOnPreparedListener {
                    if (!isVideoViewInit) {
                        isVideoViewInit = true
                        stopIv.visibility = View.VISIBLE
                        clipEnd = videoView.duration.toFloat()
                        clipProgressBar.setMax(videoView.duration.toFloat())
                        val length = TimeUtil.getMsFromMilSeconds(videoView.duration)
                        progressCurrent.text = length
//                        //读取相册图片到/sdcard/android/data
//                        BitmapUtils.readImgUri2File(uri, inPath)
                        inPath = uri?.let { it1 -> UriUtils.uri2File(it1).path }.toString()
                        //提取第一帧加载
                        showIv.setImageBitmap(getVideoFrame(inPath, 0))

                        //根据长度算出生成4张图片
                        val bitmaps = ArrayList<Bitmap>()
                        for (i in 0..3) {
                            val time = (i + 1) / 4 * videoView.duration
                            val bitmap = getVideoFrame(inPath, time.toLong())
                            if (bitmap != null) {
                                bitmaps.add(bitmap)
                            }
                            Log.d(TAG, "$bitmap")
                            videoView.post {
                                frameRcy.adapter = FrameRcyAdapter(this@VideoParserActivity, bitmaps)
                            }
                        }
                        frameRcy.post {
                            clipProgressBar.visibility = View.VISIBLE
                            progressCurrent.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    //使用Android原生提取为bitmap
    fun getVideoFrame(path: String, time: Long): Bitmap? {
        val mmr = MediaMetadataRetriever()
//        mmr.setDataSource(path)
//        return mmr.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST)
        return null
    }

    //视频转图片
    private fun video2pic(inPath: String, outPath: String, outDirectory: String, rate: Float) {
        Log.d(TAG, "rate $rate")
        EpEditor.video2pic(inPath, outPath, 720, 1080, rate, object : OnEditorListener {
            override fun onSuccess() {
                Log.d(TAG, "success")
                frameRcy.post {
                    clipProgressBar.visibility = View.VISIBLE
                    progressCurrent.visibility = View.VISIBLE
                }
            }

            override fun onFailure() {
                Log.d(TAG, "failure")
            }

            override fun onProgress(progress: Float) {
                Log.d(TAG, "progress " + progress)
                //有时候progress会一开始大于1
                val directory = File(outDirectory)
                val files = directory.list()
                val bitmaps = ArrayList<Bitmap>()
                files.forEach {
                    Log.d(TAG, "all frames " + it.toString())
                    bitmaps.add(BitmapUtils.getBitmapFromUri(UriUtils.file2Uri(File(outDirectory + "/" + it.toString()))))
                }
                videoView.post {
                    frameRcy.adapter = FrameRcyAdapter(this@VideoParserActivity, bitmaps)
                }
            }
        })
    }

    private fun shareVideo(path: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "这是标题")//添加分享内容标题
        shareIntent.putExtra(Intent.EXTRA_STREAM, UriUtils.file2Uri(File(path)))//添加分享内容
        this.startActivity(Intent.createChooser(shareIntent, "分享title"))

        //只分享到微信
        var shareWechat = false
        if (shareWechat) {
//            "com.tencent.mm.ui.tools.ShareImgUI"    微信分享
//            "com.tencent.mm.ui.tools.AddFavoriteUI" 微信收藏
            val name = "com.tencent.mm.ui.tools.ShareImgUI"
            val resInfo = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resInfo != null && resInfo.size > 0) {
                for (info in resInfo) {
                    //判断匹配 包名和类名
                    if (info.activityInfo.name.toLowerCase().contains(name) || info.activityInfo.packageName.toLowerCase().contains(
                            name
                        )
                    ) {
                        shareIntent.setPackage(info.activityInfo.packageName)
                        // 微信和微信收藏是 packageName一样的,指定打开的class，就不会出现让用户选择的界面了
                        shareIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name)

//                        启动
//                        startActivity
                        break
                    }
                }
            }
        }
    }

    private fun shareLocalVideo(path: String) {
        shareVideo(path)
        return
        val gameVideoFileObject = WXGameVideoFileObject()
        gameVideoFileObject.filePath = path
//        val video = WXVideoObject()
//        video.videoUrl = path

        val msg = WXMediaMessage()
//        msg.setThumbImage(Util.extractThumbNail(path, 150, 150, true))
        msg.title = "this is title"
        msg.description = "this is description"
        msg.mediaObject = gameVideoFileObject

        val req = SendMessageToWX.Req()
        req.transaction = buildTransaction("appdata")
        req.message = msg
        req.scene = mTargetScene
        api!!.sendReq(req)
    }

    private fun buildTransaction(type: String?): String {
        return if (type == null) System.currentTimeMillis().toString() else type + System.currentTimeMillis()
    }

    class FrameRcyAdapter(val context: Context, val datas: List<Bitmap>) :
        RecyclerView.Adapter<FrameRcyAdapter.FrameRcyAdapterVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameRcyAdapterVH {
            return FrameRcyAdapterVH(
                LayoutInflater.from(context).inflate(
                    R.layout.video_parser_frame_item_layout,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int = datas.size

        override fun onBindViewHolder(holder: FrameRcyAdapterVH, position: Int) {
            holder.iv.setImageBitmap(datas[position])
        }

        inner class FrameRcyAdapterVH(view: View) : RecyclerView.ViewHolder(view) {
            var iv: ImageView = view.findViewById(R.id.frame_item_iv)
        }

    }

}
