package com.example.calorie.api

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Response  // ← ВАЖНО: retrofit2, а не okhttp3!

object ApiParser {
    private val gson = Gson()

    fun parseAnalyzeResponse(response: Response<ResponseBody>): ApiAnalyzeResult {
        if (!response.isSuccessful) {
            return ApiAnalyzeResult.HttpError(
                response.code(),
                response.errorBody()?.string() ?: "Unknown error"
            )
        }

        val body = response.body()?.string() ?: return ApiAnalyzeResult.Error("Empty response")

        return try {
            val json = JsonParser.parseString(body).asJsonObject
            val status = json.get("status")?.asString ?: "error"

            when (status) {
                "success" -> {
                    val foodItems = gson.fromJson<Map<String, ApiFoodItem>>(
                        json.get("food_items"),
                        object : com.google.gson.reflect.TypeToken<Map<String, ApiFoodItem>>() {}.type
                    )
                    ApiAnalyzeResult.Success(foodItems ?: emptyMap())
                }
                "not_found" -> {
                    val message = json.get("message")?.asString ?: "Food not found"
                    ApiAnalyzeResult.NotFound(message)
                }
                "danger" -> {
                    val message = json.get("message")?.asString ?: "Dangerous food detected"
                    val foodItems = gson.fromJson<Map<String, ApiFoodItem>>(
                        json.get("food_items"),
                        object : com.google.gson.reflect.TypeToken<Map<String, ApiFoodItem>>() {}.type
                    )
                    ApiAnalyzeResult.Danger(message, foodItems ?: emptyMap())
                }
                "error" -> {
                    val message = json.get("message")?.asString ?: "Analysis error"
                    ApiAnalyzeResult.Error(message)
                }
                else -> ApiAnalyzeResult.Error("Unknown status: $status")
            }
        } catch (e: Exception) {
            ApiAnalyzeResult.Error("Parse error: ${e.message}")
        }
    }
}