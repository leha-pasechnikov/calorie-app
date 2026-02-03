package com.example.calorie

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.FoodPhotoEntity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

class HomeFragment : Fragment() {

    private var pieChart: PieChart? = null
    private var progressContainer: LinearLayout? = null
    private var weekContainer: LinearLayout? = null
    private var textCaloriesValue: TextView? = null
    private var textWeekRange: TextView? = null
    private var btnPrevWeek: ImageButton? = null
    private var btnNextWeek: ImageButton? = null
    private var currentMonday: Calendar = CalendarHelper.getCurrentMonday()

    private lateinit var db: AppDatabase
    private lateinit var foodPhotoAdapter: FoodPhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())
        foodPhotoAdapter = FoodPhotoAdapter()

        // Инициализация View
        pieChart = view.findViewById(R.id.pieCalories)
        progressContainer = view.findViewById(R.id.progressContainer)
        weekContainer = view.findViewById(R.id.weekContainer)
        textCaloriesValue = view.findViewById(R.id.textCaloriesValue)
        textWeekRange = view.findViewById(R.id.textWeekRange)
        btnPrevWeek = view.findViewById(R.id.btnPrevWeek)
        btnNextWeek = view.findViewById(R.id.btnNextWeek)

        // Настройка RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMeals)
        recyclerView.adapter = foodPhotoAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        setupWeekNavigation()
        refreshWeek()
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun refreshWeek() {
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

        CalendarHelper.setupWeek(
            container = weekContainer!!,
            inflater = layoutInflater,
            currentMonday = currentMonday,
            onDayClick = { date ->
                updateUIForDate(date)
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
    private fun updateUIForDate(date: Calendar) {
        // Преобразуем Calendar в LocalDate
        val localDate = LocalDate.of(
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH) + 1,
            date.get(Calendar.DAY_OF_MONTH)
        )
        val dateStr = localDate.toString() // "YYYY-MM-DD"

        // Загружаем данные асинхронно
        lifecycleScope.launch {
            val client = db.appDao().getClient()
            val foodPhotos = db.appDao().getFoodPhotosByDate(dateStr)

            // Считаем суммы
            val consumedCalories = foodPhotos.sumOf { it.calories ?: 0 }
            val consumedProteins = foodPhotos.sumOf { (it.proteins ?: 0.0).toInt() }
            val consumedFats = foodPhotos.sumOf { (it.fats ?: 0.0).toInt() }
            val consumedCarbs = foodPhotos.sumOf { (it.carbs ?: 0.0).toInt() }
            val consumedWater = foodPhotos.sumOf { (it.water ?: 0.0).toInt() }

            // Цели из клиента
            val goalCalories = client?.targetCalories ?: 2200
            val goalProteins = (client?.targetProteins ?: 150.0).toInt()
            val goalFats = (client?.targetFats ?: 70.0).toInt()
            val goalCarbs = (client?.targetCarbs ?: 250.0).toInt()
            val goalWater = ((client?.targetWater ?: (2.5 * 1000))).toInt() // л → мл

            // Обновляем UI
            requireActivity().runOnUiThread {
                updateCaloriePieChart(consumedCalories.toFloat(), goalCalories.toFloat())
                updateProgressBars(
                    Pair(consumedProteins, goalProteins),
                    Pair(consumedFats, goalFats),
                    Pair(consumedCarbs, goalCarbs),
                    Pair(consumedWater, goalWater)
                )
                foodPhotoAdapter.updatePhotos(foodPhotos)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCaloriePieChart(consumed: Float, goal: Float) {
        val remaining = goal - consumed
        val entries = listOf(
            PieEntry(consumed.coerceAtLeast(0f), ""),
            PieEntry(remaining.coerceAtLeast(0f), "")
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
            isRotationEnabled = false
            holeRadius = 70f
            transparentCircleRadius = 75f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            maxAngle = 270f
            rotationAngle = 135f
            setTouchEnabled(false)
            setDrawEntryLabels(false)
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
            this.progress = progress.coerceAtMost(max)
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
}

class FoodPhotoAdapter : RecyclerView.Adapter<FoodPhotoAdapter.ViewHolder>() {

    private var photos = listOf<FoodPhotoEntity>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textMealName)
        private val textNutrients: TextView = itemView.findViewById(R.id.textNutrients)
        private val imageMeal: ImageView = itemView.findViewById(R.id.imageMeal)

        fun bind(photo: FoodPhotoEntity) {
            textName.text = photo.name ?: "Блюдо"
            textNutrients.text = buildString {
                append("Калории: ${photo.calories ?: 0}\n")
                append("Белки: ${(photo.proteins ?: 0.0).toInt()} г | ")
                append("Жиры: ${(photo.fats ?: 0.0).toInt()} г | ")
                append("Углеводы: ${(photo.carbs ?: 0.0).toInt()} г\n")
                append("Вода: ${(photo.water ?: 0.0).toInt()} мл")
            }

            if (photo.photoPath.isNotBlank()) {
                Glide.with(imageMeal.context)
                    .load(File(photo.photoPath))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageMeal)
            } else {
                imageMeal.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    @SuppressLint("NotifyDataSetChanged")
    fun updatePhotos(newPhotos: List<FoodPhotoEntity>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}
