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
import com.example.calorie.data.AppDatabase
import com.example.calorie.data.FoodPhotoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            saveToDatabase(compressedPath)
            returnResult(compressedPath)
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
                    saveToDatabase(compressedPath)
                    returnResult(compressedPath)
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
}