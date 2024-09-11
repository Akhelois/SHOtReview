package edu.bluejack24_1.shot_review.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.DecimalFormat

@Parcelize
data class CoffeeShops(
    var coffeeShopId: String,
    var coffeeShopName: String,
    var coffeeShopAddress: String,
    var coffeeShopPicture: String,
    var coffeeShopTime: String,
    var coffeeShopPhoneNumber: String,
    var coffeeShopMenu: String,
    var coffeeShopPublicFacilities: List<String> = listOf(),
    var coffeeShopGallery: List<String> = listOf(),
    var ratings: List<Double> = listOf(),
    var reviews: List<Review> = listOf()
) : Parcelable {

    fun getAverageRating(): String {
        val decimalFormat = DecimalFormat("#.#")
        return if (ratings.isNotEmpty()) {
            decimalFormat.format(ratings.average().toDouble())
        } else {
            "0.0"
        }
    }
}