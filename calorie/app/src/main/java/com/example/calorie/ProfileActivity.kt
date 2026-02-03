package com.example.calorie

import android.annotation.SuppressLint
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
import androidx.lifecycle.lifecycleScope
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.ClientEntity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var settingsContainer: LinearLayout
    private lateinit var db: AppDatabase
    private var client: ClientEntity? = null

    private val dividerColor by lazy { ContextCompat.getColor(this, R.color.divider) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = AppDatabase.getInstance(this)
        settingsContainer = findViewById(R.id.settingsContainer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Профиль"

        // Загружаем данные из БД
        loadClientData()
    }

    private fun loadClientData() {
        lifecycleScope.launch {
            client = db.appDao().getClient()
            if (client == null) {
                // Создаём клиента по умолчанию
                client = ClientEntity(
                    id = 1,
                    gender = "male",
                    age = 30,
                    height = 180.0,
                    currentWeight = 75.0,
                    targetWeight = 70.0,
                    targetDate = "2026-06-01",
                    targetCalories = 2200,
                    targetProteins = 120.0,
                    targetFats = 70.0,
                    targetCarbs = 300.0,
                    targetWater = 2000.0
                )
                db.appDao().insertClient(client!!)
            }
            runOnUiThread {
                buildUI()
            }
        }
    }

    private fun buildUI() {
        settingsContainer.removeAllViews()

        // 1. Персональные данные
        addSectionHeader("Персональные данные")
        addSettingItem("Пол", getGenderDisplay(client?.gender)) { editGender() }
        addDivider()
        addSettingItem("Возраст", "${client?.age ?: 0} лет") { editAge() }
        addDivider()
        addSettingItem("Рост", "${client?.height?.toInt() ?: 0} см") { editHeight() }
        addDivider()
        addSettingItem("Текущий вес", "${client?.currentWeight?.toInt() ?: 0} кг") { editWeight() }
        addDivider()
        addSettingItem("Цель по весу", "${client?.targetWeight?.toInt() ?: 0} кг") { editTargetWeight() }
        addDivider()
        addSettingItem("Дата цели", formatDateForDisplay(client?.targetDate)) { editTargetDate() }

        addSectionSpacing()

        // 2. Цели по питанию
        addSectionHeader("Цели по питанию")
        addSettingItem("Калории", "${client?.targetCalories ?: 0} ккал") { editCalories() }
        addDivider()
        addSettingItem("Белки", "${client?.targetProteins?.toInt() ?: 0} г") { editProtein() }
        addDivider()
        addSettingItem("Жиры", "${client?.targetFats?.toInt() ?: 0} г") { editFat() }
        addDivider()
        addSettingItem("Углеводы", "${client?.targetCarbs?.toInt() ?: 0} г") { editCarbs() }
        addDivider()
        addSettingItem("Вода", "${(client?.targetWater ?: 0.0).toInt()} мл") { editWater() }

        addSectionSpacing()

        // 3. Способ оплаты
        addSectionHeader("Способ оплаты")
        addSettingItem("Тариф", "Бесплатная версия") {}

        addSectionSpacing()

        // 4. Поддержка
        addSectionHeader("Поддержка")
        addSettingItem("Email", "support@calorie.app") { sendEmail() }
        addDivider()
        addSettingItem("Политика конфиденциальности", "") { openPrivacyPolicy() }
        addDivider()
        addSettingItem("Пользовательское соглашение", "") { openTerms() }

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

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private fun getGenderDisplay(gender: String?): String {
        return when (gender) {
            "male" -> "Мужской"
            "female" -> "Женский"
            else -> "Не указано"
        }
    }

    private fun formatDateForDisplay(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "Не указана"
        // Преобразуем YYYY-MM-DD в DD.MM.YYYY
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseDateFromInput(input: String): String? {
        // Преобразуем DD.MM.YYYY в YYYY-MM-DD
        if (input.length == 10 && input[2] == '.' && input[5] == '.') {
            val day = input.substring(0, 2)
            val month = input.substring(3, 5)
            val year = input.substring(6, 10)
            return "$year-$month-$day"
        }
        return null
    }

    // === РЕДАКТИРОВАНИЕ ===

    private fun editGender() {
        val options = arrayOf("Мужской", "Женский")
        val current = when (client?.gender) {
            "male" -> 0
            "female" -> 1
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle("Пол")
            .setSingleChoiceItems(options, current) { dialog, which ->
                client = client?.copy(gender = if (which == 0) "male" else "female")
                saveClient()
                buildUI()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun editAge() = editNumberField("Возраст (лет)", client?.age ?: 0) { value ->
        client = client?.copy(age = value)
        saveClient()
        buildUI()
    }

    private fun editHeight() = editNumberField("Рост (см)", client?.height?.toInt() ?: 0) { value ->
        client = client?.copy(height = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editWeight() = editNumberField("Текущий вес (кг)", client?.currentWeight?.toInt() ?: 0) { value ->
        client = client?.copy(currentWeight = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editTargetWeight() = editNumberField("Цель по весу (кг)", client?.targetWeight?.toInt() ?: 0) { value ->
        client = client?.copy(targetWeight = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editCalories() = editNumberField("Калории (ккал)", client?.targetCalories ?: 0) { value ->
        client = client?.copy(targetCalories = value)
        saveClient()
        buildUI()
    }

    private fun editProtein() = editNumberField("Белки (г)", client?.targetProteins?.toInt() ?: 0) { value ->
        client = client?.copy(targetProteins = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editFat() = editNumberField("Жиры (г)", client?.targetFats?.toInt() ?: 0) { value ->
        client = client?.copy(targetFats = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editCarbs() = editNumberField("Углеводы (г)", client?.targetCarbs?.toInt() ?: 0) { value ->
        client = client?.copy(targetCarbs = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editWater() = editNumberField("Вода (мл)", (client?.targetWater ?: 0.0).toInt()) { value ->
        client = client?.copy(targetWater = value.toDouble())
        saveClient()
        buildUI()
    }

    private fun editTargetDate() {
        val displayDate = formatDateForDisplay(client?.targetDate)
        val input = EditText(this).apply {
            setText(displayDate)
        }
        AlertDialog.Builder(this)
            .setTitle("Дата цели (дд.мм.гггг)")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    val parsedDate = parseDateFromInput(text)
                    if (parsedDate != null) {
                        client = client?.copy(targetDate = parsedDate)
                        saveClient()
                        buildUI()
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

    private fun saveClient() {
        lifecycleScope.launch {
            client?.let { db.appDao().updateClient(it) }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // === ПОДДЕРЖКА ===

    @SuppressLint("QueryPermissionsNeeded")
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