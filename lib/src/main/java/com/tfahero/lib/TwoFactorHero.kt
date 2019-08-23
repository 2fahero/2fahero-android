package com.tfahero.lib

import android.os.Handler
import android.os.Looper
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.lang.Exception
import java.util.concurrent.Executors

interface Callback {
    fun onCodeSent(codeIdentifier: String)
    fun onError(e: Error)
    fun onSuccess()
}

private const val BASE_URL = "https://2fahero.com/api/"

// heart of 2faHero API.
// @param publicKey is user's account publicKey
class TwoFactorHero(val publicKey: String) {

    init {
        if (publicKey == "") {
            throw IllegalArgumentException("public key is empty")
        }
    }

    private val service = createHttpService(publicKey)

    // create single thread executor to dispatch API requests
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    // send a 2fa code to a phone number
    // callback is required to report request's state
    fun send2fa(phone: String, callback: Callback) {
        val request = Send2faRequest(phone)
        executor.execute {
            try {
                val response = service.send2fa(request).execute()
                val body = response.body()
                if (response.isSuccessful) {
                    val data = body?.data
                    data?.codeIdentifier?.let {
                        handler.post { callback.onCodeSent(it) }
                    }
                }else {
                    val code = response.code()
                    handler.post { callback.onError(Error(body?.message, code, ErrorType.ServerError)) }
                }
            }catch (e: Exception) {
                handler.post { callback.onError(Error(e.localizedMessage, 0, ErrorType.HttpError)) }
            }
        }
    }

    // verify user's input code against code sent in a 2fa request
    fun verify2fa(code: String, codeIdentifier: String, callback: Callback) {
        val request = Verify2faRequest(code, codeIdentifier)
        try {
            val response = service.verify2fa(request).execute()
            val body = response.body()
            if (response.isSuccessful) {
                val data = body?.data
                when(data?.code) {
                    CODE_SUCCESS -> { handler.post { callback.onSuccess() } }
                    else -> { handler.post {callback.onError(Error(data?.message, data?.code, ErrorType.ServerError)) } }
                }
            }else {
                handler.post { callback.onError(Error(body?.message, response.code(), ErrorType.ServerError)) }
            }
        }catch (e: Exception) {
            handler.post { callback.onError(Error(e.localizedMessage, 0, ErrorType.HttpError)) }
        }
    }

    private fun createHttpService(publicKey: String): APIService {
        val okClient = createOkHttpClient(publicKey)
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okClient)
            .build()
        return retrofit.create(APIService::class.java)
    }

    private fun createOkHttpClient(publicKey: String): OkHttpClient {
        val interceptor = Interceptor {chain ->
            val newRequest = chain.request().newBuilder()
            newRequest.addHeader("2faHero-Auth-Key", publicKey)
            return@Interceptor chain.proceed(newRequest.build())
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
}

fun defaultCallback(): Callback {
    return object: Callback {
        override fun onCodeSent(codeIdentifier: String) { /* no-op */ }
        override fun onError(e: Error) { /* no-op */ }
        override fun onSuccess() {/* no-op */}
    }
}

interface APIService {
    @POST("2fa/new")
    fun send2fa(@Body request: Send2faRequest): Call<HttpResponse<Send2faResponse>>

    @POST("2fa/verify")
    fun verify2fa(@Body request: Verify2faRequest): Call<HttpResponse<Verify2faResponse>>
}