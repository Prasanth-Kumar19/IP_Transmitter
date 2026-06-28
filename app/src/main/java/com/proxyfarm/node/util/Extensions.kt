package com.proxyfarm.node.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Project-wide Kotlin extension functions.
 * Keeps call-sites clean and avoids boilerplate repetition.
 */

// ─────────────────────────────────────────────────────────────────
// Context Extensions
// ─────────────────────────────────────────────────────────────────

/** Shows a short Toast from a string resource. */
fun Context.toast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

/** Shows a short Toast from a plain string. */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Starts a foreground service safely, using [startForegroundService]
 * on API 26+ and falling back to [startService] on older versions.
 */
fun Context.startForegroundServiceCompat(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

// ─────────────────────────────────────────────────────────────────
// String Extensions
// ─────────────────────────────────────────────────────────────────

/**
 * Returns the string truncated to [maxLength] characters,
 * appending "…" if truncation occurred.
 */
fun String.truncate(maxLength: Int): String =
    if (length <= maxLength) this else "${take(maxLength)}…"

/**
 * Returns null if the string is blank, otherwise returns the string.
 * Useful for treating empty API fields as absent values.
 */
fun String.nullIfBlank(): String? = ifBlank { null }

// ─────────────────────────────────────────────────────────────────
// Numeric Extensions
// ─────────────────────────────────────────────────────────────────

/**
 * Formats a byte count into a human-readable size string.
 * e.g. 1_500_000L → "1.4 MB"
 */
fun Long.toReadableBytes(): String = when {
    this < 1_024L         -> "$this B"
    this < 1_048_576L     -> "${"%.1f".format(this / 1_024.0)} KB"
    this < 1_073_741_824L -> "${"%.1f".format(this / 1_048_576.0)} MB"
    else                  -> "${"%.2f".format(this / 1_073_741_824.0)} GB"
}

/**
 * Formats a bandwidth value in Kbps to a human-readable string.
 * e.g. 1500 → "1.5 Mbps", 800 → "800 Kbps"
 */
fun Int.toReadableBandwidth(): String = when {
    this <= 0     -> "—"
    this >= 1_000 -> "${"%.1f".format(this / 1_000.0)} Mbps"
    else          -> "$this Kbps"
}

// ─────────────────────────────────────────────────────────────────
// Boolean Extensions
// ─────────────────────────────────────────────────────────────────

/**
 * Returns [trueValue] if this is true, [falseValue] otherwise.
 * Useful for concise conditional string selection in Compose.
 */
fun <T> Boolean.select(trueValue: T, falseValue: T): T =
    if (this) trueValue else falseValue