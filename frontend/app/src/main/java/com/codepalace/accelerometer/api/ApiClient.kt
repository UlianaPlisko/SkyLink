package com.codepalace.accelerometer.api

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
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

    private lateinit var sessionStorage: SessionStorage

    @Volatile
    private var baseUrlOverride: String? = null

    @Volatile
    private var publicRetrofit: Retrofit? = null

    @Volatile
    private var authRetrofit: Retrofit? = null

    fun init(context: Context) {
        sessionStorage = SessionStorage(context.applicationContext)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val publicOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private val authOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStorage))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    private fun publicRetrofit(): Retrofit {
        publicRetrofit?.let { return it }
        return synchronized(this) {
            publicRetrofit ?: createPublicRetrofit().also { publicRetrofit = it }
        }
    }

    private fun authRetrofit(): Retrofit {
        authRetrofit?.let { return it }
        return synchronized(this) {
            authRetrofit ?: createAuthRetrofit().also { authRetrofit = it }
        }
    }

    private fun createPublicRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl())
            .client(publicOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl())
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun currentBaseUrl(): String = baseUrlOverride ?: ApiConfig.BASE_URL

    @VisibleForTesting
    fun overrideBaseUrlForTests(baseUrl: String?) {
        baseUrlOverride = baseUrl?.let { if (it.endsWith("/")) it else "$it/" }
        publicRetrofit = null
        authRetrofit = null
    }

    val celestialApi: CelestialApi
        get() = publicRetrofit().create(CelestialApi::class.java)

    val authApi: AuthApi
        get() = publicRetrofit().create(AuthApi::class.java)

    val profileApi: ProfileApi
        get() = authRetrofit().create(ProfileApi::class.java)

    val favoriteApi: FavoriteApi
        get() = authRetrofit().create(FavoriteApi::class.java)

    val eventApi: EventApi
        get() = authRetrofit().create(EventApi::class.java)

    val weatherApi: WeatherApi
        get() = publicRetrofit().create(WeatherApi::class.java)

    val chatApi: ChatApi
        get() = authRetrofit().create(ChatApi::class.java)

    fun getSessionStorage(): SessionStorage = sessionStorage
}
