package com.example.ui

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    data class Loading(val message: String = "Loading telemetry data...") : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed class OperationState {
    object Idle : OperationState()
    data class Loading(val message: String) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
