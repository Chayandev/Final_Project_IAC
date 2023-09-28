package com.example.cloudcounselagecompass.views.fragments

import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.Intent
import android.credentials.PrepareGetCredentialResponse
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.FragmentSignUpBinding
import com.example.cloudcounselagecompass.views.data.UserModelData
import com.example.cloudcounselagecompass.views.utils.GoogleSignInHelper
import com.example.cloudcounselagecompass.views.utils.ProgressBarUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.GoogleAuthProvider.getCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Properties

//import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {
    private var binding: FragmentSignUpBinding? = null
    private var isUserValid = false
    private var isEmailValid = false
    private var isPasswordValid = false

    //google sign in
    private lateinit var  googleSignInHelper: GoogleSignInHelper

    //fire base auth
    private lateinit var firebaseAuth: FirebaseAuth

    //real time data base
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = Firebase.auth
        database = Firebase.database.reference
        googleSignInHelper=GoogleSignInHelper(this, resources)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding!!.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.goSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
        binding!!.SignUpBtn.setOnClickListener {
            checkUserValidity()
            checkPasswordValidity()
            checkEmailValidity()
            if (isUserValid && isEmailValid && isPasswordValid) {
                val userName = binding!!.usernameEt.text.toString()
                val email = binding!!.emailEt.text.toString().trim()
                val password = binding!!.passwordEt.text.toString().trim()
                ProgressBarUtil.showCustomProgressBar(requireContext(),R.layout.custom_progress_bar)

                //fire-base sign up logic
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            //success
                            //email verification
                            firebaseAuth.currentUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Verification e-mail is send",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    saveData(binding!!.emailEt.text.toString().trim(),binding!!.usernameEt.text.toString().trim(),"")
                                    updateUi()
                                }?.addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(), it.toString(), Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ProgressBarUtil.cancelCustomProgressBar()
                        } else {
                            val exception = task.exception
                            if (exception is FirebaseAuthUserCollisionException) {
                                // Handle the case where the email is already in use
                                Toast.makeText(
                                    requireActivity(),
                                    "Email is already in use. Please use a different email.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (exception is FirebaseAuthWeakPasswordException) {
                                // Handle the case where the password is too weak
                                Toast.makeText(
                                    requireActivity(),
                                    "Weak password. Please choose a stronger password.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Handle other authentication errors
                                Toast.makeText(
                                    requireActivity(),
                                    "Authentication failed: ${exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ProgressBarUtil.cancelCustomProgressBar()
                        }
                    }
            } else {
                // Log.d("valid","IsUserValid:$isUserValid\t IsEmailValid:$isEmailValid\t IsPasswordValid:$isPasswordValid")
                Toast.makeText(
                    requireActivity(), "fill the form correctly", Toast.LENGTH_SHORT
                ).show()
              ProgressBarUtil.cancelCustomProgressBar()
            }
        }
        binding!!.backBtn.setOnClickListener {
           ProgressBarUtil.cancelCustomProgressBar()
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }

        binding!!.usernameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkUserValidity()
            }
        })
        binding!!.emailEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                checkUserValidity()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkEmailValidity()
            }
        })
        binding!!.passwordEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                checkUserValidity()
                checkEmailValidity()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkPasswordValidity()
            }
        })

        binding!!.confirmPasswordEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                checkUserValidity()
                checkEmailValidity()
                checkPasswordValidity()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkConfirmPasswordValidity()
            }

        })
        binding!!.google.setOnClickListener {
            ProgressBarUtil.showCustomProgressBar(requireContext(), R.layout.custom_progress_bar)
            googleSignInHelper.startGoogleSignIn { account ->
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            // Successfully signed in with Google
                            val user = firebaseAuth.currentUser
                            // Get user information from Google account
                            val googleEmail = account.email
                            val googleUserName = account.displayName
                            val googleDp=account.photoUrl.toString()
                            // Save user data to Firebase Realtime Database
                            if (user != null) {
                                saveData(googleUserName!!, googleEmail!!, googleDp)
                            }
                            //updateUI
                            findNavController().navigate(R.id.action_signUpFragment_to_homeActivity)
                           ProgressBarUtil.cancelCustomProgressBar()
                            requireActivity().finish()
                        } else {
                            // Google Sign-In failed
                            Toast.makeText(requireContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                            ProgressBarUtil.cancelCustomProgressBar()
                        }
                    }
            }
        }
    }
    private fun saveData(email:String,userName:String,picture:String) {
//        val email = binding!!.emailEt.text.toString().trim()
//        val userName = binding!!.usernameEt.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
       // val defaultImageUrl = getResourceUri(defaultImageResourceId)
        val user = UserModelData(userName,email,picture)

        database.child("User").child(userId).setValue(user).addOnCompleteListener{
            if(it.isSuccessful){
                Toast.makeText(requireContext(),"Account Details are saved successfully!",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(requireContext(),"Account Details Saving failed",Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateUi() {
        findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
      ProgressBarUtil.cancelCustomProgressBar()
    }

    //confirmation password validity check
    private fun checkConfirmPasswordValidity() {
        val confirmPasswordTv = binding!!.confirmPasswordEt.text.toString()
        if (confirmPasswordTv.isEmpty()) {
            binding!!.confirmPasswordEtLl.error = "Required"
            isPasswordValid = false
        } else if (binding!!.passwordEt.text.toString() != confirmPasswordTv) {
            binding!!.confirmPasswordEtLl.error = "Not matching"
            isPasswordValid = false
        } else {
            binding!!.confirmPasswordEtLl.error = null
            isPasswordValid = true
        }
        binding!!.confirmPasswordEtLl.errorIconDrawable = null
    }

    //initial password validity check
    private fun checkPasswordValidity() {
        val passwordText = binding!!.passwordEt.text.toString()
        if (passwordText.isEmpty()) {
            binding!!.passwordEtLl.error = "Required"
        } else if (passwordText.length < 6) {
            binding!!.passwordEtLl.error = "Minimum 6 Character Password"
        } else if (!passwordText.matches(".*[A-Z].*".toRegex())) {
            binding!!.passwordEtLl.error = "Must Contain 1 Upper-case Character"
        } else if (!passwordText.matches(".*[a-z].*".toRegex())) {
            binding!!.passwordEtLl.error = "Must Contain 1 Lower-case Character"
        } else if (!passwordText.matches(".*[@#\$%^&+=].*".toRegex())) {
            binding!!.passwordEtLl.error = "Must Contain 1 Special Character (@#\$%^&+=)"
        } else {
            binding!!.passwordEtLl.error = null
        }
        binding!!.passwordEtLl.errorIconDrawable = null
    }

    //Email validity check
    private fun checkEmailValidity() {
        val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        if (binding!!.emailEt.text!!.isEmpty()) {
            binding!!.emailEtLl.error = "Required"
            isEmailValid = false
        } else if (!binding!!.emailEt.text.toString().trim().matches(emailPattern)) {
            binding!!.emailEtLl.error = "Invalid Email"
            isEmailValid = false
        } else {
            binding!!.emailEtLl.error = null
            isEmailValid = true
        }
        binding!!.emailEtLl.errorIconDrawable = null
    }

    //username validity check
    private fun checkUserValidity() {
        if (binding!!.usernameEt.text!!.isEmpty()) {
            binding!!.usernameEtLl.error = "Required"
            isUserValid = false
        } else if (binding!!.usernameEt.text!!.length < 5) {
            binding!!.usernameEtLl.error = "User Name is too short!"
            isUserValid = false
        } else {
            binding!!.usernameEtLl.error = null
            isUserValid = true
        }
        binding!!.usernameEtLl.errorIconDrawable = null
    }
}