package com.example.util

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import com.example.BuildConfig

/**
 * Performance Profiler for Debug builds.
 * Tracks frame render times in milliseconds and logs any dropped frames exceeding 16.6ms (60 FPS target)
 * to identify specific scroll jank bottlenecks and expensive render passes.
 */
object FrameRenderProfiler {

    private const val TAG = "FrameRenderProfiler"
    private const val JANK_THRESHOLD_MS = 16.6f

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var frameMetricsListener: Window.OnFrameMetricsAvailableListener? = null
    private var isStarted = false

    private var totalFrames = 0L
    private var jankyFrames = 0L

    fun start(activity: Activity) {
        if (!BuildConfig.DEBUG) return
        if (isStarted) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val thread = HandlerThread("FrameMetricsProfilerThread").apply { start() }
            handlerThread = thread
            val h = Handler(thread.looper)
            handler = h

            val listener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, dropCountSinceLastInvocation ->
                val durationNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val durationMs = durationNs / 1_000_000f
                totalFrames++

                if (durationMs > JANK_THRESHOLD_MS) {
                    jankyFrames++
                    val layoutNs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) / 1_000_000f
                    val drawNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) / 1_000_000f
                    val animNs = frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION) / 1_000_000f
                    val syncNs = frameMetrics.getMetric(FrameMetrics.SYNC_DURATION) / 1_000_000f

                    val jankRatio = if (totalFrames > 0) (jankyFrames.toFloat() / totalFrames) * 100f else 0f

                    Log.w(
                        TAG,
                        "⚠️ DROPPED FRAME (>16.6ms): Render time = %.2f ms (+%.2f ms over 16.6ms budget) | Layout/Measure: %.2f ms | Draw: %.2f ms | Anim: %.2f ms | Sync: %.2f ms | Dropped frames: %d | Jank rate: %.1f%% (%d/%d frames)"
                            .format(
                                durationMs,
                                durationMs - JANK_THRESHOLD_MS,
                                layoutNs,
                                drawNs,
                                animNs,
                                syncNs,
                                dropCountSinceLastInvocation,
                                jankRatio,
                                jankyFrames,
                                totalFrames
                            )
                    )
                } else if (totalFrames % 600 == 0L) {
                    // Periodic health summary every ~10s of 60fps rendering
                    val jankRatio = (jankyFrames.toFloat() / totalFrames) * 100f
                    Log.d(
                        TAG,
                        "✅ Frame Render Health: %d total frames | %d janky frames (%.2f%% jank rate)"
                            .format(totalFrames, jankyFrames, jankRatio)
                    )
                }
            }

            frameMetricsListener = listener
            try {
                activity.window.addOnFrameMetricsAvailableListener(listener, h)
                isStarted = true
                Log.i(TAG, "FrameRenderProfiler active in Debug build. Monitoring frame render times (>16.6ms threshold)...")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach FrameMetricsListener: ${e.message}")
            }
        }
    }

    fun stop(activity: Activity) {
        if (!BuildConfig.DEBUG) return
        if (!isStarted) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameMetricsListener?.let {
                try {
                    activity.window.removeOnFrameMetricsAvailableListener(it)
                } catch (_: Exception) {}
            }
            frameMetricsListener = null
            handlerThread?.quitSafely()
            handlerThread = null
            handler = null
            isStarted = false
            Log.i(TAG, "FrameRenderProfiler stopped. Final stats: $totalFrames frames tracked, $jankyFrames janky frames.")
        }
    }
}
