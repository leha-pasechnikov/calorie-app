package com.example.calorie

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.Calendar
import java.util.GregorianCalendar

class HomeFragment : Fragment() {

    private var pieChart: PieChart? = null
    private var progressContainer: LinearLayout? = null
    private var weekContainer: LinearLayout? = null
    private var mealAdapter: MealAdapter? = null
    private var textCaloriesValue: TextView? = null
    private var textWeekRange: TextView? = null
    private var btnPrevWeek: ImageButton? = null
    private var btnNextWeek: ImageButton? = null
    private var currentMonday: Calendar = CalendarHelper.getCurrentMonday()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация всех View
        pieChart = view.findViewById(R.id.pieCalories)
        progressContainer = view.findViewById(R.id.progressContainer)
        weekContainer = view.findViewById(R.id.weekContainer)
        textCaloriesValue = view.findViewById(R.id.textCaloriesValue)
        textWeekRange = view.findViewById(R.id.textWeekRange)
        btnPrevWeek = view.findViewById(R.id.btnPrevWeek)
        btnNextWeek = view.findViewById(R.id.btnNextWeek)

        setupMealHistory(view)
        setupWeekNavigation()

        refreshWeek()
    }

    private fun setupWeekNavigation() {
        btnPrevWeek?.setOnClickListener {
            currentMonday.add(Calendar.WEEK_OF_YEAR, -1)
            refreshWeek()
        }

        btnNextWeek?.setOnClickListener {
            currentMonday.add(Calendar.WEEK_OF_YEAR, 1)
            refreshWeek()
        }
    }

    private fun getCurrentMonday(): Calendar {
        val cal = GregorianCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToMonday = when (dayOfWeek) {
                Calendar.SUNDAY -> -6
                Calendar.MONDAY -> 0
                else -> 2 - dayOfWeek
            }
            add(Calendar.DAY_OF_MONTH, daysToMonday)
        }
        return cal
    }

    @SuppressLint("SetTextI18n")
    private fun refreshWeek() {
        // Обновляем заголовок недели
        val sunday = GregorianCalendar().apply {
            timeInMillis = currentMonday.timeInMillis
            add(Calendar.DAY_OF_MONTH, 6)
        }

        val mondayDay = currentMonday.get(Calendar.DAY_OF_MONTH)
        val mondayMonth = currentMonday.get(Calendar.MONTH) + 1
        val sundayDay = sunday.get(Calendar.DAY_OF_MONTH)
        val sundayMonth = sunday.get(Calendar.MONTH) + 1

        val mondayMonthStr = mondayMonth.toString().padStart(2, '0')
        val sundayMonthStr = sundayMonth.toString().padStart(2, '0')

        textWeekRange?.text = "$mondayDay.$mondayMonthStr - $sundayDay.$sundayMonthStr"

        // Генерируем дни через helper
        CalendarHelper.setupWeek(
            container = weekContainer!!,
            inflater = layoutInflater,
            currentMonday = currentMonday,
            onDayClick = { date ->
                updateUIForDate(date)
            },
            todayHighlight = true
        )

        // Автоматически выбираем нужный день
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

    private fun getShortDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Пн"
            Calendar.TUESDAY -> "Вт"
            Calendar.WEDNESDAY -> "Ср"
            Calendar.THURSDAY -> "Чт"
            Calendar.FRIDAY -> "Пт"
            Calendar.SATURDAY -> "Сб"
            Calendar.SUNDAY -> "Вс"
            else -> "??"
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isDateInWeek(date: Calendar, monday: Calendar): Boolean {
        val start = monday.timeInMillis
        val end = start + 6 * 24 * 60 * 60 * 1000L
        val target = date.timeInMillis
        return target >= start && target <= end
    }

    private fun updateUIForDate(date: Calendar) {
        val day = date.get(Calendar.DAY_OF_MONTH)
        val consumedCalories = when (day % 3) {
            0 -> 1800f
            1 -> 2100f
            else -> 1500f
        }
        val goalCalories = 2200f

        val proteins = Pair(45 + day % 10, 80)
        val fats = Pair(60 + day % 15, 70)
        val carbs = Pair(200 + day * 2, 300)
        val water = Pair(1500 + day * 50, 2000)

        updateCaloriePieChart(consumedCalories, goalCalories)
        updateProgressBars(proteins, fats, carbs, water)
        updateMealHistory(date)
    }

    @SuppressLint("SetTextI18n")
    private fun updateCaloriePieChart(consumed: Float, goal: Float) {
        val remaining = goal - consumed
        val entries = listOf(
            PieEntry(consumed, ""),
            PieEntry(remaining, "")
        )

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.green),
            ContextCompat.getColor(requireContext(), R.color.gray_300)
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 0f
        }

        pieChart?.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false

            // Настройки для "спидометра"
            isRotationEnabled = false // Отключаем вращение
            holeRadius = 70f // Внутренний радиус (делаем кольцо)
            transparentCircleRadius = 75f
            setDrawHoleEnabled(true) // Включаем "дырку"
            setHoleColor(Color.TRANSPARENT)

            // Настройка "спидометра" - угол начала и конца
            maxAngle = 270f // Максимальный угол (360 = полный круг)
            rotationAngle = 135f // Начало отсчета (снизу по центру)

            // Отступ между секторами (если нужно)
            // dataSet.sliceSpace = 2f

            setTouchEnabled(false)
            setDrawEntryLabels(false)

            // Анимация
            animateY(1000)

            invalidate()
        }

        textCaloriesValue?.text = "${consumed.toInt()} / ${goal.toInt()}"
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun createProgressItem(label: String, progress: Int, max: Int): View {
        val view = layoutInflater.inflate(R.layout.item_progress, null)
        view.findViewById<TextView>(R.id.textLabel).text = label
        view.findViewById<ProgressBar>(R.id.progressBar).apply {
            this.max = max
            this.progress = progress
        }
        view.findViewById<TextView>(R.id.textValue).text = "$progress / $max"

        return view
    }

    private fun updateProgressBars(
        proteins: Pair<Int, Int>,
        fats: Pair<Int, Int>,
        carbs: Pair<Int, Int>,
        water: Pair<Int, Int>
    ) {
        progressContainer?.apply {
            removeAllViews()
            addView(createProgressItem("Белки", proteins.first, proteins.second))
            addView(createProgressItem("Жиры", fats.first, fats.second))
            addView(createProgressItem("Углеводы", carbs.first, carbs.second))
            addView(createProgressItem("Вода", water.first, water.second))
        }
    }

    private fun setupMealHistory(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMeals)
        mealAdapter = MealAdapter(emptyList())
        recyclerView.adapter = mealAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun updateMealHistory(date: Calendar) {
        val day = date.get(Calendar.DAY_OF_MONTH)
        val meals = listOf(
            Meal("Овсянка с фруктами", "Калории: ${300 + day}\nБелки: ${10 + day % 5} г | Жиры: ${8 + day % 3} г | Углеводы: ${40 + day % 10} г\nВода: ${200 + day * 10} мл", R.drawable.photo1),
            Meal("Куриная грудка", "Калории: ${250 + day}\nБелки: ${30 + day % 5} г | Жиры: ${5 + day % 2} г | Углеводы: ${0} г\nВода: ${100 + day * 5} мл", R.drawable.photo2)
        )
        mealAdapter?.updateMeals(meals)
    }
}


// --- Вспомогательные классы ---

data class Meal(val name: String, val nutrients: String, val imageRes: Int)

class MealAdapter(var meals: List<Meal>) : RecyclerView.Adapter<MealAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(meal: Meal) {
            itemView.findViewById<TextView>(R.id.textMealName).text = meal.name
            itemView.findViewById<TextView>(R.id.textNutrients).text = meal.nutrients
            itemView.findViewById<ImageView>(R.id.imageMeal).setImageResource(meal.imageRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(meals[position])
    }

    override fun getItemCount() = meals.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateMeals(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }
}