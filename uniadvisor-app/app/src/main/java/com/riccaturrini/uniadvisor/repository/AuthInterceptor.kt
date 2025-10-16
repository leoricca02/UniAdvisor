// File: repository/AuthInterceptor.kt
package com.riccaturrini.uniadvisor.repository

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor leggero e non bloccante.
 * Non accede a Firebase: il token viene passato dal Repository.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().build()
        return chain.proceed(request)
    }
}
