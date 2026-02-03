package com.example.calorie

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.DishEntity
import com.example.calorie.databinding.FragmentSearchBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var adapter: DishAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        adapter = DishAdapter(emptyList()) { dish ->
            showRecipeDialog(dish)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        // Загружаем все блюда при старте
        loadAllDishes()

        // 🔥 АВТО-ПОИСК ПРИ ВВОДЕ
        binding.searchInput.addTextChangedListener { text ->
            lifecycleScope.launch {
                val query = text.toString()

                val dishes = if (query.isBlank()) {
                    db.appDao().getDishes()
                } else {
                    db.appDao().searchDishes(query)
                }

                adapter.updateDishes(dishes)
            }
        }
    }

    private fun loadAllDishes() {
        lifecycleScope.launch {
            val dishes = db.appDao().getDishes()
            adapter.updateDishes(dishes)
        }
    }

    private fun showRecipeDialog(dish: DishEntity) {
        val view = layoutInflater.inflate(R.layout.dialog_recipe, null)

        val image = view.findViewById<ImageView>(R.id.imageDialog)
        val text = view.findViewById<TextView>(R.id.textRecipe)

        text.text = dish.description ?: "Описание отсутствует"

        Glide.with(this)
            .load("file:///android_asset/${dish.photoPath}")
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(image)

        MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


// === АДАПТЕР ДЛЯ DISHES ===
class DishAdapter(
    private var dishes: List<DishEntity>,
    private val onDishClick: (DishEntity) -> Unit
) : RecyclerView.Adapter<DishAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.imageView)
        private val textView: TextView = view.findViewById(R.id.textView)

        fun bind(dish: DishEntity, onDishClick: (DishEntity) -> Unit) {
            textView.text = dish.name

            if (!dish.photoPath.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load("file:///android_asset/${dish.photoPath}")
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView)
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onDishClick(dish) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dish, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dishes[position], onDishClick)
    }

    override fun getItemCount(): Int = dishes.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateDishes(newDishes: List<DishEntity>) {
        dishes = newDishes
        notifyDataSetChanged()
    }
}
