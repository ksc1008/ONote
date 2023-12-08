package com.ksc.onote

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.ksc.onote.utils.Base64Tool
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

class ServerRequestViewModel: ViewModel() {
    suspend fun getFormulaFromServer(image: Bitmap):Pair<String,Boolean>{
        var finished = false
        var response:String = ""
        var success:Boolean = false

        val request = Base64Tool.encodeImage(image)

        NetworkManager.getInstance()
            ?.postRequestOcrServer(request,object: NetworkGetListener<String?> {
            override fun getResult(`object`: String?) {
                if(`object`==null){
                    success = false
                }
                else {
                    response = `object`
                    success = true
                }
                finished = true
            }

                override fun getError(message: String) {
                    success = false
                    finished = true
                }

            })
        while(!finished){
            delay(100L)
        }
        return Pair(response,success)
    }
}