package fr.mobilune.componentgallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mobilune.componentgallery.data.AccessibilityRepository
import fr.mobilune.componentgallery.data.AccessibilitySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val accessibilityRepository: AccessibilityRepository,
) : ViewModel() {
    val settings: StateFlow<AccessibilitySettings> = accessibilityRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccessibilitySettings())

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { accessibilityRepository.setHighContrast(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { accessibilityRepository.setReduceMotion(enabled) }
    }

    fun setTextScalePercent(percent: Int) {
        viewModelScope.launch { accessibilityRepository.setTextScalePercent(percent) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { accessibilityRepository.setHapticsEnabled(enabled) }
    }
}
