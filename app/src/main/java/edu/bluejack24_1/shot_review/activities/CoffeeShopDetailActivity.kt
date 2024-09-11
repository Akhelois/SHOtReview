package edu.bluejack24_1.shot_review.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginRight
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_1.shot_review.R
import edu.bluejack24_1.shot_review.databinding.ActivityCoffeeShopDetailBinding
import edu.bluejack24_1.shot_review.fragments.HomeFragment
import edu.bluejack24_1.shot_review.models.CoffeeShops
import edu.bluejack24_1.shot_review.models.Favorite
import edu.bluejack24_1.shot_review.models.Review
import java.util.UUID

class CoffeeShopDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoffeeShopDetailBinding
    private lateinit var fAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isFavorited = false

    companion object {
        private const val CALL_PHONE_PERMISSION_REQUEST_CODE = 1
    }

    // Fungsi ekstensi untuk mengonversi dp ke piksel
    fun Int.dp(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoffeeShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val data = intent.getParcelableExtra<CoffeeShops>("DATA")
        if (data == null) {
            Log.e("CoffeeShopDetail", "Data not found in intent!")
            finish()
            return
        }

        // Set detail kedai kopi
        Glide.with(binding.coffeeShopImage).load(data?.coffeeShopPicture).into(binding.coffeeShopImage)
        binding.coffeeShopName.text = data?.coffeeShopName
        binding.coffeeShopAddress.text = data?.coffeeShopAddress
        binding.coffeeShopPhoneNumber.text = data?.coffeeShopPhoneNumber
        binding.coffeeShopTime.text = data?.coffeeShopTime
        binding.ratings.text = data?.getAverageRating().toString()

        val facilitiesContainer = binding.publicFacilitiesContainer
        facilitiesContainer.removeAllViews()

        if (data?.coffeeShopPublicFacilities.isNullOrEmpty()) {
            val noFacilitiesView = TextView(this)
            noFacilitiesView.text = "No public facilities available"
            facilitiesContainer.addView(noFacilitiesView)
        } else {
            data?.coffeeShopPublicFacilities?.forEach { facility ->
                val facilityView = TextView(this)
                facilityView.text = facility
                val layout = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layout.marginEnd = 10.dp(this)
                layout.bottomMargin = 8.dp(this)
                facilityView.layoutParams = layout
                facilityView.setTextColor(ContextCompat.getColor(this, R.color.gray))
                facilityView.textSize = 14f
                facilitiesContainer.addView(facilityView)
            }
        }

        // Memuat galeri kedai kopi
        data?.coffeeShopGallery?.forEach { imageUrl ->
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                180.dp(this)
            )
            layoutParams.marginEnd = 16.dp(this)
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            Glide.with(this).load(imageUrl).into(imageView)
            binding.coffeeShopGalleryContainer.addView(imageView)
        }

        // Memuat menu kedai kopi
        if (!data?.coffeeShopMenu.isNullOrEmpty()) {
            Glide.with(binding.menuImage).load(data?.coffeeShopMenu).into(binding.menuImage)
        } else {
            binding.menuImage.visibility = View.GONE
        }

        // Cek apakah kedai kopi sudah di-favorite oleh user saat ini ama review
        data?.let { coffeeShop ->
            checkIfFavoritedByCurrentUser(coffeeShop.coffeeShopId)
            loadReviews(coffeeShop.coffeeShopId)
//            checkIfUserReviewed(coffeeShop.coffeeShopId)
        }

        // Menangani klik pada nomor telepon
        binding.coffeeShopPhoneNumber.setOnClickListener {
            val phoneNumber = binding.coffeeShopPhoneNumber.text.toString()
            val intentToCallNumber = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intentToCallNumber)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_PERMISSION_REQUEST_CODE)
            }
        }

        // Menangani klik pada ikon favorite
        binding.favoriteIcon.setOnClickListener {
            isFavorited = !isFavorited
            updateFavoriteIcon(isFavorited)

            val userId = fAuth.currentUser?.uid ?: "" // Mendapatkan ID user saat ini

            if (isFavorited) {
                // Menambahkan favorite ke Firestore
                val favoriteId = UUID.randomUUID().toString()
                val favorite = Favorite(favoriteId, userId, data?.coffeeShopId ?: "")

                db.collection("favorites")
                    .document(favoriteId)
                    .set(favorite)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Favorite berhasil ditambahkan")
                        val resultIntent = Intent()
                        resultIntent.putExtra("refreshFavorites", true)
                        setResult(RESULT_OK, resultIntent)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error menambahkan favorite", e)
                    }
            } else {
                // Menghapus favorite dari Firestore
                val favoriteQuery = db.collection("favorites")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("coffeeShopId", data?.coffeeShopId ?: "")

                favoriteQuery.get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            db.collection("favorites")
                                .document(document.id)
                                .delete()
                                .addOnSuccessListener {
                                    Log.d("Firebase", "Favorite berhasil dihapus")
                                    val resultIntent = Intent()
                                    resultIntent.putExtra("refreshFavorites", true)
                                    setResult(RESULT_OK, resultIntent)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Error menghapus favorite", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error mendapatkan favorite", e)
                    }
            }
        }

        // back button handling
        binding.backBtn.setOnClickListener {
            finish()
        }

        // handle ke review
        binding.writeReviewBtn.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("coffeeShopId", data?.coffeeShopId)
            intent.putExtra("uid", fAuth.currentUser?.uid.toString())
            startActivity(intent)
        }
    }

    // Cek apakah kedai kopi telah di-favorite oleh user saat ini
    private fun checkIfFavoritedByCurrentUser(coffeeShopId: String) {
        val userId = fAuth.currentUser?.uid ?: return
        val favoriteQuery = db.collection("favorites")
            .whereEqualTo("userId", userId)
            .whereEqualTo("coffeeShopId", coffeeShopId)

        favoriteQuery.get()
            .addOnSuccessListener { documents ->
                isFavorited = !documents.isEmpty
                updateFavoriteIcon(isFavorited)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error memeriksa apakah sudah di-favorite", e)
            }
    }

    // Update ikon favorite berdasarkan status favorite
    private fun updateFavoriteIcon(isFavorited: Boolean) {
        if (isFavorited) {
            binding.favoriteIcon.setImageResource(R.drawable.icon_favorite_fill)  // Ikon terisi
        } else {
            binding.favoriteIcon.setImageResource(R.drawable.icon_favorite)  // Ikon dengan garis tepi
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                val phoneNumber = binding.coffeeShopPhoneNumber.text.toString()
                val intentToCallNumber = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                startActivity(intentToCallNumber)
            } else {
                // Permission denied
                Log.e("Permission", "Call phone permission denied")
            }
        }
    }

    // Memuat review untuk kedai kopi dengan update real-time
    private fun loadReviews(coffeeShopId: String) {
        Log.d("loadReviews", "Memuat review untuk coffeeShopId: $coffeeShopId")

        db.collection("Review")
            .whereEqualTo("coffeeShopId", coffeeShopId)  // Filter berdasarkan coffeeShopId
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Review", "Error memuat review", e)
                    return@addSnapshotListener
                }

                // Clear previous reviews
                binding.reviewContainer.removeAllViews()
                val ratingsList = mutableListOf<Int>()  // List to store ratings
                val displayedReviewIds = mutableSetOf<String>()  // To avoid duplicate reviews

                if (snapshots != null && !snapshots.isEmpty) {
                    for (document in snapshots) {
                        val reviewId = document.id
                        if (displayedReviewIds.contains(reviewId)) continue

                        val review = Review(
                            reviewId = reviewId,
                            userId = document.getString("userId") ?: "",
                            coffeeShopId = document.getString("coffeeShopId") ?: "",
                            rating = document.getLong("rating")?.toInt() ?: 0,
                            reviewText = document.getString("reviewText") ?: ""
                        )

                        // Add rating to list
                        ratingsList.add(review.rating)
                        displayedReviewIds.add(reviewId)

                        // Mendapatkan nama pengguna dan menambahkan review
                        fetchUsernameAndAddReview(review)
                    }

                    // Perbarui rating setelah semua review dimuat
                    updateCoffeeShopRating(coffeeShopId, ratingsList)
                } else {
                    Log.d("Review", "Tidak ada review yang ditemukan untuk kedai kopi ini.")
                    updateCoffeeShopRating(coffeeShopId, ratingsList)
                }
            }
    }

    // Fungsi untuk menghitung rata-rata rating dan memperbarui coffee shop
    private fun updateCoffeeShopRating(coffeeShopId: String, ratingsList: List<Int>) {
        if (ratingsList.isNotEmpty()) {
            val averageRating = ratingsList.average()
            binding.ratings.text = "${String.format("%.1f", averageRating)} ★"  // Update UI

            // Update rating in Firestore
            db.collection("coffeeShops")
                .document(coffeeShopId)
                .update("ratings", ratingsList.map { it.toDouble() })  // Update ratings list
                .addOnSuccessListener {
                    Log.d("Firestore", "CoffeeShop rating updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating CoffeeShop rating", e)
                }
        } else {
            binding.ratings.text = "0.0 ★"  // Set default rating if no reviews
        }
    }

    // Cek apakah user sudah pernah mereview coffee shop ini
    private fun checkIfUserReviewed(coffeeShopId: String) {
        val userId = fAuth.currentUser?.uid ?: return
        db.collection("Review")
            .whereEqualTo("userId", userId)
            .whereEqualTo("coffeeShopId", coffeeShopId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val review = documents.documents[0].toObject(Review::class.java)
                    review?.let {
                        // Here we just log the user's review, or update the UI in other ways if needed
                        Log.d("UserReview", "User has already reviewed: Rating = ${it.rating}, ReviewText = ${it.reviewText}")

                        // You can update other parts of the UI if necessary, but don't reference non-existent views
                        // For example, you could disable the "Write Review" button if the user has already reviewed
                        binding.writeReviewBtn.isEnabled = false
                        binding.writeReviewBtn.text = "You've already reviewed this shop"
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error checking user review", e)
            }
    }

    // Mendapatkan username yang terkait dengan review dan menambahkannya ke tampilan
    private fun fetchUsernameAndAddReview(review: Review) {
        db.collection("users")
            .document(review.userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "Pengguna Tidak Diketahui"
                    addReviewView(review, username)  // Mengirimkan username untuk ditampilkan
                } else {
                    addReviewView(review, "Pengguna Tidak Diketahui")  // Jika user tidak ditemukan, tampilkan "Pengguna Tidak Diketahui"
                }
            }
            .addOnFailureListener { e ->
                Log.e("Review", "Error mendapatkan detail pengguna", e)
                addReviewView(review, "Pengguna Tidak Diketahui")  // Jika terjadi error, tampilkan "Pengguna Tidak Diketahui"
            }
    }

    // Menambahkan tampilan review secara dinamis ke UI
    private fun addReviewView(review: Review, username: String) {
        val reviewView = LayoutInflater.from(this).inflate(R.layout.review_item, null)
        val userNameTextView = reviewView.findViewById<TextView>(R.id.userName)
        val reviewTextTextView = reviewView.findViewById<TextView>(R.id.reviewText)
        val ratingTextView = reviewView.findViewById<TextView>(R.id.rating)

        userNameTextView.text = username
        reviewTextTextView.text = review.reviewText
//        ratingTextView.text = review.rating.toString()
        ratingTextView.text = "${review.rating} ★"

        binding.reviewContainer.addView(reviewView)
    }
}
