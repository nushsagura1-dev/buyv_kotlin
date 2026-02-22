package com.project.e_commerce.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.CertificatePinner

/**
 * H-5: Android HTTP engine with OkHttp certificate pinning.
 *
 * Pins target the Let's Encrypt certificate chain used by Railway.app:
 *  - R13 intermediate CA (stable for years)
 *  - ISRG Root X1 root CA (valid until 2035)
 *
 * If Railway changes its TLS provider or Let's Encrypt rotates its
 * intermediates, update the pins below using:
 *   .\get-ssl-pins.ps1 buyv-api.up.railway.app
 */
actual fun createPlatformEngine(): HttpClientEngine {
    val certificatePinner = CertificatePinner.Builder()
        // Let's Encrypt R13 intermediate CA
        .add("buyv-api.up.railway.app", "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=")
        // ISRG Root X1 (Let's Encrypt root CA — valid until 2035)
        .add("buyv-api.up.railway.app", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
        .build()

    return OkHttp.create {
        config {
            certificatePinner(certificatePinner)
        }
    }
}
