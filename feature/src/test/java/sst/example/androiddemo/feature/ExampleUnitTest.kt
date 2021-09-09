package sst.example.androiddemo.feature

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val a = A()
        a.name = "a"
        setName(a)
        println(a.name)
    }

    class A {
        var name: String? = null
    }



    fun setName(a: A) {
        a.name = "b"
    //kotlin不支持
//        a = A()
        a.name = "c"
    }
}
