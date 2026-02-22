package com.project.e_commerce.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * H-5: iOS HTTP engine with Darwin (NSURLSession).
 *
 * Baseline TLS security is enforced by App Transport Security (ATS).
 * Additional certificate pinning via URLSession server-trust evaluation
 * requires macOS to compile and test — see CONFIGURATION_IOS_MACOS_REQUISE.md.
 *
 * TODO (macOS): Add handleChallenge callback for SPKI certificate pinning
 * matching the pins used in the Android implementation.
 */
actual fun createPlatformEngine(): HttpClientEngine {
    return Darwin.create {
        // ATS already enforces TLS 1.2+ with strong cipher suites.
        // Full SPKI pinning via handleChallenge requires macOS build — see client doc.
    }
}
