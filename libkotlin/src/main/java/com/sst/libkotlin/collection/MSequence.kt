

//Flow与Sequence的关系 https://juejin.cn/post/7066832858664402980
//Sequence是一种容器
//Flow是异步的Sequence
class MSequence {
  companion object{

    @JvmStatic
    fun main(args: Array<String>){

      //Collection的处理
      val wordList = listOf("The","quick","brown", "fox", "jumps", "over", "the", "lazy", "dog")
      val lengthsList = wordList
        .filter { it.length > 3 } //生成一个collection
        .map { it.length } //生成一个collection
        .take(4) //生成一个collection
      println("Lengths of first 4 words longer than 3 chars:${lengthsList}") //5, 5, 5, 4
      //集合的每一个操作都会返回一个新的集合，然后对新的集合做下一个操作  操作越多，越浪费内存

      //filter的实现
      // public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean): List<T> {
      //   return filterTo(ArrayList<T>(), predicate) //创建一个新的ArrayList，存放数据
      // }


      //Sequence中的元素，按顺序一个一个处理，执行完全部集合操作后返回
      //Sequence这种操作方式就像IO流，当我们读磁盘上的一个文件时，读取数据就流水一样,一波一波的流向InputStream，
    // 而不是等全部数据加载完成后再返回给InputStream
      //流式操作的优势
      //更加灵活，高效，内存消耗低。
      //可以处理大量数据的情况   例如从100万数据中取10个，使用collection需要生成多个中间collection，并且100万数据可能遍历多次
      //   而sequence只需要一个collection，最好情况是只遍历4个元素，最差是全部遍历完成


      //数据结果的触发时机
      //collection每步执行全部数据，所以依次打印字符
      listOf("The","quick","brown").map {
        println("map $it")
        it
      }
      // sequence map不打印结果
      // Sequence的操作分为中间操作（intermediate operations）和末端操作（terminal operations）。
      // Sequence的操作函数如果返回值的类型是Sequence，那么这个操作就是一个中间操作。这些操作并不会触发数据的发射和遍历。 map,filter,take等
      //否则这个操作就是末端操作。只有对Sequence执行末端操作才会触发数据的发射和遍历 forEach,first,count,fold等
      listOf("The","quick","brown").asSequence().map {
        println("Sequence map $it")
        it
      }


      // 创建Sequence
      val numbersSequence1 = sequenceOf("four", "three", "two", "one")
      val numbersSequence2 = listOf("one", "two", "three", "four").asSequence() //创建为sequence

      val sequence = generateSequence(1) { it + 2 } //生成一个从1开始，每次增加2的sequence  元素为null时，视为结束

      //在协程里面创建Sequence
      val seq = sequence {//这是一个协程体
        yield(1)
        yield(2) //SequenceScope.yield(value)是挂起函数，只能在SequenceScope的协程体中执行
        yieldAll(listOf(3, 4, 5))
        yieldAll(sequenceOf(6, 7, 8))
        yieldAll(generateSequence(9) {
          if (it <= 11) {
            it + 1
          } else {
            null
          }
        })
      }
    }
  }
}