package com.codepalace.accelerometer.ui.activity

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

fun AppCompatActivity.applyTopBarInsets(headerBar: View, extraTopDp: Int = 10) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    ViewCompat.setOnApplyWindowInsetsListener(headerBar) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = systemBars.top + extraTopDp.dp(view)
        view.layoutParams = params

        insets
    }

    ViewCompat.requestApplyInsets(headerBar)
}

private fun Int.dp(view: View): Int {
    return (this * view.resources.displayMetrics.density).toInt()
}