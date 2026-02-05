package com.example.calorie

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.calorie.data.AppDatabase
import kotlinx.coroutines.launch

class ExerciseDetailActivity : AppCompatActivity() {
    // В ExerciseDetailActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        // Получаем ID упражнения из intent
        val exerciseId = intent.getIntExtra("exercise_id", -1)
        if (exerciseId == -1) {
            finish()
            return
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish() // ← закрываем активность
        }

        loadExerciseData(exerciseId)
    }

    private fun loadExerciseData(exerciseId: Int) {
        lifecycleScope.launch {
            val exercise = AppDatabase.getInstance(this@ExerciseDetailActivity)
                .appDao()
                .getExerciseById(exerciseId)

            if (exercise != null) {
                runOnUiThread {
                    findViewById<TextView>(R.id.textName).text = exercise.name
                    findViewById<TextView>(R.id.textDescription).text = exercise.description
                    findViewById<TextView>(R.id.textTips).text = exercise.tips ?: ""
                }
            }
        }
    }
}