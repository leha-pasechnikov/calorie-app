package com.example.calorie

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.FoodPhotoEntity
import com.example.calorie.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val analyzeFragment = AnalyzeFragment()
    private val workoutFragment = WorkoutFragment()

    private var currentFragment: Fragment = homeFragment

    // Современный способ получения результата из камеры
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val imagePath = result.data?.getStringExtra("image_path")
                if (imagePath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    savePhotoToDatabase(imagePath)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO удалить перед релизом
        val updateToday = true
        if (updateToday) {
            deleteDatabase("calorie.db")
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация БД
        db = AppDatabase.getInstance(this)

        // Первый фрагмент
        supportFragmentManager.beginTransaction()
            .add(R.id.contentContainer, homeFragment, "home")
            .commit()

        startGlowAnimation()
        setupBottomNavigation()
    }

    // 🔵 Анимация блеска кнопки камеры
    private fun startGlowAnimation() {
        val glow = AnimationUtils.loadAnimation(this, R.anim.neon_glow)
        binding.btnCamera.startAnimation(glow)
    }

    // 🔵 Сохранение фото в БД
    @RequiresApi(Build.VERSION_CODES.O)
    private fun savePhotoToDatabase(imagePath: String) {

        lifecycleScope.launch {
            db.appDao().insertFoodPhoto(
                FoodPhotoEntity(
                    photoPath = imagePath,
                    name = "Новое блюдо",
                    calories = 0,
                    proteins = 0.0,
                    fats = 0.0,
                    carbs = 0.0,
                    water = 0.0,
                    weight = 0.0,
                    takenDatetime = LocalDateTime.now()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    createdAt = LocalDateTime.now()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        }

        Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {

        binding.btnHome.setOnClickListener { switchFragment(homeFragment) }

        binding.btnSearch.setOnClickListener { switchFragment(searchFragment) }

        binding.btnAnalyze.setOnClickListener { switchFragment(analyzeFragment) }

        binding.btnWorkout.setOnClickListener { switchFragment(workoutFragment) }

        binding.btnCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent)
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