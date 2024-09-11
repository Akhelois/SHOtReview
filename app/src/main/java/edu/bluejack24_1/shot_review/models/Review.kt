package edu.bluejack24_1.shot_review.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    var reviewId: String,
    var userId: String,
    var coffeeShopId: String,
    var rating: Int,
    var reviewText: String,
) : Parcelable
