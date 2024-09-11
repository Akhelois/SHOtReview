package edu.bluejack24_1.shot_review.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import edu.bluejack24_1.shot_review.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var fAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Proceed to MainActivity after successful biometric authentication
                navigateToMainActivity()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@LoginActivity, "Biometric Authentication Failed", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
//                Toast.makeText(this@LoginActivity, "Authentication Error: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        // Check jka biometric on di profilepage
        val sharedPref = getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)
        val isBiometricEnabled = sharedPref.getBoolean("biometric_enabled", false)

        if (isBiometricEnabled) {
            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                // Show the biometric prompt
                biometricPrompt.authenticate(promptInfo)
            } else {
                Toast.makeText(this, "Biometric Authentication is not available.", Toast.LENGTH_SHORT).show()
            }
        }

//        Handle Register
        binding.btnRegister.setOnClickListener {
            val intentToRegister = Intent(this, RegisterActivity::class.java)
            startActivity(intentToRegister)
        }

//        Handle Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() || password.isNotEmpty()) {
                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
//                        Toast.makeText(this,fAuth.currentUser?.uid.toString(), Toast.LENGTH_SHORT).show()
                        showDialog("Login Successful", "You have successfully logged in.") {
                            val intentToMain = Intent(this, MainActivity::class.java)
                            intentToMain.putExtra("uid", fAuth.currentUser?.uid.toString())
                            startActivity(intentToMain)
                            finish()
                        }
                    } else {
//                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        showDialog("Login Failed", "Invalid credentials. Please try again.")
                    }
                }
            } else {
                Toast.makeText(this, "Please fill the fiels!!", Toast.LENGTH_SHORT).show()
            }
        }

//        Handle Fingerprint Button Click
        binding.ivFingerprint.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

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

    // function redirect ke mainactivity
    private fun navigateToMainActivity() {
        val intentToMain = Intent(this, MainActivity::class.java)
        intentToMain.putExtra("uid", fAuth.currentUser?.uid.toString())
        startActivity(intentToMain)
        finish()
    }

    private fun saveBiometricPreference(isEnabled: Boolean) {
        val sharedPref = getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("biometric_enabled", isEnabled)
            apply()
        }
    }

//    override fun onStart() {
//        super.onStart()
//
//        if (fb.currentUser != null) {
//            val intentToMain = Intent(this, MainActivity::class.java)
//            startActivity(intentToMain)
//            finish()
//        }
//    }
}