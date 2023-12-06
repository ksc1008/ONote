package com.ksc.onote.formulaViewer

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FormulaViewModel : ViewModel() {
    private var hasFormula:Boolean = false

    val outputString: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }

    fun setFormula(s:String){
        outputString.value = s
        hasFormula = true
    }

    fun hasFormula():Boolean{
        return hasFormula
    }
}