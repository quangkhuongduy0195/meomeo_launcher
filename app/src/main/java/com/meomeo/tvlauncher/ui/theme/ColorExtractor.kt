package com.meomeo.tvlauncher.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object ColorExtractor {
    private val colorCache = mutableMapOf<String, Color>()

    fun getDominantColor(packageName: String, bitmap: Bitmap?): Color {
        if (colorCache.containsKey(packageName)) {
            return colorCache[packageName]!!
        }

        if (bitmap == null) {
            val fallback = Color(0xFF6366F1)
            colorCache[packageName] = fallback
            return fallback
        }

        try {
            // Sample a 16x16 grid of pixels for instant performance
            var redSum = 0L
            var greenSum = 0L
            var blueSum = 0L
            var sampleCount = 0

            val stepX = (bitmap.width / 12).coerceAtLeast(1)
            val stepY = (bitmap.height / 12).coerceAtLeast(1)

            var maxSaturation = 0f
            var mostVibrantColor = Color(0xFF6366F1)

            for (x in 0 until bitmap.width step stepX) {
                for (y in 0 until bitmap.height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xff
                    if (alpha < 128) continue // Skip transparent pixels

                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff

                    // Filter out near-white and near-black pixels for vibrant aura
                    val brightness = (r * 299 + g * 587 + b * 114) / 1000
                    if (brightness in 35..235) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.RGBToHSV(r, g, b, hsv)
                        val saturation = hsv[1]
                        if (saturation > maxSaturation) {
                            maxSaturation = saturation
                            mostVibrantColor = Color(r, g, b)
                        }
                        redSum += r
                        greenSum += g
                        blueSum += b
                        sampleCount++
                    }
                }
            }

            val result = if (maxSaturation > 0.25f) {
                mostVibrantColor
            } else if (sampleCount > 0) {
                Color(
                    (redSum / sampleCount).toInt(),
                    (greenSum / sampleCount).toInt(),
                    (blueSum / sampleCount).toInt()
                )
            } else {
                Color(0xFF6366F1)
            }

            colorCache[packageName] = result
            return result
        } catch (e: Exception) {
            val fallback = Color(0xFF6366F1)
            colorCache[packageName] = fallback
            return fallback
        }
    }

    /** Mean colour of an image; unlike [getDominantColor] this is not biased to the most vibrant pixel. */
    fun averageColor(bitmap: Bitmap): Color {
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val px = bitmap.getPixel(x, y)
                r += (px shr 16) and 0xff; g += (px shr 8) and 0xff; b += px and 0xff; n++
            }
        }
        return if (n == 0) Color.White else Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }
}
