package com.project.e_commerce.android.presentation.utils

import coil3.fetch.Fetcher
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.ImageLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import java.io.IOException

/**
 * Custom Fetcher for Cloudinary URLs that Coil 3.x cannot handle by default.
 * This fetcher downloads the image data and provides it as a SourceResult.
 */
class CloudinaryUriFetcher(
    private val url: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        android.util.Log.d("CloudinaryUriFetcher", "🔄 Fetching Cloudinary image: $url")

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)  // 15s pour établir connexion
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)     // 30s pour lire les données
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // 30s pour écrire les données
                .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)     // 30s timeout total
                .build()
            val request = Request.Builder()
                .url(url)
                .build()

            android.util.Log.d("CloudinaryUriFetcher", "📡 Making HTTP request to: $url")
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                android.util.Log.e(
                    "CloudinaryUriFetcher",
                    "❌ HTTP request failed: ${response.code} - ${response.message}"
                )
                throw IOException("Failed to fetch image: ${response.code} - ${response.message}")
            }

            val body = response.body
            if (body == null) {
                android.util.Log.e("CloudinaryUriFetcher", "❌ Response body is null")
                throw IOException("Response body is null")
            }

            android.util.Log.d(
                "CloudinaryUriFetcher",
                "✅ HTTP request successful, content type: ${body.contentType()}"
            )

            // Convert body to ImageSource instead of Buffer
            val imageSource = ImageSource(
                source = body.source().buffer(),
                fileSystem = options.fileSystem
            )

            android.util.Log.d("CloudinaryUriFetcher", "✅ ImageSource created successfully")

            return SourceFetchResult(
                source = imageSource,
                mimeType = body.contentType()?.toString(),
                dataSource = DataSource.NETWORK
            )
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryUriFetcher", "❌ Exception during fetch: ${e.message}", e)
            throw IOException("Failed to fetch Cloudinary image: ${e.message}", e)
        }
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(
            data: String,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            android.util.Log.d("CloudinaryUriFetcher", "🔍 Checking URL: $data")

            // Only handle Cloudinary URLs - return null for others to let Coil use default behavior
            if (CloudinaryUtils.isCloudinaryUrl(data)) {
                android.util.Log.d(
                    "CloudinaryUriFetcher",
                    "✅ Creating CloudinaryUriFetcher for: $data"
                )
                return CloudinaryUriFetcher(data, options)
            }
            // Return null for non-Cloudinary URLs to let Coil use its default fetchers
            android.util.Log.d("CloudinaryUriFetcher", "⏭️ Not a Cloudinary URL, skipping: $data")
            return null
        }
    }
}
