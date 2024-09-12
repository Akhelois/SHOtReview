package edu.bluejack24_1.shot_review.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_1.shot_review.R
import edu.bluejack24_1.shot_review.activities.LoginActivity
import edu.bluejack24_1.shot_review.activities.MainActivity
import edu.bluejack24_1.shot_review.databinding.FragmentProfileBinding
import java.util.concurrent.Executor

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var db: FirebaseFirestore

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        val intentUID = arguments?.getString("uid")!!
        Log.d("ProfileFragment", "${arguments?.getString("uid")}")

        val docRef = db.collection("users").document(intentUID.toString())

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("ProfileFragment", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val username = snapshot.getString("username")
                val bio = snapshot.getString("bio")
                val profilePictureUrl = snapshot.getString("profilePicture")

                Log.d("username", username.toString())
                binding.userName.text = username
                binding.userBio.text = bio

                Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.placeholder_profile_picture)
                    .error(R.drawable.placeholder_profile_picture)
                    .circleCrop()
                    .into(binding.profilePicture)

                // Fetch the total number of reviews by the user
                fetchTotalReviews(intentUID)
            } else {
                Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Load the saved biometric preference state
        val sharedPref = requireContext().getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)
        val isBiometricEnabled = sharedPref.getBoolean("biometric_enabled", false)

        // Set the ToggleButton state based on the saved preference
        binding.biometricBtn.isChecked = isBiometricEnabled

        // Initialize BiometricPrompt
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                // Check if the user is logged in after biometric authentication
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    // Fetch user data after successful biometric login
                    val userUID = currentUser.uid
                    fetchUserData(userUID)
                } else {
                    Toast.makeText(requireContext(), "Authentication failed. Please log in manually.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                binding.biometricBtn.isChecked = false
                Toast.makeText(requireContext(), "Biometric authentication failed.", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                binding.biometricBtn.isChecked = false
                Toast.makeText(requireContext(), "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        binding.biometricBtn.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPref.edit()) {
                putBoolean("biometric_enabled", isChecked)
                apply()
            }

            if (isChecked) {
                // Check if the device supports biometric authentication
                val biometricManager = BiometricManager.from(requireContext())
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                    // Show the biometric prompt to the user
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    // If biometric is not supported or not available, uncheck the ToggleButton
                    binding.biometricBtn.isChecked = false
                    with(sharedPref.edit()) {
                        putBoolean("biometric_enabled", false)
                        apply()
                    }
                }
            }
        }

        // Mengatur logo untuk kembali ke home
        binding.ivLogo.setOnClickListener {
            // Navigate to HomeFragment
            parentFragmentManager.beginTransaction().apply {
                var homeFragment = parentFragmentManager.findFragmentByTag("HomeFragment") as? HomeFragment
                if (homeFragment == null) {
                    homeFragment = HomeFragment()
                    add(R.id.fragmentContainer, homeFragment, "HomeFragment")
                } else {
                    show(homeFragment)
                }
                hide(this@ProfileFragment)
                addToBackStack(null)
            }.commit()

            // Update bottom navigation selected item
            (activity as MainActivity).binding.botNav.selectedItemId = R.id.menuHome
        }

        binding.btnEditProfile.setOnClickListener {
            (activity as? MainActivity)?.showEditProfileFragment()
        }

        binding.logoutBtn.setOnClickListener {
            // Clear shared preferences for biometric on logout
            with(sharedPref.edit()) {
                putBoolean("biometric_enabled", false)
                apply()
            }

            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            // Navigate to login activity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return binding.root

    }

    private fun fetchTotalReviews(userId: String) {
        db.collection("Review")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                Log.d("ProfileFragment", "Query result: ${result.documents}")
                val reviewCount = result.size()
                Log.d("ProfileFragment", "Total reviews for userId $userId: $reviewCount")
                binding.userNumber.text = reviewCount.toString()
            }
            .addOnFailureListener { exception ->
                Log.w("ProfileFragment", "Error getting documents.", exception)
            }
    }

    private fun fetchUserData(userUID: String) {
        val docRef = db.collection("users").document(userUID)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("ProfileFragment", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val username = snapshot.getString("username")
                val bio = snapshot.getString("bio")
                val profilePictureUrl = snapshot.getString("profilePicture")

                binding.userName.text = username
                binding.userBio.text = bio

                Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.placeholder_profile_picture)
                    .error(R.drawable.placeholder_profile_picture)
                    .circleCrop()
                    .into(binding.profilePicture)

                fetchTotalReviews(userUID)
            } else {
                Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}