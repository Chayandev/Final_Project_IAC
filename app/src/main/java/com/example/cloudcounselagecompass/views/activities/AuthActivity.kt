package com.example.cloudcounselagecompass.views.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.ActivityAuthBinding
import com.example.cloudcounselagecompass.views.fragments.SignInFragment

class AuthActivity : AppCompatActivity(){
    private lateinit var binding: ActivityAuthBinding
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding=ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the NavHostFragment from the layout
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.auth_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController



    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}