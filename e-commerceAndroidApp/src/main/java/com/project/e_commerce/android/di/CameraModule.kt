package com.project.e_commerce.android.di

import com.project.e_commerce.android.presentation.viewModel.CameraViewModel
import com.project.e_commerce.domain.platform.CameraController
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * 2.18 — Koin DI module for the in-app camera feature.
 *
 * Registered in [EcommerceApp] alongside [viewModelModule] and [marketplaceModule]:
 * ```kotlin
 * modules(sharedModules + viewModelModule + marketplaceModule + cameraModule)
 * ```
 *
 * [CameraController] is a `single` so the same session/preview is reused if the
 * composable re-enters the composition within the same activity lifecycle.
 * It is `release()`d in [CameraViewModel.onCleared].
 */
val cameraModule = module {

    /**
     * Platform-specific camera controller backed by CameraX + GPUImage on Android.
     * Receives [android.content.Context] via [androidContext()].
     */
    single {
        CameraController(androidContext())
    }

    /**
     * ViewModel that drives [CameraScreen].
     * Uses `factory` semantics implicitly — a new VM is created each time
     * the Camera destination is first entered and destroyed on back-stack pop.
     */
    viewModel {
        CameraViewModel(cameraController = get())
    }
}
