package me.secure.vault.secureme.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.secure.vault.secureme.core.security.SessionManager
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val sessionManager: SessionManager
) : ViewModel()
