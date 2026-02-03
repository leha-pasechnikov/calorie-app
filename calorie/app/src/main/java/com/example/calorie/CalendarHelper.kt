package com.example.calorie

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import java.util.GregorianCalendar

object CalendarHelper {

    private var selectedDate: Calendar? = null
    /**
     * Заполняет контейнер 7 днями недели, начиная с понедельника
     * @param container — LinearLayout, куда добавлять дни
     * @param inflater — LayoutInflater для создания макетов
     * @param currentMonday — понедельник текущей недели
     * @param onDayClick — обработчик нажатия на день
     * @param todayHighlight — если true, выделяет сегодняшний день
     */


    fun setupWeek(
        container: LinearLayout,
        inflater: LayoutInflater,
        currentMonday: Calendar,
        onDayClick: (Calendar) -> Unit,
        todayHighlight: Boolean = true
    ) {
        container.removeAllViews()
        selectedDate = null

        val today = if (todayHighlight) {
            GregorianCalendar().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else null

        for (i in 0..6) {
            val date = GregorianCalendar().apply {
                timeInMillis = currentMonday.timeInMillis
                add(Calendar.DAY_OF_MONTH, i)
            }

            val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
            val shortDay = getShortDayName(date.get(Calendar.DAY_OF_WEEK))

            val dayView = inflater.inflate(R.layout.item_day, container, false).apply {
                findViewById<TextView>(R.id.textDay).text = shortDay
                findViewById<TextView>(R.id.textDate).text = "$dayOfMonth"

                if (todayHighlight && isSameDay(date, today)) {
                    setBackgroundResource(R.drawable.bg_day_selected)
                }
            }

            dayView.setOnClickListener {
                // Снимаем выделение со всех
                for (j in 0 until container.childCount) {
                    container.getChildAt(j).setBackgroundResource(0)
                }
                // Выделяем текущий
                it.setBackgroundResource(R.drawable.bg_day_selected)
                selectedDate = date
                onDayClick(date)
            }

            container.addView(dayView)
        }
    }

    // --- Вспомогательные функции ---
    fun getSelectedDate(): Calendar? {
        return selectedDate
    }

    fun getSelectedDateString(format: String = "yyyy-MM-dd"): String? {
        return selectedDate?.let {
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
            sdf.format(it.time)
        }
    }

    fun getCurrentMonday(): Calendar {
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

    fun getShortDayName(dayOfWeek: Int): String {
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

    fun isSameDay(cal1: Calendar, cal2: Calendar?): Boolean {
        if (cal2 == null) return false
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isDateInWeek(date: Calendar, monday: Calendar): Boolean {
        val start = monday.timeInMillis
        val end = start + 6 * 24 * 60 * 60 * 1000L
        val target = date.timeInMillis
        return target >= start && target <= end
    }
}