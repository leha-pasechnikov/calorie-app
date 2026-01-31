package com.example.calorie

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.GregorianCalendar
import android.widget.ImageView

import com.google.android.material.button.MaterialButton

class WorkoutFragment : Fragment() {

    private var currentMonday = CalendarHelper.getCurrentMonday()
    private var weekContainer: LinearLayout? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: WorkoutAdapter? = null
    private var textWeekRange: TextView? = null
    private var btnPrevWeek: ImageButton? = null
    private var btnNextWeek: ImageButton? = null


    // Тестовые данные по дням (ключ = день месяца)
    private val workoutDataByDay = mapOf(
        1 to listOf(
            Workout("08:00", "09:00", "Завершена", "green"),
            Workout("18:00", "19:00", "Пропущена", "red")
        ),
        2 to listOf(
            Workout("07:30", "08:30", "Предстоящая", "yellow")
        ),
        3 to emptyList(),
        4 to listOf(
            Workout("12:00", "13:00", "Завершена", "green"),
            Workout("17:00", "18:00", "Завершена", "green")
        ),
        5 to listOf(
            Workout("06:00", "07:00", "Пропущена", "red")
        ),
        6 to listOf(
            Workout("09:00", "10:00", "Предстоящая", "yellow"),
            Workout("19:00", "20:00", "Предстоящая", "yellow")
        ),
        7 to emptyList(),
        8 to listOf(
            Workout("08:30", "09:30", "Завершена", "green"),
            Workout("18:30", "19:30", "Завершена", "green")
        ),
        9 to listOf(
            Workout("07:00", "08:00", "Предстоящая", "yellow")
        ),
        10 to emptyList(),
        11 to listOf(
            Workout("10:00", "11:00", "Завершена", "green"),
            Workout("16:00", "17:00", "Пропущена", "red")
        ),
        12 to listOf(
            Workout("06:30", "07:30", "Пропущена", "red")
        ),
        13 to listOf(
            Workout("09:30", "10:30", "Завершена", "green"),
            Workout("20:00", "21:00", "Предстоящая", "yellow")
        ),
        14 to emptyList(),
        15 to listOf(
            Workout("08:00", "09:00", "Завершена", "green"),
            Workout("19:00", "20:00", "Завершена", "green"),
            Workout("21:00", "22:00", "Предстоящая", "yellow")
        ),
        16 to listOf(
            Workout("07:00", "08:00", "Пропущена", "red")
        ),
        17 to emptyList(),
        18 to listOf(
            Workout("12:30", "13:30", "Завершена", "green"),
            Workout("17:30", "18:30", "Завершена", "green")
        ),
        19 to listOf(
            Workout("06:00", "07:00", "Предстоящая", "yellow")
        ),
        20 to emptyList(),
        21 to listOf(
            Workout("08:15", "09:15", "Пропущена", "red"),
            Workout("18:45", "19:45", "Предстоящая", "yellow")
        ),
        22 to listOf(
            Workout("10:00", "11:00", "Завершена", "green")
        ),
        23 to emptyList(),
        24 to listOf(
            Workout("07:45", "08:45", "Завершена", "green"),
            Workout("17:15", "18:15", "Пропущена", "red"),
            Workout("20:30", "21:30", "Предстоящая", "yellow")
        ),
        25 to listOf(
            Workout("09:00", "10:00", "Завершена", "green")
        ),
        26 to emptyList(),
        27 to listOf(
            Workout("06:30", "07:30", "Пропущена", "red"),
            Workout("18:00", "19:00", "Завершена", "green")
        ),
        28 to listOf(
            Workout("08:20", "09:20", "Предстоящая", "yellow")
        ),
        29 to emptyList(),
        30 to listOf(
            Workout("11:00", "12:00", "Завершена", "green"),
            Workout("16:30", "17:30", "Завершена", "green"),
            Workout("19:45", "20:45", "Пропущена", "red")
        ),
        31 to listOf(
            Workout("07:00", "08:00", "Предстоящая", "yellow"),
            Workout("17:00", "18:00", "Предстоящая", "yellow")
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_workout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация View
        weekContainer = view.findViewById(R.id.weekContainer)
        recyclerView = view.findViewById(R.id.recyclerViewWorkouts)
        textWeekRange = view.findViewById(R.id.textWeekRange)
        btnPrevWeek = view.findViewById(R.id.btnPrevWeek)
        btnNextWeek = view.findViewById(R.id.btnNextWeek)

        // Адаптер
        adapter = WorkoutAdapter { workout ->
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
    private fun refreshWeek() {
        // Обновляем заголовок
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

        // Генерируем календарь
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

    private fun updateWorkoutsForDate(date: Calendar) {
        val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
        val workouts = workoutDataByDay[dayOfMonth] ?: emptyList()
        adapter?.updateWorkouts(workouts)
    }
}


// --- Модель тренировки ---
data class Workout(
    val startTime: String,
    val endTime: String,
    val status: String,
    val statusColor: String,
    val imageRes: Int = R.drawable.workout_image // по умолчанию
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

            // Иконка тренировки (можно заменить на разные)
            imageWorkout.setImageResource(workout.imageRes)

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