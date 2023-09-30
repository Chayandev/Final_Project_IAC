package com.example.cloudcounselagecompass.views.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.cloudcounselagecompass.R
import com.example.cloudcounselagecompass.databinding.FragmentProfileBinding
import com.example.cloudcounselagecompass.views.activities.AuthActivity
import com.example.cloudcounselagecompass.views.utils.ProgressBarUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.Properties


// TODO: Rename parameter arguments, choose names that match

class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var binding: FragmentProfileBinding? = null
    private lateinit var database: DatabaseReference
    private var userId: String? = null
    private var customProgressDialog: Dialog? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var imageUri: Uri? = null

    //image uri setup
    private val selectImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            imageUri = it
            binding!!.userImage.setImageURI(imageUri)
            uploadProfileImage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = Firebase.auth
        database = Firebase.database.reference
        userId = FirebaseAuth.getInstance().currentUser!!.uid

        //get the cliend id
        val inputStream = resources.openRawResource(R.raw.app_config)
        val properties = Properties()
        properties.load(inputStream)
        val webClientId = properties.getProperty("firebase_web_client_id")


        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId) // Your web client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        binding!!.logoutBtn.setOnClickListener {
            Firebase.auth.signOut()
            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener {
                // Redirect to the sign-in page or handle it as needed
                val intent = Intent(requireActivity(), AuthActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
        binding!!.backBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding!!.profilePicEditBtn.setOnClickListener {
            selectImage.launch("image/*")
        }
    }

    private fun updateUI() {
        //fetching data from database
        ProgressBarUtil.showCustomProgressBar(requireContext(),R.layout.custom_progress_bar)
        database.child("User").child(userId!!).get().addOnSuccessListener {
            val name = it.child("name").value.toString()
            val eMail = it.child("email").value.toString()
            val profileImageUrl = it.child("image").value.toString()

            //set the fetched data
            binding!!.usernameTv.text = name
            binding!!.emailTv.text = eMail
            // Load the profile picture using Firebase
            if (profileImageUrl.isNotEmpty() && profileImageUrl != null) {
                // Load and display the image using Glide
                Glide.with(requireContext())
                    .load(profileImageUrl) // Provide the URL of the image
                    .placeholder(R.drawable.boy) // Placeholder image while loading
                    .error(R.drawable.boy) // Error image if the load fails
                    .into(binding!!.userImage) // ImageView to display the image
                    ProgressBarUtil.cancelCustomProgressBar()
            } else {
                // If the profile picture URL is empty, you can set a default image here.
                binding!!.userImage.setImageResource(R.drawable.boy)
                ProgressBarUtil.cancelCustomProgressBar()
            }
        }.addOnFailureListener {
            Toast.makeText(requireActivity(), it.toString(), Toast.LENGTH_SHORT).show()
            ProgressBarUtil.cancelCustomProgressBar()
        }
    }

    private fun uploadProfileImage() {
        // Ensure that imageUri is not null before proceeding with the upload
        if (imageUri != null) {
            ProgressBarUtil.showCustomProgressBar(requireContext(),R.layout.custom_progress_bar)
            // Upload the image to Firebase Storage and update the user's profile picture URL in Firebase Realtime Database
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val storageRef = FirebaseStorage.getInstance().getReference("profile").child(userId!!)
                .child("Profile.jpg")

            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        storeImage(imageUrl)
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireActivity(), "Failed to get image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    ProgressBarUtil.cancelCustomProgressBar()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireActivity(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                ProgressBarUtil.cancelCustomProgressBar()
                }
        } else {
            // Handle the case where imageUri is null (no image selected)
            Toast.makeText(requireActivity(), "Please select an image to upload.", Toast.LENGTH_SHORT).show()
          //  uploadProfileImage()
        }
    }


    private fun storeImage(imageUrl: Uri?) {
        val updateImage = hashMapOf<String, Any>(
            "image" to imageUrl.toString()
        )
        database.child("User").child(userId!!).updateChildren(updateImage)
            .addOnSuccessListener {
            Toast.makeText(requireActivity(),"Profile Picture is Updated Successfully!",Toast.LENGTH_SHORT).show()
                ProgressBarUtil.cancelCustomProgressBar()
        }
            .addOnFailureListener { error ->
                Toast.makeText(requireActivity(),error.message,Toast.LENGTH_SHORT).show()
                ProgressBarUtil.cancelCustomProgressBar()
            }
    }
}