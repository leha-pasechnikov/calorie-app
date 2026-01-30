package com.example.calorie

import Dish
import DishAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.calorie.databinding.FragmentSearchBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // Пример данных
    private val dishes = listOf(
        Dish(R.drawable.image1, "Картофель, тушенный с мясом и грибами, - популярное блюдо домашней кухни, очень сытное и очень вкусное. Небольшой секрет: если в конце приготовления сдобрить всё сметаной, жаркое получится ещё вкуснее. Сметана превращается в бархатистый соус с лёгкой кислинкой и сливочным послевкусием и прекрасно объединяет все компоненты."),
        Dish(R.drawable.image2, "Простое, но очень ароматное и вкусное блюдо для семейного обеда или ужина."),
        Dish(R.drawable.image3, "Очень популярную закуску - жульен (точнее, жюльен) с курицей и грибами, можно приготовить на сковороде примерно за полчаса, совершенно не напрягая себя приобретением кокотниц и доведением блюда до готовности в духовке. А получается так же вкусно!"),
        Dish(R.drawable.image4, "Блюдо привлекательно тем, что макароны отдельно отваривать не нужно, они тушатся в соусе вместе с мясом. Таким образом макароны полностью пропитываются мясной подливкой."),
        Dish(R.drawable.image5, "Очень простая в приготовлении, но очень вкусная запеканка из картофеля и мясного фарша, с помидорами и сыром."),
        Dish(R.drawable.image6, "На приготовление курицы с овощами по-китайски требуется совсем немного времени. Куриное мясо и овощи сначала обжариваются, а затем тушатся в кисло-сладком соусе с лёгкой остринкой. Благодаря овощам и соусу блюдо получается сочным, оригинальным и очень вкусным."),
        Dish(R.drawable.image7, "Чтобы гречневая каша получилась вкусной, мы не будем отдельно готовить к ней подливку, а приготовим гречку вместе с кусочками куриного филе, шампиньонами и морковью в сковороде. Крупа впитает соки овощей, грибов и куриного мяса, оставаясь при этом рассыпчатой, а благодаря сливкам получится ещё вкуснее.")
        )

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

        val adapter = DishAdapter(dishes) { dish ->
            showRecipeDialog(dish)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            this.adapter = adapter
        }
    }

    private fun showRecipeDialog(dish: Dish) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recipe, null).apply {
            findViewById<ImageView>(R.id.imageDialog).setImageResource(dish.imageResId)
            findViewById<TextView>(R.id.textRecipe).text = dish.recipe
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}