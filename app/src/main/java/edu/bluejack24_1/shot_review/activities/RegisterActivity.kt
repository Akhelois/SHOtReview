package edu.bluejack24_1.shot_review.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_1.shot_review.databinding.ActivityRegisterBinding
import edu.bluejack24_1.shot_review.models.Users

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var fAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Handle Login
        binding.btnLogin.setOnClickListener {
            val intentToLogin = Intent(this, LoginActivity::class.java)
            startActivity(intentToLogin)
        }

        // Handle Register
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password.length >= 6 && username.length > 3) {
                    if (password == confirmPassword) {
                        // Check if email already exists in Firestore
                        db.collection("users").whereEqualTo("email", email).get().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (task.result.isEmpty) {
                                    // Email doesn't exist, proceed with registration
                                    fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
                                        if (authTask.isSuccessful) {
                                            // Create Users object
                                            val user = Users(
                                                userId = fAuth.currentUser?.uid ?: "",
                                                username = username,
                                                email = email,
                                                password = password,
                                                profilePicture = "",
                                                bio = ""
                                            )

                                            // Save user to Firestore
                                            db.collection("users").document(user.userId).set(user).addOnCompleteListener {
                                                showDialog("Registration Successful", "Your account has been created successfully.") {
                                                    val intentToLogin = Intent(this, LoginActivity::class.java)
                                                    startActivity(intentToLogin)
                                                }
                                            }
                                        } else {
                                            showDialog("Registration Failed", authTask.exception?.message ?: "An unknown error occurred.")
                                        }
                                    }
                                } else {
                                    // Email already exists
                                    showDialog("Error", "Email is already registered!")
                                }
                            } else {
                                showDialog("Error", "Failed to check email uniqueness. Please try again.")
                            }
                        }
                    } else {
                        showDialog("Error", "Passwords do not match!")
                    }
                } else {
                    showDialog("Error", "User must more than 3 characters or Password must be at least 6 characters!")
                }
            } else {
                showDialog("Error", "Please fill in all the fields!")
            }
        }
    }

    // Function to show alert dialog
    private fun showDialog(title: String, message: String, onPositiveAction: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onPositiveAction?.invoke()
        }
        builder.create().show()
    }
}
