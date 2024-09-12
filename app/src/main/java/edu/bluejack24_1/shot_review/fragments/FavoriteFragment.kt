package edu.bluejack24_1.shot_review.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.bluejack24_1.shot_review.R
import edu.bluejack24_1.shot_review.activities.CoffeeShopDetailActivity
import edu.bluejack24_1.shot_review.activities.MainActivity
import edu.bluejack24_1.shot_review.adapters.CoffeeShopAdapter
import edu.bluejack24_1.shot_review.databinding.FragmentFavoriteBinding
import edu.bluejack24_1.shot_review.models.CoffeeShops

class FavoriteFragment : Fragment() {

    private lateinit var binding: FragmentFavoriteBinding
    private lateinit var fAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val coffeeShops = mutableListOf<CoffeeShops>()
    private var favoriteListenerRegistration: ListenerRegistration? = null
    private lateinit var coffeeShopAdapter: CoffeeShopAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        fAuth = FirebaseAuth.getInstance() // Inisialisasi FirebaseAuth untuk autentikasi pengguna
        db = FirebaseFirestore.getInstance() // Inisialisasi Firestore untuk mengakses database

        coffeeShopAdapter =
            CoffeeShopAdapter(coffeeShops) // Membuat adapter untuk menampilkan daftar coffee shops
        coffeeShopAdapter.setOnItemClickCallback(object : CoffeeShopAdapter.IOnItemClickCallback {
            override fun onItemClick(coffeeShops: CoffeeShops) {
                // Intent untuk memulai aktivitas detail coffee shop saat item diklik
                val intent = Intent(context, CoffeeShopDetailActivity::class.java)
                intent.putExtra("DATA", coffeeShops)
                startActivity(intent)
            }
        })

        // Mengatur RecyclerView dengan adapter dan GridLayoutManager
        binding.rvFavoriteCoffeeShops.adapter = coffeeShopAdapter
        binding.rvFavoriteCoffeeShops.layoutManager = GridLayoutManager(context, 2)
        binding.rvFavoriteCoffeeShops.setHasFixedSize(true)

        val userId = fAuth.currentUser?.uid
            ?: return binding.root // Mengambil userId dari pengguna yang sedang login

        val docRef = db.collection("users")
            .document(userId.toString()) // Mengambil data pengguna dari Firestore

        // Mengambil dan menampilkan foto profil dari Firestore
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(requireContext(), "Listen failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val profilePictureUrl = snapshot.getString("profilePicture")

                Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.banner_profile)
                    .error(R.drawable.banner_profile)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            } else {
                Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Memuat coffee shop favorit secara real-time
        listenForFavoriteCoffeeShops(userId)

        // Mengatur logo untuk kembali ke home
        binding.ivLogo.setOnClickListener {
            // Navigate to HomeFragment
            parentFragmentManager.beginTransaction().apply {
                var homeFragment =
                    parentFragmentManager.findFragmentByTag("HomeFragment") as? HomeFragment
                if (homeFragment == null) {
                    homeFragment = HomeFragment()
                    add(R.id.fragmentContainer, homeFragment, "HomeFragment")
                } else {
                    show(homeFragment)
                }
                hide(this@FavoriteFragment)
                addToBackStack(null)
            }.commit()

            // Update bottom navigation selected item
            (activity as MainActivity).binding.botNav.selectedItemId = R.id.menuHome
        }

        binding.ivProfilePicture.setOnClickListener {
            // Navigate to ProfileFragment
            parentFragmentManager.beginTransaction().apply {
                var profileFragment =
                    parentFragmentManager.findFragmentByTag("ProfileFragment") as? ProfileFragment
                if (profileFragment == null) {
                    profileFragment = ProfileFragment()
                    add(R.id.fragmentContainer, profileFragment, "ProfileFragment")
                } else {
                    show(profileFragment)
                }
                hide(this@FavoriteFragment)
                addToBackStack(null)
            }.commit()

            // Update bottom navigation selected item
            (activity as MainActivity).binding.botNav.selectedItemId = R.id.menuProfile
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Menghapus listener Firestore untuk mencegah kebocoran memori
        favoriteListenerRegistration?.remove()
    }

    private fun listenForFavoriteCoffeeShops(userId: String) {
        // Mendengarkan perubahan pada koleksi 'favorites' untuk pengguna saat ini secara real-time
        favoriteListenerRegistration = db.collection("favorites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firebase", "Error mendengarkan data favorit", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    coffeeShops.clear()  // Mengosongkan daftar coffee shops saat ini
                    for (document in snapshots) {
                        val coffeeShopId = document.getString("coffeeShopId") ?: ""
                        db.collection("CoffeeShops")
                            .document(coffeeShopId)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val coffeeShop = CoffeeShops(
                                        coffeeShopId = doc.getString("coffeeShopId") ?: "",
                                        coffeeShopName = doc.getString("coffeeShopName") ?: "",
                                        coffeeShopAddress = doc.getString("coffeeShopAddress")
                                            ?: "",
                                        coffeeShopPicture = doc.getString("coffeeShopPicture")
                                            ?: "",
                                        coffeeShopTime = doc.getString("coffeeShopTime") ?: "",
                                        coffeeShopPhoneNumber = doc.getString("coffeeShopPhoneNumber")
                                            ?: "",
                                        coffeeShopGallery = doc.get("coffeeShopGallery") as? List<String>
                                            ?: listOf(),
                                        coffeeShopMenu = doc.getString("coffeeShopMenu") ?: "",
                                        ratings = doc.get("ratings") as? List<Double> ?: listOf()
                                    )
                                    coffeeShops.add(coffeeShop)

                                    // Memulai pendengaran ulasan untuk meng-update rating
                                    listenForReviews(coffeeShop.coffeeShopId)
                                }
                            }
                            .addOnFailureListener { error ->
                                Log.e("Firebase", "Error mendapatkan detail coffee shop", error)
                            }
                    }
                    binding.rvFavoriteCoffeeShops.adapter?.notifyDataSetChanged() // Memberi tahu adapter bahwa data telah berubah
                }
            }
    }

    private fun listenForReviews(coffeeShopId: String) {
        // Mendengarkan perubahan pada koleksi 'Review' untuk coffee shop tertentu
        db.collection("Review")
            .whereEqualTo("coffeeShopId", coffeeShopId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FavoriteFragment", "Error mendengarkan ulasan", e)
                    return@addSnapshotListener
                }

                snapshots?.let {
                    val updatedCoffeeShops = coffeeShops.map { coffeeShop ->
                        if (coffeeShop.coffeeShopId == coffeeShopId) {
                            val ratingsList = mutableListOf<Int>()
                            for (document in it) {
                                val rating = document.getLong("rating")?.toInt() ?: 0
                                ratingsList.add(rating)
                            }
                            coffeeShop.copy(ratings = ratingsList.map { it.toDouble() })
                        } else {
                            coffeeShop
                        }
                    }

                    coffeeShops.clear()
                    coffeeShops.addAll(updatedCoffeeShops)
                    coffeeShopAdapter.notifyDataSetChanged()
                }
            }
    }
}
