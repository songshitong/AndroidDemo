
https://sharrychoo.github.io/blog/opengl-es-2.0/vertex-buffer


在前面绘制图形和纹理时, 我们的顶点坐标的使用流程如下
定义 java 的 float[]
将 float[] 写入 Native, 使用 FloatBuffer 描述
在绘制时, 将 FloatBuffer 传入着色器  //减少每次绘制时的内存拷贝
可以看到, 每次绘制都需要将 FloatBuffer 中的数据, 从 Native 拷贝到 GL 着色器所在的 GPU 内存中, 当顶点数据比较庞大时, 这也会是一笔非常大的开销

VBO 便是解决这个问题很好的途径
一. 什么是 VBO?
VBO 即 Vertex buffer object 顶点缓冲对象, 它通过在 GPU 中开辟一块内存专门用于存放顶点坐标数据的方式, 
减少 GPU 绘制时从物理内存拷贝到 GPU 内存空间所带来的性能损耗


对象的创建
```
 public static native void glGenBuffers(
        int n,             // 缓冲对象数量
        int[] buffers,     // 传出参数, 用于保存创建好的 vbo id
        int offset         // 描述 buffers 的偏移量
    );


    public static native void glGenBuffers(
        int n,                      // 缓冲对象数量
        java.nio.IntBuffer buffers  // Native 的 int array, 用于保存创建好的 vbo id  简称vboid
    );
```
绑定与解绑
```
 public static native void glBindBuffer(
        int target, // 描述绑定的缓冲区类型
        int buffer  // vbo 的 id     buffer 传值为 0 表示, 解绑 vboId
    );
```
target类型
```
target 值	缓冲区类型
GL_ARRAY_BUFFER	数组缓冲
GL_ELEMENT_ARRAY_BUFFER	元素数组缓冲
GL_COPY_READ_BUFFER	复制只读缓冲
GL_COPY_WRITE_BUFFER	复制可写缓冲
GL_PIXEL_PACK_BUFFER	像素打包缓冲
GL_PIXEL_UNPACK_BUFFER	像素解包缓冲
GL_TRANSFORM_FEEDBACK_BUFFER	变换反馈缓冲
GL_UNIFORM_BUFFER	一致变量缓冲
```

数据写入
```
//主要用于存储空间和数据的初始化
 public static native void glBufferData(
        int target,
        int size,
        java.nio.Buffer data,
        int usage
    );

// 主要用于数据的更新
    public static native void glBufferSubData(
        int target,
        int offset,
        int size,
        java.nio.Buffer data
    );
```
target类型
```
缓冲区用途可选参数值	参数说明
GL_STATIC_DRAW	在绘制时，缓冲区对象数据可以被修改一次，使用多次
GL_STATIC_READ	在 OpenGL ES 中读回的数据，缓冲区对象数据可以被修改一次，使用多次，且该数据可以冲应用程序中查询
GL_STATIC_COPY	从 OpenGL ES 中读回的数据，缓冲区对象数据可以被修改一次，使用多次，该数据将直接作为绘制图元或者指定图像的信息来源
GL_DYNAMIC_DRAW	在绘制时，缓冲区对象数据可以被重复修改、使用多次
GL_DYNAMIC_READ	从 OpenGL ES 中读回的数据，缓冲区对象数据可以被重复修改、使用多次，且该数据可以从应用程序中查询
GL_DYNAMIC_COPY	从 OpenGL ES 中读回的数据，缓冲区对象数据可以被重复修改、使用多次，该数据将直接作为绘制图元或者指定图像的信息来源
GL_STREAM_DRAW	在绘制时，缓冲区对象数据可以被修改一次，使用少数几次
GL_STREAM_READ	从 OpenGL ES 中读回的数据，缓冲区对象数据可以被修改一次，使用少数几次，且该数据可以从应用程序中查询
GL_STREAM_COPY	从 OpenGL ES 中读回的数据，缓冲区对象数据可以被修改一次，使用少数几次，且该数据将直接作为绘制图元或者指定图像的信息来源
```

缓冲对象删除
```
 public static native void glDeleteBuffers(
        int n,          
        int[] buffers,
        int offset
    );

    public static native void glDeleteBuffers(
        int n,
        java.nio.IntBuffer buffers
    );
```


缓冲查询
```
    public static native void glGetBufferParameteriv(
        int target, //与写入时的 target 一致
        int pname,
        int[] params, // 传出参数, 用于保存查询结果
        int offset
    );

    public static native void glGetBufferParameteriv(
        int target,
        int pname,
        java.nio.IntBuffer params // 传出参数, 用于保存查询结果
    );
```
pname: 表述要查询的信息
```
缓冲区查询信息	说明
GL_BUFFER_SIZE	缓冲区以字节计的大小
GL_BUFFER_USAGE	缓冲区用途
GL_BUFFER_MAPPED	是否为映射缓冲区
GL_BUFFER_ACCESS_FLAGS	缓冲区访问标志
GL_BUFFER_MAP_LENGTH	缓冲区映射长度
GL_BUFFER_MAP_OFFSET	缓冲区映射偏移
```


示例：
```
public class TextureRenderer implements GLSurfaceView.Renderer {
    
    private float[] mVertexCoordinate;
    private float[] mTextureCoordinate;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;
    
    private int mVboId;
    
    ......
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        ......
        // 创建 vbo(Vertex Buffer Object)
        int vboSize = 1;
        int[] vboIds = new int[vboSize];
        // 1. 创建缓冲对象
        GLES20.glGenBuffers(
                vboSize,      // n: 缓冲区数量
                vboIds,       // buffers: 传出参数, 用于保存创建好的 vbo id
                0             // offset: 描述 buffers 的偏移量
        );
        mVboId = vboIds[0];
        // 2. 绑定缓冲对象
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        
        // 3. 为缓冲对象开辟缓冲区
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, (mVertexCoordinate.length + mTextureCoordinate.length) * 4,
                null, GLES20.GL_STATIC_DRAW);
        // 4.1 将顶点坐标写入vbo
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,
                mVertexCoordinate.length * 4, mVertexBuffer);
        // 4.2 将纹理坐标写入vbo
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mVertexCoordinate.length * 4,
                mTextureCoordinate.length * 4, mTextureBuffer);
                
        // 5. 解绑缓冲对象
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
    
    
     @Override
    public void onDrawFrame(GL10 gl) {
        ......
        // 绑定 vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        // 写入顶点坐标数据
        GLES20.glEnableVertexAttribArray(aVertexPosition);
        GLES20.glVertexAttribPointer(aVertexPosition, 2, GLES20.GL_FLOAT, false, 8, 0); //0为vbo的偏移量
        // 写入纹理坐标数据
        GLES20.glEnableVertexAttribArray(aTexturePosition);
        GLES20.glVertexAttribPointer(aTexturePosition, 2, GLES20.GL_FLOAT, false, 8,
                mVertexCoordinate.length * 4); //mVertexCoordinate.length * 4 为vbo的偏移量
        // 解绑 vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ......
    }
}
```