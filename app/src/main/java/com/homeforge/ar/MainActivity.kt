package com.homeforge.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.homeforge.ar.ui.HomeForgeApp
import com.homeforge.ar.ui.theme.HomeForgeARTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkArCoreAvailability()
            } else {
                Toast.makeText(this, "Camera permission is required for AR scanning", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HomeForgeARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeForgeApp()
                }
            }
        }

        // Request camera permission first
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                checkArCoreAvailability()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkArCoreAvailability() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        when {
            availability.isTransient -> {
                // Retry later
                window.decorView.postDelayed({ checkArCoreAvailability() }, 200)
            }
            availability.isSupported -> {
                // ARCore is ready – proceed with app
            }
            else -> {
                Toast.makeText(
                    this,
                    "This device does not support ARCore Depth API. Basic measurement still available.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
