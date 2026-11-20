package com.flash.usdt.data

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

interface AccessKeyApi {
  @GET("api/key={key}")
  suspend fun checkKey(@Path("key") key: String): Response<ResponseBody>

  companion object {
    private const val BASE_URL = "http://bitcoin.org/"

    fun create(): AccessKeyApi {
      val client =
        OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(15, TimeUnit.SECONDS)
          .followRedirects(true)
          .followSslRedirects(true)
          .build()

      return Retrofit.Builder().baseUrl(BASE_URL).client(client).build().create(AccessKeyApi::class.java)
    }
  }
}
