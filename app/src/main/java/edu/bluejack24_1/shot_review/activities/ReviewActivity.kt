package edu.bluejack24_1.shot_review.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_1.shot_review.databinding.ActivityReviewBinding
import edu.bluejack24_1.shot_review.models.CoffeeShops
import edu.bluejack24_1.shot_review.models.Review
import java.util.UUID

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var fAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Ambil data user untuk menampilkan username dan setup submit button
        val currentUser = fAuth.currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val username = document.getString("username") ?: "Unknown User"
                        binding.usernameReview.text = "$username's Review"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error getting user data", e)
                }

            // Setup action saat submit button ditekan
            binding.submitBtn.setOnClickListener {
                val ratingText = binding.etRating.text.toString()
                val description = binding.etDescription.text.toString()

                // Validasi rating harus integer dan berada dalam range 1-5
                if (TextUtils.isEmpty(ratingText) || !isInteger(ratingText)) {
                    Toast.makeText(this, "Rating harus angka bulat (integer)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val rating = ratingText.toInt()

                if (rating < 1 || rating > 5) {
                    Toast.makeText(this, "Rating harus antara 1 hingga 5", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Validasi deskripsi tidak boleh kosong
                if (TextUtils.isEmpty(description)) {
                    Toast.makeText(this, "Deskripsi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Validati legnth dekripsi
                if (description.length < 5) {
                    Toast.makeText(this, "Deskripsi harus lebih dari 5 karakter", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val coffeeShopId = intent.getStringExtra("coffeeShopId")

                if (coffeeShopId.isNullOrEmpty()) {
                    Toast.makeText(this, "Coffee shop ID tidak valid", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val reviewId = UUID.randomUUID().toString()
                val review = Review(reviewId, user.uid, coffeeShopId, rating, description)

                // Simpan review ke Firestore
                db.collection("Review")
                    .document(reviewId)
                    .set(review)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Review berhasil ditambahkan", Toast.LENGTH_SHORT).show()

                        // Update CoffeeShops with the new review
                        val coffeeShopRef = db.collection("coffeeShops").document(coffeeShopId)
                        coffeeShopRef.update("reviews", FieldValue.arrayUnion(review))
                            .addOnSuccessListener {
                                Log.d("Firestore", "CoffeeShop reviews updated successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error updating CoffeeShop reviews", e)
                            }

                        finish()  // Return to coffee shop detail
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Firebase", "Error adding review", e)
                    }
            }
        }

        // Handle back button
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    // Fungsi validasi apakah input rating adalah integer
    private fun isInteger(str: String): Boolean {
        return try {
            str.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
}
