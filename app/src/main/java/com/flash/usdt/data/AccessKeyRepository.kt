package com.flash.usdt.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface AccessKeyCheckResult {
  data class Success(val message: String? = null) : AccessKeyCheckResult

  data class Rejected(val httpCode: Int, val message: String? = null) : AccessKeyCheckResult

  data class NetworkError(val message: String? = null) : AccessKeyCheckResult
}

class AccessKeyRepository(private val api: AccessKeyApi = AccessKeyApi.create()) {

  suspend fun verifyKey(key: String): AccessKeyCheckResult =
    withContext(Dispatchers.IO) {
      try {
        val response = api.checkKey(key.trim())
        val rawBody = response.body()?.string()?.trim().orEmpty()
        val errorBody = response.errorBody()?.string()?.trim().orEmpty()

        if (response.isSuccessful) {
          val parsed = parseJsonObject(rawBody)
          if (parsed != null) {
            val isExplicitlyInvalid =
              (parsed.has("valid") && !parsed.optBoolean("valid", true)) ||
                (parsed.has("success") && !parsed.optBoolean("success", true)) ||
                (parsed.has("authorized") && !parsed.optBoolean("authorized", true))
            val serverMsg =
              parsed.optString("message").takeIf { it.isNotBlank() }
                ?: parsed.optString("error").takeIf { it.isNotBlank() }
            if (isExplicitlyInvalid) {
              AccessKeyCheckResult.Rejected(httpCode = response.code(), message = serverMsg)
            } else {
              AccessKeyCheckResult.Success(message = serverMsg)
            }
          } else if (rawBody.equals("false", ignoreCase = true) || rawBody.equals("invalid", ignoreCase = true)) {
            AccessKeyCheckResult.Rejected(httpCode = response.code(), message = null)
          } else {
            AccessKeyCheckResult.Success(message = null)
          }
        } else {
          val parsedErr = parseJsonObject(errorBody)
          val serverMsg =
            parsedErr?.optString("message")?.takeIf { it.isNotBlank() }
              ?: parsedErr?.optString("error")?.takeIf { it.isNotBlank() }
          AccessKeyCheckResult.Rejected(httpCode = response.code(), message = serverMsg)
        }
      } catch (e: Exception) {
        AccessKeyCheckResult.NetworkError(message = e.localizedMessage)
      }
    }

  private fun parseJsonObject(raw: String): JSONObject? {
    if (!raw.startsWith("{")) return null
    return runCatching { JSONObject(raw) }.getOrNull()
  }
}
