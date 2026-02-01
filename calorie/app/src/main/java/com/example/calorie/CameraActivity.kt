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
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private var currentPhotoPath: String = ""

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_PICK_IMAGE = 2
        private const val PERMISSIONS_REQUEST_CAMERA = 100
        private const val PERMISSIONS_REQUEST_STORAGE = 101

        // Для FileProvider
        private const val FILE_PROVIDER_AUTHORITY = "com.example.calorie.fileprovider"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Простой интерфейс: две кнопки
        findViewById<android.widget.Button>(R.id.btnTakePhoto).setOnClickListener {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent()
            }
        }

        findViewById<android.widget.Button>(R.id.btnChooseFromGallery).setOnClickListener {
            if (checkStoragePermission()) {
                openGallery()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
            return false
        }
        return true
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSIONS_REQUEST_STORAGE)
                false
            } else {
                true
            }
        } else {
            // Android 12 и ниже
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_STORAGE)
                false
            } else {
                true
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(packageManager) == null) {
                Log.e("CameraActivity", "Нет приложения камеры!")
                Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
                return
            }
            takePictureIntent.resolveActivity(packageManager)?.also {
                photoFile = createImageFile()
                photoFile.also {
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        FILE_PROVIDER_AUTHORITY,
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
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

    private fun processCapturedImage(file: File) {
        // Сжатие и сохранение
        val compressedPath = compressAndSaveImage(file.absolutePath)
        if (compressedPath != null) {
            sendToApi(compressedPath)
            returnResult(compressedPath)
        } else {
            finish()
        }
    }

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
                    sendToApi(compressedPath)
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

            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            // Целевой размер — не более 1280px по большей стороне
            val targetSize = 1280
            val scaleFactor = if (imageWidth > imageHeight) {
                imageWidth / targetSize
            } else {
                imageHeight / targetSize
            }.coerceAtLeast(1)

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

    private fun sendToApi(imagePath: String) {
        // Заглушка: отправка на localhost
        Log.d("CameraActivity", "Отправка $imagePath на http://10.0.2.2:8000/api/upload")
        // TODO: Реализовать Retrofit/OkHttp запрос
    }

    private fun returnResult(imagePath: String) {
        val resultIntent = Intent().apply {
            putExtra("image_path", imagePath)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}