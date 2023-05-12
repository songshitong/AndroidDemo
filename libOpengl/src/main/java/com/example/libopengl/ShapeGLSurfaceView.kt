package com.example.libopengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class ShapeGLSurfaceView(context: Context,attributeSet: AttributeSet?): GLSurfaceView(context,attributeSet) {

  init {
    // 设置 GL 版本号
    setEGLContextClientVersion(2)
    // setRenderer(Line())
    // setRenderer(Triangle())
    setRenderer(Rect())
    // setRenderer(Circle())
    // 只有当数据改变时才渲染
    renderMode = RENDERMODE_WHEN_DIRTY
  }
}

abstract class BaseDrawRender : GLSurfaceView.Renderer{
  //Vertex Shader - 支持矩阵变换的顶点着色器
  // uMatrix，aPosition从java传入，gl_Position为输出
  private val VERTEX_SHADER_CODE = """attribute vec4 aPosition;
    uniform mat4 uMatrix;
    void main() {
        gl_Position = uMatrix * aPosition;
    }"""

  //Fragment Shader - 简单的片元着色器
  //uColor从java输入 gl_FragColor输出
  private val FRAGMENT_SHADER_CODE = "precision mediump float;" +
    "uniform vec4 uColor;" +
    "void main() {" +
    "  gl_FragColor = uColor;" +
    "}"

  private val LINE_COLOR = floatArrayOf(
    0.63671875f,  // red
    0.76953125f,  // green
    0.22265625f,  // blue
    1.0f // alpha
  )

  private val mViewMatrix = FloatArray(16) // 视图矩阵
  private val mProjectionMatrix = FloatArray(16) // 投影矩阵
  private val mClipMatrix = FloatArray(16) // 裁剪矩阵

  private var mProgram = 0

  // 存储顶点数据的缓冲区域
  private lateinit var mVertexBuffer: FloatBuffer


  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    mVertexBuffer = ByteBuffer.allocateDirect(getCoors().size * 4)
      .order(ByteOrder.nativeOrder())    // use the device hardware's native byte order
      .asFloatBuffer()

    // 在 Native 开辟一块空间, 用于存储顶点坐标,java数组native无法读取
    mVertexBuffer.put(getCoors());            // add the coordinates to the FloatBuffer
    mVertexBuffer.position(0);                 // set the buffer to read the first coordinate

    //使用shader创建
    mProgram = createProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)
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
  open fun compileShader(shaderType: Int, source: String?): Int {
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

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    //视口的大小
    GLES20.glViewport(0, 0, width, height)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glClearColor(1f, 1f, 1f, 1.0f)
    // 1. 通过模型矩阵, 将坐标系从 局部空间 -> 世界空间
    // ...... 我们定义的初始坐标系便是世界坐标系
    // 2. 通过视图矩阵, 将坐标系从 世界空间 -> 观察空间
    Matrix.setLookAtM(
      mViewMatrix, 0,
      0F, 0F, 7.0f,  // 描述眼睛位置  z轴上方
      0f, 0f, 0f,  // 描述眼睛看向的位置 看向原点
      0f, 1.0f, 0.0f // 描述视线的垂线
    )
    // 3. 通过透视投影矩阵, 将坐标系从 观察空间 -> 裁剪空间
    val aspect: Float = width.toFloat() / height
    Matrix.perspectiveM(
      mProjectionMatrix, 0,
      30F,  // 视角度数
      aspect,  // 平面的宽高比
      3F,  // 近平面距离
      7F // 远平面距离
    )
    // 4. 裁剪矩阵 = 投影矩阵 * 视图矩阵 * 模型矩阵
    Matrix.multiplyMM(
      mClipMatrix, 0,
      mProjectionMatrix, 0,
      mViewMatrix, 0
    )
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glClearColor(1f, 1f, 1f, 1.0f)

    // 1. 在 OpenGL ES 的环境中添加可执行程序
    GLES20.glUseProgram(mProgram)

    // 2. 设置顶点数据
    val attribHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
    GLES20.glEnableVertexAttribArray(attribHandle) // 启动顶点属性数组

    GLES20.glVertexAttribPointer(
      attribHandle,
      getVertexUnit(),  // 每个顶点的坐标个数
      GLES20.GL_FLOAT,  // 顶点描述单位
      false,  // 是否标准化
      getVertexUnit() * 4,  // 顶点的跨幅, 即一个顶点占用的字节数
      mVertexBuffer // 顶点数据
    )
    // 3. 设置矩阵变化
    val matrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix")
    GLES20.glUniformMatrix4fv(
      matrixHandler,
      1,
      false,
      mClipMatrix,
      0
    )
    // 4. 设置绘制时的颜色值
    val colorHandle = GLES20.glGetUniformLocation(mProgram, "uColor")
    GLES20.glUniform4fv(
      colorHandle,
      1,  // 颜色的数量
      LINE_COLOR,  // 具体的颜色
      0 // 数据读取位置
    )
    // 5. 执行绘制
    onChildDraw()
    // 6.关闭可执行程序
    GLES20.glDisableVertexAttribArray(attribHandle)
  }

  //每个顶点 由几个坐标构成
  abstract fun getVertexUnit(): Int

  //图形顶点的坐标
  abstract fun getCoors(): FloatArray

  open fun onChildDraw() {

  }
}

class Line : BaseDrawRender() {
  private val LINE_COORS = floatArrayOf(
    0.0f, 2f, 0.0f,  // start  x,y,z
    0.0f, -2f, 0.0f
  )

  override fun getVertexUnit(): Int{
    return 3
  }

  override fun getCoors(): FloatArray {
    return LINE_COORS
  }

  override fun onChildDraw() {
    GLES20.glDrawArrays(
      GLES20.GL_LINES,  // 绘制类型
      0,  // 数据读取位置
      2 // 顶点数量
    )
  }
}

class Triangle : BaseDrawRender(){
  /**
   * 顶点坐标(世界坐标系, 近似正三角形)
   */
  private val TRIANGLE_COORS = floatArrayOf(
    0.0f, 0.6f, 0.0f,  // top
    -0.5f, -0.3f, 0.0f,  // bottom left
    0.5f, -0.3f, 0.0f // bottom right
  )

  override fun getVertexUnit(): Int {
    return 3
  }

  override fun getCoors(): FloatArray {
    return TRIANGLE_COORS
  }

  override fun onChildDraw() {
    super.onChildDraw()
    GLES20.glDrawArrays(
      GLES20.GL_TRIANGLES,                        // 绘制类型
      0,                                          // 数据读取位置
      3    // 顶点数量
    );
  }
}

class Rect : BaseDrawRender(){
  private val SQUARE_VERTEX = floatArrayOf( // 左半个三角形
    -0.5f, 0.5f, 0.0f,  // top left
    -0.5f, -0.5f, 0.0f,  // bottom left
    0.5f, -0.5f, 0.0f,  // bottom right
    // 右半个三角形
    -0.5f, 0.5f, 0.0f,  // top left
    0.5f, -0.5f, 0.0f,  // bottom right
    0.5f, 0.5f, 0.0f // top right
  )

  override fun getVertexUnit(): Int {
    return 3
  }

  override fun getCoors(): FloatArray {
    return SQUARE_VERTEX
  }

  override fun onChildDraw() {
    super.onChildDraw()
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
  }
}


class Circle :BaseDrawRender(){
  //将圆形分割为多少个扇形
  // private val SPLIT_NUM:Int = 8 //8个的时候是个菱形
  private val SPLIT_NUM:Int = 100 //100个基本上是圆形

  /**
   * 描述顶点坐标
   * <p>
   * 圆心 + {@link #SPLIT_NUM} 个三角形({@link #SPLIT_NUM} 个顶点) + 闭合时的顶点
   * 共 {@link #SPLIT_NUM}+2 个顶点
   * <p>
   * 每个顶点为 x, y(忽略 z 坐标)  所以为*2
   */
  private val  mCircleVertexes = FloatArray((SPLIT_NUM + 2) * 2) //n+2个顶点，每个点2个坐标
  override fun getVertexUnit(): Int {
    return 2
  }

  override fun getCoors(): FloatArray {
    // 填充圆心
    mCircleVertexes[0] = 0F
    mCircleVertexes[1] = 0F
    // 填充圆环上的顶点
    val  radius = 0.8f//园的直径为0.8f
    val  angle =  2 * Math.PI / SPLIT_NUM
    for(i in 0..SPLIT_NUM){
      val curAngle = angle * i
      // 计算 x 坐标
      mCircleVertexes[2 * i + 2] = (radius * cos(curAngle)).toFloat()
      // 计算 y 坐标
      mCircleVertexes[2 * i + 1 + 2] = (radius * sin(curAngle)).toFloat()
    }

    // 补充最后一个顶点
    mCircleVertexes[mCircleVertexes.size - 2] = (radius * cos(0.0)).toFloat()
    mCircleVertexes[mCircleVertexes.size - 1] = (radius * sin(0.0)).toFloat()
    return mCircleVertexes
  }

  override fun onChildDraw() {
    super.onChildDraw()
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, SPLIT_NUM + 2)
  }
}