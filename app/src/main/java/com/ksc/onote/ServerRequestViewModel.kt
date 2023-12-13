package com.ksc.onote

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.ksc.onote.utils.Base64Tool
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.Base64


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