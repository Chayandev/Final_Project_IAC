package com.example.cloudcounselagecompass.views.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.ActivitySplashScreenBinding
import java.util.Timer
import java.util.TimerTask


@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding:ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Change status bar color
        // Create a gradient drawable for the status bar background
        val gradientDrawable = ContextCompat.getDrawable(this, R.drawable.gradient_rectangular_bg) as Drawable
        val window: Window = window
        window.setBackgroundDrawable(gradientDrawable)
        window.statusBarColor = ContextCompat.getColor(this,R.color.transparent) // Use your desired color here
        window.navigationBarColor=ContextCompat.getColor(this,R.color.transparent)

        val animation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        binding.logoImgView.startAnimation(animation)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                startNewActivity()
            }
        }, 1100

        )
    }  private fun startNewActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

}