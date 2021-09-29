package com.chuckerteam.chucker.internal.ui.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.support.PrefUtils
import com.chuckerteam.chucker.internal.ui.BaseChuckerActivity
import com.chuckerteam.chucker.internal.ui.transaction.ui.main.RequestActionListener
import com.chuckerteam.chucker.internal.ui.transaction.ui.main.TransactionEditFragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

internal class TransactionEditActivity : BaseChuckerActivity(), RequestActionListener {
    private val sendRequestCallback: Callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.message?.let { it1 -> Log.d(CurrentTransaction.transaction?.url.toString(), it1) }
            runOnUiThread {
                showToast(getString(R.string.chucker_request_failed))
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { it1 -> Log.d(CurrentTransaction.transaction?.url.toString(), it1) }
            runOnUiThread {
                showToast(getString(R.string.chucker_request_complete))
                finish()
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val collector = ChuckerCollector(
            context = applicationContext,
            showNotification = true,
            retentionPeriod = PrefUtils.getInstance(this).getRetentionPeriod()
        )

        val redactedHeaders = PrefUtils.getInstance(this).getRedactedHeaders()

        val chuckerInterceptor = ChuckerInterceptor.Builder(applicationContext)
            .collector(collector)
            .redactHeaders(redactedHeaders)
            .build()

        OkHttpClient.Builder()
            .addNetworkInterceptor(chuckerInterceptor)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chucker_transaction_edit_activity)
        setupToolbar()
        if (savedInstanceState == null) {
            CurrentTransaction.transaction?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, TransactionEditFragment.newInstance(it, this))
                    .commitNow()
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.title = getString(R.string.chucker_edit_transaction)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        CurrentTransaction.transaction = null
    }

    companion object {
        fun getLaunchIntent(
            context: Context,
            transaction: HttpTransaction
        ): Intent = Intent(
            context,
            TransactionEditActivity::class.java
        ).also {
            CurrentTransaction.transaction = transaction
        }
    }

    override fun sendRequest(transaction: HttpTransaction) {
        transaction.let {
            val headers = it.getParsedRequestHeaders()
            val body = it.getFormattedRequestBody()

            val requestBuilder: Request.Builder = Request.Builder().url(it.url.toString())
            if (headers != null) {
                for (header in headers) {
                    requestBuilder.addHeader(header.name, header.value)
                }
            }
            if (it.method != null && it.requestContentType != null) {
                requestBuilder.method(it.method!!, body.toRequestBody(it.requestContentType!!.toMediaType()))
            }
            okHttpClient.newCall(requestBuilder.build()).enqueue(sendRequestCallback)
        }
    }
}

internal object CurrentTransaction {
    var transaction: HttpTransaction? = null
}
