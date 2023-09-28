package com.example.cloudcounselagecompass.views.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpViewModel: ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signUp(email: String, password: String): FirebaseUser? {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).result?.user
    }
}