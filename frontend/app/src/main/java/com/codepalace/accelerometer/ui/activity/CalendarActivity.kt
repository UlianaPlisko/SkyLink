package com.codepalace.accelerometer.ui.activity
import android.os.Build
import android.os.Bundle
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

    private lateinit var weekDaysAdapter: WeekDaysAdapter
    private lateinit var scheduledEventsAdapter: ScheduledEventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

        // Setup adapters
        weekDaysAdapter = WeekDaysAdapter { selectedDate ->
            viewModel.selectDate(selectedDate)
        }

        scheduledEventsAdapter = ScheduledEventsAdapter()

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

        // Observe ViewModel states
        lifecycleScope.launch {
            viewModel.currentDate.collect { date ->
                tvCurrentDate.text = formatDisplayDate(date)
                tvDateLabel.text = "Today"
            }
        }

        lifecycleScope.launch {
            viewModel.weekDays.collect { days ->
                weekDaysAdapter.submitList(days)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedDate.collect { selectedDate ->
                weekDaysAdapter.setSelectedDate(selectedDate)
                tvScheduledTitle.text = "Scheduled for ${formatMonthDay(selectedDate)}"
            }
        }

        lifecycleScope.launch {
            viewModel.scheduledEvents.collect { events ->
                scheduledEventsAdapter.submitList(events)
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
}
