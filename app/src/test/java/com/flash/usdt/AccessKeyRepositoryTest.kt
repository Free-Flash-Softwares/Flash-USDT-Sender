package com.flash.usdt

import com.flash.usdt.data.AccessKeyApi
import com.flash.usdt.data.AccessKeyCheckResult
import com.flash.usdt.data.AccessKeyRepository
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccessKeyRepositoryTest {

  private fun createApiWithStub(
    code: Int,
    body: String,
    onRequestUrl: (String) -> Unit = {},
  ): AccessKeyApi {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val request = chain.request()
          onRequestUrl(request.url.toString())
          Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Stub")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        }
        .build()

    return Retrofit.Builder()
      .baseUrl("http://bitcoin.org/")
      .client(client)
      .build()
      .create(AccessKeyApi::class.java)
  }

  @Test
  fun `verifyKey hits exact endpoint path and returns Success on 200 valid`() = runTest {
    var capturedUrl = ""
    val api =
      createApiWithStub(200, """{"valid": true, "message": "Key accepted"}""") { url ->
        capturedUrl = url
      }
    val repository = AccessKeyRepository(api)

    val result = repository.verifyKey("XXXXXXXX")

    assertEquals("http://bitcoin.org/api/key=XXXXXXXX", capturedUrl)
    assertEquals(AccessKeyCheckResult.Success("Key accepted"), result)
  }

  @Test
  fun `verifyKey returns Rejected when server returns valid false`() = runTest {
    val api = createApiWithStub(200, """{"valid": false, "message": "Expired key"}""")
    val repository = AccessKeyRepository(api)

    val result = repository.verifyKey("TEST1234")

    assertEquals(AccessKeyCheckResult.Rejected(200, "Expired key"), result)
  }

  @Test
  fun `verifyKey returns Rejected on HTTP 404`() = runTest {
    val api = createApiWithStub(404, """{"error": "Not found"}""")
    val repository = AccessKeyRepository(api)

    val result = repository.verifyKey("TEST1234")

    assertTrue(result is AccessKeyCheckResult.Rejected)
    assertEquals(404, (result as AccessKeyCheckResult.Rejected).httpCode)
  }
}
