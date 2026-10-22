package com.meomeo.tvlauncher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val id: String, // typically packageName or packageName/activityName
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable?,
    val banner: Drawable? = null,
    val isTvApp: Boolean = true,
    val isFavorite: Boolean = false
)

