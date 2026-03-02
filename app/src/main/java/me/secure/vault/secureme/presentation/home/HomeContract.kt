package me.secure.vault.secureme.presentation.home

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.IMAGES
)

enum class HomeTab {
    IMAGES, VIDEOS, DOCUMENTS, OTHER
}

sealed class HomeUiIntent {
    data class OnTabSelected(val tab: HomeTab) : HomeUiIntent()
}

sealed class HomeUiEffect {
    data class NavigateToFileViewer(val fileId: String) : HomeUiEffect()
}
