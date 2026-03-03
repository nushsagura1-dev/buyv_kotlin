package com.project.e_commerce.android.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Translation(
    @SerialName("official")
    val official: String,

    @SerialName("common")
    val common: String
)