package edu.bluejack24_1.shot_review.activities

import android.content.Intent
import android.os.Bundle
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.bluejack24_1.shot_review.databinding.ActivityRegisterBinding
import edu.bluejack24_1.shot_review.models.Users
import com.google.firebase.auth.FirebaseAuth

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
//        FirebaseFirestore.setLoggingEnabled(true)
//        fAuth.setLanguageCode("en")

//        Handle Login
        binding.btnLogin.setOnClickListener {
            val intentToLogin = Intent(this, LoginActivity::class.java)
            startActivity(intentToLogin)
        }

//        Handle Register
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (username.isNotEmpty() || email.isNotEmpty() || password.isNotEmpty() || confirmPassword.isNotEmpty()) {
                if (password.length >= 6) {
                    if (password == confirmPassword) {
                        // ini create ke auth
                        fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                            if (it.isSuccessful) {
                                // Get data from object
                                val user = Users (
                                    userId = fAuth.currentUser?.uid?:"",
                                    username = username,
                                    email = email,
                                    password = password,
                                    profilePicture = "",
                                    bio = "",
                                )

                                // Save ke firestornya
                                db.collection("users").document(user.userId).set(user).addOnCompleteListener {
                                    val intentToLogin = Intent(this, LoginActivity::class.java)
                                    startActivity(intentToLogin)
                                }
                            } else {
                                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all the fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}