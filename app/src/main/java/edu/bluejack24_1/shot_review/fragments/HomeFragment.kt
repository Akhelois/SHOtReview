package edu.bluejack24_1.shot_review.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_1.shot_review.R
import edu.bluejack24_1.shot_review.activities.CoffeeShopDetailActivity
import edu.bluejack24_1.shot_review.activities.MainActivity
import edu.bluejack24_1.shot_review.databinding.FragmentHomeBinding
import edu.bluejack24_1.shot_review.adapters.CoffeeShopAdapter
import edu.bluejack24_1.shot_review.models.CoffeeShops
import edu.bluejack24_1.shot_review.models.Review

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var db: FirebaseFirestore
    private var coffeeShops = mutableListOf<CoffeeShops>()
    private var filteredCoffeeShops = mutableListOf<CoffeeShops>()
    private lateinit var coffeeShopAdapter: CoffeeShopAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        val intentUID = arguments?.getString("uid")
        Log.d("HomeFragment", "${arguments?.getString("uid")}")

        // Inisialisasi RecyclerView dan Adapter
        coffeeShopAdapter = CoffeeShopAdapter(filteredCoffeeShops)
        binding.rvCoffeeShop.adapter = coffeeShopAdapter
        binding.rvCoffeeShop.layoutManager = GridLayoutManager(context, 2)
        binding.rvCoffeeShop.setHasFixedSize(true)

        // Mengatur nama pengguna dan gambar profil dari Firestore
        val docRef = db.collection("users").document(intentUID.toString())
        docRef.get().addOnSuccessListener {
            if (it.exists()) {
                val username = it.getString("username")
                val profilePictureUrl = it.getString("profilePicture")
                binding.tvUsername.text = "Hello, ${username.toString()}"

                Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.banner_profile)
                    .error(R.drawable.banner_profile)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            } else {
                Toast.makeText(requireContext(), "Dokumen tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        // Mengambil data coffee shop dari Firestore secara real-time
        db.collection("CoffeeShops").addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("HomeFragment", "Gagal mendengarkan.", error)
                return@addSnapshotListener
            }

            coffeeShops.clear()
            filteredCoffeeShops.clear()

            snapshots?.let {
                for (document in snapshots) {
                    val coffeeShop = CoffeeShops(
                        coffeeShopId = document.getString("coffeeShopId") ?: "",
                        coffeeShopName = document.getString("coffeeShopName") ?: "",
                        coffeeShopAddress = document.getString("coffeeShopAddress") ?: "",
                        coffeeShopPicture = document.getString("coffeeShopPicture") ?: "",
                        coffeeShopTime = document.getString("coffeeShopTime") ?: "",
                        coffeeShopPhoneNumber = document.getString("coffeeShopPhoneNumber") ?: "",
                        coffeeShopPublicFacilities = document.get("publicFacilities") as? List<String> ?: listOf(),
                        coffeeShopGallery = document.get("coffeeShopGallery") as? List<String> ?: listOf(),
                        coffeeShopMenu = document.getString("coffeeShopMenu") ?: "",
                        ratings = document.get("ratings") as? List<Double> ?: listOf()
                    )
                    coffeeShops.add(coffeeShop)
                    filteredCoffeeShops.add(coffeeShop)

                    // Mengatur pendengar real-time untuk ulasan
                    listenForReviews(coffeeShop.coffeeShopId)
                }
                coffeeShopAdapter.notifyDataSetChanged()
            }
        }

        // Mengatur logo untuk kembali ke home
        binding.ivLogo.setOnClickListener {
            // Jika ada fragment lain di backstack, kembali ke HomeFragment
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack("HomeFragment", 0)
            } else {
                // Jika tidak ada di backstack, tambahkan atau tampilkan HomeFragment
                val homeFragment = parentFragmentManager.findFragmentByTag("HomeFragment") as? HomeFragment ?: HomeFragment().apply {
                    arguments = Bundle().apply {
                        putString("uid", arguments?.getString("uid"))
                    }
                }
                parentFragmentManager.beginTransaction().apply {
                    replace(R.id.fragmentContainer, homeFragment, "HomeFragment")
                    addToBackStack("HomeFragment")
                }.commit()
            }
        }

        binding.ivProfilePicture.setOnClickListener {
            // Navigate to ProfileFragment
            parentFragmentManager.beginTransaction().apply {
                var profileFragment = parentFragmentManager.findFragmentByTag("ProfileFragment") as? ProfileFragment
                if (profileFragment == null) {
                    profileFragment = ProfileFragment()
                    add(R.id.fragmentContainer, profileFragment, "ProfileFragment")
                } else {
                    show(profileFragment)
                }
                hide(this@HomeFragment)
                addToBackStack(null)
            }.commit()

            // Update bottom navigation selected item
            (activity as MainActivity).binding.botNav.selectedItemId = R.id.menuProfile
        }

        // Mengatur klik pada coffee shop untuk navigasi ke detail
        coffeeShopAdapter.setOnItemClickCallback(object: CoffeeShopAdapter.IOnItemClickCallback {
            override fun onItemClick(coffeeShop: CoffeeShops) {
                val intentToDetail = Intent(activity, CoffeeShopDetailActivity::class.java)
                intentToDetail.putExtra("DATA", coffeeShop)
                startActivity(intentToDetail)
            }
        })

        // Mengatur TextWatcher untuk EditText pencarian
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCoffeeShops(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return binding.root
    }

    private fun listenForReviews(coffeeShopId: String) {
        db.collection("Review")
            .whereEqualTo("coffeeShopId", coffeeShopId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HomeFragment", "Error mendengarkan ulasan", e)
                    return@addSnapshotListener
                }

                snapshots?.let {
                    val updatedCoffeeShops = coffeeShops.map { coffeeShop ->
                        if (coffeeShop.coffeeShopId == coffeeShopId) {
                            val ratingsList = mutableListOf<Int>()
                            for (document in it) {
                                val review = Review(
                                    reviewId = document.getString("reviewId") ?: "",
                                    userId = document.getString("userId") ?: "",
                                    coffeeShopId = document.getString("coffeeShopId") ?: "",
                                    rating = document.getLong("rating")?.toInt() ?: 0,
                                    reviewText = document.getString("reviewText") ?: ""
                                )
                                ratingsList.add(review.rating)
                            }
                            coffeeShop.copy(ratings = ratingsList.map { it.toDouble() })
                        } else {
                            coffeeShop
                        }
                    }

                    coffeeShops.clear()
                    coffeeShops.addAll(updatedCoffeeShops)
                    filteredCoffeeShops.clear()
                    filteredCoffeeShops.addAll(updatedCoffeeShops)
                    coffeeShopAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun filterCoffeeShops(query: String) {
        val filteredList = coffeeShops.filter { coffeeShop ->
            coffeeShop.coffeeShopName.contains(query, ignoreCase = true)
        }
        filteredCoffeeShops.clear()
        filteredCoffeeShops.addAll(filteredList)
        coffeeShopAdapter.notifyDataSetChanged()

        if (filteredList.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}
