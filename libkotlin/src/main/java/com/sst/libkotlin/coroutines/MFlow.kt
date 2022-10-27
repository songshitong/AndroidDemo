package com.sst.libkotlin.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class MFlow {

  companion object{
    @JvmStatic
    fun main(args: Array<String>) {

    }

   suspend fun simpleFlow(){
     flow {
       for (i in 1..5) {
         delay(100)
         emit(i)
       }
     }.collect{
       println(it)
     }
   }
  }
}