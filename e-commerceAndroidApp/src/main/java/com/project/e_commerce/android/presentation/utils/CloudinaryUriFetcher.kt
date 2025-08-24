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
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Failed to fetch image: ${response.code} - ${response.message}")
            }

            val body = response.body

            // Convert body to ImageSource instead of Buffer
            val imageSource = ImageSource(
                source = body.source().buffer(),
                fileSystem = options.fileSystem
            )

            return SourceFetchResult(
                source = imageSource,
                mimeType = body.contentType()?.toString(),
                dataSource = DataSource.NETWORK
            )
        } catch (e: Exception) {
            throw IOException("Failed to fetch Cloudinary image: ${e.message}", e)
        }
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(
            data: String,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            // Only handle Cloudinary URLs - return null for others to let Coil use default behavior
            if (CloudinaryUtils.isCloudinaryUrl(data)) {
                return CloudinaryUriFetcher(data, options)
            }
            // Return null for non-Cloudinary URLs to let Coil use its default fetchers
            return null
        }
    }
}
