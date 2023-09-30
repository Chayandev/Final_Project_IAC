package com.example.cloudcounselagecompass.views.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.FragmentSignInBinding
import com.example.cloudcounselagecompass.databinding.ResetPaswordDialogBinding
import com.example.cloudcounselagecompass.views.data.UserModelData
import com.example.cloudcounselagecompass.views.utils.GoogleSignInHelper
import com.example.cloudcounselagecompass.views.utils.ProgressBarUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignInFragment : Fragment() {
    private var binding: FragmentSignInBinding? = null
    private var isEmailValid = false
    private var isPasswordValid = false
    private lateinit var rpdBinding: ResetPaswordDialogBinding
    private lateinit var resetPasswordDialog: Dialog

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    //google log in
   private lateinit var  googleSignInHelper:GoogleSignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = Firebase.auth
        database = Firebase.database.reference
        googleSignInHelper=GoogleSignInHelper(this, resources)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.goSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_sign_in_to_sign_up)
        }

        binding!!.loginBtn.setOnClickListener {
            checkEmailValidity()
            checkPasswordValidity()
            ProgressBarUtil.showCustomProgressBar(requireContext(), R.layout.custom_progress_bar)
            val email = binding!!.emailEt.text.toString().trim()
            val password = binding!!.passwordEt.text.toString().trim()
            if (isEmailValid && isPasswordValid) {

                //log in
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val verification = firebaseAuth.currentUser?.isEmailVerified
                            if (verification == true) {
                                updateUi()
                            } else {
                                Toast.makeText(
                                    requireActivity(),
                                    "You haven't verified your Email!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ProgressBarUtil.cancelCustomProgressBar()
                        } else {
                            val exception = task.exception
                            if (exception is FirebaseAuthInvalidCredentialsException) {
                                // Handle the case where the credentials are invalid
                                Toast.makeText(
                                    requireActivity(),
                                    "Invalid login credentials. Please check your email and password.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                ProgressBarUtil.cancelCustomProgressBar()
                            } else {
                                // Handle other authentication errors
                                Toast.makeText(
                                    requireActivity(),
                                    "Authentication failed: ${exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                ProgressBarUtil.cancelCustomProgressBar()
                            }

                        }
                    }
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Fill the required fields correctly",
                    Toast.LENGTH_SHORT
                )
                    .show()
                ProgressBarUtil.cancelCustomProgressBar()
            }
        }
        binding!!.emailEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkEmailValidity()
            }
        })
        binding!!.passwordEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                checkEmailValidity()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkPasswordValidity()
            }
        })
        binding!!.forgotTxtClickable.setOnClickListener {
            // Initialize the dialog
            resetPasswordDialog = Dialog(requireContext())
            rpdBinding = ResetPaswordDialogBinding.inflate(layoutInflater)
            resetPasswordDialog.setContentView(rpdBinding.root)
            showResetPasswordDialog()
            //handel the send btn click
            rpdBinding.btnSend.setOnClickListener {
                val verifiedEmail = rpdBinding.emailEtv.text.toString()
                firebaseAuth.sendPasswordResetEmail(verifiedEmail)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "To reset password check email:$verifiedEmail",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnFailureListener {
                        Toast.makeText(requireActivity(), "Failed:$it", Toast.LENGTH_SHORT).show()
                    }
                resetPasswordDialog.dismiss()
            }
            //handel the cancel btn click
            rpdBinding.btnCancel.setOnClickListener {
                resetPasswordDialog.dismiss()
            }
        }
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
                            updateUi()
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
    private fun saveData(email: String, userName: String,picture:String) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        // val defaultImageUrl = getResourceUri(defaultImageResourceId)
        val user = UserModelData(userName, email ,picture)

        database.child("User").child(userId).setValue(user).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(
                    requireActivity(),
                    "Account Details are saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Account Details Saving failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun showResetPasswordDialog() {
        //val dialogLayout = rpdBinding.root
        resetPasswordDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // resetPasswordDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Show the dialog
        resetPasswordDialog.show()
        resetPasswordDialog.setCancelable(false)
    }


    private fun updateUi() {
        findNavController().navigate(R.id.action_signInFragment_to_homeActivity)
        requireActivity().finish()
        ProgressBarUtil.cancelCustomProgressBar()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            if (findNavController().getBackStackEntry(R.id.signInFragment).destination.id != R.id.homeActivity)
                updateUi()
            else
                requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun checkPasswordValidity() {
        val passwordText = binding!!.passwordEt.text.toString()
        if (passwordText.isEmpty()) {
            binding!!.passwordEtLl.error = "Required"
            isPasswordValid = false
        } else if (passwordText.length < 6) {
            binding!!.passwordEtLl.error = "Minimum 6 Character Password"
            isPasswordValid = false
        } else if (!passwordText.matches(".*[A-Z].*".toRegex())) {
            binding!!.passwordEtLl.error = "Must Contain 1 Upper-case Character"
            isPasswordValid = false
        } else if (!passwordText.matches(".*[a-z].*".toRegex())) {
            binding!!.passwordEtLl.error = "Must Contain 1 Lower-case Character"
            isPasswordValid = false
        } else if (!passwordText.matches(".*[@#\$%^&+=].*".toRegex())) {
            binding!!.passwordEtLl.error = "Must Contain 1 Special Character (@#\$%^&+=)"
            isPasswordValid = false
        } else {
            binding!!.passwordEtLl.error = null
            isPasswordValid = true
        }
        binding!!.passwordEtLl.errorIconDrawable = null
    }

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
}