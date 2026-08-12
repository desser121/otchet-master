package com.otchetmaster.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.otchetmaster.app.updater.UpdateManager

class HomeViewModelFactory(
    private val updateManager: UpdateManager,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(updateManager) as T
    }
}
