package com.flash.usdt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.usdt.data.AccessKeyCheckResult
import com.flash.usdt.data.AccessKeyRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthCheckStatus {
  data object Idle : AuthCheckStatus

  data object Loading : AuthCheckStatus

  data object Success : AuthCheckStatus

  data class Error(
    val httpCode: Int? = null,
    val message: String? = null,
    val isNetworkError: Boolean = false,
  ) : AuthCheckStatus
}

data class AccessKeyUiState(
  val accessKey: String = "",
  val showLengthError: Boolean = false,
  val status: AuthCheckStatus = AuthCheckStatus.Idle,
)

sealed interface AccessKeyEvent {
  data class Verified(val message: String?) : AccessKeyEvent
}

class AccessKeyViewModel(
  private val repository: AccessKeyRepository = AccessKeyRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(AccessKeyUiState())
  val uiState: StateFlow<AccessKeyUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<AccessKeyEvent>()
  val events: SharedFlow<AccessKeyEvent> = _events.asSharedFlow()

  fun onAccessKeyChange(value: String) {
    _uiState.update { current ->
      current.copy(
        accessKey = value,
        showLengthError =
          if (current.showLengthError && value.trim().length >= 8) false
          else current.showLengthError,
        status = if (current.status is AuthCheckStatus.Error) AuthCheckStatus.Idle else current.status,
      )
    }
  }

  fun dismissError() {
    _uiState.update { current ->
      if (current.status is AuthCheckStatus.Error) current.copy(status = AuthCheckStatus.Idle)
      else current
    }
  }

  fun authenticate() {
    val current = _uiState.value
    if (current.status is AuthCheckStatus.Loading) return

    val trimmedKey = current.accessKey.trim()
    if (trimmedKey.length < 8) {
      _uiState.update { it.copy(showLengthError = true) }
      return
    }

    _uiState.update { it.copy(showLengthError = false, status = AuthCheckStatus.Loading) }

    viewModelScope.launch {
      when (val result = repository.verifyKey(trimmedKey)) {
        is AccessKeyCheckResult.Success -> {
          _uiState.update { it.copy(status = AuthCheckStatus.Success) }
          _events.emit(AccessKeyEvent.Verified(result.message))
        }
        is AccessKeyCheckResult.Rejected -> {
          _uiState.update {
            it.copy(
              status =
                AuthCheckStatus.Error(
                  httpCode = result.httpCode,
                  message = result.message,
                  isNetworkError = false,
                )
            )
          }
        }
        is AccessKeyCheckResult.NetworkError -> {
          _uiState.update {
            it.copy(
              status =
                AuthCheckStatus.Error(
                  httpCode = null,
                  message = result.message,
                  isNetworkError = true,
                )
            )
          }
        }
      }
    }
  }
}
