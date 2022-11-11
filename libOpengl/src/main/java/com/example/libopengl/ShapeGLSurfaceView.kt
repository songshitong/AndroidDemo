package com.example.libopengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.jar.Attributes
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ShapeGLSurfaceView(context: Context,attributeSet: AttributeSet?): GLSurfaceView(context,attributeSet) {

  init {
    // 设置 GL 版本号
    setEGLContextClientVersion(2)
    setRenderer(Line())
    // 只有当数据改变时才渲染
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }
}

class Line :GLSurfaceView.Renderer{
  private val LINE_COORS = floatArrayOf(
    0.0f, 0.5f, 0.0f,  // start  x,y,z
    0.0f, -0.5f, 0.0f
  )

  private val LINE_COLOR = floatArrayOf(
    0.63671875f,  // red
    0.76953125f,  // green
    0.22265625f,  // blue
    1.0f // alpha
  )
  // 存储顶点数据的缓冲区域
  private var mVertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(LINE_COORS.size * 4)
    .order(ByteOrder.nativeOrder())    // use the device hardware's native byte order
    .asFloatBuffer()

  init {
// 在 Native 开辟一块空间, 用于存储顶点坐标,java数组native无法读取
    mVertexBuffer.put(LINE_COORS);            // add the coordinates to the FloatBuffer
    mVertexBuffer.position(0);                 // set the buffer to read the first coordinate
  }

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

  private val mViewMatrix = FloatArray(16) // 视图矩阵
  private val mProjectionMatrix = FloatArray(16) // 投影矩阵
  private val mClipMatrix = FloatArray(16) // 裁剪矩阵

  private var mProgram = 0
  override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
    //使用shader创建
    mProgram = createProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)
  }

  override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
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

  override fun onDrawFrame(p0: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glClearColor(1f, 1f, 1f, 1.0f)

    // 1. 在 OpenGL ES 的环境中添加可执行程序
    GLES20.glUseProgram(mProgram)

    // 2. 设置顶点数据
    val attribHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
    GLES20.glEnableVertexAttribArray(attribHandle) // 启动顶点属性数组

    GLES20.glVertexAttribPointer(
      attribHandle,
      3,  // 每个顶点的数量
      GLES20.GL_FLOAT,  // 顶点描述单位
      false,  // 是否标准化
      3 * 4,  // 顶点的跨幅, 即一个顶点占用的字节数
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
    GLES20.glDrawArrays(
      GLES20.GL_LINES,  // 绘制类型
      0,  // 数据读取位置
      2 // 顶点数量
    )
    // 6.关闭可执行程序
    GLES20.glDisableVertexAttribArray(attribHandle)
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


}