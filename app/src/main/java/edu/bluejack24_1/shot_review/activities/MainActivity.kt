package edu.bluejack24_1.shot_review.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import edu.bluejack24_1.shot_review.R
import edu.bluejack24_1.shot_review.databinding.ActivityMainBinding
import edu.bluejack24_1.shot_review.fragments.HomeFragment
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_1.shot_review.fragments.EditProfileFragment
import edu.bluejack24_1.shot_review.fragments.FavoriteFragment
import edu.bluejack24_1.shot_review.fragments.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var homeFragment: HomeFragment
    private lateinit var favoriteFragment: FavoriteFragment
    private lateinit var profileFragment: ProfileFragment
    private lateinit var editProfileFragment: EditProfileFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Ambil data dari login activity
        val uid = intent.getStringExtra("uid")

//        // Pass data ke fragment HomeFragment pake bundle
//        val homeFragment = HomeFragment()
//        val bundle = Bundle()
//        bundle.putString("uid", uid)
//        homeFragment.arguments = bundle
//
//        // Default pas login di replace fragment ama home
//        replaceFragment(homeFragment)

        // init fragments
        homeFragment = HomeFragment().apply {
            arguments = Bundle().apply {
                putString("uid", uid)
            }
        }

        favoriteFragment = FavoriteFragment()

        profileFragment = ProfileFragment().apply {
            arguments = Bundle().apply {
                putString("uid", uid)
            }
        }

        editProfileFragment = EditProfileFragment().apply {
            arguments = Bundle().apply {
                putString("uid", uid)
            }
        }

        // Taro Fragment ke fragment manager buat tampilkan
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, homeFragment, "HomeFragment").hide(homeFragment)
            add(R.id.fragmentContainer, favoriteFragment, "FavoriteFragment").hide(favoriteFragment)
            add(R.id.fragmentContainer, profileFragment, "ProfileFragment").hide(profileFragment)
            add(R.id.fragmentContainer, editProfileFragment, "EditProfileFragment").hide(editProfileFragment)
            show(homeFragment)
        }.commit()

        activeFragment = homeFragment

        binding.botNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menuHome -> {
                    switchFragment(homeFragment)
                }
                R.id.menuProfile -> {
                    switchFragment(profileFragment)
                }
                R.id.menuFavorite -> {
                    switchFragment(favoriteFragment)
                }
            }
            true
        }

        // Handle edit profile
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.botNav.selectedItemId = R.id.menuProfile
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        if (fragment != activeFragment) {
            supportFragmentManager.beginTransaction().apply {
                activeFragment?.let { hide(it) }
                show(fragment)
            }.commit()
            activeFragment = fragment
        }
    }

    fun showEditProfileFragment() {
        supportFragmentManager.beginTransaction().apply {
            hide(profileFragment)
            show(editProfileFragment)
            addToBackStack(null)
        }.commit()
        activeFragment = editProfileFragment
    }

    fun showProfileFragment() {
        supportFragmentManager.beginTransaction().apply {
            hide(editProfileFragment)
            show(profileFragment)
            addToBackStack(null)
        }.commit()
        activeFragment = profileFragment
    }

//    private fun replaceFragment(fragment: Fragment) {
//        val fragmentTransaction = supportFragmentManager.beginTransaction()
//        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
//        fragmentTransaction.commit()
//    }
}