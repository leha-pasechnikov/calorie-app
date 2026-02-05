package com.example.calorie

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calorie.data.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

class WorkoutFragment : Fragment() {

    private var currentMonday = CalendarHelper.getCurrentMonday()
    private var weekContainer: LinearLayout? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: WorkoutAdapter? = null
    private var textWeekRange: TextView? = null
    private var btnPrevWeek: ImageButton? = null
    private var btnNextWeek: ImageButton? = null

    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_workout, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        // Инициализация View
        weekContainer = view.findViewById(R.id.weekContainer)
        recyclerView = view.findViewById(R.id.recyclerViewWorkouts)
        textWeekRange = view.findViewById(R.id.textWeekRange)
        btnPrevWeek = view.findViewById(R.id.btnPrevWeek)
        btnNextWeek = view.findViewById(R.id.btnNextWeek)

        // Адаптер
        adapter = WorkoutAdapter {
            startActivity(Intent(requireContext(), WorkoutDetailActivity::class.java))
        }
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)

        // Кнопки навигации
        btnPrevWeek?.setOnClickListener {
            currentMonday.add(Calendar.WEEK_OF_YEAR, -1)
            refreshWeek()
        }
        btnNextWeek?.setOnClickListener {
            currentMonday.add(Calendar.WEEK_OF_YEAR, 1)
            refreshWeek()
        }

        val btnEditSchedule = view.findViewById<MaterialButton>(R.id.btnEditSchedule)
        btnEditSchedule.setOnClickListener {
            startActivity(Intent(requireContext(), EditScheduleActivity::class.java))
        }

        refreshWeek()
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshWeek() {
        val sunday = GregorianCalendar().apply {
            timeInMillis = currentMonday.timeInMillis
            add(Calendar.DAY_OF_MONTH, 6)
        }

        val mondayDay = currentMonday.get(Calendar.DAY_OF_MONTH)
        val mondayMonth = currentMonday.get(Calendar.MONTH) + 1
        val sundayDay = sunday.get(Calendar.DAY_OF_MONTH)
        val sundayMonth = sunday.get(Calendar.MONTH) + 1

        val mondayStr = "$mondayDay.${mondayMonth.toString().padStart(2, '0')}"
        val sundayStr = "$sundayDay.${sundayMonth.toString().padStart(2, '0')}"
        textWeekRange?.text = "$mondayStr – $sundayStr"

        CalendarHelper.setupWeek(
            container = weekContainer!!,
            inflater = layoutInflater,
            currentMonday = currentMonday,
            onDayClick = { date ->
                updateWorkoutsForDate(date)
            },
            todayHighlight = true
        )

        // Выбираем сегодня или первый день
        val today = GregorianCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayToSelect = if (CalendarHelper.isDateInWeek(today, currentMonday)) {
            val diff = ((today.timeInMillis - currentMonday.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            diff.coerceIn(0, 6)
        } else {
            0
        }

        weekContainer?.getChildAt(dayToSelect)?.performClick()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateWorkoutsForDate(date: Calendar) {
        // Преобразуем Calendar в строку даты "YYYY-MM-DD"
        val localDate = LocalDate.of(
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH) + 1,
            date.get(Calendar.DAY_OF_MONTH)
        )
        val dateStr = localDate.toString()

        // Загружаем тренировки из БД
        lifecycleScope.launch {
            val workouts = db.appDao().getWorkoutsByDate(dateStr)
            val uiWorkouts = workouts.map { entity ->
                // Определяем цвет статуса
                val statusColor = when (entity.status) {
                    "completed" -> "green"
                    "skipped" -> "red"
                    else -> "yellow" // in_progress
                }
                // Создаём UI-модель
                Workout(
                    startTime = entity.plannedStartTime ?: "00:00",
                    endTime = entity.plannedEndTime ?: "00:00",
                    status = getStatusText(entity.status),
                    statusColor = statusColor
                )
            }
            requireActivity().runOnUiThread {
                adapter?.updateWorkouts(uiWorkouts)
            }
        }
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "completed" -> "Завершена"
            "skipped" -> "Пропущена"
            else -> "Предстоящая"
        }
    }
}

// --- Модель тренировки для UI ---
data class Workout(
    val startTime: String,
    val endTime: String,
    val status: String,
    val statusColor: String
)

// --- Адаптер для RecyclerView ---
class WorkoutAdapter(
    private val onWorkoutClick: (Workout) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    private var workouts = listOf<Workout>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val imageWorkout: ImageView = itemView.findViewById(R.id.imageWorkout)

        @SuppressLint("SetTextI18n")
        fun bind(workout: Workout) {
            textTime.text = "${workout.startTime} – ${workout.endTime}"
            textStatus.text = workout.status

            // Устанавливаем цвет фона статуса
            val bgRes = when (workout.statusColor) {
                "green" -> R.drawable.bg_status_green
                "yellow" -> R.drawable.bg_status_yellow
                "red" -> R.drawable.bg_status_red
                else -> R.drawable.bg_status_gray
            }
            textStatus.setBackgroundResource(bgRes)

            // Иконка тренировки
            imageWorkout.setImageResource(R.drawable.workout_image)

            itemView.setOnClickListener { onWorkoutClick(workout) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(workouts[position])
    }

    override fun getItemCount(): Int = workouts.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateWorkouts(newWorkouts: List<Workout>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }
}