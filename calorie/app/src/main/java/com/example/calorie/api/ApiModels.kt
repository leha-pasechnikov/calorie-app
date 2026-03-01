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

// === Варианты ответов ===

data class ApiSuccessResponse(
    val status: String = "success",
    val food_items: Map<String, ApiFoodItem>
)

data class ApiNotFoundResponse(
    val status: String = "not_found",
    val message: String
)

data class ApiErrorResponse(
    val status: String = "error",
    val message: String
)

data class ApiDangerResponse(
    val status: String = "danger",
    val message: String,
    val food_items: Map<String, ApiFoodItem>
)

// === Обёртка для удобного парсинга ===
sealed class ApiAnalyzeResult {
    data class Success(val foodItems: Map<String, ApiFoodItem>) : ApiAnalyzeResult()
    data class NotFound(val message: String) : ApiAnalyzeResult()
    data class Danger(val message: String, val foodItems: Map<String, ApiFoodItem>) : ApiAnalyzeResult()
    data class Error(val message: String) : ApiAnalyzeResult()
    data class HttpError(val code: Int, val detail: String) : ApiAnalyzeResult()
}