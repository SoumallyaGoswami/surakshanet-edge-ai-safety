package com.surakshanet.detector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import android.widget.FrameLayout

class DetectorActivity : ComponentActivity() {

    private lateinit var awsIoTManager: AWSIoTManager

    private var lastProcessTime = 0L

    private var smoothedRelativeSpeed = 0f

    private var trackedCenterX = 0f
    private var trackedCenterY = 0f
    private var hasTrackedVehicle = false

    private var previousCenterY = 0f

    private var dynamicZone = 0.25f

    private var lastVehicleSeenTime = 0L
    private var lastDetections: List<org.tensorflow.lite.task.vision.detector.Detection> = emptyList()

    private val persistenceTime = 800L   // milliseconds

    private lateinit var previewView: PreviewView
    private lateinit var objectDetector: ObjectDetector
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var overlayView: OverlayView

    private var smoothedTti = 0f

    private var previousHeight = 0f
    private var previousTime = 0L
    private val heightHistory = mutableListOf<Float>()

    private var lastDetectionTime = 0L

    private var lastSentState = "SAFE"
    private var stateCounter = 0
    private val requiredStableFrames = 5

    private var dangerCooldownEnd = 0L

    private var lastDetectedState = "SAFE"

    private val dangerHoldTime = 2000L   // 3 seconds



    private fun sendSignalToIndicator(state: String) {
        Thread {
            try {
                val socket = java.net.Socket("10.103.39.86", 9999)
                val writer = socket.getOutputStream()
                writer.write((state + "\n").toByteArray())
                writer.flush()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setupDetector()
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        awsIoTManager = AWSIoTManager(this)

        previewView = PreviewView(this)
        val rootLayout = FrameLayout(this)

        overlayView = OverlayView(this)

        rootLayout.addView(previewView)
        rootLayout.addView(overlayView)

        setContentView(rootLayout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setupDetector()
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupDetector() {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(0.5f)   // 🔥 increase this
            .setMaxResults(5)
            .build()

        objectDetector = ObjectDetector.createFromFileAndOptions(
            this,
            "vehicle_model.tflite",
            options
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(600, 400))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                processImage(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        }, ContextCompat.getMainExecutor(this))
    }


    private fun processImage(imageProxy: ImageProxy) {

        val now = System.currentTimeMillis()


        val earlyZone = 0.15f
        val dangerZone = dynamicZone

        val bitmap = imageProxy.toBitmap()
        val tensorImage = TensorImage.fromBitmap(bitmap)

        val rawResults = objectDetector.detect(tensorImage)

        val filteredResults = rawResults.filter { detection ->
            val category = detection.categories.firstOrNull()?.label ?: ""
            val score = detection.categories.firstOrNull()?.score ?: 0f

            val isVehicle =
                category == "car" ||
                        category == "bus" ||
                        category == "truck" ||
                        category == "motorcycle"

            val box = detection.boundingBox
            val centerY = (box.top + box.bottom) / 2


            val inEarlyZone = centerY > bitmap.height * earlyZone

            isVehicle && score >= 0.5f && inEarlyZone

        }


        val currentTime = now

        // -------- OBJECT PERSISTENCE --------

        if (filteredResults.isNotEmpty()) {
            lastVehicleSeenTime = now
            lastDetections = filteredResults
        }

        val activeDetections =
            if (filteredResults.isNotEmpty()) {
                filteredResults
            } else if (now - lastVehicleSeenTime < persistenceTime) {
                lastDetections
            } else {
                emptyList()
            }

        // -------- VEHICLE LOGIC --------

        if (activeDetections.isNotEmpty()) {

            val selectedVehicle = if (!hasTrackedVehicle) {

                // FIRST VEHICLE LOCK
                val biggest = activeDetections.maxByOrNull {
                    it.boundingBox.width() * it.boundingBox.height()
                }

                biggest?.let {
                    val box = it.boundingBox
                    trackedCenterX = (box.left + box.right) / 2
                    trackedCenterY = (box.top + box.bottom) / 2
                    hasTrackedVehicle = true
                }

                biggest

            } else {

                // FIND THE CLOSEST VEHICLE TO PREVIOUS TRACK
                activeDetections.minByOrNull { detection ->

                    val box = detection.boundingBox
                    val centerX = (box.left + box.right) / 2
                    val centerY = (box.top + box.bottom) / 2

                    val dx = centerX - trackedCenterX
                    val dy = centerY - trackedCenterY

                    dx*dx + dy*dy
                }
            }

            selectedVehicle?.let { detection ->

                val box = detection.boundingBox
                trackedCenterX = (box.left + box.right) / 2
                trackedCenterY = (box.top + box.bottom) / 2
                val currentHeight = box.height().toFloat()
                val centerY = (box.top + box.bottom) / 2
                val currentTime = System.currentTimeMillis()

                if (centerY < bitmap.height * earlyZone) {
                    return@let
                }

                heightHistory.add(currentHeight)
                if (heightHistory.size > 5) {
                    heightHistory.removeAt(0)
                }
                //android.util.Log.d("VEHICLE", "Tracking vehicle: ${detection.categories.firstOrNull()?.label}")

                val averageHeight = heightHistory.average().toFloat()

                if (previousTime != 0L) {

                    val deltaTime = (currentTime - previousTime) / 1000f
                    val deltaHeight = averageHeight - previousHeight

                    val deltaCenterY = centerY - previousCenterY
                    val movingTowardCamera = deltaCenterY > 0

                    val approaching = deltaHeight > 0 && movingTowardCamera

                    if (deltaTime > 0f && approaching) {

                        val growthRate = deltaHeight / deltaTime
                        val relativeSpeedRaw = growthRate / averageHeight

// Smooth the relative speed
                        val speedAlpha = 0.3f

                        smoothedRelativeSpeed =
                            if (smoothedRelativeSpeed == 0f)
                                relativeSpeedRaw
                            else
                                smoothedRelativeSpeed * (1 - speedAlpha) + relativeSpeedRaw * speedAlpha
                        dynamicZone = when {
                            smoothedRelativeSpeed > 0.20f -> 0.10f
                            smoothedRelativeSpeed > 0.10f -> 0.15f
                            smoothedRelativeSpeed > 0.05f -> 0.20f
                            else -> 0.25f
                        }

                        if (smoothedRelativeSpeed > 0.005f){

                            val tti = 1f / smoothedRelativeSpeed
                            val safeTti = tti.coerceIn(0.5f, 10f)

                            val alpha = 0.2f

                            smoothedTti =
                                if (smoothedTti == 0f) safeTti
                                else smoothedTti * (1 - alpha) + safeTti * alpha

                            //android.util.Log.d("TTI", "Smoothed TTI: $smoothedTti seconds")

                            var state = when {
                                smoothedTti > 6 -> "SAFE"
                                smoothedTti > 4 -> "WARNING"
                                else -> "DANGER"
                            }

                            if (state == "DANGER") {
                                dangerCooldownEnd = now + dangerHoldTime
                            }

                            if (now < dangerCooldownEnd) {
                                state = "DANGER"
                            }

                            //android.util.Log.d("STATE", "Collision State: $state")

                            if (state == lastDetectedState) {
                                stateCounter++
                            } else {
                                lastDetectedState = state
                                stateCounter = 1
                            }

                            if (stateCounter >= requiredStableFrames) {

                                if (state != lastSentState) {

                                    awsIoTManager.publishState(state, smoothedTti)

                                    //android.util.Log.d(
                                        //"STATE",
                                        //"Stable State Sent: $state"
                                    //)

                                    lastSentState = state
                                }
                            }
                        }
                    }
                }

                previousHeight = averageHeight
                previousCenterY = centerY
                previousTime = currentTime
            }
        }

        // -------- OVERLAY CONTROL --------

        if (activeDetections.isNotEmpty()) {

            lastDetectionTime = now

            runOnUiThread {
                overlayView.setResults(
                    activeDetections,
                    bitmap.width,
                    bitmap.height,
                    smoothedTti
                )            }

        } else {

            if (now - lastDetectionTime > 300) {
                hasTrackedVehicle = false
                smoothedRelativeSpeed = 0f

                runOnUiThread {
                    overlayView.clear()
                }

                smoothedTti = 10f

                if (now > dangerCooldownEnd && lastSentState != "SAFE") {

                    awsIoTManager.publishState("SAFE", smoothedTti)
                    //android.util.Log.d("STATE", "Cooldown finished → SAFE")

                    lastSentState = "SAFE"
                }
            }
        }

        imageProxy.close()
    }
}