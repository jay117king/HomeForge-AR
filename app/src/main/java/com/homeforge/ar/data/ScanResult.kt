package com.homeforge.ar.data

/**
 * Holds the result of a completed AR scan so it can be passed to the 3D room view.
 * For MVP we store key dimensions and a simple rectangular room approximation.
 */
data class ScanResult(
    val widthMeters: Float = 4.0f,   // X
    val depthMeters: Float = 3.5f,   // Z
    val heightMeters: Float = 2.5f,  // Y
    val planeCount: Int = 0,
    val lockedDistanceMeters: Float? = null
)

/** Simple in-memory holder so ScanScreen can hand data to RoomViewScreen */
object ScanResultHolder {
    @Volatile
    var latest: ScanResult = ScanResult()
}
