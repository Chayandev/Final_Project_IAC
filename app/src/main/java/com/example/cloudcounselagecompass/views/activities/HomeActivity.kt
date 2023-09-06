package com.example.cloudcounselagecompass.views.activities

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.ActivityAuthBinding
import com.example.cloudcounselagecompass.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private  var binding:ActivityHomeBinding?=null
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Create a gradient drawable for the status bar background
        val gradientDrawable = ContextCompat.getDrawable(this, R.drawable.gradient_rectangular_bg) as Drawable
        val window: Window = window
//        window.setBackgroundDrawable(gradientDrawable)
//        window.statusBarColor = ContextCompat.getColor(this,R.color.transparent)
        val navHostFragment=supportFragmentManager.findFragmentById(R.id.home_nav_host_fragment) as NavHostFragment
        navController=navHostFragment.navController

    }
    // Handle Up button press to navigate back
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}