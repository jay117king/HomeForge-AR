package com.homeforge.ar.ar

import com.google.ar.core.Pose
import kotlin.math.sqrt

/**
 * Simple virtual tape measure.
 * User taps two points on detected planes / depth; distance is computed in meters.
 * Lock freezes the current measurement.
 */
class TapeMeasure {

    private var pointA: Pose? = null
    private var pointB: Pose? = null
    private var locked = false
    private var lockedDistanceMeters: Float? = null

    val isLocked: Boolean get() = locked
    val currentDistanceMeters: Float?
        get() = if (locked) lockedDistanceMeters else computeDistance()

    fun setPointA(pose: Pose) {
        if (locked) return
        pointA = pose
        pointB = null
    }

    fun setPointB(pose: Pose) {
        if (locked) return
        pointB = pose
    }

    fun lock() {
        lockedDistanceMeters = computeDistance()
        locked = lockedDistanceMeters != null
    }

    fun unlock() {
        locked = false
        lockedDistanceMeters = null
    }

    fun reset() {
        pointA = null
        pointB = null
        locked = false
        lockedDistanceMeters = null
    }

    fun hasBothPoints(): Boolean = pointA != null && pointB != null

    private fun computeDistance(): Float? {
        val a = pointA ?: return null
        val b = pointB ?: return null
        val dx = a.tx() - b.tx()
        val dy = a.ty() - b.ty()
        val dz = a.tz() - b.tz()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceCm(): Float? = currentDistanceMeters?.times(100f)

    fun formattedDistance(): String {
        val cm = distanceCm() ?: return "—"
        return if (cm >= 100f) {
            String.format("%.2f m", cm / 100f)
        } else {
            String.format("%.1f cm", cm)
        }
    }
}
