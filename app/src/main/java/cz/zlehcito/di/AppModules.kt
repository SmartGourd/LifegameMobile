package cz.zlehcito.di

import cz.zlehcito.viewmodel.GamePageViewModel
import cz.zlehcito.viewmodel.GameSetupViewModel
import cz.zlehcito.viewmodel.LobbyViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Define ViewModels
    viewModel { LobbyViewModel() } // Parameters will be added if needed, e.g., SavedStateHandle
    viewModel { GameSetupViewModel(get()) } // Koin will provide SavedStateHandle
    viewModel { GamePageViewModel(get()) }    // Koin will provide SavedStateHandle

    // Potentially other dependencies like Repositories or UseCases if you create them
    // single { WebSocketManager } // WebSocketManager is an object, usually not needed to be managed by Koin unless for testing/swapping implementations
}
