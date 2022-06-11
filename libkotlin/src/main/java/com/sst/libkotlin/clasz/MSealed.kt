package com.sst.libkotlin.clasz

class MSealed {
    companion object{
        //对于接口或枚举类，需要新增额外的else分支
        fun getIResult(result:IResult) = when(result){
            is ISuccess -> "success"
            is IError ->" error"
            else -> {}//这个 else 就是一个无用分支，这仅仅是为了满足编译器的要求
        }

        //使用密封类不需要添加额外的else分支，当存在未覆盖的分支会提示错误，比如新增分支InProgress
        fun getResultMsg(result: Result<String>) = when (result){
            is Result.Success -> "Success"
            is Result.Error -> "Failure"
        }
    }


}
//1 枚举自身存在许多限制。枚举类型的每个值只允许有一个实例，同时枚举也无法为每个类型添加额外信息，
//   例如，您无法为枚举中的 "Error" 添加相关的 Exception 类型数据。
//2 可以使用一个抽象类然后让一些类继承它，这样就可以随意扩展，但这会失去枚举所带来的有限集合的优势
//3 sealed class 密封类包含抽象类表示的灵活性和枚举里集合的受限性

//密封类可用于表示层级关系。子类可以是任意的类: 数据类、Kotlin 对象、普通的类，甚至也可以是另一个密封类。但不同于抽象类的是，
//您必须把层级声明在同一文件中，或者嵌套在类的内部。
sealed class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
//    object InProgress : Result<Nothing>()
}
//密封类的编译
// 1 密封类的元数据Metadata中保存了一个子类的列表，编译器可以在需要的地方使用这些信息
// 2 Result是一个抽象类，构造器有两个，一个是私有的，一个是合成构造方法，只有kotlin编译器可以调用
//@Metadata(
//    ...
//d2 = {"Lio/testapp/Result;", "T", "", "()V", "Error", "Success", "Lio/testapp/Result$Success;", "Lio/testapp/Result$Error;" ...}
//)
//
//public abstract class Result {
//    private Result() {
//    }
//
//    // $FF: synthetic method
//    public Result(DefaultConstructorMarker $constructor_marker) {
//        this();
//    }
//}

//success类的编译  success继承Result,并调用Result 的合成构造方法
//public final class Success extends Result {
//    @NotNull
//    private final Object data;
//   public Success(@NotNull Object data) {
//        Intrinsics.checkParameterIsNotNull(data, "data");
//        super((DefaultConstructorMarker)null);
//        this.data = data;
//    }

interface IResult{}
class ISuccess :IResult{}
class IError :IResult{}