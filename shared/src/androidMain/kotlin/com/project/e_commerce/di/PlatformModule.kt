package com.project.e_commerce.di

import com.project.e_commerce.data.local.CartStorage
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.domain.platform.AudioExtractor
import org.koin.dsl.module

/**
 * Implémentation Android du platformModule.
 */
actual val platformModule = module {
    single { TokenManager(get()) }
    single { CartStorage(get()) }
    single { AudioExtractor(get()) }       // 2.17: audio extraction (Context injected)
}
