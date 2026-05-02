package com.codepalace.accelerometer.api

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.codepalace.accelerometer.config.ApiConfig
import com.codepalace.accelerometer.data.local.SessionStorage
import com.codepalace.accelerometer.util.InstantTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
object ApiClient {
    private const val BASE_URL = ApiConfig.BASE_URL

    private lateinit var sessionStorage: SessionStorage

    fun init(context: Context) {
        sessionStorage = SessionStorage(context.applicationContext)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val publicOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val authOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStorage))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    private val publicRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(publicOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))  // ← use custom gson
            .build()
    }

    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))  // ← use custom gson
            .build()
    }

    val celestialApi: CelestialApi by lazy {
        publicRetrofit.create(CelestialApi::class.java)
    }

    val authApi: AuthApi by lazy {
        publicRetrofit.create(AuthApi::class.java)
    }

    val profileApi: ProfileApi by lazy {
        authRetrofit.create(ProfileApi::class.java)
    }

    val favoriteApi: FavoriteApi by lazy {
        authRetrofit.create(FavoriteApi::class.java)
    }

    val eventApi: EventApi by lazy {
        authRetrofit.create(EventApi::class.java)
    }

    fun getSessionStorage(): SessionStorage = sessionStorage
}
