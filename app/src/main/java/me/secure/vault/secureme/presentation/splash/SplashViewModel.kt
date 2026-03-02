package me.secure.vault.secureme.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _effect = MutableSharedFlow<SplashUiEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            delay(2000)
            _isLoading.value = false
            _effect.emit(SplashUiEffect.NavigateForward)
        }
    }
}