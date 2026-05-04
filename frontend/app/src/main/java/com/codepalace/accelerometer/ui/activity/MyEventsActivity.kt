package com.codepalace.accelerometer.ui.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.util.toEventResponse
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.local.EventEntity
import com.codepalace.accelerometer.data.model.dto.EventResponse
import com.codepalace.accelerometer.data.repository.EventRepository
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.util.DisplayDateFormatter
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
@Suppress("DEPRECATION")
class MyEventsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private val repository by lazy {
        EventRepository(
            api = ApiClient.eventApi,
            database = AppDatabase.getDatabase(this)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_my_events)
        applyTopBarInsets(findViewById(R.id.headerBar))

        container = findViewById(R.id.eventsContainer)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadEvents()
    }

    private fun loadEvents() {
        lifecycleScope.launch {
            showLoading()

            // 1. Cache first — instant, never throws
            val cached = repository.getCachedMyEvents()
            if (cached.isNotEmpty()) showEvents(cached)

            // 2. Network refresh
            try {
                val fresh = repository.refreshMyEvents()
                if (fresh.isEmpty()) showEmpty() else showEvents(fresh)
            } catch (e: Exception) {
                if (cached.isEmpty()) showError("Could not load your events.")
                // else: silently keep showing cache
            }
        }
    }

    private fun showEvents(events: List<EventResponse>) {
        container.removeAllViews()
        events.forEach { event ->
            container.addView(createEventCard(event))
        }
    }

    private fun showLoading() {
        showStatus("Loading your events...")
    }

    private fun showEmpty() {
        showStatus("You are not enrolled in any upcoming events yet.")
    }

    private fun showError(message: String) {
        showStatus(message)
        showAppMessage(message, MessageKind.ERROR)
    }

    private fun showStatus(message: String) {
        container.removeAllViews()
        container.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                if (message.startsWith("Loading", ignoreCase = true)) {
                    addView(
                        ImageView(this@MyEventsActivity).apply {
                            setImageResource(R.mipmap.ic_launcher)
                            contentDescription = getString(R.string.app_name)
                            val size = 76.dp()
                            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                                bottomMargin = 18.dp()
                            }
                        }
                    )
                }

                addView(
                    TextView(this@MyEventsActivity).apply {
                        text = message
                        setTextColor(getColor(R.color.color_accent))
                        textSize = 18f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                )
            }
        )
    }

    private fun createEventCard(event: EventResponse): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_menu)
            setPadding(28, 22, 28, 22)

            val selectable = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, selectable, true)
            foreground = ContextCompat.getDrawable(this@MyEventsActivity, selectable.resourceId)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val titleText = TextView(this).apply {
            text = event.title
            setTextColor(getColor(R.color.color_primary))
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val typeText = TextView(this).apply {
            text = DisplayDateFormatter.formatEnumLabel(event.eventType)
            setTextColor(getColor(R.color.color_primary))
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.85f
        }

        val startText = TextView(this).apply {
            val start = DisplayDateFormatter.formatEventStart(event.startAt.toString())
            text = start?.let { "Starts $it" } ?: "Start time is not available"
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.75f
        }

        val creatorText = TextView(this).apply {
            text = "Created by user #${event.creatorId}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.7f
        }

        card.addView(titleText)
        card.addView(typeText)
        card.addView(startText)
        card.addView(creatorText)

        return card
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
