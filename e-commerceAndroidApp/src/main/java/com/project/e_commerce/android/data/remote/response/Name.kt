package com.project.e_commerce.android.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Name(
    @SerialName("common")
    val common: String,

    @SerialName("translations")
    val translations: Map<String, Translation>
)
