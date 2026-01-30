package com.example.calorie

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.calorie.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.io.File
import android.content.Context
import com.google.gson.reflect.TypeToken

// Модель данных
data class UserData(
    var gender: String = "Мужской",
    var age: Int = 30,
    var height: Int = 180,
    var weight: Int = 75,
    var targetWeight: Int = 70,
    var targetDate: String = "01.06.2026",
    var caloriesGoal: Int = 2200,
    var proteinGoal: Int = 120,
    var fatGoal: Int = 70,
    var carbsGoal: Int = 300,
    var waterGoal: Int = 2000
)

class MainActivity : AppCompatActivity() {
    companion object {
        var userData = UserData()
        private const val DATA_FILE = "user_data.json"

        fun saveData(context: Context) {
            val json = Gson().toJson(userData)
            val file = File(context.filesDir, DATA_FILE)
            file.writeText(json)
        }

        fun loadData(context: Context) {
            val file = File(context.filesDir, DATA_FILE)
            if (file.exists()) {
                try {
                    val json = file.readText()

                    // Вариант 1: Использовать TypeToken (рекомендуется)
                    val type = object : TypeToken<UserData>() {}.type
                    userData = Gson().fromJson(json, type)

                    // Или Вариант 2: Явно указать класс
                    // userData = Gson().fromJson(json, UserData::class.java)

                } catch (e: Exception) {
                    e.printStackTrace()
                    userData = UserData() // Восстановление по умолчанию при ошибке
                }
            }
        }
    }
    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val analyzeFragment = AnalyzeFragment()
    private val profileFragment = WorkoutFragment()

    private var currentFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(R.id.contentContainer, homeFragment, "home")
            .commit()

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            switchFragment(homeFragment)
        }
        binding.btnSearch.setOnClickListener {
            switchFragment(searchFragment)
        }
        binding.btnCamera.setOnClickListener {
            Toast.makeText(this, "Открытие камеры", Toast.LENGTH_SHORT).show()
        }
        binding.btnAnalyze.setOnClickListener {
            switchFragment(analyzeFragment)
        }
        binding.btnWorkout.setOnClickListener {
            switchFragment(profileFragment)
        }
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun switchFragment(newFragment: Fragment) {
        if (newFragment == currentFragment) return

        supportFragmentManager.beginTransaction().apply {
            hide(currentFragment)
            if (!newFragment.isAdded) {
                add(R.id.contentContainer, newFragment)
            } else {
                show(newFragment)
            }
            commit()
        }
        currentFragment = newFragment
    }
}