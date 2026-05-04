package com.codepalace.accelerometer.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codepalace.accelerometer.R
import com.google.android.material.snackbar.Snackbar

fun AppCompatActivity.showAppMessage(
    message: String,
    kind: MessageKind = MessageKind.INFO,
    actionLabel: String? = null,
    action: (() -> Unit)? = null
) {
    val root = findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)

    val background = when (kind) {
        MessageKind.SUCCESS -> R.color.color_accent
        MessageKind.ERROR -> R.color.color_error
        MessageKind.INFO -> R.color.color_secondary
    }

    val text = when (kind) {
        MessageKind.SUCCESS -> R.color.color_primary
        MessageKind.ERROR,
        MessageKind.INFO -> R.color.color_text_on_background
    }

    snackbar.view.backgroundTintList = ColorStateList.valueOf(
        ContextCompat.getColor(this, background)
    )
    snackbar.setTextColor(ContextCompat.getColor(this, text))
    snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        ?.maxLines = 4

    if (actionLabel != null && action != null) {
        snackbar.setAction(actionLabel) { action() }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.color_accent))
    }

    snackbar.show()
}
