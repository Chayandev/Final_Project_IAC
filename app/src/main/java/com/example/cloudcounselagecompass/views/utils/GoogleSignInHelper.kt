package com.example.cloudcounselagecompass.views.utils

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.cloudcounselagecompass.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.io.IOException
import java.util.Properties

class GoogleSignInHelper(private val fragment: Fragment, private val resources: Resources) {
    private val RC_SIGN_IN = 123 // Request code for Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var webClientId: String
    private var onSuccessCallback: ((GoogleSignInAccount) -> Unit)? = null
    init {
        initWebClientId()
        setupGoogleSignIn()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(fragment.requireActivity(), gso)

        googleSignInLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data = result.data
                if (result.resultCode == Activity.RESULT_OK && data != null) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    handleGoogleSignInResult(task)
                } else {
                    // Handle the case where Google Sign-In was canceled or failed
                    Toast.makeText(
                        fragment.requireContext(),
                        "Google Sign-In canceled or failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    ProgressBarUtil.cancelCustomProgressBar()
                }
            }
    }

    private fun initWebClientId() {
        val inputStream = resources.openRawResource(R.raw.app_config)
        val properties = Properties()
        try {
            properties.load(inputStream)
            webClientId = properties.getProperty("firebase_web_client_id")
        } catch (e: IOException) {
            // Handle the exception if the app_config file cannot be read
            webClientId = ""
            e.printStackTrace()
        }
    }

    fun startGoogleSignIn(onSuccess: (GoogleSignInAccount) -> Unit) {
        onSuccessCallback = onSuccess
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        //ProgressBarUtil.showCustomProgressBar(fragment.requireContext(),R.layout.custom_progress_bar)
        try {
            val account = completedTask.getResult(ApiException::class.java)
            onSuccessCallback?.invoke(account) // Invoke the callback function
        } catch (e: ApiException) {
            // Google Sign-In failed
            Toast.makeText(fragment.requireContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            ProgressBarUtil.cancelCustomProgressBar()
        }
    }
}
