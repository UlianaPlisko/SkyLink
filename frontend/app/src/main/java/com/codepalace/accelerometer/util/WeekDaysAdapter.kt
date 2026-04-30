package com.codepalace.accelerometer.util

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.data.model.calendar.WeekDay
import java.time.LocalDate

class WeekDaysAdapter(
    private val onDateSelected: (LocalDate) -> Unit
) : ListAdapter<WeekDay, WeekDaysAdapter.WeekDayViewHolder>(WeekDayDiffCallback()) {

    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate: LocalDate = LocalDate.now()

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_day, parent, false)
        return WeekDayViewHolder(view, onDateSelected) { date ->
            selectedDate = date
            notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WeekDayViewHolder, position: Int) {
        val weekDay = getItem(position)
        holder.bind(weekDay, weekDay.date == selectedDate)
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    fun setSelectedDate(date: LocalDate) {
        selectedDate = date
        notifyDataSetChanged()
    }

    class WeekDayViewHolder(
        itemView: View,
        private val onDateSelected: (LocalDate) -> Unit,
        private val onSelectionChanged: (LocalDate) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val container: View = itemView.findViewById(R.id.dayContainer)

        fun bind(weekDay: WeekDay, isSelected: Boolean) {
            tvDayName.text = weekDay.dayName.uppercase()
            tvDayNumber.text = weekDay.dayNumber

            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_week_day_selected)
                tvDayName.setTextColor(ContextCompat.getColor(itemView.context, R.color.color_primary))
                tvDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.color_primary))
            } else {
                container.setBackgroundResource(R.drawable.bg_week_day_unselected)
                tvDayName.setTextColor(ContextCompat.getColor(itemView.context, R.color.color_text_primary))
                tvDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.color_text_primary))
            }

            itemView.setOnClickListener {
                onDateSelected(weekDay.date)
                onSelectionChanged(weekDay.date)
            }
        }
    }

    class WeekDayDiffCallback : DiffUtil.ItemCallback<WeekDay>() {
        override fun areItemsTheSame(oldItem: WeekDay, newItem: WeekDay) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: WeekDay, newItem: WeekDay) =
            oldItem == newItem
    }
}
