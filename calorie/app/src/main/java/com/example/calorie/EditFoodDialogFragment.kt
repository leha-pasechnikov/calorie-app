package com.example.calorie.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.calorie.R
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.FoodPhotoEntity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditFoodDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_FOOD_PHOTO_ID = "food_photo_id"
        private const val ARG_FOOD_PHOTO_PATH = "food_photo_path"

        fun newInstance(photoId: Int, photoPath: String?) = EditFoodDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_FOOD_PHOTO_ID, photoId)
                putString(ARG_FOOD_PHOTO_PATH, photoPath)
            }
        }
    }

    private var listener: OnFoodEditedListener? = null
    // 🔥 Выносим photo как свойство класса
    private var photo: FoodPhotoEntity? = null

    interface OnFoodEditedListener {
        fun onFoodUpdated()
        fun onFoodDeleted()
    }

    fun setListener(listener: OnFoodEditedListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoId = requireArguments().getInt(ARG_FOOD_PHOTO_ID)
        val photoPath = requireArguments().getString(ARG_FOOD_PHOTO_PATH)

        return activity?.let { activity ->
            val view = layoutInflater.inflate(R.layout.dialog_edit_food, null)

            // Поля ввода
            val editName = view.findViewById<TextInputEditText>(R.id.editName)
            val editWeight = view.findViewById<TextInputEditText>(R.id.editWeight)
            val editCalories = view.findViewById<TextInputEditText>(R.id.editCalories)
            val editProteins = view.findViewById<TextInputEditText>(R.id.editProteins)
            val editFats = view.findViewById<TextInputEditText>(R.id.editFats)
            val editCarbs = view.findViewById<TextInputEditText>(R.id.editCarbs)
            val editWater = view.findViewById<TextInputEditText>(R.id.editWater)

            val btnSave = view.findViewById<Button>(R.id.btnSave)
            val btnCancel = view.findViewById<Button>(R.id.btnCancel)
            val btnDelete = view.findViewById<Button>(R.id.btnDelete)

            // 🔥 Загружаем данные и сохраняем в свойство класса
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                photo = db.appDao().getFoodPhotoById(photoId)

                photo?.let {
                    editName.setText(it.name)
                    editWeight.setText(it.weight?.toInt()?.toString() ?: "0")
                    editCalories.setText(it.calories?.toString() ?: "0")
                    editProteins.setText(it.proteins?.toString() ?: "0.0")
                    editFats.setText(it.fats?.toString() ?: "0.0")
                    editCarbs.setText(it.carbs?.toString() ?: "0.0")
                    editWater.setText(it.water?.toString() ?: "0.0")
                }
            }

            // Сохранение
            btnSave.setOnClickListener {
                lifecycleScope.launch {
                    val updated = FoodPhotoEntity(
                        id = photoId,
                        photoPath = photoPath ?: "",
                        name = editName.text.toString().takeIf { it.isNotBlank() } ?: "Без названия",
                        calories = editCalories.text.toString().toIntOrNull() ?: 0,
                        proteins = editProteins.text.toString().toDoubleOrNull() ?: 0.0,
                        fats = editFats.text.toString().toDoubleOrNull() ?: 0.0,
                        carbs = editCarbs.text.toString().toDoubleOrNull() ?: 0.0,
                        water = editWater.text.toString().toDoubleOrNull() ?: 0.0,
                        weight = editWeight.text.toString().toDoubleOrNull() ?: 0.0,
                        // 🔥 Теперь photo доступна
                        takenDatetime = photo?.takenDatetime ?: "",
                        createdAt = photo?.createdAt
                    )

                    val db = AppDatabase.getInstance(requireContext())
                    db.appDao().updateFoodPhoto(updated)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "✅ Сохранено", Toast.LENGTH_SHORT).show()
                        listener?.onFoodUpdated()
                        dismiss()
                    }
                }
            }

            // Удаление
            btnDelete.setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle("Удалить блюдо?")
                    .setMessage("Это действие нельзя отменить")
                    .setPositiveButton("Удалить") { _, _ ->
                        lifecycleScope.launch {
                            val db = AppDatabase.getInstance(requireContext())
                            db.appDao().deleteFoodPhotoById(photoId)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "🗑️ Удалено", Toast.LENGTH_SHORT).show()
                                listener?.onFoodDeleted()
                                dismiss()
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }

            // Отмена
            btnCancel.setOnClickListener { dismiss() }

            AlertDialog.Builder(activity)
                .setView(view)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}