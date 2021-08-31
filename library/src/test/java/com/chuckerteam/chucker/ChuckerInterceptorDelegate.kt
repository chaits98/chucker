package com.chuckerteam.chucker

import android.content.Context
import android.content.SharedPreferences
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.support.CacheDirectoryProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Response
import org.mockito.ArgumentMatchers
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

internal class ChuckerInterceptorDelegate(
    maxContentLength: Long = 250000L,
    headersToRedact: Set<String> = emptySet(),
    alwaysReadResponseBody: Boolean = false,
    cacheDirectoryProvider: CacheDirectoryProvider,
) : Interceptor {
    private val idGenerator = AtomicLong()
    private val transactions = CopyOnWriteArrayList<HttpTransaction>()

    private val mockEditor = mockk<SharedPreferences.Editor> {
        every { remove(ArgumentMatchers.anyString()) } returns this
        every { remove("chucker_saved_redacted_headers") } returns this
        every { putString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()) } returns this
        every { putString("chucker_saved_redacted_headers", ArgumentMatchers.anyString()) } returns this
        every { putString("chucker_saved_redacted_headers", "Header-To-Redact") } returns this
        every { apply() } returns Unit
    }
    private val sharedPrefs = mockk<SharedPreferences> {
        every { edit() } returns mockEditor
        every { getString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()) } returns "dummy_value"
        every { getString(ArgumentMatchers.anyString(), null) } returns "dummy_value"
    }
    private val mockContext = mockk<Context> {
        every { getString(R.string.chucker_body_content_truncated) } returns "\n\n--- Content truncated ---"
        every { getSharedPreferences("chucker_prefs", 0) } returns sharedPrefs
    }

    private val mockCollector = mockk<ChuckerCollector> {
        every { onRequestSent(any()) } returns Unit
        every { onResponseReceived(any()) } answers {
            val transaction = (args[0] as HttpTransaction)
            transaction.id = idGenerator.getAndIncrement()
            transactions.add(transaction)
        }
    }

    private val chucker = ChuckerInterceptor(
        context = mockContext,
        collector = mockCollector,
        maxContentLength = maxContentLength,
        headersToRedact = headersToRedact,
        cacheDirectoryProvider = cacheDirectoryProvider,
        alwaysReadResponseBody = alwaysReadResponseBody,
    )

    internal fun expectTransaction(): HttpTransaction {
        if (transactions.isEmpty()) {
            throw AssertionError("Expected transaction but was empty")
        }
        return transactions.removeAt(0)
    }

    internal fun expectNoTransactions() {
        if (transactions.isNotEmpty()) {
            throw AssertionError("Expected no transactions but found ${transactions.size}")
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        return chucker.intercept(chain)
    }
}
