package com.example.canvastext

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.Base64


private fun longLog(TAG:String, msg:String){
    val len = msg.length
    val MAX_LEN = 2000
        if (len > MAX_LEN) {
            var idx = 0
            var nextIdx = 0
            while (idx < len) {
                nextIdx += MAX_LEN
                Log.d(TAG, msg.substring(idx, if (nextIdx > len) len else nextIdx))
                idx = nextIdx
            }
        }
    else{
            Log.d(TAG,msg)
        }
}

private fun encodeImage(bm: Bitmap): String? {
    val baos = ByteArrayOutputStream()
    bm.compress(Bitmap.CompressFormat.PNG, 100, baos)
    val b = baos.toByteArray()
    val encoder = Base64.getEncoder()

    longLog("Encode Result", encoder.encodeToString(b))
    return encoder.encodeToString(b)
}
class ServerRequestViewModel: ViewModel() {
    suspend fun getFormulaFromServer(image: Bitmap):Pair<String,Boolean>{
        var finished = false
        var response:String = ""
        var success:Boolean = false

        val request = encodeImage(image)

        NetworkManager.getInstance()?.somePostRequestReturningString(request,object:NetworkGetListener<String?>{
            override fun getResult(`object`: String?) {
                if(`object`==null){
                    success = false
                }
                else
                    response=`object`
                finished = true
            }

        })
        while(!finished){
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