package com.example.calorie.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

@Database(
    entities = [
        ClientEntity::class,
        ExerciseEntity::class,
        DishEntity::class,
        FoodPhotoEntity::class,
        WorkoutEntity::class,
        WorkoutScheduleEntity::class,
        WorkoutSetEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "calorie.db"
            )
                .addCallback(SeedCallback(context))
                .build()
        }
    }

    private class SeedCallback(
        private val context: Context
    ) : Callback() {

        private fun copyAssetToInternalStorage(context: Context, assetName: String): String {
            val folder = File(context.filesDir, "CalorieFolder")
            if (!folder.exists()) folder.mkdirs()

            val outFile = File(folder, assetName)

            if (!outFile.exists()) {
                context.assets.open(assetName).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            return outFile.absolutePath
        }


        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            // Заполняем тестовыми данными в фоне
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                val dao = database.appDao()

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

                // 📁 Создаём папку
                val folder = File(context.filesDir, "CalorieFolder")
                if (!folder.exists()) folder.mkdirs()
                // 🖼 Копируем картинки
                val photo1Path = copyAssetToInternalStorage(context, "photo1.jpg")
                val image1Path = copyAssetToInternalStorage(context, "image1.webp")
                val image2Path = copyAssetToInternalStorage(context, "image2.webp")
                val image3Path = copyAssetToInternalStorage(context, "image3.jpg")
                val image4Path = copyAssetToInternalStorage(context, "image4.jpg")
                val image5Path = copyAssetToInternalStorage(context, "image5.jpg")

                // CLIENT
                dao.insertClient(
                    ClientEntity(
                        id = 1,
                        gender = "male",
                        age = 30,
                        height = 180.0,
                        currentWeight = 85.0,
                        targetWeight = 78.0,
                        targetDate = today.plusMonths(5).toString(),
                        targetCalories = 2200,
                        targetProteins = 150.0,
                        targetFats = 70.0,
                        targetCarbs = 250.0,
                        targetWater = 2500.0
                    )
                )

                // EXERCISES
                dao.insertExercises(
                    listOf(
                        ExerciseEntity(
                            name = "Приседания",
                            description = "Базовое упражнение для ног",
                            imagePath = null,
                            videoPath = null,
                            tips = null,
                            muscleGroup = "legs",
                            difficulty = "beginner",
                            createdAt = null
                        ),
                        ExerciseEntity(
                            name = "Жим лежа",
                            description = "Грудные мышцы",
                            imagePath = null,
                            videoPath = null,
                            tips = null,
                            muscleGroup = "chest",
                            difficulty = "intermediate",
                            createdAt = null
                        )
                    )
                )

                // WORKOUT TODAY
                dao.insertWorkout(
                    WorkoutEntity(
                        workoutDate = today.toString(),
                        status = "completed",
                        plannedStartTime = "10:00",
                        plannedEndTime = "11:00",
                        actualStartDatetime = today.atTime(10, 5).toString(),
                        actualEndDatetime = today.atTime(11, 0).toString(),
                        rating = 8,
                        notes = "Тренировка сегодня",
                        createdAt = null
                    )
                )

                // WORKOUT TOMORROW
                dao.insertWorkout(
                    WorkoutEntity(
                        workoutDate = tomorrow.toString(),
                        status = "in_progress",
                        plannedStartTime = "18:00",
                        plannedEndTime = "19:00",
                        actualStartDatetime = null,
                        actualEndDatetime = null,
                        rating = null,
                        notes = "Запланировано",
                        createdAt = null
                    )
                )

                // FOOD PHOTO TODAY
                dao.insertFoodPhoto(
                    FoodPhotoEntity(
                        photoPath = photo1Path,
                        name = "Завтрак",
                        calories = 350,
                        proteins = 25.0,
                        fats = 12.0,
                        carbs = 40.0,
                        water = 150.0,
                        weight = 300.0,
                        takenDatetime = LocalDateTime.now().toString(),
                        createdAt = null
                    )
                )

                // DISHES (добавьте после упражнений)
                val dishes = listOf(
                    DishEntity(
                        name = "Омлет с овощами",
                        description = "Омлет с болгарским перцем, помидорами и зеленью",
                        photoPath = image1Path, // ← путь к asset
                        calories = 350,
                        proteins = 28.0,
                        fats = 22.0,
                        carbs = 12.0,
                        water = 100.0,
                        createdAt = null
                    ),
                    DishEntity(
                        name = "Куриная грудка с гречкой",
                        description = "Запеченная куриная грудка с гречневой кашей",
                        photoPath = image2Path,
                        calories = 420,
                        proteins = 45.0,
                        fats = 8.0,
                        carbs = 50.0,
                        water = 120.0,
                        createdAt = null
                    ),
                    DishEntity(
                        name = "Творог с бананом",
                        description = "Обезжиренный творог с бананом и медом",
                        photoPath = image3Path,
                        calories = 280,
                        proteins = 35.0,
                        fats = 2.0,
                        carbs = 30.0,
                        water = 80.0,
                        createdAt = null
                    ),
                    DishEntity(
                        name = "Салат Цезарь",
                        description = "Классический салат Цезарь с курицей",
                        photoPath = image4Path,
                        calories = 320,
                        proteins = 25.0,
                        fats = 18.0,
                        carbs = 20.0,
                        water = 150.0,
                        createdAt = null
                    ),
                    DishEntity(
                        name = "Лосось на пару с брокколи",
                        description = "Филе лосося на пару с отварной брокколи",
                        photoPath = image5Path,
                        calories = 380,
                        proteins = 35.0,
                        fats = 22.0,
                        carbs = 15.0,
                        water = 110.0,
                        createdAt = null
                    )
                )

                dao.insertDishes(dishes)
            }
        }
    }
}
