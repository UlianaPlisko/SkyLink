package com.codepalace.accelerometer.ui.activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.ui.viewmodel.CalendarViewModel
import com.codepalace.accelerometer.util.ScheduledEventsAdapter
import com.codepalace.accelerometer.util.WeekDaysAdapter
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class CalendarActivity : AppCompatActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvDateLabel: TextView
    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton
    private lateinit var rvWeekDays: RecyclerView
    private lateinit var tvScheduledTitle: TextView
    private lateinit var rvScheduledEvents: RecyclerView

    private lateinit var tvEmpty: TextView

    private lateinit var progressBar: View

    private lateinit var btnCreateEvent: Button

    private lateinit var weekDaysAdapter: WeekDaysAdapter
    private lateinit var scheduledEventsAdapter: ScheduledEventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            showAppMessage("Log in to view calendar.", MessageKind.INFO)
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_calendar)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        tvDateLabel = findViewById(R.id.tvDateLabel)
        btnPrevWeek = findViewById(R.id.btnPrevWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)
        rvWeekDays = findViewById(R.id.rvWeekDays)
        tvScheduledTitle = findViewById(R.id.tvScheduledTitle)
        rvScheduledEvents = findViewById(R.id.rvScheduledEvents)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)

        // Setup adapters
        weekDaysAdapter = WeekDaysAdapter { selectedDate ->
            viewModel.selectDate(selectedDate)
        }

        scheduledEventsAdapter = ScheduledEventsAdapter { event ->
            handleEnroll(event)
        }

        rvWeekDays.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvWeekDays.adapter = weekDaysAdapter

        rvScheduledEvents.layoutManager = LinearLayoutManager(this)
        rvScheduledEvents.adapter = scheduledEventsAdapter

        // Set window insets padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Previous week button
        btnPrevWeek.setOnClickListener {
            viewModel.previousWeek()
        }

        // Next week button
        btnNextWeek.setOnClickListener {
            viewModel.nextWeek()
        }

        val role = ApiClient.getSessionStorage().getRole()
        btnCreateEvent.visibility = if (role == "CONTRIBUTOR") View.VISIBLE else View.GONE

        btnCreateEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }

        // Observe ViewModel states
        lifecycleScope.launch {
            viewModel.selectedDate.collect { selectedDate ->

                val today = java.time.LocalDate.now()

                tvDateLabel.text =
                    if (selectedDate == today) "Today"
                    else selectedDate.dayOfWeek.name.lowercase()
                        .replaceFirstChar { it.uppercase() }

                tvScheduledTitle.text = "Scheduled for ${formatMonthDay(selectedDate)}"
            }
        }

        lifecycleScope.launch {
            viewModel.weekDays.collect { days ->
                weekDaysAdapter.submitList(days)
            }
        }

        lifecycleScope.launch {
            viewModel.scheduledEvents.collect { events ->
                scheduledEventsAdapter.submitList(events)
                val isLoading = viewModel.isLoading.value
                tvEmpty.visibility = if (events.isEmpty() && !isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

                if (!isLoading) {
                    // When loading finishes → immediately check if we should show "No events"
                    val events = viewModel.scheduledEvents.value
                    tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentDate.collect { date ->
                tvCurrentDate.text = formatDisplayDate(date)
            }
        }
    }

    private fun formatDisplayDate(date: java.time.LocalDate): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        return date.format(formatter)
    }

    private fun formatMonthDay(date: java.time.LocalDate): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d")
        return date.format(formatter)
    }

    private fun handleEnroll(event: ScheduledEvent) {
        lifecycleScope.launch {
            try {
                if (event.isEnrolled) {
                    viewModel.signOut(event.id.toLong())

                    showAppMessage(
                        "You have signed out.",
                        MessageKind.SUCCESS
                    )
                } else {
                    viewModel.enroll(event.id.toLong())

                    showAppMessage(
                        "Successfully enrolled.",
                        MessageKind.SUCCESS
                    )
                }

            } catch (e: retrofit2.HttpException) {
                showAppMessage(
                    "Action failed. Please try again.",
                    MessageKind.ERROR
                )

            } catch (e: java.io.IOException) {
                showAppMessage(
                    "You are offline. Try again later.",
                    MessageKind.INFO
                )
            }
        }
    }
}
