package com.example.calorie

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.WorkoutEntity
import com.example.calorie.data.WorkoutScheduleEntity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var workoutId: Int = -1
    private var currentStatus: String = "in_progress"
    private var workoutTimer: CountDownTimer? = null
    private var elapsedSeconds = 0L
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        db = AppDatabase.getInstance(this)
        workoutId = intent.getIntExtra("workout_id", -1)

        if (workoutId == -1) {
            finish()
            return
        }

        setupUI()
        loadWorkoutData()
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun getPlannedDurationSeconds(workout: WorkoutEntity): Int {
        return try {
            val start = workout.plannedStartTime ?: "00:00"
            val end = workout.plannedEndTime ?: "00:00"

            val startParts = start.split(":").map { it.toInt() }
            val endParts = end.split(":").map { it.toInt() }

            val startSeconds = startParts[0] * 3600 + startParts[1] * 60
            val endSeconds = endParts[0] * 3600 + endParts[1] * 60

            (endSeconds - startSeconds).coerceAtLeast(0) // чтобы не было отрицательного значения
        } catch (e: Exception) {
            5400 // запасной вариант: 1.5 часа
        }
    }


    private fun loadWorkoutData() {
        lifecycleScope.launch {
            val workout = db.appDao().getWorkoutById(workoutId)
            val exercises = db.appDao().getWorkoutScheduleByWorkoutId(workoutId)

            if (workout != null) {
                currentStatus = workout.status
                elapsedSeconds = workout.elapsedSeconds // сколько уже прошло
                val plannedDuration = getPlannedDurationSeconds(workout)

                runOnUiThread {
                    updateUI(workout, exercises)
                }

                // Восстанавливаем таймер
                if (workout.status == "in_gym" || workout.status == "paused") {
                    startWorkoutTimer(plannedDuration, elapsedSeconds)
                    if (workout.status == "paused") pauseWorkout() // чтобы не сразу запускать
                }
            } else {
                finish()
            }
        }

    }



    @SuppressLint("SetTextI18n")
    private fun updateUI(
        workout: WorkoutEntity,
        exercises: List<WorkoutScheduleEntity>
    ) {
        // Дата и время
        val datePart = formatDate(workout.workoutDate)
        val timePart = "${workout.plannedStartTime ?: "00:00"} – ${workout.plannedEndTime ?: "00:00"}"
        findViewById<TextView>(R.id.textDateTime).text = "$datePart, $timePart"

        // Статус
        updateStatusView(workout.status)

        // Кнопки управления
        setupControlButtons(workout.status)

        // Список упражнений
        val exerciseAdapter = ExerciseAdapter(exercises.toMutableList())
        findViewById<RecyclerView>(R.id.recyclerViewExercises).apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(this@WorkoutDetailActivity)
        }

        // Финальные элементы
        if (workout.status == "completed") {
            findViewById<TextView>(R.id.textCompleted).visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnFinalFinish).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.textCompleted).visibility = View.GONE
            findViewById<MaterialButton>(R.id.btnFinalFinish).visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnFinalFinish).setOnClickListener {
                confirmFinishWorkout()
            }
        }
    }

    private fun updateStatusView(status: String) {
        val statusView = findViewById<TextView>(R.id.textStatus)
        when (status) {
            "completed" -> {
                statusView.text = "Завершена"
                statusView.setBackgroundResource(R.drawable.bg_status_green)
            }
            "skipped" -> {
                statusView.text = "Пропущена"
                statusView.setBackgroundResource(R.drawable.bg_status_red)
            }
            "in_gym" -> {
                statusView.text = "В зале"
                statusView.setBackgroundResource(R.drawable.bg_status_yellow)
            }
            else -> {
                statusView.text = "Предстоящая"
                statusView.setBackgroundResource(R.drawable.bg_status_yellow)
            }
        }
    }

    private fun setupControlButtons(status: String) {
        val btnStart = findViewById<MaterialButton>(R.id.btnStart)
        val btnPause = findViewById<MaterialButton>(R.id.btnPause)
        val btnFinish = findViewById<MaterialButton>(R.id.btnFinish)

        when (status) {
            "in_progress" -> {
                btnStart.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
                btnFinish.visibility = View.GONE
                btnStart.setOnClickListener { startWorkout() }
            }
            "in_gym" -> {
                btnStart.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
                btnFinish.visibility = View.VISIBLE
                btnPause.text = "Приостановить"
                btnPause.setOnClickListener { pauseWorkout() }
                btnFinish.setOnClickListener { confirmFinishWorkout() }
            }
            "paused" -> {
                btnStart.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
                btnFinish.visibility = View.VISIBLE
                btnPause.text = "Продолжить"
                btnPause.setOnClickListener { resumeWorkout() }
                btnFinish.setOnClickListener { confirmFinishWorkout() }
            }
            "completed" -> {
                btnStart.visibility = View.GONE
                btnPause.visibility = View.GONE
                btnFinish.visibility = View.GONE
            }
        }
    }

    private fun startWorkout() {
        lifecycleScope.launch {
            db.appDao().updateWorkoutStatus(workoutId, "in_gym")
            db.appDao().updateWorkoutElapsedSeconds(workoutId, elapsedSeconds)
            currentStatus = "in_gym"

            // Запускаем таймер (например, 90 минут = 5400 сек)
            val plannedDuration = 5400 // можно получить из БД
            startWorkoutTimer(plannedDuration)

            setResult(Activity.RESULT_OK)

            updateStatusView("in_gym")
            setupControlButtons("in_gym")

        }
    }

    private fun pauseWorkout() {
        workoutTimer?.cancel()
        isTimerRunning = false
        lifecycleScope.launch {
            db.appDao().updateWorkoutStatus(workoutId, "paused")
            db.appDao().updateWorkoutElapsedSeconds(workoutId, elapsedSeconds)
            currentStatus = "paused"
            runOnUiThread {
                updateStatusView("paused")
                setupControlButtons("paused")
            }
        }
    }

    private fun resumeWorkout() {
        // Возобновляем с текущего времени
        val remainingSeconds = (5400 - elapsedSeconds).toInt() // нужно хранить plannedDuration
        workoutTimer?.cancel()
        workoutTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentElapsed = 5400 - (millisUntilFinished / 1000)
                elapsedSeconds = currentElapsed
                updateTimerDisplay(currentElapsed, 5400)
                updateCaloriesDisplay(currentElapsed)
            }
            override fun onFinish() {
                elapsedSeconds = 5400
                updateTimerDisplay(5400, 5400)
                updateCaloriesDisplay(5400)
                isTimerRunning = false
            }
        }.start()
        isTimerRunning = true

        lifecycleScope.launch {
            db.appDao().updateWorkoutStatus(workoutId, "in_gym")
            db.appDao().updateWorkoutElapsedSeconds(workoutId, elapsedSeconds)
            currentStatus = "in_gym"
            runOnUiThread {
                updateStatusView("in_gym")
                setupControlButtons("in_gym")
            }
        }
    }

    private fun confirmFinishWorkout() {
        AlertDialog.Builder(this)
            .setTitle("Завершить тренировку?")
            .setMessage("Вы уверены, что хотите завершить тренировку? Это действие нельзя отменить.")
            .setPositiveButton("Да, завершить") { _, _ ->
                finishWorkout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun finishWorkout() {
        // Останавливаем таймер, если он идёт
        workoutTimer?.cancel()
        isTimerRunning = false

        lifecycleScope.launch {
            db.appDao().updateWorkoutStatus(workoutId, "completed")
            setResult(Activity.RESULT_OK)
            loadWorkoutData()

        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            val day = parts[2].toInt()
            val month = when (parts[1]) {
                "01" -> "января"
                "02" -> "февраля"
                "03" -> "марта"
                "04" -> "апреля"
                "05" -> "мая"
                "06" -> "июня"
                "07" -> "июля"
                "08" -> "августа"
                "09" -> "сентября"
                "10" -> "октября"
                "11" -> "ноября"
                "12" -> "декабря"
                else -> ""
            }
            "$day $month"
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun startWorkoutTimer(totalDuration: Int, startFromSeconds: Long = 0L) {
        workoutTimer?.cancel()
        val remainingMillis = (totalDuration - startFromSeconds) * 1000L

        workoutTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds = totalDuration - (millisUntilFinished / 1000)
                updateTimerDisplay(elapsedSeconds, totalDuration)
                updateCaloriesDisplay(elapsedSeconds)

                lifecycleScope.launch(Dispatchers.IO) {
                    db.appDao().updateWorkoutElapsedSeconds(workoutId, elapsedSeconds)
                }
            }

            override fun onFinish() {
                elapsedSeconds = totalDuration.toLong()
                updateTimerDisplay(elapsedSeconds, totalDuration)
                updateCaloriesDisplay(elapsedSeconds)
                isTimerRunning = false
            }
        }.start()

        isTimerRunning = true
    }



    @SuppressLint("SetTextI18n")
    private fun updateTimerDisplay(elapsed: Long, total: Int) {
        val elapsedFormatted = formatTime(elapsed.toInt())
        val totalFormatted = formatTime(total)
        findViewById<TextView>(R.id.textTimer).text = "$elapsedFormatted / $totalFormatted"
    }

    @SuppressLint("SetTextI18n")
    private fun updateCaloriesDisplay(elapsedSeconds: Long) {
        // Пример: 5 ккал/минуту
        val calories = (elapsedSeconds / 60 * 5).toInt()
        findViewById<TextView>(R.id.textCalories).text = "~$calories ккал"
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
}


// Внизу файла WorkoutDetailActivity.kt
class ExerciseAdapter(
    private val exercises: MutableList<WorkoutScheduleEntity>, // ← MutableList

) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

    // Внутри ExerciseAdapter
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exerciseImage: ImageView = itemView.findViewById(R.id.exerciseImage)
        private val exerciseName: TextView = itemView.findViewById(R.id.exerciseName)
        private val repsStatus: TextView = itemView.findViewById(R.id.repsStatus)
        private val restStatus: TextView = itemView.findViewById(R.id.restStatus)

        @SuppressLint("SetTextI18n")
        fun bind(exercise: WorkoutScheduleEntity) {
            exerciseName.text = "Упражнение ${exercise.exerciseId}"

            // Статус подходов
            repsStatus.text = "${exercise.plannedSets ?: 0} раз"
            repsStatus.setBackgroundResource(
                if (exercise.status == "completed") R.drawable.bg_status_green
                else R.drawable.bg_status_gray
            )

            // Статус перерыва
            restStatus.text = "${(exercise.restDuration ?: 0) / 60} мин"
            restStatus.setBackgroundResource(
                if (exercise.status == "completed") R.drawable.bg_status_green
                else R.drawable.bg_status_gray
            )

            // Нажатие на изображение → детали упражнения
            exerciseImage.setOnClickListener {
                val intent = Intent(itemView.context, ExerciseDetailActivity::class.java).apply {
                    putExtra("exercise_id", exercise.exerciseId)
                }
                itemView.context.startActivity(intent)
            }

            // Нажатие на подходы → редактирование
            repsStatus.setOnClickListener {
                showRepsDialog(itemView.context, exercise)
            }

            // Нажатие на перерыв → таймер
            restStatus.setOnClickListener {
                showRestTimerDialog(itemView.context, exercise.restDuration ?: 0, exercise)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount() = exercises.size

    @OptIn(DelicateCoroutinesApi::class)
    private fun showRepsDialog(context: Context, exercise: WorkoutScheduleEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reps, null)
        val repsInput = dialogView.findViewById<EditText>(R.id.inputReps)
        val weightInput = dialogView.findViewById<EditText>(R.id.inputWeight)

        repsInput.setText((exercise.plannedSets ?: 0).toString())
        weightInput.setText((exercise.exerciseDuration ?: 0).toString())

        AlertDialog.Builder(context)
            .setTitle("Редактировать подход")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                try {
                    val newReps = repsInput.text.toString().toInt()
                    val newWeight = weightInput.text.toString().toFloat()

                    // Сохраняем в БД
                    GlobalScope.launch(Dispatchers.IO) {
                        // Обновляем расписание
                        val updatedSchedule = exercise.copy(
                            plannedSets = newReps,
                            exerciseDuration = (newWeight).toInt() // условно
                        )
                        AppDatabase.getInstance(context).appDao().updateWorkoutSchedule(updatedSchedule)

                        // Отмечаем как выполненный
                        if (newReps > 0) {
                            AppDatabase.getInstance(context).appDao().updateWorkoutScheduleStatus(
                                exercise.id, "completed"
                            )
                        }


                        // Обновляем список упражнений
                        withContext(Dispatchers.Main) {
                            // Обновляем конкретный элемент в списке
                            val position = exercises.indexOfFirst { it.id == exercise.id }
                            if (position != -1) {
                                // Создаём обновлённую копию
                                val updatedExercise = exercise.copy(
                                    plannedSets = newReps,
                                    exerciseDuration = (newWeight).toInt(), // временно
                                    status = if (newReps > 0) "completed" else exercise.status
                                )
                                exercises[position] = updatedExercise
                                notifyItemChanged(position)
                            }
                        }
                    }

                    Toast.makeText(context, "Сохранено!", Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "Введите корректные значения", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showRestTimerDialog(context: Context, restDuration: Int, exercise: WorkoutScheduleEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rest_timer, null)
        val timerText = dialogView.findViewById<TextView>(R.id.textTimer)
        val btnPlus10 = dialogView.findViewById<Button>(R.id.btnPlus10)
        val btnMinus10 = dialogView.findViewById<Button>(R.id.btnMinus10)
        val btnStartTimer = dialogView.findViewById<Button>(R.id.btnStartTimer)

        var currentSeconds = restDuration
        var restTimer: CountDownTimer? = null
        var isTimerRunning = false

        fun updateTimerDisplay() {
            timerText.text = formatTime(currentSeconds)
        }

        fun startTimer() {
            isTimerRunning = true
            restTimer = object : CountDownTimer(currentSeconds * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    currentSeconds = (millisUntilFinished / 1000).toInt()
                    (context as Activity).runOnUiThread {
                        updateTimerDisplay()
                    }
                }
                override fun onFinish() {
                    (context as Activity).runOnUiThread {
                        currentSeconds = 0
                        updateTimerDisplay()
                        isTimerRunning = false
                        // Отмечаем как завершённый
                        GlobalScope.launch(Dispatchers.IO) {
                            AppDatabase.getInstance(context).appDao().updateWorkoutScheduleStatus(
                                exercise.id, "completed"
                            )
                        }
                    }
                }
            }.start()
        }

        updateTimerDisplay()

        btnPlus10.setOnClickListener {
            if (isTimerRunning) {
                currentSeconds += 10
                restTimer?.cancel()
                startTimer()
            } else {
                currentSeconds += 10
                updateTimerDisplay()
            }
        }

        btnMinus10.setOnClickListener {
            if (isTimerRunning) {
                currentSeconds = maxOf(0, currentSeconds - 10)
                restTimer?.cancel()
                startTimer()
            } else {
                currentSeconds = maxOf(0, currentSeconds - 10)
                updateTimerDisplay()
            }
        }



        btnStartTimer.setOnClickListener {
            if (!isTimerRunning) {
                startTimer()
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Таймер перерыва")
            .setView(dialogView)
            .setNegativeButton("Закрыть") { _, _ ->
                restTimer?.cancel()
            }
            .show()
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}