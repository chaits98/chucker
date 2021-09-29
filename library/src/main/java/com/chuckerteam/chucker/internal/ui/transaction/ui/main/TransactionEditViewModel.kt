package com.chuckerteam.chucker.internal.ui.transaction.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction

internal class TransactionEditViewModel : ViewModel() {
    private val _transaction: MutableLiveData<HttpTransaction> = MutableLiveData()

    var transaction: HttpTransaction?
        get() {
            return _transaction.value
        }
        set(value) {
            _transaction.value = value
        }
}
