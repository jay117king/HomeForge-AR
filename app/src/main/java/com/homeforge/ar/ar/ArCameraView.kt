package com.homeforge.ar.ar

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

/**
 * Full ARCore camera view with:
 * - Camera background rendering
 * - Plane detection visualization (simple colored grid)
 * - Hit testing for tape measure
 * - Lifecycle-aware session management
 */
@Composable
fun ArCameraView(
    modifier: Modifier = Modifier,
    onPlaneCountChanged: (Int) -> Unit = {},
    onDistanceChanged: (String) -> Unit = {},
    onLockedChanged: (Boolean) -> Unit = {},
    measureRequested: Boolean = false,
    lockRequested: Boolean = false,
    resetMeasure: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val sessionManager = remember { ArSessionManager(context) }
    val tapeMeasure = remember { TapeMeasure() }

    // Expose latest values to the renderer via a holder that survives recomposition
    val stateHolder = remember {
        ArViewState(
            sessionManager = sessionManager,
            tapeMeasure = tapeMeasure,
            onPlaneCountChanged = onPlaneCountChanged,
            onDistanceChanged = onDistanceChanged,
            onLockedChanged = onLockedChanged
        )
    }

    // Keep callbacks up to date
    stateHolder.onPlaneCountChanged = onPlaneCountChanged
    stateHolder.onDistanceChanged = onDistanceChanged
    stateHolder.onLockedChanged = onLockedChanged

    // React to measure / lock / reset requests from the Compose UI
    LaunchedEffect(measureRequested) {
        if (measureRequested) {
            stateHolder.requestMeasure = true
        }
    }
    LaunchedEffect(lockRequested) {
        if (lockRequested) {
            stateHolder.requestLock = true
        }
    }
    LaunchedEffect(resetMeasure) {
        if (resetMeasure) {
            tapeMeasure.reset()
            onDistanceChanged("—")
            onLockedChanged(false)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (sessionManager.createSession()) {
                    sessionManager.configureSession()
                    sessionManager.resume()
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                sessionManager.pause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                sessionManager.close()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sessionManager.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val glView = GLSurfaceView(ctx).apply {
                preserveEGLContextOnPause = true
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                setRenderer(ArRenderer(stateHolder))
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        stateHolder.lastTouchX = event.x
                        stateHolder.lastTouchY = event.y
                        stateHolder.requestHitTest = true
                    }
                    true
                }
            }
            glView
        },
        modifier = modifier.fillMaxSize(),
        update = { /* no-op – state is driven via stateHolder */ }
    )
}

/** Mutable state shared between Compose and the GL renderer thread */
class ArViewState(
    val sessionManager: ArSessionManager,
    val tapeMeasure: TapeMeasure,
    var onPlaneCountChanged: (Int) -> Unit,
    var onDistanceChanged: (String) -> Unit,
    var onLockedChanged: (Boolean) -> Unit
) {
    @Volatile var requestMeasure = false
    @Volatile var requestLock = false
    @Volatile var requestHitTest = false
    @Volatile var lastTouchX = 0f
    @Volatile var lastTouchY = 0f
    @Volatile var viewWidth = 1
    @Volatile var viewHeight = 1
}

/**
 * Minimal OpenGL ES 2.0 renderer that:
 * 1. Draws the AR camera feed as background
 * 2. Draws detected planes as simple translucent grids
 * 3. Performs hit tests for the tape measure
 */
class ArRenderer(private val state: ArViewState) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArRenderer"
    }

    private var backgroundRenderer: BackgroundRenderer? = null
    private var planeRenderer: PlaneRenderer? = null

    private var viewportWidth = 1
    private var viewportHeight = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        backgroundRenderer = BackgroundRenderer()
        planeRenderer = PlaneRenderer()

        // Create session if not already done
        if (state.sessionManager.getSession() == null) {
            state.sessionManager.createSession()
            state.sessionManager.configureSession()
        }

        // Set the camera texture for ARCore
        state.sessionManager.getSession()?.let { session ->
            backgroundRenderer?.createOnGlThread()
            val textureId = backgroundRenderer?.textureId ?: return
            session.setCameraTextureName(textureId)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        state.viewWidth = width
        state.viewHeight = height
        GLES20.glViewport(0, 0, width, height)

        state.sessionManager.getSession()?.setDisplayGeometry(
            android.view.Surface.ROTATION_0, width, height
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = state.sessionManager.getSession() ?: return

        try {
            session.setCameraTextureName(backgroundRenderer?.textureId ?: return)

            val frame = session.update() ?: return
            val camera = frame.camera

            // Draw camera background
            backgroundRenderer?.draw(frame)

            // Only draw geometry when tracking
            if (camera.trackingState != TrackingState.TRACKING) return

            // Update plane count
            val planes = session.getAllTrackables(Plane::class.java)
                .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
            state.onPlaneCountChanged(planes.size)

            // Draw planes
            val projMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
            camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f)
            camera.getViewMatrix(viewMatrix, 0)

            planeRenderer?.drawPlanes(planes, viewMatrix, projMatrix)

            // Handle measure / hit-test requests
            handleMeasureRequests(frame, camera)

            // Keep distance UI updated
            state.onDistanceChanged(state.tapeMeasure.formattedDistance())
            state.onLockedChanged(state.tapeMeasure.isLocked)

        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in frame", e)
        }
    }

    private fun handleMeasureRequests(frame: Frame, camera: Camera) {
        // Center-screen measure (button press)
        if (state.requestMeasure) {
            state.requestMeasure = false
            performHitTest(frame, viewportWidth / 2f, viewportHeight / 2f)
        }

        // Touch-based measure
        if (state.requestHitTest) {
            state.requestHitTest = false
            performHitTest(frame, state.lastTouchX, state.lastTouchY)
        }

        // Lock request
        if (state.requestLock) {
            state.requestLock = false
            if (state.tapeMeasure.hasBothPoints()) {
                state.tapeMeasure.lock()
            } else if (state.tapeMeasure.isLocked) {
                state.tapeMeasure.unlock()
            }
        }
    }

    private fun performHitTest(frame: Frame, x: Float, y: Float) {
        val hits = frame.hitTest(x, y)
        val hit = hits.firstOrNull { result ->
            val trackable = result.trackable
            (trackable is Plane && trackable.isPoseInPolygon(result.hitPose)) ||
                    trackable is DepthPoint ||
                    trackable is InstantPlacementPoint
        } ?: return

        val pose = hit.hitPose

        if (!state.tapeMeasure.hasBothPoints() && state.tapeMeasure.currentDistanceMeters == null) {
            // First point
            state.tapeMeasure.setPointA(pose)
        } else if (!state.tapeMeasure.isLocked) {
            // Second point (or replace B)
            state.tapeMeasure.setPointB(pose)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Minimal camera background renderer (ARCore standard pattern)
// ─────────────────────────────────────────────────────────────

class BackgroundRenderer {
    var textureId = -1
        private set

    private var quadVertices: FloatBuffer? = null
    private var quadTexCoord: FloatBuffer? = null
    private var program = 0
    private var positionAttrib = 0
    private var texCoordAttrib = 0

    fun createOnGlThread() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val vs = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()

        val fs = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """.trimIndent()

        program = loadProgram(vs, fs)
        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        texCoordAttrib = GLES20.glGetAttribLocation(program, "a_TexCoord")

        val vertices = floatArrayOf(
            -1f, -1f, 0f,
             1f, -1f, 0f,
            -1f,  1f, 0f,
             1f,  1f, 0f
        )
        quadVertices = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices)
        quadVertices?.position(0)

        val texCoords = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
        quadTexCoord = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(texCoords)
        quadTexCoord?.position(0)
    }

    fun draw(frame: Frame) {
        if (textureId == -1 || program == 0) return

        // Update texture coordinates if display geometry changed
        val transformedCoords = FloatArray(8)
        frame.transformDisplayUvCoords(
            floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f),
            transformedCoords
        )
        quadTexCoord?.put(transformedCoords)?.position(0)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, quadVertices)

        GLES20.glEnableVertexAttribArray(texCoordAttrib)
        GLES20.glVertexAttribPointer(texCoordAttrib, 2, GLES20.GL_FLOAT, false, 0, quadTexCoord)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDisableVertexAttribArray(texCoordAttrib)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}

// ─────────────────────────────────────────────────────────────
// Simple plane grid renderer
// ─────────────────────────────────────────────────────────────

class PlaneRenderer {
    private var program = 0
    private var positionAttrib = 0
    private var mvpUniform = 0
    private var colorUniform = 0

    init {
        val vs = """
            uniform mat4 u_Mvp;
            attribute vec3 a_Position;
            void main() {
                gl_Position = u_Mvp * vec4(a_Position, 1.0);
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """.trimIndent()

        program = loadProgram(vs, fs)
        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        mvpUniform = GLES20.glGetUniformLocation(program, "u_Mvp")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
    }

    fun drawPlanes(planes: Collection<Plane>, viewMatrix: FloatArray, projMatrix: FloatArray) {
        if (program == 0) return

        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionAttrib)

        val mvp = FloatArray(16)
        val model = FloatArray(16)

        for (plane in planes) {
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) continue

            plane.centerPose.toMatrix(model, 0)
            multiplyMatrix(mvp, projMatrix, viewMatrix, model)

            GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0)

            // Color based on plane type
            val color = when (plane.type) {
                Plane.Type.HORIZONTAL_UPWARD_FACING -> floatArrayOf(0.3f, 0.7f, 1.0f, 0.35f)
                Plane.Type.VERTICAL -> floatArrayOf(1.0f, 0.6f, 0.2f, 0.35f)
                else -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.3f)
            }
            GLES20.glUniform4fv(colorUniform, 1, color, 0)

            // Draw a simple 1m x 1m grid centered on the plane
            drawGrid()
        }

        GLES20.glDisableVertexAttribArray(positionAttrib)
    }

    private fun drawGrid() {
        val size = 0.5f // half-extent → 1m x 1m
        val step = 0.1f
        val lines = mutableListOf<Float>()

        var x = -size
        while (x <= size + 0.001f) {
            lines.addAll(listOf(x, 0f, -size, x, 0f, size))
            x += step
        }
        var z = -size
        while (z <= size + 0.001f) {
            lines.addAll(listOf(-size, 0f, z, size, 0f, z))
            z += step
        }

        val buffer = ByteBuffer.allocateDirect(lines.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(lines.toFloatArray())
        buffer.position(0)

        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lines.size / 3)
    }

    private fun multiplyMatrix(result: FloatArray, proj: FloatArray, view: FloatArray, model: FloatArray) {
        val temp = FloatArray(16)
        android.opengl.Matrix.multiplyMM(temp, 0, view, 0, model, 0)
        android.opengl.Matrix.multiplyMM(result, 0, proj, 0, temp, 0)
    }
}

// ─────────────────────────────────────────────────────────────
// Shader helpers
// ─────────────────────────────────────────────────────────────

private fun loadProgram(vertexShader: String, fragmentShader: String): Int {
    val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
    val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
    if (vs == 0 || fs == 0) return 0

    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vs)
    GLES20.glAttachShader(program, fs)
    GLES20.glLinkProgram(program)

    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] == 0) {
        Log.e("Shader", "Program link failed: ${GLES20.glGetProgramInfoLog(program)}")
        GLES20.glDeleteProgram(program)
        return 0
    }
    return program
}

private fun loadShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)

    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        Log.e("Shader", "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
        GLES20.glDeleteShader(shader)
        return 0
    }
    return shader
}
