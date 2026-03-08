package com.surakshanet.detector

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create vertical layout
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)

        // Detector button
        val detectorButton = Button(this)
        detectorButton.text = "Detector Device"

        // Indicator button
        val indicatorButton = Button(this)
        indicatorButton.text = "Indicator Device"

        // Add buttons to layout
        layout.addView(detectorButton)
        layout.addView(indicatorButton)

        setContentView(layout)

        // Open detector mode
        detectorButton.setOnClickListener {
            val intent = Intent(this, DetectorActivity::class.java)
            startActivity(intent)
        }

        // Open indicator mode
        indicatorButton.setOnClickListener {
            val intent = Intent(this, IndicatorActivity::class.java)
            startActivity(intent)
        }
    }
}