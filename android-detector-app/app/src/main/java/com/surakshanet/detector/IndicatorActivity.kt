package com.surakshanet.detector

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context

class IndicatorActivity : ComponentActivity() {

    private var currentAlertState = "SAFE"
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var alertRunnable: Runnable? = null

    private lateinit var rootLayout: FrameLayout

    private var lastAlertTime = 0L

    private lateinit var vibrator: Vibrator
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mediaPlayer = MediaPlayer.create(this, R.raw.warning)

        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(Color.GREEN)

        setContentView(rootLayout)

        // AWS IoT Subscriber
        AWSIoTSubscriber(this) { state ->
            runOnUiThread {
                updateIndicator(state)
            }
        }
    }

    private fun triggerAlert() {

        val now = System.currentTimeMillis()

        if (now - lastAlertTime < 1500) return

        lastAlertTime = now

        vibrator.vibrate(
            VibrationEffect.createOneShot(
                500,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )

        mediaPlayer?.start()
    }

    private fun startAlert(interval: Long) {

        stopAlert()

        alertRunnable = object : Runnable {
            override fun run() {

                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        200,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()

                handler.postDelayed(this, interval)
            }
        }

        handler.post(alertRunnable!!)
    }

    private fun stopAlert() {

        alertRunnable?.let {
            handler.removeCallbacks(it)
        }

        alertRunnable = null
    }

    private fun updateIndicator(state: String) {

        if (state != currentAlertState) {

            currentAlertState = state

            when (state) {

                "SAFE" -> {
                    rootLayout.setBackgroundColor(Color.GREEN)
                    stopAlert()
                }

                "WARNING" -> {
                    rootLayout.setBackgroundColor(Color.YELLOW)
                    startAlert(1500)
                }

                "DANGER" -> {
                    rootLayout.setBackgroundColor(Color.RED)
                    startAlert(150)
                }
            }
        }
    }
}