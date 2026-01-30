package com.example.calorie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AnalyzeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = inflater.context
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Данные для графиков
        val chartsInfo = listOf(
            ChartConfig("Калории за неделю", ChartType.LINE, "Суммарное количество калорий, потреблённых за каждый день недели."),
            ChartConfig("Белки/Жиры/Углеводы", ChartType.LINE, "Соотношение БЖУ по дням."),
            ChartConfig("Активность", ChartType.BAR, "Минуты активности в день."),
            ChartConfig("Распределение приёмов пищи", ChartType.PIE, "Процент калорий по завтраку, обеду, ужину и перекусам.")
        )

        for (config in chartsInfo) {
            val item = inflater.inflate(R.layout.item_chart, root, false)
            val title = item.findViewById<TextView>(R.id.textChartTitle)
            val helpBtn = item.findViewById<ImageButton>(R.id.buttonHelp)
            val chartContainer = item.findViewById<FrameLayout>(R.id.chartContainer)

            // Удаляем стандартный LineChart из макета
            chartContainer.removeAllViews()

            val chart: View = when (config.type) {
                ChartType.LINE -> {
                    val lineChart = LineChart(context)
                    setupLineChart(lineChart, createLineData(context))
                    lineChart
                }
                ChartType.BAR -> {
                    val barChart = BarChart(context)
                    setupBarChart(barChart, createBarData(context))
                    barChart
                }
                ChartType.PIE -> {
                    val pieChart = PieChart(context)
                    setupPieChart(pieChart, createPieData(context))
                    pieChart
                }
            }

            chart.id = View.generateViewId()
            chartContainer.addView(chart)

            title.text = config.title
            helpBtn.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Справка")
                    .setMessage(config.description)
                    .setPositiveButton("Понятно", null)
                    .show()
            }

            root.addView(item)
        }

        return ScrollView(context).apply {
            addView(root)
        }
    }

    // --- Создание данных ---
    private fun createLineData(context: android.content.Context): LineData {
        val entries = listOf(
            Entry(0f, 2000f),
            Entry(1f, 1800f),
            Entry(2f, 2200f),
            Entry(3f, 1900f),
            Entry(4f, 2100f),
            Entry(5f, 1700f),
            Entry(6f, 2300f)
        )
        val dataSet = LineDataSet(entries, "Калории").apply {
            color = ContextCompat.getColor(context, R.color.purple_500)
            valueTextColor = ContextCompat.getColor(context, R.color.purple_500)
            setDrawCircles(true)
            circleRadius = 4f
        }
        return LineData(dataSet)
    }

    private fun createBarData(context: android.content.Context): BarData {
        val entries = listOf(
            BarEntry(0f, 45f),
            BarEntry(1f, 60f),
            BarEntry(2f, 30f),
            BarEntry(3f, 75f),
            BarEntry(4f, 50f),
            BarEntry(5f, 40f),
            BarEntry(6f, 65f)
        )
        val dataSet = BarDataSet(entries, "Минуты").apply {
            color = ContextCompat.getColor(context, R.color.teal_700)
        }
        return BarData(dataSet)
    }

    private fun createPieData(context: android.content.Context): PieData {
        val entries = listOf(
            PieEntry(30f, "Завтрак"),
            PieEntry(40f, "Обед"),
            PieEntry(20f, "Ужин"),
            PieEntry(10f, "Перекусы")
        )
        val colors = listOf(
            ContextCompat.getColor(context, R.color.blue),
            ContextCompat.getColor(context, R.color.green),
            ContextCompat.getColor(context, R.color.orange),
            ContextCompat.getColor(context, R.color.red)
        )
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
        }
        return PieData(dataSet)
    }

    // --- Настройка графиков ---
    private fun setupLineChart(chart: LineChart, data: LineData) {
        chart.data = data
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.setDrawGridLines(false)
        chart.setTouchEnabled(false)
        chart.invalidate()
    }

    private fun setupBarChart(chart: BarChart, data: BarData) {
        chart.data = data
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.setDrawGridLines(false)
        chart.setTouchEnabled(false)
        chart.invalidate()
    }

    private fun setupPieChart(chart: PieChart, data: PieData) {
        chart.data = data
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.holeRadius = 0f
        chart.transparentCircleRadius = 0f
        chart.invalidate()
    }
}

// Вспомогательные классы
enum class ChartType { LINE, BAR, PIE }

data class ChartConfig(val title: String, val type: ChartType, val description: String)