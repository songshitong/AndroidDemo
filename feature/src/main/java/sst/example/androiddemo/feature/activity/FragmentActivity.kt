package sst.example.androiddemo.feature.activity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R

class FragmentActivity : AppCompatActivity() , NormalFragment.OnFragmentInteractionListener{

    private val  TAG = "FragmentActivity"
    override fun onFragmentInteraction(uri: Uri?) {
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG," onCreate ==== ")

        setContentView(R.layout.activity_fragment)
        val fragment =NormalFragment()
        supportFragmentManager.beginTransaction().add(R.id.container_fragment,fragment).commit()
        findViewById<View>(R.id.showFragment).setOnClickListener {
            supportFragmentManager.beginTransaction().show(fragment).commit()
        }
          findViewById<View>(R.id.hideFragment).setOnClickListener {
            supportFragmentManager.beginTransaction().hide(fragment).commit()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG," onStart ==== ")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG," onResume ==== ")

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG," onPause ==== ")

    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG," onStop ==== ")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG," onDestroy ==== ")

    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG," onRestart ==== ")

    }

}
