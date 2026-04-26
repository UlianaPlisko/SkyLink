package com.codepalace.accelerometer.ui

import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codepalace.accelerometer.R
import com.google.android.material.snackbar.Snackbar

fun AppCompatActivity.showAppMessage(
    message: String,
    kind: MessageKind = MessageKind.INFO
) {
    val root = findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)

    val background = when (kind) {
        MessageKind.SUCCESS -> R.color.accent_gold
        MessageKind.ERROR -> R.color.error_red
        MessageKind.INFO -> R.color.blue
    }

    val text = when (kind) {
        MessageKind.SUCCESS -> R.color.dark_blue
        MessageKind.ERROR,
        MessageKind.INFO -> R.color.text_primary_light
    }

    snackbar.view.backgroundTintList = ColorStateList.valueOf(
        ContextCompat.getColor(this, background)
    )
    snackbar.setTextColor(ContextCompat.getColor(this, text))
    snackbar.show()
}
