package com.example.calorie.data

import androidx.room.*

@Dao
interface AppDao {

    // ================= CLIENT =================
    @Query("SELECT * FROM client WHERE id = 1")
    suspend fun getClient(): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    // ================= EXERCISES =================
    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Int): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(list: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(entity: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(entity: ExerciseEntity)

    // ================= DISHES =================
    @Query("SELECT * FROM dishes ORDER BY name")
    suspend fun getDishes(): List<DishEntity>

    @Query("""
        SELECT * FROM dishes 
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'
    """)
    suspend fun searchDishes(query: String): List<DishEntity>
    @Insert
    suspend fun insertDish(entity: DishEntity): Long

    @Update
    suspend fun updateDish(entity: DishEntity)

    @Delete
    suspend fun deleteDish(entity: DishEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDishes(list: List<DishEntity>)

    // ================= FOOD PHOTOS =================
    @Query("SELECT * FROM food_photos WHERE date(taken_datetime) = :date ORDER BY taken_datetime DESC")
    suspend fun getFoodPhotosByDate(date: String): List<FoodPhotoEntity>

    @Query("SELECT * FROM food_photos ORDER BY taken_datetime DESC")
    suspend fun getFoodPhotos(): List<FoodPhotoEntity>

    @Insert
    suspend fun insertFoodPhoto(entity: FoodPhotoEntity)

    @Update
    suspend fun updateFoodPhoto(entity: FoodPhotoEntity)

    @Delete
    suspend fun deleteFoodPhoto(entity: FoodPhotoEntity)

    // ================= WORKOUTS =================
    @Query("SELECT * FROM workouts ORDER BY workout_date DESC")
    suspend fun getWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE workout_date = :date")
    suspend fun getWorkoutByDate(date: String): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(entity: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(entity: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(entity: WorkoutEntity)

    // ================= WORKOUT SCHEDULE =================
    @Query("SELECT * FROM workout_schedule WHERE workout_id = :workoutId ORDER BY order_number")
    suspend fun getWorkoutSchedule(workoutId: Int): List<WorkoutScheduleEntity>

    @Insert
    suspend fun insertWorkoutSchedule(entity: WorkoutScheduleEntity): Long

    @Update
    suspend fun updateWorkoutSchedule(entity: WorkoutScheduleEntity)

    @Delete
    suspend fun deleteWorkoutSchedule(entity: WorkoutScheduleEntity)

    // ================= WORKOUT SETS =================
    @Query("SELECT * FROM workout_sets WHERE workout_schedule_id = :scheduleId ORDER BY set_number")
    suspend fun getWorkoutSets(scheduleId: Int): List<WorkoutSetEntity>

    @Insert
    suspend fun insertWorkoutSet(entity: WorkoutSetEntity)

    @Update
    suspend fun updateWorkoutSet(entity: WorkoutSetEntity)

    @Delete
    suspend fun deleteWorkoutSet(entity: WorkoutSetEntity)
}
