package com.example.quickpay.data.api

import com.example.quickpay.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var currentBaseUrl: String = Constants.DEFAULT_BASE_URL
    private var apiServiceInstance: QuickPayApiService? = null

    private fun buildOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getApiService(baseUrl: String = currentBaseUrl): QuickPayApiService {
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (apiServiceInstance == null || currentBaseUrl != formattedUrl) {
            currentBaseUrl = formattedUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(buildOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiServiceInstance = retrofit.create(QuickPayApiService::class.java)
        }

        return apiServiceInstance!!
    }

    fun setBaseUrl(newBaseUrl: String) {
        getApiService(newBaseUrl)
    }
}
