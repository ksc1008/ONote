package com.example.canvastext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FormulaViewModel : ViewModel() {
    val outputString: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }

    fun setFormula(s:String){
        outputString.value = s
    }
}