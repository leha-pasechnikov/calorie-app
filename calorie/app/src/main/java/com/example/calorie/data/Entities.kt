package com.example.calorie.data

import androidx.room.*

@Entity(tableName = "client")
data class ClientEntity(
    @PrimaryKey val id: Int = 1,
    val gender: String?,
    val age: Int?,
    val height: Double?,
    @ColumnInfo(name = "current_weight") val currentWeight: Double?,
    @ColumnInfo(name = "target_weight") val targetWeight: Double?,
    @ColumnInfo(name = "target_date") val targetDate: String?,
    @ColumnInfo(name = "target_calories") val targetCalories: Int?,
    @ColumnInfo(name = "target_proteins") val targetProteins: Double?,
    @ColumnInfo(name = "target_fats") val targetFats: Double?,
    @ColumnInfo(name = "target_carbs") val targetCarbs: Double?,
    @ColumnInfo(name = "target_water") val targetWater: Double?
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "video_path") val videoPath: String?,
    val tips: String?,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String?,
    val difficulty: String,
    @ColumnInfo(name = "created_at") val createdAt: String?
)

@Entity(tableName = "dishes")
data class DishEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val calories: Int?,
    val proteins: Double?,
    val fats: Double?,
    val carbs: Double?,
    val water: Double?,
    @ColumnInfo(name = "created_at") val createdAt: String?
)

@Entity(tableName = "food_photos")
data class FoodPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "photo_path") val photoPath: String,
    val name: String?,
    val calories: Int?,
    val proteins: Double?,
    val fats: Double?,
    val carbs: Double?,
    val water: Double?,
    val weight: Double?,
    @ColumnInfo(name = "taken_datetime") val takenDatetime: String,
    @ColumnInfo(name = "created_at") val createdAt: String?
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "workout_date") val workoutDate: String,
    val status: String,
    @ColumnInfo(name = "planned_start_time") val plannedStartTime: String?,
    @ColumnInfo(name = "planned_end_time") val plannedEndTime: String?,
    @ColumnInfo(name = "actual_start_datetime") val actualStartDatetime: String?,
    @ColumnInfo(name = "actual_end_datetime") val actualEndDatetime: String?,
    val rating: Int?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?
)

@Entity(
    tableName = "workout_schedule",
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workout_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exercise_id"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("workout_id"), Index("exercise_id")]
)
data class WorkoutScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "workout_id") val workoutId: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: Int,
    @ColumnInfo(name = "planned_sets") val plannedSets: Int?,
    @ColumnInfo(name = "exercise_duration") val exerciseDuration: Int?,
    @ColumnInfo(name = "rest_duration") val restDuration: Int?,
    val status: String,
    @ColumnInfo(name = "order_number") val orderNumber: Int?
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(entity = WorkoutScheduleEntity::class, parentColumns = ["id"], childColumns = ["workout_schedule_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("workout_schedule_id")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "workout_schedule_id") val workoutScheduleId: Int,
    @ColumnInfo(name = "set_number") val setNumber: Int,
    @ColumnInfo(name = "planned_reps") val plannedReps: Int?,
    @ColumnInfo(name = "planned_weight") val plannedWeight: Double?,
    @ColumnInfo(name = "actual_reps") val actualReps: Int?,
    @ColumnInfo(name = "actual_weight") val actualWeight: Double?,
    @ColumnInfo(name = "set_completed") val setCompleted: Boolean,
    @ColumnInfo(name = "rest_after_set") val restAfterSet: Int?,
    @ColumnInfo(name = "completed_at") val completedAt: String?
)
