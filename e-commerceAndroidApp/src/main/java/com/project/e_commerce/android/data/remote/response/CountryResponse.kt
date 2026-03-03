package com.project.e_commerce.android.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountryResponse(
    @SerialName("name")
    val name: Name,
    @SerialName("flags")
    val flags: Flags
)