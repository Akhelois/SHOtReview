package edu.bluejack24_1.shot_review.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Users(
    var userId : String,
    var username: String,
    var email: String,
    var password: String,
    var profilePicture: String,
    var bio: String
) : Parcelable
