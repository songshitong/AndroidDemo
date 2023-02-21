package sst.example.androiddemo.feature.service

import org.greenrobot.eventbus.EventBus
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.random.Random

//存储服务
interface ISaveService{
  fun saveString(file: File,str:String){
    if(file.parentFile?.exists()!=true){
      File(file.parent?:"").mkdirs()
    }
    if(!file.exists()){
      file.createNewFile()
    }
    onSaveString(file,str)
  }

  fun onSaveString(file: File,str:String)

  fun readString(file: File):String{
    if(!file.exists()){
      return ""
    }
    return onReadString(file)
  }

   fun onReadString(file: File):String
}

object SaveService : ISaveService{

  @Synchronized
  override fun onReadString(file: File): String {
    return ""
  }

  @Synchronized
  override fun onSaveString(file: File, str: String) {

  }
}

//task相关
object TaskManager {
  private const val TASK_FILE = "task_file"
  fun saveTask(task: Task){
    SaveService.saveString(File(TASK_FILE),task.toString())
  }
  fun listTask():List<Task>{
    val taskString = SaveService.readString(File(TASK_FILE))
    val taskList = mutableListOf<Task>()
    return taskList
  }
}

//定义一个任务
class Task(val name: String) {

  private val actionQueue = ArrayDeque<Action>()
  fun nextAction(action: Action) {
    actionQueue.add(action)
  }

  fun save(){
    TaskManager.saveTask(this)
  }
}

object SkillManager {
  private val actionQueue = ArrayDeque<SkillAction>()
  private val skillMap = HashMap<String, SkillAction>(20)

  fun initSkillAction(skillAction: SkillAction) {
    skillMap[skillAction.name] = skillAction
    //2110 798  attack
  }

  fun action(skill: SkillAction) {
    actionQueue.add(skill)
  }
}

//触发方式
interface ActionTrigger {
  fun init() {
  }

  fun tap(x: Int, y: Int)
}

class AdbTrigger : ActionTrigger {
  override fun tap(x: Int, y: Int) {
   val process = ProcessBuilder("adb shell","input tap $x $y").also {
      it.redirectErrorStream(true)
    }.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
      println(line)
    }
  }
}

class AccessibilityService : ActionTrigger {
  override fun tap(x: Int, y: Int) {
    EventBus.getDefault().post("")
  }
}

open class Action(
  val name: String,
  val centerX: Int,
  val centerY: Int,
) {

}

class SkillAction(
  name: String,
  centerX: Int,
  centerY: Int,
  //技能对应的英雄等级
  val level: Int,
  //冷却
  val sleepTime: Long,
  //点技能的时间
  val activeTime: Long,
  //长按时间
  val holdTime: Long = 0,
  //连击数
  val continueCount: Int = 1,
) : Action(name, centerX, centerY) {

  fun nextActiveTime(): Long {
    return activeTime + holdTime + sleepTime
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