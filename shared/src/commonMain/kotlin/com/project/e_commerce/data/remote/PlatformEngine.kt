package com.project.e_commerce.data.remote

import io.ktor.client.engine.HttpClientEngine

/**
 * H-5: Platform-specific HTTP engine with certificate pinning.
 *
 * Each platform provides its own engine implementation:
 * - Android: OkHttp with CertificatePinner
 * - iOS: Darwin with server trust evaluation (ATS + optional handleChallenge)
 *
 * Pins target the Let's Encrypt intermediate (R13) and root (ISRG Root X1) CAs
 * used by buyv-api.up.railway.app.
 * The leaf certificate is NOT pinned because Railway rotates it every 90 days.
 */
expect fun createPlatformEngine(): HttpClientEngine
