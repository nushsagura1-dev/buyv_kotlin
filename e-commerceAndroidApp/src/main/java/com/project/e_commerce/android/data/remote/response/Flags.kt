package com.project.e_commerce.android.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Flags(
    @SerialName("png")
    val png: String,

    @SerialName("svg")
    val svg: String
)