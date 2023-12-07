package com.ksc.onote.formulaViewer

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FormulaViewModel : ViewModel() {
    private var hasFormula:Boolean = false
    private var calculated:Boolean = false

    val outputString: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }

    val calculationResult: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }

    fun setFormula(s:String){
        outputString.value = s
        hasFormula = true
        calculated = false
    }

    fun hasFormula():Boolean{
        return hasFormula
    }

    fun isCalculated():Boolean{
        return calculated
    }

    fun setCalculation(s:String){
        calculationResult.value = s
        calculated = true
    }

    fun getFormula():String{
        return outputString.value?:""
    }
}