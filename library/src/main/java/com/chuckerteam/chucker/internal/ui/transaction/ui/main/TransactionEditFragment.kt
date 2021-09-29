package com.chuckerteam.chucker.internal.ui.transaction.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chuckerteam.chucker.databinding.ChuckerTransactionEditFragmentBinding
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction

internal class TransactionEditFragment(
    private val transaction: HttpTransaction,
    private val requestActionListener: RequestActionListener
) : Fragment() {
    private lateinit var viewModel: TransactionEditViewModel
    private lateinit var transactionBinding: ChuckerTransactionEditFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        transactionBinding = ChuckerTransactionEditFragmentBinding.inflate(inflater, container, false)
        return transactionBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TransactionEditViewModel::class.java)
        viewModel.transaction = transaction
        with(transactionBinding) {
            viewModel.transaction?.requestBody?.let {
                etBody.setText(it)
            }
            viewModel.transaction?.requestHeaders?.let {
                etHeaders.setText(it)
            }
            btSend.setOnClickListener {
                val newHttpTransaction = HttpTransaction(viewModel.transaction!!)
                newHttpTransaction.requestBody = etBody.text.toString()
                newHttpTransaction.requestHeaders = etHeaders.text.toString()
                requestActionListener.sendRequest(newHttpTransaction)
            }
        }
    }

    companion object {
        fun newInstance(
            transaction: HttpTransaction,
            requestActionListener: RequestActionListener
        ): TransactionEditFragment = TransactionEditFragment(
            transaction,
            requestActionListener
        )
    }
}

internal interface RequestActionListener {
    fun sendRequest(transaction: HttpTransaction)
}
