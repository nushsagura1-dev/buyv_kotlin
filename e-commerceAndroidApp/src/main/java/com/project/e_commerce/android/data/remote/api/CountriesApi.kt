package com.project.e_commerce.android.data.remote.api

import com.project.e_commerce.android.data.remote.response.CountryResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class CountriesApi(private val httpClient: HttpClient) {

    suspend fun getCountry(): List<CountryResponse> =
        httpClient.get("https://restcountries.com/v3.1/all").body()

}
