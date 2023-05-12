package com.example.libopengl

import android.R.attr
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TextureSurfaceView(context: Context,attributeSet: AttributeSet?): GLSurfaceView(context,attributeSet){
  init {
    // 设置 GL 版本号
    setEGLContextClientVersion(2)
    setRenderer(CenterCropRender(BitmapFactory.decodeResource(resources,R.drawable.dog)))
    // 只有当数据改变时才渲染
    renderMode = RENDERMODE_WHEN_DIRTY
  }

}

class CenterCropRender(bitmap: Bitmap):FitXYRender(bitmap){
  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    // super.onSurfaceChanged(gl, width, height)
    val aspectBitmap =  bitmap.width/ bitmap.height.toFloat()
    // // 计算当前画布 Surface 的宽高比
    // val aspectSurface = attr.width / attr.height.toFloat()
    val scale = bitmap.width/width.toFloat() //缩放
    Matrix.orthoM(
      mProjectionMatrix, 0,
      -1f, 1f, -aspectBitmap/scale, aspectBitmap/scale,
      1f, -1f
    )
  }

}

class FitCenterRender(bitmap: Bitmap):FitXYRender(bitmap){
  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    // super.onSurfaceChanged(gl, width, height)
    // 2. 实现 fitCenter
    // 计算 Bitmap 的宽高比
    val aspectBitmap =  bitmap.width/ bitmap.height.toFloat()
    // 计算当前画布 Surface 的宽高比
    val aspectSurface = attr.width / attr.height.toFloat()
    // fitCenter
    fitCenter(aspectSurface, aspectBitmap)
  }

  private fun fitCenter(aspectPlane: Float, aspectTexture: Float) {
    val left: Float
    val top: Float
    val right: Float
    val bottom: Float
    // 1. 纹理比例 > 投影平面比例
    if (aspectTexture > aspectPlane) {
      left = -1f
      right = 1f
      top =  aspectTexture / aspectPlane
      bottom = -top
    } else {
      left = -aspectPlane / aspectTexture
      right = -left
      top = 1f
      bottom = -1f
    }
    Matrix.orthoM(
      mProjectionMatrix, 0,
      left, right, bottom, top,
      1f, -1f
    )
  }
}

open class FitXYRender( val bitmap: Bitmap): GLSurfaceView.Renderer{
  private val vertex_shader = """
    // 定义一个属性，图形顶点坐标
    attribute vec4 aShapeCoords;
    // 定义一个属性，纹理顶点坐标
    attribute vec2 aTextureCoords;
    // 顶点裁剪矩阵
    uniform mat4 uMatrix;
    // varying 可用于相互传值
    varying vec2 vTextureCoords;
    void main() {
        // 暂存纹理的坐标, 到片元着色器中使用
        vTextureCoords = aTextureCoords;
        // gl_Position 为内置变量, 根据 u_Matrix 计算出裁剪坐标系的位置
        gl_Position = aShapeCoords * uMatrix;
    }
  """

  private val fragment_shader = """
    // 着色器纹理扩展类型
    #extension GL_OES_EGL_image_external : require
    // 设置精度，中等精度
    precision mediump float;
    // varying 可用于相互传值
    varying vec2 vTextureCoords;
    // 2D 纹理 ，uniform 用于 application 向 gl 传值
    uniform sampler2D uTexture;
    void main() {
      gl_FragColor = texture2D(uTexture, vTextureCoords);//进行纹理采样,拿到当前颜色
    }
  """

  /**
   * 矩形顶点坐标(定义在 GL 世界坐标系中)
   */
  private val mRectCoords = floatArrayOf( // 矩形顶点坐标
    -1f, 1f,  // 左上
    -1f, -1f,  // 左下
    1f, 1f,  // 右上
    1f, -1f  //右下
  )
  private lateinit var mRectCoordsBuffer: FloatBuffer

  /**
   * 取纹理区域的坐标(原始纹理的范围为 [0, 1], 我们可以指定取纹理的哪些部分)
   * 因为要贴到上面定义的矩形上, 因此其位置要与矩形的顶点一一对应
   * 图片的 Y 轴通常是向下的, 我在取我们所需部分时, 需要上下颠倒一下映射, 这样可以保证纹理映射到矩形上时正常的
   */
  private val mTextureCoords = floatArrayOf(
    0f, 0f,  // 纹理左下
    0f, 1f,  // 纹理左上
    1f, 0f,  // 纹理右下
    1f, 1f // 纹理右上
  )
  private lateinit var mTextureCoordsBuffer: FloatBuffer
  private var mProgram = 0
  private var mTextureId =0
  val mProjectionMatrix = FloatArray(16) // 投影矩阵

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    // 初始化顶点坐标
    mRectCoordsBuffer = ByteBuffer.allocateDirect(mRectCoords.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .put(mRectCoords);
    mRectCoordsBuffer.position(0);
    // 初始化纹理顶点坐标
    mTextureCoordsBuffer = ByteBuffer.allocateDirect(mTextureCoords.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .put(mTextureCoords);
    mTextureCoordsBuffer.position(0);
    mProgram = createProgram(vertex_shader,fragment_shader)

    mTextureId = createTextureFromRes(bitmap)
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    //视口的大小
    GLES20.glViewport(0, 0, width, height)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glClearColor(1f, 1f, 1f, 1.0f)
    //这里省略了视图矩阵, 我们初始坐标系即观察者坐标系
    Matrix.orthoM(
      mProjectionMatrix, 0,
      -1f, 1f, -1f, 1f, //默认观察范围
      1f, -1f
    )
  }

  override fun onDrawFrame(gl: GL10?) {
    // 清屏并绘制白色
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    GLES20.glClearColor(1f, 1f, 1f, 1f);
    // 绘制纹理
    // 激活 Program
    GLES20.glUseProgram(mProgram);
    // 写入顶点坐标数据
    val aShapeCoords = GLES20.glGetAttribLocation(mProgram, "aShapeCoords")
    GLES20.glEnableVertexAttribArray(aShapeCoords)
    GLES20.glVertexAttribPointer(aShapeCoords, 2, GLES20.GL_FLOAT, false, 8, mRectCoordsBuffer);
    // 写入纹理坐标数据
    val aTextureCoords = GLES20.glGetAttribLocation(mProgram, "aTextureCoords")
    GLES20.glEnableVertexAttribArray(aTextureCoords);
    GLES20.glVertexAttribPointer(aTextureCoords, 2, GLES20.GL_FLOAT, false, 8,
      mTextureCoordsBuffer);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    // 写入裁剪矩阵数据
    val matrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix")
    GLES20.glUniformMatrix4fv(matrixHandler, 1, false, mProjectionMatrix, 0);

    // 激活纹理
    // GLES20.glUniform1i(uTexture, 0); //纹理单元的位置 uTexture=glGetUniformLocation(ourShader.Program, "ourTexture1")
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0) //opengl2提供了32个单元，使用第一个
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

    // 执行绘制
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4) //三角形条带

    // 解绑纹理
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
  }

  /**
   * 创建纹理
   */
  private fun createTextureFromRes(bitmap: Bitmap): Int {
    // 生成绑定纹理
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    val textureId = textures[0]
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    // 设置环绕方向
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
    // 设置纹理过滤方式
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    // 将 Bitmap 生成 2D 纹理
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    // 解绑
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    return textureId
  }

  //创建一个 OpenGL 程序  vertexSource   顶点着色器源码  fragmentSource 片元着色器源码
  fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
    // 分别加载创建着色器
    val vertexShaderId: Int = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    val fragmentShaderId: Int = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    if (vertexShaderId != 0 && fragmentShaderId != 0) {
      // 创建 OpenGL 程序 ID
      val programId: Int = GLES20.glCreateProgram()
      if (programId == 0) {
        return 0
      }
      // 链接上 顶点着色器
      GLES20.glAttachShader(programId, vertexShaderId)
      // 链接上 片段着色器
      GLES20.glAttachShader(programId, fragmentShaderId)
      // 链接 OpenGL 程序
      GLES20.glLinkProgram(programId)
      // 验证链接结果是否失败
      val status = IntArray(1)
      GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, status, 0)
      if (status[0] != GLES20.GL_TRUE) {
        // 失败后删除这个 OpenGL 程序
        GLES20.glDeleteProgram(programId)
        return 0
      }
      return programId
    }
    return 0
  }

  //编译着色器  shaderType 着色器的类型  source     资源源代码
   fun compileShader(shaderType: Int, source: String?): Int {
    // 创建着色器 ID
    val shaderId = GLES20.glCreateShader(shaderType)
    if (shaderId != 0) {
      // 1. 将着色器 ID 和着色器程序内容关联
      GLES20.glShaderSource(shaderId, source)
      // 2. 编译着色器
      GLES20.glCompileShader(shaderId)
      // 3. 验证编译结果
      val status = IntArray(1)
      GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, status, 0)
      if (status[0] != GLES20.GL_TRUE) {
        // 编译失败删除这个着色器 id
        GLES20.glDeleteShader(shaderId)
        return 0
      }
    }
    return shaderId
  }



}