package com.example.service.charging

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import com.example.util.BatteryChargingState
import kotlinx.coroutines.*
import kotlin.math.sin

/**
 * Dedicated Canvas renderer for the luxury Charging Animation directly on the Wallpaper SurfaceHolder.
 *
 * Features:
 * - Deep Obsidian canvas (#09090C)
 * - Restrained Champagne Gold accents (#E5C07B, #D4AF37)
 * - Ambient breathing pulse & smooth 12-second halo rotational sweep
 * - Real hardware battery percentage & power source badge
 * - Smooth fade-in & fade-out crossfade transitions without black frame flashes
 * - High efficiency: only renders when charging is active and surface is visible
 */
class WallpaperChargingRenderer(private val context: Context) {

    private enum class FadeState {
        FADING_IN,
        ACTIVE,
        FADING_OUT,
        IDLE
    }

    private var renderJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var isRunning = false

    @Volatile
    private var fadeState = FadeState.IDLE

    @Volatile
    private var transitionAlpha = 0f

    @Volatile
    private var onFadeOutComplete: (() -> Unit)? = null

    @Volatile
    private var currentBatteryState: BatteryChargingState = BatteryChargingState(
        isCharging = true,
        batteryPercent = -1,
        chargingSource = "Standard"
    )

    // Paints
    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val batteryArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val iconBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val percentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val chargingLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        letterSpacing = 0.25f
    }

    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        letterSpacing = 0.15f
    }

    fun updateBatteryState(state: BatteryChargingState) {
        currentBatteryState = state
    }

    fun start(surfaceHolder: SurfaceHolder) {
        onFadeOutComplete = null
        fadeState = FadeState.FADING_IN

        if (isRunning) return
        isRunning = true

        renderJob = scope.launch {
            val startTime = System.currentTimeMillis()
            var fadeStartTime = System.currentTimeMillis()
            val initialFadeAlpha = transitionAlpha

            val fadeInDuration = 400f // 400ms smooth fade-in
            val fadeOutDuration = 350f // 350ms smooth fade-out

            while (isActive && isRunning) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime

                // Compute transition alpha
                when (fadeState) {
                    FadeState.FADING_IN -> {
                        val fadeElapsed = (now - fadeStartTime).toFloat()
                        val progress = (fadeElapsed / fadeInDuration).coerceIn(0f, 1f)
                        // Ease out cubic
                        val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
                        transitionAlpha = initialFadeAlpha + (1f - initialFadeAlpha) * eased
                        if (progress >= 1f) {
                            transitionAlpha = 1f
                            fadeState = FadeState.ACTIVE
                        }
                    }
                    FadeState.ACTIVE -> {
                        transitionAlpha = 1f
                    }
                    FadeState.FADING_OUT -> {
                        val fadeElapsed = (now - fadeStartTime).toFloat()
                        val progress = (fadeElapsed / fadeOutDuration).coerceIn(0f, 1f)
                        val eased = progress * progress // Ease in
                        transitionAlpha = (initialFadeAlpha * (1f - eased)).coerceIn(0f, 1f)
                        if (progress >= 1f || transitionAlpha <= 0.01f) {
                            transitionAlpha = 0f
                            fadeState = FadeState.IDLE
                            isRunning = false
                            val callback = onFadeOutComplete
                            onFadeOutComplete = null
                            callback?.invoke()
                            break
                        }
                    }
                    FadeState.IDLE -> {
                        transitionAlpha = 0f
                        isRunning = false
                        break
                    }
                }

                // Compute motion variables
                val rotationAngle = (elapsed % 12000L) / 12000f * 360f

                // Pulse alpha between 0.35 and 0.75 over 2.8s
                val pulsePhase = (elapsed % 2800L) / 2800f * Math.PI * 2
                val pulseAlpha = 0.35f + 0.40f * ((sin(pulsePhase).toFloat() + 1f) / 2f)

                // Scale pulse between 0.98 and 1.02 over 3.2s
                val scalePhase = (elapsed % 3200L) / 3200f * Math.PI * 2
                val ringScale = 0.98f + 0.04f * ((sin(scalePhase).toFloat() + 1f) / 2f)

                if (transitionAlpha > 0f) {
                    drawFrame(surfaceHolder, rotationAngle, pulseAlpha, ringScale, transitionAlpha)
                }

                // Frame rate regulation (~45-60 FPS, battery-friendly 18ms delay)
                val frameDuration = System.currentTimeMillis() - now
                val sleepTime = (18L - frameDuration).coerceAtLeast(6L)
                delay(sleepTime)
            }
        }
    }

    fun stopWithFade(onComplete: (() -> Unit)? = null) {
        if (!isRunning || transitionAlpha <= 0f) {
            isRunning = false
            fadeState = FadeState.IDLE
            transitionAlpha = 0f
            onComplete?.invoke()
            return
        }
        onFadeOutComplete = onComplete
        fadeState = FadeState.FADING_OUT
    }

    fun stopImmediate() {
        isRunning = false
        fadeState = FadeState.IDLE
        transitionAlpha = 0f
        onFadeOutComplete = null
        renderJob?.cancel()
        renderJob = null
    }

    private fun drawFrame(
        surfaceHolder: SurfaceHolder,
        rotationAngle: Float,
        pulseAlpha: Float,
        ringScale: Float,
        alpha: Float
    ) {
        var canvas: Canvas? = null
        try {
            canvas = surfaceHolder.lockCanvas() ?: return
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            if (width <= 0 || height <= 0) return

            val density = context.resources.displayMetrics.density
            val centerX = width / 2f
            val centerY = height / 2f

            // 1. Draw Obsidian Dark Background with transition alpha
            backgroundPaint.color = Color.argb((255 * alpha).toInt(), 9, 9, 12)
            canvas.drawRect(0f, 0f, width, height, backgroundPaint)

            // 2. Base Ring Dimensions
            val baseRadius = 110f * density * ringScale
            val strokeWidth = 2.5f * density

            trackPaint.strokeWidth = strokeWidth
            trackPaint.color = Color.argb((255 * 0.06f * alpha).toInt(), 255, 255, 255)

            batteryArcPaint.strokeWidth = strokeWidth
            batteryArcPaint.color = Color.argb((255 * alpha).toInt(), 0xE5, 0xC0, 0x7B)

            val ringRect = RectF(
                centerX - baseRadius,
                centerY - baseRadius,
                centerX + baseRadius,
                centerY + baseRadius
            )

            // Outer Track Circle
            canvas.drawCircle(centerX, centerY, baseRadius, trackPaint)

            // 3. Ambient Soft Halo Arc (Breathing gradient)
            val haloStroke = strokeWidth * 2.5f
            haloPaint.strokeWidth = haloStroke
            val haloAlpha = (255 * (pulseAlpha * 0.45f) * alpha).toInt().coerceIn(0, 255)
            haloPaint.color = Color.argb(haloAlpha, 0xE5, 0xC0, 0x7B)

            val haloRect = RectF(
                centerX - baseRadius,
                centerY - baseRadius,
                centerX + baseRadius,
                centerY + baseRadius
            )
            canvas.drawArc(haloRect, rotationAngle, 260f, false, haloPaint)

            // 4. Precision Battery Progress Arc
            val batteryPercent = currentBatteryState.batteryPercent
            if (batteryPercent in 0..100) {
                val progressSweep = (batteryPercent / 100f) * 360f
                canvas.drawArc(ringRect, -90f, progressSweep, false, batteryArcPaint)
            }

            // 5. Central Bolt Badge
            val badgeRadius = 18f * density
            val badgeCenterY = centerY - 38f * density
            iconBadgePaint.color = Color.argb((255 * 0.12f * alpha).toInt(), 0xE5, 0xC0, 0x7B)
            canvas.drawCircle(centerX, badgeCenterY, badgeRadius, iconBadgePaint)

            // Draw Bolt Icon inside badge
            boltPaint.color = Color.argb((255 * alpha).toInt(), 0xE5, 0xC0, 0x7B)
            drawBolt(canvas, centerX, badgeCenterY, 14f * density, boltPaint)

            // 6. Percentage Text
            val percentString = if (batteryPercent in 0..100) "$batteryPercent%" else "--"
            percentTextPaint.color = Color.argb((255 * alpha).toInt(), 255, 255, 255)
            percentTextPaint.textSize = 42f * density
            val textY = centerY + 14f * density
            canvas.drawText(percentString, centerX, textY, percentTextPaint)

            // 7. "CHARGING" Label
            chargingLabelPaint.color = Color.argb((255 * 0.9f * alpha).toInt(), 0xE5, 0xC0, 0x7B)
            chargingLabelPaint.textSize = 10f * density
            canvas.drawText("CHARGING", centerX, textY + 22f * density, chargingLabelPaint)

            // 8. Source Pill ("FAST AC POWER", "USB CABLE POWER", "WIRELESS POWER")
            val sourceText = "${currentBatteryState.chargingSource.uppercase()} POWER"
            pillTextPaint.color = Color.argb((255 * 0.60f * alpha).toInt(), 255, 255, 255)
            pillTextPaint.textSize = 11f * density

            val pillPaddingH = 16f * density
            val pillPaddingV = 8f * density
            val textBounds = Rect()
            pillTextPaint.getTextBounds(sourceText, 0, sourceText.length, textBounds)

            val pillWidth = textBounds.width() + pillPaddingH * 2
            val pillHeight = textBounds.height() + pillPaddingV * 2
            val pillTop = centerY + baseRadius + 32f * density
            val pillRect = RectF(
                centerX - pillWidth / 2f,
                pillTop,
                centerX + pillWidth / 2f,
                pillTop + pillHeight
            )
            val cornerRadius = 18f * density
            pillBgPaint.color = Color.argb((255 * 0.05f * alpha).toInt(), 255, 255, 255)
            canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillBgPaint)

            val pillTextY = pillTop + pillHeight / 2f + textBounds.height() / 2f - 2f
            canvas.drawText(sourceText, centerX, pillTextY, pillTextPaint)

        } catch (_: Exception) {
            // Guard against surface destruction during draw
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {}
            }
        }
    }

    private fun drawBolt(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            val halfW = size * 0.35f
            val halfH = size * 0.5f

            moveTo(cx + halfW * 0.2f, cy - halfH)
            lineTo(cx - halfW, cy + halfH * 0.05f)
            lineTo(cx, cy + halfH * 0.05f)
            lineTo(cx - halfW * 0.2f, cy + halfH)
            lineTo(cx + halfW, cy - halfH * 0.05f)
            lineTo(cx, cy - halfH * 0.05f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    fun release() {
        stopImmediate()
        scope.cancel()
    }
}

