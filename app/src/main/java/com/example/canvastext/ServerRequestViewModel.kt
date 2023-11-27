package com.example.canvastext

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay

class ServerRequestViewModel: ViewModel() {
    suspend fun getFormulaFromServer(image: Bitmap):Pair<String,Boolean>{
        var finished = false
        var response:String = ""
        var success:Boolean = false
        NetworkManager.getInstance()?.somePostRequestReturningString("",object:NetworkGetListener<String?>{
            override fun getResult(`object`: String?) {
                if(`object`==null){
                    success = false
                }
                else
                    response=`object`
                finished = true
            }

        })
        while(finished){
            delay(100L)
        }
        return Pair(response,success)
    }

    suspend fun getGraphFromServer(function: String):Bitmap{
        delay(500L)
        return Bitmap.createBitmap(0,0,Bitmap.Config.ARGB_8888)
    }

    suspend fun getCalculateResultFromServer(formula: List<String>):List<String>{
        delay(500L)
        return listOf()
    }
}