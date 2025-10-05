package com.example.baytro.viewModel.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.baytro.data.IdCardInfoWithImages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IdCardDataViewModel : ViewModel() {

    companion object {
        private const val TAG = "IdCardDataViewModel"
    }

    private val _idCardInfoWithImages = MutableStateFlow<IdCardInfoWithImages?>(null)
    val idCardInfoWithImages: StateFlow<IdCardInfoWithImages?> = _idCardInfoWithImages

    fun setIdCardInfo(infoWithImages: IdCardInfoWithImages) {
        Log.d(TAG, "setIdCardInfo: Storing ID card data with images: ${infoWithImages.idCardInfo}")
        Log.d(TAG, "setIdCardInfo: Front image URL: ${infoWithImages.frontImageUrl}")
        Log.d(TAG, "setIdCardInfo: Back image URL: ${infoWithImages.backImageUrl}")
        _idCardInfoWithImages.value = infoWithImages
        Log.d(TAG, "setIdCardInfo: Current state after setting: ${_idCardInfoWithImages.value}")
    }

    fun clearIdCardInfo() {
        Log.d(TAG, "clearIdCardInfo: Clearing stored ID card data with images")
        _idCardInfoWithImages.value = null
    }
}
