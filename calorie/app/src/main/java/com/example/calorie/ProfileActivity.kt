package com.example.calorie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

class ProfileActivity : AppCompatActivity() {

    private lateinit var settingsContainer: LinearLayout
    private val dividerColor by lazy { ContextCompat.getColor(this, R.color.divider) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Профиль"

        settingsContainer = findViewById(R.id.settingsContainer)

        // 1. Персональные данные
        addSectionHeader("Персональные данные")
        addSettingItem("Пол", MainActivity.userData.gender) { editGender() }
        addDivider()
        addSettingItem("Возраст", "${MainActivity.userData.age} лет") { editAge() }
        addDivider()
        addSettingItem("Рост", "${MainActivity.userData.height} см") { editHeight() }
        addDivider()
        addSettingItem("Текущий вес", "${MainActivity.userData.weight} кг") { editWeight() }
        addDivider()
        addSettingItem("Цель по весу", "${MainActivity.userData.targetWeight} кг") { editTargetWeight() }
        addDivider()
        addSettingItem("Дата цели", MainActivity.userData.targetDate) { editTargetDate() }

        // Добавляем отступ между секциями
        addSectionSpacing()

        // 2. Цели по питанию
        addSectionHeader("Цели по питанию")
        addSettingItem("Калории", "${MainActivity.userData.caloriesGoal} ккал") { editCalories() }
        addDivider()
        addSettingItem("Белки", "${MainActivity.userData.proteinGoal} г") { editProtein() }
        addDivider()
        addSettingItem("Жиры", "${MainActivity.userData.fatGoal} г") { editFat() }
        addDivider()
        addSettingItem("Углеводы", "${MainActivity.userData.carbsGoal} г") { editCarbs() }
        addDivider()
        addSettingItem("Вода", "${MainActivity.userData.waterGoal} мл") { editWater() }

        // Добавляем отступ между секциями
        addSectionSpacing()

        // 3. Способ оплаты
        addSectionHeader("Способ оплаты")
        addSettingItem("Тариф", "Бесплатная версия") {
            // TODO: открыть экран премиум
        }

        // Добавляем отступ между секциями
        addSectionSpacing()

        // 4. Поддержка
        addSectionHeader("Поддержка")
        addSettingItem("Email", "support@calorie.app") { sendEmail() }
        addDivider()
        addSettingItem("Политика конфиденциальности", "") { openPrivacyPolicy() }
        addDivider()
        addSettingItem("Пользовательское соглашение", "") { openTerms() }

        // Версия
        addVersion()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun addSectionHeader(title: String) {
        val header = TextView(this).apply {
            text = title
            setTextAppearance(android.R.style.TextAppearance_Medium)
            setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.black))
            setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(8))
        }
        settingsContainer.addView(header)
    }

    private fun addSettingItem(label: String, value: String, onClick: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            setBackgroundResource(R.drawable.bg_ripple)
            setOnClickListener { onClick() }

            val labelView = TextView(this@ProfileActivity).apply {
                text = label
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.black))
            }

            val valueView = TextView(this@ProfileActivity).apply {
                text = value
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.gray_700))
                gravity = android.view.Gravity.END
            }

            addView(labelView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(valueView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        settingsContainer.addView(item)
    }

    private fun addDivider() {
        val divider = android.view.View(this).apply {
            setBackgroundColor(dividerColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(2)
            ).apply {
                setMargins(dpToPx(42), 0, dpToPx(42), 0)
            }
        }
        settingsContainer.addView(divider)
    }

    private fun addSectionSpacing() {
        val spacing = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(24)
            )
        }
        settingsContainer.addView(spacing)
    }

    private fun addVersion() {
        val version = TextView(this).apply {
            text = "v.0.1"
            setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.gray_500))
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(0, dpToPx(32), 0, dpToPx(32))
        }
        settingsContainer.addView(version)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // === РЕДАКТИРОВАНИЕ ===

    private fun editGender() {
        val options = arrayOf("Мужской", "Женский")
        val current = if (MainActivity.userData.gender == "Мужской") 0 else 1
        AlertDialog.Builder(this)
            .setTitle("Пол")
            .setSingleChoiceItems(options, current) { dialog, which ->
                MainActivity.userData.gender = options[which]
                saveAndReload()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun editAge() = editNumberField("Возраст (лет)", MainActivity.userData.age) { value ->
        MainActivity.userData.age = value
    }

    private fun editHeight() = editNumberField("Рост (см)", MainActivity.userData.height) { value ->
        MainActivity.userData.height = value
    }

    private fun editWeight() = editNumberField("Текущий вес (кг)", MainActivity.userData.weight) { value ->
        MainActivity.userData.weight = value
    }

    private fun editTargetWeight() = editNumberField("Цель по весу (кг)", MainActivity.userData.targetWeight) { value ->
        MainActivity.userData.targetWeight = value
    }

    private fun editCalories() = editNumberField("Калории (ккал)", MainActivity.userData.caloriesGoal) { value ->
        MainActivity.userData.caloriesGoal = value
    }

    private fun editProtein() = editNumberField("Белки (г)", MainActivity.userData.proteinGoal) { value ->
        MainActivity.userData.proteinGoal = value
    }

    private fun editFat() = editNumberField("Жиры (г)", MainActivity.userData.fatGoal) { value ->
        MainActivity.userData.fatGoal = value
    }

    private fun editCarbs() = editNumberField("Углеводы (г)", MainActivity.userData.carbsGoal) { value ->
        MainActivity.userData.carbsGoal = value
    }

    private fun editWater() = editNumberField("Вода (мл)", MainActivity.userData.waterGoal) { value ->
        MainActivity.userData.waterGoal = value
    }

    private fun editTargetDate() {
        val input = EditText(this).apply {
            setText(MainActivity.userData.targetDate)
        }
        AlertDialog.Builder(this)
            .setTitle("Дата цели (дд.мм.гггг)")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    // Простая валидация формата
                    if (text.length == 10 && text[2] == '.' && text[5] == '.') {
                        MainActivity.userData.targetDate = text
                        saveAndReload()
                    } else {
                        showError("Неверный формат даты. Используйте дд.мм.гггг")
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun editNumberField(title: String, currentValue: Int, onSave: (Int) -> Unit) {
        val input = EditText(this).apply {
            setText(currentValue.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                try {
                    val value = input.text.toString().toInt()
                    if (value > 0) {
                        onSave(value)
                        saveAndReload()
                    } else {
                        showError("Значение должно быть больше 0")
                    }
                } catch (e: NumberFormatException) {
                    showError("Введите корректное число")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveAndReload() {
        MainActivity.saveData(this)
        recreate()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // === ПОДДЕРЖКА ===

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@calorie.app")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun openPrivacyPolicy() {
        startActivity(Intent(this, LegalActivity::class.java).apply {
            putExtra(LegalActivity.EXTRA_HTML_FILE, "privacy_policy.html")
        })
    }

    private fun openTerms() {
        startActivity(Intent(this, LegalActivity::class.java).apply {
            putExtra(LegalActivity.EXTRA_HTML_FILE, "terms_of_service.html")
        })
    }
}