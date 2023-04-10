package com.rightfromleftsw.mirrorcam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rightfromleftsw.mirrorcam.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
  private lateinit var viewBinding: ActivityMainBinding

  private lateinit var cameraExecutor: ExecutorService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    // Request camera permissions
    if (allPermissionsGranted()) {
      startCamera()
    } else {
      ActivityCompat.requestPermissions(
        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
      )
    }

    cameraExecutor = Executors.newSingleThreadExecutor()

    hideSystemUi()
  }

  private fun hideSystemUi() {
    val windowInsetsController =
      WindowCompat.getInsetsController(window, window.decorView)
    // Configure the behavior of the hidden system bars.
    windowInsetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
  }

  private fun startCamera() {
    // keep the screen awake
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
      // Used to bind the lifecycle of cameras to the lifecycle owner
      val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

      // Preview
      val preview = Preview.Builder()
        .build()
        .also {
          it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
        }

      // Select back camera as a default
      val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

      try {
        // Unbind use cases before rebinding
        cameraProvider.unbindAll()

        // Bind use cases to camera
        cameraProvider.bindToLifecycle(
          this, cameraSelector, preview
        )

      } catch (exc: Exception) {
        Log.e(TAG, "Use case binding failed", exc)
      }


    }, ContextCompat.getMainExecutor(this))
  }

  private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(
      baseContext, it
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun onDestroy() {
    super.onDestroy()
    cameraExecutor.shutdown()
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults:
    IntArray
  ) {
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
      if (allPermissionsGranted()) {
        startCamera()
      } else {
        Toast.makeText(
          this,
          "Permissions not granted by the user.",
          Toast.LENGTH_SHORT
        ).show()
        finish()
      }
    }
  }

  companion object {
    private const val TAG = "MirrorCam"
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS =
      mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
      ).toTypedArray()
  }
}