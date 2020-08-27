package com.dontsu.asynctask

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    init {
        //timber initialize
        Timber.plant(Timber.DebugTree())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (checkPermission(STORAGE_PERMISSION, FLAG_PERM_STORAGE)) {
            downloadBtn.setOnClickListener {
                val asyncTask = object : AsyncTask<String, Void, Bitmap>() {
                    override fun doInBackground(vararg params: String?): Bitmap? {
                        val urlString = params[0]
                        try {
                            val url = URL(urlString)
                            val stream = url.openStream()
                            return BitmapFactory.decodeStream(stream)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return null
                        }
                    }

                    override fun onProgressUpdate(vararg values: Void?) {
                        super.onProgressUpdate(*values)
                    }

                    override fun onPostExecute(result: Bitmap?) {
                        super.onPostExecute(result)

                        if (result != null) {
                            imagePrev.setImageBitmap(result)
                            saveImageFile(newFileName(), "image/jpg", result)
                        } else{
                            Toast.makeText(this@MainActivity, "다운로드 오류", Toast.LENGTH_SHORT).show()
                        }
                    }

                }

                asyncTask?.execute(urlET.text.toString())
            }
        }

    }


    private fun saveImageFile(filename: String, mimeType: String, bitmap: Bitmap) : Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        try {
            if (uri != null) {
                var descriptor = contentResolver.openFileDescriptor(uri, "w")
                if (descriptor != null) {
                    val fos = FileOutputStream(descriptor.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100 ,fos)
                    fos.close()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("saveImageFile, error = ${e.localizedMessage}")
        }

        return uri
    }

    private fun newFileName() : String {
        val sdf = SimpleDateFormat("yyyy/MM/dd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())

        return "$filename.jpg"
    }

    private fun checkPermission(permissions: Array<out String>, flag: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                val permissionState = ContextCompat.checkSelfPermission(this@MainActivity, permission)
                if (permissionState != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, permissions, flag)
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            FLAG_PERM_STORAGE -> {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this@MainActivity, "저장소 권한을 승인해야만 합니다!", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }
    }

    companion object {
        val STORAGE_PERMISSION = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        const val FLAG_PERM_STORAGE = 200
        const val FLAG_REQ_STORAGE = 111
    }

}
