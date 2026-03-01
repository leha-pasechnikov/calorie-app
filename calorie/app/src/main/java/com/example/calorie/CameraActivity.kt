package com.example.calorie

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.calorie.api.ApiAnalyzeResult
import com.example.calorie.api.ApiClient
import com.example.calorie.api.ApiFoodItem
import com.example.calorie.api.ApiParser
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.FoodPhotoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private var currentPhotoPath = ""

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_PICK_IMAGE = 2
        private const val PERMISSIONS_REQUEST_CAMERA = 100
        private const val PERMISSIONS_REQUEST_STORAGE = 101
        private const val FILE_PROVIDER_AUTHORITY = "com.example.calorie.fileprovider"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent()
            }
        }

        findViewById<Button>(R.id.btnChooseFromGallery).setOnClickListener {
            if (checkStoragePermission()) {
                openGallery()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
            false
        } else {
            true
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSIONS_REQUEST_STORAGE)
                false
            } else {
                true
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_STORAGE)
                false
            } else {
                true
            }
        }
    }

    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "food_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                photoFile = createImageFile()
                val photoURI = FileProvider.getUriForFile(
                    this,
                    FILE_PROVIDER_AUTHORITY,
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } ?: run {
                Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { intent ->
            startActivityForResult(intent, REQUEST_PICK_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                }
            }
            PERMISSIONS_REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    processCapturedImage(photoFile)
                }
                REQUEST_PICK_IMAGE -> {
                    data?.data?.let { uri ->
                        processPickedImage(uri)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processCapturedImage(file: File) {
        val compressedPath = compressAndSaveImage(file.absolutePath)
        if (compressedPath != null) {
            // 🔥 Запускаем корутину для API-запроса
            lifecycleScope.launch(Dispatchers.IO) {
                // 1. Отправляем на API и сохраняем результат в БД
                sendToApiAndSave(compressedPath)

                // 2. Возвращаем результат в MainActivity (уже в главном потоке)
                withContext(Dispatchers.Main) {
                    returnResult(compressedPath)
                }
            }
        } else {
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processPickedImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val tempFile = createImageFile()
                FileOutputStream(tempFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                val compressedPath = compressAndSaveImage(tempFile.absolutePath)
                if (compressedPath != null) {
                    // 🔥 Тот же вызов API
                    lifecycleScope.launch(Dispatchers.IO) {
                        sendToApiAndSave(compressedPath)
                        withContext(Dispatchers.Main) {
                            returnResult(compressedPath)
                        }
                    }
                } else {
                    finish()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            finish()
        }
    }

    private fun compressAndSaveImage(originalPath: String): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalPath, options)

            val scaleFactor = calculateInSampleSize(options, 1280, 1280)
            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
                inJustDecodeBounds = false
            }

            val bitmap = BitmapFactory.decodeFile(originalPath, finalOptions)
            val compressedFile = createImageFile()
            FileOutputStream(compressedFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            compressedFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveToDatabase(imagePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CameraActivity)
            val selectedDateStr = CalendarHelper.getSelectedDateString("yyyy-MM-dd HH:mm:ss")

            val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            val takenDatetime = selectedDateStr ?: createdAt


            val photo = FoodPhotoEntity(
                photoPath = imagePath,
                name = "Новое блюдо",
                calories = 0, // Будет обновлено после API
                proteins = 0.0,
                fats = 0.0,
                carbs = 0.0,
                water = 0.0,
                weight = 0.0,
                takenDatetime = takenDatetime,
                createdAt = createdAt
            )
            db.appDao().insertFoodPhoto(photo)
        }
    }

    private fun returnResult(imagePath: String) {
        val resultIntent = Intent().apply {
            putExtra("image_path", imagePath)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }


    // В companion object или как поле
    private val apiService = ApiClient.apiService

    // После compressAndSaveImage, перед returnResult:
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun sendToApiAndSave(imagePath: String): Boolean {
        val file = File(imagePath)
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        return try {
            val response = apiService.analyzeFood(body)
            val result = ApiParser.parseAnalyzeResponse(response)

            // 🔥 Все Toast — только на главном потоке!
            withContext(Dispatchers.Main) {
                when (result) {
                    is ApiAnalyzeResult.Success -> {
                        updateFoodPhotoWithAnalysis(imagePath, result.foodItems)
                        Toast.makeText(this@CameraActivity, "✅ Анализ завершён", Toast.LENGTH_SHORT).show()
                    }
                    is ApiAnalyzeResult.Danger -> {
                        updateFoodPhotoWithAnalysis(imagePath, result.foodItems)
                        Toast.makeText(this@CameraActivity, "⚠️ ${result.message}", Toast.LENGTH_LONG).show()
                    }
                    is ApiAnalyzeResult.NotFound -> {
                        saveFoodPhotoWithDefaults(imagePath, name = "Не распознано")
                        Toast.makeText(this@CameraActivity, "🔍 ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    is ApiAnalyzeResult.Error -> {
                        saveFoodPhotoWithDefaults(imagePath, name = "Ошибка анализа")
                        Toast.makeText(this@CameraActivity, "❌ ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    is ApiAnalyzeResult.HttpError -> {
                        saveFoodPhotoWithDefaults(imagePath, name = "Сетевая ошибка")
                        Toast.makeText(this@CameraActivity, "🌐 Ошибка ${result.code}: ${result.detail}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                saveFoodPhotoWithDefaults(imagePath, name = "Ошибка отправки")
                Toast.makeText(this@CameraActivity, "⚠️ Не удалось связаться с сервером", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun updateFoodPhotoWithAnalysis(imagePath: String, foodItems: Map<String, ApiFoodItem>) {
        // Берём первое распознанное блюдо (можно расширить логику)
        val (foodName, nutrition) = foodItems.entries.firstOrNull() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CameraActivity)
            val existingPhoto = db.appDao().getFoodPhotos().find { it.photoPath == imagePath }

            val updatedPhoto = existingPhoto?.copy(
                name = foodName,
                calories = calculateCalories(nutrition),
                proteins = nutrition.proteins,
                fats = nutrition.fats,
                carbs = nutrition.carbohydrates, // обратите внимание: API возвращает "carbohydrates"
                water = nutrition.water,
                weight = nutrition.weight.toDouble()
            ) ?: FoodPhotoEntity(
                photoPath = imagePath,
                name = foodName,
                calories = calculateCalories(nutrition),
                proteins = nutrition.proteins,
                fats = nutrition.fats,
                carbs = nutrition.carbohydrates,
                water = nutrition.water,
                weight = nutrition.weight.toDouble(),
                takenDatetime = CalendarHelper.getSelectedDateString("yyyy-MM-dd HH:mm:ss")
                    ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            if (existingPhoto != null) {
                db.appDao().updateFoodPhoto(updatedPhoto)
            } else {
                db.appDao().insertFoodPhoto(updatedPhoto)
            }
        }
    }

    private fun saveFoodPhotoWithDefaults(imagePath: String, name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CameraActivity)
            db.appDao().insertFoodPhoto(
                FoodPhotoEntity(
                    photoPath = imagePath,
                    name = name,
                    calories = 0,
                    proteins = 0.0,
                    fats = 0.0,
                    carbs = 0.0,
                    water = 0.0,
                    weight = 0.0,
                    takenDatetime = CalendarHelper.getSelectedDateString("yyyy-MM-dd HH:mm:ss")
                        ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
            )
        }
    }

    private fun calculateCalories(nutrition: ApiFoodItem): Int {
        // Формула: 4 ккал/г белки, 9 ккал/г жиры, 4 ккал/г углеводы
        return (nutrition.proteins * 4 + nutrition.fats * 9 + nutrition.carbohydrates * 4).toInt()
    }

    // Extension для File
    private fun File.asRequestBody(mediaType: MediaType?): RequestBody {
        return RequestBody.create(mediaType, this)
    }
}