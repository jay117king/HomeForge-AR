package com.homeforge.ar.ar

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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Full ARCore camera view with:
 * - Camera background rendering
 * - Plane detection visualization (colored grids)
 * - Hit testing for tape measure (tap or center button)
 * - Lifecycle-aware session management
 */
@Composable
fun ArCameraView(
    modifier: Modifier = Modifier,
    onPlaneCountChanged: (Int) -> Unit = {},
    onDistanceChanged: (String) -> Unit = {},
    onLockedChanged: (Boolean) -> Unit = {},
    measureTrigger: Int = 0,
    lockTrigger: Int = 0,
    resetTrigger: Int = 0
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val sessionManager = remember { ArSessionManager(context) }
    val tapeMeasure = remember { TapeMeasure() }

    val stateHolder = remember {
        ArViewState(
            sessionManager = sessionManager,
            tapeMeasure = tapeMeasure,
            onPlaneCountChanged = onPlaneCountChanged,
            onDistanceChanged = onDistanceChanged,
            onLockedChanged = onLockedChanged
        )
    }

    // Keep callbacks current
    stateHolder.onPlaneCountChanged = onPlaneCountChanged
    stateHolder.onDistanceChanged = onDistanceChanged
    stateHolder.onLockedChanged = onLockedChanged

    // One-shot triggers via Int counters
    LaunchedEffect(measureTrigger) {
        if (measureTrigger > 0) stateHolder.requestMeasure = true
    }
    LaunchedEffect(lockTrigger) {
        if (lockTrigger > 0) stateHolder.requestLock = true
    }
    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            tapeMeasure.reset()
            onDistanceChanged("—")
            onLockedChanged(false)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (sessionManager.getSession() == null) {
                    if (sessionManager.createSession()) {
                        sessionManager.configureSession()
                    }
                }
                sessionManager.resume()
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
            GLSurfaceView(ctx).apply {
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
        },
        modifier = modifier.fillMaxSize()
    )
}

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

class ArRenderer(private val state: ArViewState) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArRenderer"
    }

    private var backgroundRenderer: BackgroundRenderer? = null
    private var planeRenderer: PlaneRenderer? = null
    private var viewportWidth = 1
    private var viewportHeight = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        backgroundRenderer = BackgroundRenderer().also { it.createOnGlThread() }
        planeRenderer = PlaneRenderer()

        state.sessionManager.getSession()?.let { session ->
            session.setCameraTextureName(backgroundRenderer!!.textureId)
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
            val textureId = backgroundRenderer?.textureId ?: return
            session.setCameraTextureName(textureId)

            val frame = session.update() ?: return
            val camera = frame.camera

            backgroundRenderer?.draw(frame)

            if (camera.trackingState != TrackingState.TRACKING) return

            val planes = session.getAllTrackables(Plane::class.java)
                .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
            state.onPlaneCountChanged(planes.size)

            val projMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
            camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
            camera.getViewMatrix(viewMatrix, 0)

            planeRenderer?.drawPlanes(planes, viewMatrix, projMatrix)

            handleMeasureRequests(frame)

            state.onDistanceChanged(state.tapeMeasure.formattedDistance())
            state.onLockedChanged(state.tapeMeasure.isLocked)

        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available", e)
        } catch (e: Exception) {
            Log.e(TAG, "Frame error", e)
        }
    }

    private fun handleMeasureRequests(frame: Frame) {
        if (state.requestMeasure) {
            state.requestMeasure = false
            performHitTest(frame, viewportWidth / 2f, viewportHeight / 2f)
        }
        if (state.requestHitTest) {
            state.requestHitTest = false
            performHitTest(frame, state.lastTouchX, state.lastTouchY)
        }
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
            val t = result.trackable
            (t is Plane && t.isPoseInPolygon(result.hitPose)) ||
                    t is DepthPoint ||
                    t is InstantPlacementPoint
        } ?: return

        val pose = hit.hitPose
        if (state.tapeMeasure.currentDistanceMeters == null && !state.tapeMeasure.hasBothPoints()) {
            state.tapeMeasure.setPointA(pose)
        } else if (!state.tapeMeasure.isLocked) {
            state.tapeMeasure.setPointB(pose)
        }
    }
}

// ── Camera background ────────────────────────────────────────

class BackgroundRenderer {
    var textureId = -1
        private set

    private var quadVertices: java.nio.FloatBuffer? = null
    private var quadTexCoord: java.nio.FloatBuffer? = null
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

        val vertices = floatArrayOf(-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f)
        quadVertices = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).also { it.position(0) }

        val texCoords = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
        quadTexCoord = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(texCoords).also { it.position(0) }
    }

    fun draw(frame: Frame) {
        if (textureId == -1 || program == 0) return

        val transformed = FloatArray(8)
        frame.transformDisplayUvCoords(floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f), transformed)
        quadTexCoord?.put(transformed)?.position(0)

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

// ── Plane grid ───────────────────────────────────────────────

class PlaneRenderer {
    private var program = 0
    private var positionAttrib = 0
    private var mvpUniform = 0
    private var colorUniform = 0

    init {
        val vs = """
            uniform mat4 u_Mvp;
            attribute vec3 a_Position;
            void main() { gl_Position = u_Mvp * vec4(a_Position, 1.0); }
        """.trimIndent()
        val fs = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() { gl_FragColor = u_Color; }
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
            val temp = FloatArray(16)
            android.opengl.Matrix.multiplyMM(temp, 0, viewMatrix, 0, model, 0)
            android.opengl.Matrix.multiplyMM(mvp, 0, projMatrix, 0, temp, 0)

            GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0)

            val color = when (plane.type) {
                Plane.Type.HORIZONTAL_UPWARD_FACING -> floatArrayOf(0.3f, 0.7f, 1.0f, 0.4f)
                Plane.Type.VERTICAL -> floatArrayOf(1.0f, 0.55f, 0.2f, 0.4f)
                else -> floatArrayOf(0.6f, 0.6f, 0.6f, 0.35f)
            }
            GLES20.glUniform4fv(colorUniform, 1, color, 0)
            drawGrid()
        }
        GLES20.glDisableVertexAttribArray(positionAttrib)
    }

    private fun drawGrid() {
        val size = 0.6f
        val step = 0.1f
        val lines = mutableListOf<Float>()
        var v = -size
        while (v <= size + 0.001f) {
            lines.addAll(listOf(v, 0f, -size, v, 0f, size))
            lines.addAll(listOf(-size, 0f, v, size, 0f, v))
            v += step
        }
        val buf = ByteBuffer.allocateDirect(lines.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(lines.toFloatArray())
        buf.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lines.size / 3)
    }
}

// ── Shader utils ─────────────────────────────────────────────

private fun loadProgram(vsSource: String, fsSource: String): Int {
    val vs = loadShader(GLES20.GL_VERTEX_SHADER, vsSource)
    val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fsSource)
    if (vs == 0 || fs == 0) return 0
    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vs)
    GLES20.glAttachShader(program, fs)
    GLES20.glLinkProgram(program)
    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    if (status[0] == 0) {
        Log.e("Shader", GLES20.glGetProgramInfoLog(program))
        GLES20.glDeleteProgram(program)
        return 0
    }
    return program
}

private fun loadShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
        Log.e("Shader", GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        return 0
    }
    return shader
}
