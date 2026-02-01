package com.example.calorie.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            // Заполняем тестовыми данными в фоне
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                val dao = database.appDao()

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

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
                        targetWater = 2.5
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
                        photoPath = "/food/today.jpg",
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
            }
        }
    }
}
