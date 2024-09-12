package edu.bluejack24_1.shot_review.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import edu.bluejack24_1.shot_review.R
import edu.bluejack24_1.shot_review.activities.MainActivity
import edu.bluejack24_1.shot_review.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var db: FirebaseFirestore
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val intentUID = arguments?.getString("uid")!!
        Log.d("EditProfileFragment", "$intentUID")

        val docRef = db.collection("users").document(intentUID.toString())

        docRef.get().addOnSuccessListener {
            if (it.exists()) {
                val username = it.getString("username")
                val bio = it.getString("bio")
                val profilePictureUrl = it.getString("profilePicture")

                Log.d("username", username.toString())
                binding.userName.text = username
                binding.userBio.setText(bio) // Set the bio as the text in the EditText

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
                hide(this@EditProfileFragment)
                addToBackStack(null)
            }.commit()

            // Update bottom navigation selected item
            (activity as MainActivity).binding.botNav.selectedItemId = R.id.menuHome
        }

        binding.editIcon.setOnClickListener {
            openFileChooser()
        }

        binding.saveBtn.setOnClickListener {
            saveProfile(intentUID)
        }

        return binding.root
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            // Preview the selected image
            Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .into(binding.profilePicture)
        }
    }

    private fun saveProfile(uid: String?) {
        val updatedBio = binding.userBio.text.toString()

        if (imageUri != null) {
            // If a new image has been selected, upload it first
            val fileReference = storage.reference.child("profilePictures/${System.currentTimeMillis()}.jpg")
            fileReference.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        // Update both bio and profile picture URL
                        updateProfile(uid, updatedBio, imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("EditProfileFragment", "Error uploading image", e)
                    Toast.makeText(requireContext(), "Error uploading image", Toast.LENGTH_SHORT).show()
                }
        } else {
            // If no new image has been selected, only update the bio
            updateProfile(uid, updatedBio, null)
        }
    }

    private fun updateProfile(uid: String?, updatedBio: String, profilePictureUrl: String?) {
        val docRef = db.collection("users").document(uid!!)

        val updates = hashMapOf<String, Any>(
            "bio" to updatedBio
        )

        if (profilePictureUrl != null) {
            updates["profilePicture"] = profilePictureUrl
        }

        docRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.showProfileFragment() // Navigate back to profile fragment
            }
            .addOnFailureListener { e ->
                Log.w("EditProfileFragment", "Error updating profile", e)
                Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show()
            }
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
}
