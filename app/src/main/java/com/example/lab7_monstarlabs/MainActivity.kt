package com.example.lab7_monstarlabs

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lab7_monstarlabs.activity.Home
import com.example.lab7_monstarlabs.service.ServiceAudio
import kotlinx.coroutines.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var storagePermission: Array<String>
    private val READ_STORAGE_REQUEST_CODE: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        storagePermission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        when (checkPermission()) {
            true -> {
                Intent(this, ServiceAudio::class.java).also {
                    startService(it)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        Intent(this@MainActivity, Home::class.java).also {
                            startActivity(it)
                            finish()
                        }
                    }
                }
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    storagePermission,
                    READ_STORAGE_REQUEST_CODE
                )
            }
        }
    }



    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_REQUEST_CODE) {
            when {
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    Intent(this, ServiceAudio::class.java).also {
                        startService(it)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(2000)
                        withContext(Dispatchers.Main) {
                            Intent(this@MainActivity, Home::class.java).also {
                                startActivity(it)
                                finish()
                            }
                        }
                    }
                }
                else -> ActivityCompat.requestPermissions(
                    this,
                    storagePermission,
                    READ_STORAGE_REQUEST_CODE
                )
            }
        }
    }
}