package com.example.exercises7_monstarlabs

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.exercises7_monstarlabs.activity.Home
import com.example.exercises7_monstarlabs.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()
        val animatorImg = ObjectAnimator.ofFloat(binding.imgLogo, View.TRANSLATION_Y, 999f, 0f)
        animatorImg.duration = 3000L
        animatorImg.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
                binding.layoutCircle.visibility = View.VISIBLE
                val scaleXLayout = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 500f)
                val scaleYLayout = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 500f)
                val animatorLayout = ObjectAnimator.ofPropertyValuesHolder(
                    binding.layoutCircle,
                    scaleXLayout,
                    scaleYLayout
                )
                animatorLayout.addListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                        super.onAnimationEnd(animation, isReverse)
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(1500)
                            withContext(Dispatchers.Main){
                                val intent = Intent(this@MainActivity,Home::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                })
                animatorLayout.duration = 3000L
                animatorLayout.start()
            }
        })
        animatorImg.start()

    }

}