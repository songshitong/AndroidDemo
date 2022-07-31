package sst.example.androiddemo.feature.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import sst.example.androiddemo.feature.R

class DialogFragmentActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_dialog_fragment)
    val dialog =  MyDialog()
    dialog.setStyle(DialogFragment.STYLE_NORMAL,R.style.Dialog_FullScreen)
    dialog.show(supportFragmentManager,"dialogTest")
  }

  class MyDialog : DialogFragment(){
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {
      dialog?.setCanceledOnTouchOutside(false)
      dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
      return  LayoutInflater.from(context).inflate(R.layout.dialog_fragment_test,container,false)
    }

    override fun onResume() {
      super.onResume()
      val params: LayoutParams = dialog!!.window!!.attributes
      params.width = LayoutParams.MATCH_PARENT
      params.height = LayoutParams.MATCH_PARENT
      dialog!!.window!!.attributes = params as WindowManager.LayoutParams
    }
  }
}