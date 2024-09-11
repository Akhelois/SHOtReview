package edu.bluejack24_1.shot_review.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Favorite(
    var favoriteId: String,
    var userId: String,
    var coffeeShopId: String
) : Parcelable
