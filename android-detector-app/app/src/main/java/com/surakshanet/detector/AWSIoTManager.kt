package com.surakshanet.detector

import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.regions.Regions
import org.json.JSONObject
import java.util.UUID

class AWSIoTManager(context: Context) {

    private val TAG = "AWS_IOT"

    private val clientId = UUID.randomUUID().toString()

    private val endpoint =
        "a96w9sfw2eym7-ats.iot.ap-south-1.amazonaws.com"

    private val topic = "suraksha/alert"

    private val mqttManager = AWSIotMqttManager(clientId, endpoint)

    init {

        Log.d(TAG, "Creating MQTT Manager")

        val credentialsProvider =
            CognitoCachingCredentialsProvider(
                context,
                "ap-south-1:9a6dd31e-aea8-4349-94f6-f78ff3af3995",
                Regions.AP_SOUTH_1
            )

        try {

            mqttManager.connect(credentialsProvider,
                AWSIotMqttClientStatusCallback { status, throwable ->

                    Log.d(TAG, "Connection status = $status")

                    if (throwable != null) {
                        Log.e(TAG, "Connection error", throwable)
                    }
                })

        } catch (e: Exception) {

            Log.e(TAG, "MQTT connection failed", e)
        }
    }

    fun publishState(state: String, tti: Float) {

        try {

            val json = JSONObject()

            json.put("state", state)
            json.put("tti", tti)
            json.put("timestamp", System.currentTimeMillis())

            val message = json.toString()

            Log.d(TAG, "Publishing -> $message")

            mqttManager.publishString(
                message,
                topic,
                AWSIotMqttQos.QOS0
            )

        } catch (e: Exception) {

            Log.e(TAG, "Publish failed", e)
        }
    }
}