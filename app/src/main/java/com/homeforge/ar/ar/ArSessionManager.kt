package com.homeforge.ar.ar

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the ARCore session lifecycle, plane detection and depth access.
 * Designed to be used from a Compose AndroidView or a dedicated SurfaceView.
 */
class ArSessionManager(private val context: Context) {

    companion object {
        private const val TAG = "ArSessionManager"
    }

    private var session: Session? = null
    private val isSessionConfigured = AtomicBoolean(false)

    var onPlaneDetected: ((Plane) -> Unit)? = null
    var onDepthAvailable: ((Frame) -> Unit)? = null

    fun createSession(): Boolean {
        return try {
            session = Session(context)
            true
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore not installed", e)
            false
        } catch (e: UnavailableApkTooOldException) {
            Log.e(TAG, "ARCore APK too old", e)
            false
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "SDK too old", e)
            false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible with ARCore", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AR session", e)
            false
        }
    }

    fun configureSession() {
        val session = this.session ?: return

        val config = Config(session).apply {
            // Enable depth if supported
            depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }

            // Plane detection for floor + walls
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

            // Light estimation for better PBR later
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

            // Instant placement optional – keep simple for MVP
            instantPlacementMode = Config.InstantPlacementMode.DISABLED
        }

        try {
            session.configure(config)
            isSessionConfigured.set(true)
            Log.i(TAG, "AR session configured. Depth: ${config.depthMode}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure session", e)
        }
    }

    fun resume() {
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available", e)
        }
    }

    fun pause() {
        session?.pause()
    }

    fun close() {
        session?.close()
        session = null
        isSessionConfigured.set(false)
    }

    fun update(): Frame? {
        return try {
            session?.update()?.also { frame ->
                // Notify listeners
                if (frame.getUpdatedTrackables(Plane::class.java).isNotEmpty()) {
                    frame.getUpdatedTrackables(Plane::class.java).forEach { plane ->
                        onPlaneDetected?.invoke(plane)
                    }
                }
                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    onDepthAvailable?.invoke(frame)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session update failed", e)
            null
        }
    }

    fun getSession(): Session? = session

    fun isDepthSupported(): Boolean {
        return session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true
    }
}
