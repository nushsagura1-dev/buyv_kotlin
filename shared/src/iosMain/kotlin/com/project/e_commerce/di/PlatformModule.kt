package com.project.e_commerce.di

import com.project.e_commerce.data.local.CartStorage
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.domain.platform.AudioExtractor
import org.koin.dsl.module

/**
 * Implémentation iOS du platformModule.
 */
actual val platformModule = module {
    single { TokenManager() }
    single { CartStorage() }
    single { AudioExtractor() }            // 2.17: audio extraction
}
