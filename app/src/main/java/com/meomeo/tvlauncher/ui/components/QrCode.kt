package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** One boolean per QR module (true = dark), row by row. Empty if [text] cannot be encoded. */
private fun encodeQr(text: String): Array<BooleanArray> = try {
    val hints = mapOf<EncodeHintType, Any>(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    Array(matrix.height) { y -> BooleanArray(matrix.width) { x -> matrix[x, y] } }
} catch (_: Exception) {
    emptyArray()
}

/**
 * A QR code for [text], black on a white rounded plate with the quiet zone scanners need. It is
 * drawn from rectangles, so it stays razor sharp at any size and needs no bitmap.
 */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val modules = remember(text) { encodeQr(text) }
    Canvas(modifier.aspectRatio(1f)) {
        drawRoundRect(Color.White, cornerRadius = CornerRadius(size.width * 0.05f))
        val count = modules.size
        if (count == 0) return@Canvas
        // Whole pixels per module avoid hairline seams between neighbours.
        val quiet = 3
        val cell = (size.width / (count + quiet * 2)).toInt().coerceAtLeast(1).toFloat()
        val origin = (size.width - cell * count) / 2f
        for (y in 0 until count) {
            for (x in 0 until count) {
                if (modules[y][x]) {
                    drawRect(Color.Black, Offset(origin + x * cell, origin + y * cell), Size(cell, cell))
                }
            }
        }
    }
}
