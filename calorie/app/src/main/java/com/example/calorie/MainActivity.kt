package com.example.calorie

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.calorie.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val analyzeFragment = AnalyzeFragment()
    private val profileFragment = WorkoutFragment()

    private var currentFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        binding.btnProfile.setOnClickListener {
            switchFragment(profileFragment)
        }
        binding.btnSetting.setOnClickListener {
            Toast.makeText(this, "Открытие настроек", Toast.LENGTH_SHORT).show()
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