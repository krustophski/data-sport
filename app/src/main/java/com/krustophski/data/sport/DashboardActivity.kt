package com.krustophski.data.sport

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.krustophski.data.sport.events.BluetoothActionEvent
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(this.javaClass.simpleName, "onCreate")
        setContentView(R.layout.activity_dashboard)
        findNavController(R.id.nav_host_fragment_dashboard).setGraph(
            R.navigation.dashboard_nav_graph,
            intent.extras
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(this.javaClass.simpleName, "onStart")
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean(MainActivity.PREF_AUTO_BACKGROUND_AFTER_START, false)) {
            prefs.edit().putBoolean(MainActivity.PREF_AUTO_BACKGROUND_AFTER_START, false).commit()
            window.decorView.postDelayed({
                moveTaskToBack(true)
            }, 2500)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(this.javaClass.simpleName, "onStop")
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onBluetoothActionEvent(event: BluetoothActionEvent) {
        Log.d(this.javaClass.simpleName, "Show enable bluetooth dialog")
        startActivity(Intent(event.action))
    }
}
