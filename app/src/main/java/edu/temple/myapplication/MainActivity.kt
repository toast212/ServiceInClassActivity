package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val DEFAULT_COUNTDOWN_VALUE = 100

    private lateinit var countdownDisplay: TextView
    private lateinit var startButton: Button
    private lateinit var pauseResumeButton: Button

    private var timerService: TimerService.TimerBinder? = null

    // Storage-related variables
    private lateinit var preferences: SharedPreferences
    private val TIMER_VALUE_KEY = "timer_value"
    private val PAUSED_KEY = "paused_state"

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = binder as TimerService.TimerBinder
            timerService?.setHandler(timerHandler)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    private val timerHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            countdownDisplay.text = msg.what.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize preferences first
        preferences = getPreferences(Context.MODE_PRIVATE)

        // Bind UI components
        countdownDisplay = findViewById(R.id.textView)
        startButton = findViewById(R.id.startButton)
        pauseResumeButton = findViewById(R.id.stopButton)
        pauseResumeButton.text = "Pause"

        // Bind to service
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        startButton.setOnClickListener {
            // Check if we're in paused state
            val wasPaused = preferences.getBoolean(PAUSED_KEY, false)

            if (wasPaused) {
                // Get saved timer value
                val savedValue = preferences.getInt(TIMER_VALUE_KEY, DEFAULT_COUNTDOWN_VALUE)
                Log.d("TimerApp", "Resuming from saved value: $savedValue")
                timerService?.start(savedValue)

                // Reset paused state
                preferences.edit().putBoolean(PAUSED_KEY, false).apply()
                pauseResumeButton.text = "Pause"
            } else {
                // Start fresh countdown
                Log.d("TimerApp", "Starting fresh countdown")
                timerService?.start(DEFAULT_COUNTDOWN_VALUE)
                pauseResumeButton.text = "Pause"
            }
        }

        pauseResumeButton.setOnClickListener {
            if (pauseResumeButton.text == "Pause") {
                // We're running, so pause the timer
                Log.d("TimerApp", "Pausing timer")
                timerService?.pause()

                // Save current timer value using editor
                val currentValue = countdownDisplay.text.toString().toIntOrNull() ?: DEFAULT_COUNTDOWN_VALUE
                val editor = preferences.edit()
                editor.putInt(TIMER_VALUE_KEY, currentValue)
                editor.putBoolean(PAUSED_KEY, true)
                editor.apply()
                Log.d("TimerApp", "Saved value: $currentValue, paused: true")

                pauseResumeButton.text = "Resume"
            } else {
                // We're paused, so resume the timer
                Log.d("TimerApp", "Resuming timer")
                timerService?.pause() // This toggles the pause state in the service
                preferences.edit().putBoolean(PAUSED_KEY, false).apply()
                pauseResumeButton.text = "Pause"
            }
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}