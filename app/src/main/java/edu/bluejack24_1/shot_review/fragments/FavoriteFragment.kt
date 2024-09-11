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
import edu.bluejack24_1.shot_review.adapters.CoffeeShopAdapter
import edu.bluejack24_1.shot_review.databinding.FragmentFavoriteBinding
import edu.bluejack24_1.shot_review.models.CoffeeShops

class FavoriteFragment : Fragment() {

    private lateinit var binding: FragmentFavoriteBinding
    private lateinit var fAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val coffeeShops = mutableListOf<CoffeeShops>()
    private var favoriteListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        fAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val coffeeShopAdapter = CoffeeShopAdapter(coffeeShops)
        coffeeShopAdapter.setOnItemClickCallback(object : CoffeeShopAdapter.IOnItemClickCallback {
            override fun onItemClick(coffeeShops: CoffeeShops) {
                val intent = Intent(context, CoffeeShopDetailActivity::class.java)
                intent.putExtra("DATA", coffeeShops)
                startActivity(intent)
            }
        })

        binding.rvFavoriteCoffeeShops.adapter = coffeeShopAdapter
        binding.rvFavoriteCoffeeShops.layoutManager = GridLayoutManager(context, 2)
        binding.rvFavoriteCoffeeShops.setHasFixedSize(true)

        val userId = fAuth.currentUser?.uid ?: return binding.root

        val docRef = db.collection("users").document(userId.toString())

        // Set username dan profile picture dari Firestore
        docRef.get().addOnSuccessListener {
            if (it.exists()) {
                val profilePictureUrl = it.getString("profilePicture")

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

        // Load favorite coffee shops in real-time
        listenForFavoriteCoffeeShops(userId)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove Firestore listener to prevent memory leaks
        favoriteListenerRegistration?.remove()
    }

    private fun listenForFavoriteCoffeeShops(userId: String) {
        // Start listening for changes in the 'favorites' collection for the current user
        favoriteListenerRegistration = db.collection("favorites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firebase", "Error listening to favorites", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    coffeeShops.clear()  // Clear existing coffee shops
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
                                        coffeeShopAddress = doc.getString("coffeeShopAddress") ?: "",
                                        coffeeShopPicture = doc.getString("coffeeShopPicture") ?: "",
                                        coffeeShopTime = doc.getString("coffeeShopTime") ?: "",
                                        coffeeShopPhoneNumber = doc.getString("coffeeShopPhoneNumber") ?: "",
                                        coffeeShopGallery = doc.get("coffeeShopGallery") as? List<String> ?: listOf(),
                                        coffeeShopMenu = doc.getString("coffeeShopMenu") ?: "",
                                        ratings = doc.get("ratings") as? List<Double> ?: listOf()
                                    )
                                    coffeeShops.add(coffeeShop)
                                    binding.rvFavoriteCoffeeShops.adapter?.notifyDataSetChanged()  // Notify the adapter
                                }
                            }
                            .addOnFailureListener { error ->
                                Log.e("Firebase", "Error getting coffee shop details", error)
                            }
                    }
                }
            }
    }
}
