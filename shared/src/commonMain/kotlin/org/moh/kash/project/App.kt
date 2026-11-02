package org.moh.kash.project

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import org.moh.kash.project.core.designsystem.KashTheme
import org.moh.kash.project.di.appModule
import org.moh.kash.project.presentation.exchange.ExchangeRoute

/**
 * Main application composition entrypoint for Kash across Android, Desktop, and iOS.
 * Initializes KashTheme and Koin dependency injection context, then mounts [ExchangeRoute].
 */
@Composable
fun App() {
    KashTheme {
        KoinApplication(application = {
            modules(appModule)
        }) {
            ExchangeRoute()
        }
    }
}