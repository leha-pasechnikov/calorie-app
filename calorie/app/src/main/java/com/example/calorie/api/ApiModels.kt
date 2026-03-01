package com.example.calorie.api

// Базовый интерфейс для полиморфной десериализации (опционально)
// Но проще — использовать sealed class с ручным парсингом по полю "status"

data class ApiFoodItem(
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val water: Double,
    val weight: Int,
    val benefit_score: Float
)

// === Обёртка для удобного парсинга ===
sealed class ApiAnalyzeResult {
    data class Success(val foodItems: Map<String, ApiFoodItem>) : ApiAnalyzeResult()
    data class NotFound(val message: String) : ApiAnalyzeResult()
    data class Danger(val message: String, val foodItems: Map<String, ApiFoodItem>) : ApiAnalyzeResult()
    data class Error(val message: String) : ApiAnalyzeResult()
    data class HttpError(val code: Int, val detail: String) : ApiAnalyzeResult()
}