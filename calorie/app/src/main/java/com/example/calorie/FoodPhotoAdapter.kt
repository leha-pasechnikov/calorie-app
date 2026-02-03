package com.example.calorie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.calorie.data.FoodPhotoEntity
import java.io.File

class FoodPhotoAdapter : RecyclerView.Adapter<FoodPhotoAdapter.ViewHolder>() {

    private var photos = listOf<FoodPhotoEntity>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textMealName)
        private val textNutrients: TextView = itemView.findViewById(R.id.textNutrients)
        private val imageMeal: ImageView = itemView.findViewById(R.id.imageMeal)

        fun bind(photo: FoodPhotoEntity) {
            textName.text = photo.name ?: "Блюдо"
            textNutrients.text = buildString {
                append("Калории: ${photo.calories ?: 0}\n")
                append("Белки: ${(photo.proteins ?: 0.0).toInt()} г | ")
                append("Жиры: ${(photo.fats ?: 0.0).toInt()} г | ")
                append("Углеводы: ${(photo.carbs ?: 0.0).toInt()} г\n")
                append("Вода: ${(photo.water ?: 0.0).toInt()} мл")
            }

            // Загружаем изображение из файла
            val file = File(photo.photoPath)
            if (file.exists()) {
                Glide.with(imageMeal.context)
                    .load(file)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageMeal)
            } else {
                imageMeal.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    fun updatePhotos(newPhotos: List<FoodPhotoEntity>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}