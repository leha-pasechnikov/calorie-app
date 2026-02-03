package com.example.calorie

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.calorie.data.AppDatabase
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
    private val REQUEST_CAMERA = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO удалить перед релизом
        val updateToday=false
        if (updateToday) {
            deleteDatabase("calorie.db")
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация БД
        db = AppDatabase.getInstance(this)

        // Загрузка данных клиента (опционально, если нужно в глобальную переменную)
        lifecycleScope.launch {
            val client = db.appDao().getClient()
            // Можно использовать client для инициализации UI
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.contentContainer, homeFragment, "home")
            .commit()

        setupBottomNavigation()
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val imagePath = data?.getStringExtra("image_path")
            if (imagePath != null) {
                // Сохраняем фото в БД
                lifecycleScope.launch {
                    db.appDao().insertFoodPhoto(
                        com.example.calorie.data.FoodPhotoEntity(
                            photoPath = imagePath,
                            name = "Новое блюдо",
                            calories = 0, // Будет обновлено после API
                            proteins = 0.0,
                            fats = 0.0,
                            carbs = 0.0,
                            water = 0.0,
                            weight = 0.0,
                            takenDatetime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    )
                }
                Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener { switchFragment(homeFragment) }
        binding.btnSearch.setOnClickListener { switchFragment(searchFragment) }
        binding.btnCamera.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        binding.btnAnalyze.setOnClickListener { switchFragment(analyzeFragment) }
        binding.btnWorkout.setOnClickListener { switchFragment(workoutFragment) }
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