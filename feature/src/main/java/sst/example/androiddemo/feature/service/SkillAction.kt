package sst.example.androiddemo.feature.service

import java.util.Queue
import kotlin.random.Random

object SkillManager {
  private val actionQueue = ArrayDeque<SkillAction>()
  private val skillMap = HashMap<String, SkillAction>(20)

  fun initSkillAction(skillAction: SkillAction) {
    skillMap[skillAction.name] = skillAction
    //2110 798  attack
  }

  fun action(){

  }

}

//触发方式
interface ActionTrigger{

}

data class SkillAction(
  val name: String,
  val centerX: Int,
  val centerY: Int,
  //技能对应的英雄等级
  val level : Int,
  val sleepTime: Long,
  val activeTime: Long,
  //长按时间
  val holdTime:Long =0
) {

  fun nextActiveTime(): Long {
    return activeTime + holdTime +sleepTime
  }
}

class RandomGenerator {
  companion object {

    fun genTapRange(): Long {
      return Random.Default.nextLong(150, 500)
    }

    fun genSmallRange(value: Int): Int {
      return genRange(value, 2)
    }

    fun genMediumRange(value: Int): Int {
      return genRange(value, 3)
    }

    fun genBigRange(value: Int): Int {
      return genRange(value, 5)
    }

    fun genRange(value: Int, level: Int): Int {
      return genRange(value, -10 * level, 10 * level)
    }

    fun genRange(value: Int, offsetBottom: Int, offsetTop: Int): Int {
      return value + Random.Default.nextInt(offsetBottom, offsetTop)
    }
  }
}