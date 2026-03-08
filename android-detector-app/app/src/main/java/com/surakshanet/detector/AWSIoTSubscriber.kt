package com.surakshanet.detector

import android.content.Context
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import org.json.JSONObject
import java.util.UUID

class AWSIoTSubscriber(
    private val context: Context,
    private val onStateReceived: (String) -> Unit
) {

    private val TAG = "AWS_IOT"

    // Your AWS IoT endpoint
    private val endpoint = "a96w9sfw2eym7-ats.iot.ap-south-1.amazonaws.com"

    // Unique client ID
    private val clientId = UUID.randomUUID().toString()

    // MQTT topic
    private val topic = "suraksha/alert"

    private val mqttManager = AWSIotMqttManager(clientId, endpoint)

    init {

        Log.d(TAG, "Initializing AWS IoT")

        AWSMobileClient.getInstance().initialize(
            context,
            object : Callback<UserStateDetails> {

                override fun onResult(result: UserStateDetails?) {

                    Log.d(TAG, "AWSMobileClient initialized")

                    connect()
                }

                override fun onError(e: Exception?) {

                    Log.e(TAG, "AWSMobileClient init error")
                    e?.printStackTrace()
                }
            })
    }

    private fun connect() {

        try {

            mqttManager.connect(AWSMobileClient.getInstance())
            { status, throwable ->

                Log.d(TAG, "Connection status: $status")

                if (status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected) {

                    Log.d(TAG, "Connected to AWS IoT")

                    subscribe()
                }

                throwable?.printStackTrace()
            }

        } catch (e: Exception) {

            Log.e(TAG, "Connection error")
            e.printStackTrace()
        }
    }

    private fun subscribe() {

        try {

            mqttManager.subscribeToTopic(
                topic,
                AWSIotMqttQos.QOS0
            ) { topic, data ->

                val message = String(data)

                Log.d(TAG, "Message received: $message")

                try {

                    val json = JSONObject(message)

                    val state = json.getString("state")

                    onStateReceived(state)

                } catch (e: Exception) {

                    Log.e(TAG, "JSON parse error")
                    e.printStackTrace()
                }
            }

        } catch (e: Exception) {

            Log.e(TAG, "Subscribe error")
            e.printStackTrace()
        }
    }
}