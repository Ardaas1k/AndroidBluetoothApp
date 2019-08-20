package com.example.bluetoothapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.Parameters
import android.os.*
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE


)

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity() {

    private lateinit var videoCapture: VideoCapture
    private lateinit var mainHandler: Handler
    private lateinit var onRecordTask: Runnable
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        viewFinder = findViewById(R.id.view_finder)

        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        imageButton.visibility = View.GONE
        gallery_button.setOnClickListener {

            val intent: Intent = Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)

        }
        mainHandler = Handler(Looper.getMainLooper())
    }

    val PICK_IMAGE = 1

    @SuppressLint("MissingSuperCall")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE) {
            val selectedImageURI = data?.getData();
            if(selectedImageURI!=null){
                imageButton.visibility = View.VISIBLE
                imageButton.setImageURI(selectedImageURI)
            }
            imageButton.setOnClickListener {
                imageButton.visibility= View.GONE
                val intent:Intent = Intent(this,GalleryActivity::class.java)
                intent.putExtra("imageUri",selectedImageURI.toString())
                startActivity(intent)
            }
        }
    }

    private lateinit var viewFinder: TextureView


    @SuppressLint("RestrictedApi")
    fun startCamera() {

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetRotation(viewFinder.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)
            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setTargetResolution(screenSize)
                setTargetAspectRatio(screenAspectRatio)
                setTargetRotation(viewFinder.display.rotation)
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }.build()

        imageCapture = ImageCapture(imageCaptureConfig)
        val imageFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)


        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
            val imageFile = File(imageFilePath, "${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(imageFile,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(error: ImageCapture.UseCaseError,
                                         message: String, exc: Throwable?) {
                        val msg = "Photo capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.e("CameraXApp", msg)
                        exc?.printStackTrace()
                    }

                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d("CameraXApp", msg)
                    }
                })
        }

        onRecordTask = object : Runnable{
            override fun run() {
                val imageFile = File(imageFilePath, "${System.currentTimeMillis()}.jpg")
                imageCapture.takePicture(imageFile,
                    object : ImageCapture.OnImageSavedListener {
                        override fun onError(error: ImageCapture.UseCaseError,
                                             message: String, exc: Throwable?) {
                            val msg = "Photo capture failed: $message"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.e("CameraXApp", msg)
                            exc?.printStackTrace()
                        }
                        override fun onImageSaved(file: File) {
                            val msg = "Photo capture succeeded: ${file.absolutePath}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d("CameraXApp", msg)
                            println("Captured")
                            Toast.makeText(baseContext,"Captured", Toast.LENGTH_SHORT).show()
                        }
                    })
                mainHandler.postDelayed(this,1000)
            }
        }

        val videoFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val videoFile = File(videoFilePath, "${System.currentTimeMillis()}.mp4")

        videoCapture_button.setOnClickListener {
            videoCapture_button.visibility=View.GONE
            videoStopCapture_button.visibility=View.VISIBLE
            mainHandler.post(onRecordTask)
            var msg = "Video Record Started"
            Toast.makeText(baseContext,msg, Toast.LENGTH_SHORT).show()
            videoCapture.startRecording(videoFile, object: VideoCapture.OnVideoSavedListener{
                override fun onVideoSaved(file: File?) {
                    msg = "Video capture succeeded: ${file?.absolutePath}"
                    Log.d("CameraXApp", "Video File : $file")
                    mainHandler.removeCallbacks(onRecordTask)
                    Toast.makeText(baseContext,msg, Toast.LENGTH_SHORT).show()
                }
                override fun onError(useCaseError: VideoCapture.UseCaseError?, message: String?, cause: Throwable?) {
                    Log.d("CameraXApp", "Video Error: $message")
                    msg ="Video capture failed:$message"
                    Toast.makeText(baseContext,msg, Toast.LENGTH_SHORT).show()

                }
            })
        }

        videoStopCapture_button.visibility = View.GONE
        videoStopCapture_button.setOnClickListener {
            videoStopCapture_button.visibility = View.GONE
            videoCapture_button.visibility = View.VISIBLE
            videoCapture.stopRecording()
            Log.i("CameraXApp", "Video File stopped")
            val msg = "Video Record Stopped"
            Toast.makeText(baseContext,msg, Toast.LENGTH_SHORT).show()
        }


        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // Use a worker thread for image analysis to prevent glitches
            val analyzerThread = HandlerThread(
                "LuminosityAnalysis").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        ImageAnalysis(analyzerConfig).apply {
            analyzer = LuminosityAnalyzer()
        }

        CameraX.bindToLifecycle(this, preview, imageCapture,videoCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        /**
         * Helper extension function used to extract a byte array from an
         * image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            // Calculate the average luma no more often than every second
            if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)) {
                // Since format in ImageAnalysis is YUV, image.planes[0]
                // contains the Y (luminance) plane
                val buffer = image.planes[0].buffer
                // Extract image data from callback object
                val data = buffer.toByteArray()
                // Convert the data into an array of pixel values
                val pixels = data.map { it.toInt() and 0xFF }
                // Compute average luminance for the image
                val luma = pixels.average()
                // Log the new luma value
                Log.d("CameraXApp", "Average luminosity: $luma")
                // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp
            }
        }
    }
}
