package com.ksc.onote.utils

import android.content.Context
import android.util.DisplayMetrics

object DisplayTool {
    fun dp2px(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}