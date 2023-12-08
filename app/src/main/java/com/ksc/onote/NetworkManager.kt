package com.ksc.onote

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ksc.onote.authorization.AuthorizeManager
import org.json.JSONObject

interface NetworkGetListener<T> {
    fun getResult(`object`: T)
    fun getError(message:String)
}

class NetworkManager private constructor(context: Context) {
    //for Volley API
    var requestQueue: RequestQueue

    init {
        requestQueue = Volley.newRequestQueue(context.applicationContext)
    }

    fun postRequestOcrServer(param1: Any?, listener: NetworkGetListener<String?>) {
        val url = ocrURL + "api/mathpix"
        val jsonParams: MutableMap<String?, Any?> = HashMap()
        val key = AuthorizeManager.getInstance()?.getAccessToken()
        if(key==null){
            listener.getResult(null)
            return
        }

        jsonParams["img"] = param1
        jsonParams["access_token"] = key
        val request = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(jsonParams),
            { response:JSONObject ->
                Log.d(
                    "$TAG: ",
                    "somePostRequest Response : $response"
                )
                if(checkResponseSuccess(response)){
                    listener.getResult(response.getJSONObject("response").getString("img"))
                    AuthorizeManager.getInstance()?.setAccessToken(response.getString("access_token"))
                }
                else{
                    listener.getError("Error")
                }
            }
        ) { error ->
            if (null != error.networkResponse) {
                Log.d(
                    "$TAG: ",
                    "Error Response code: " + error.networkResponse.statusCode
                )
                listener.getResult(null)
            }
        }
        requestQueue.add(request)
    }

    private fun checkResponseSuccess(json:JSONObject):Boolean{
        if(!json.has("success")){
            Log.e(TAG,"Invalid response.")
            return false
        }

        if(!json.getBoolean("success")){
            Log.e(TAG,"Request Failed.")
            Log.e(TAG,json.getJSONObject("response").getString("message"))
            return false
        }

        return true
    }

    fun getAccessToken(authCode: String, listener: NetworkGetListener<Boolean>) {
        val url = authURL + "login"
        val request = JsonObjectRequest(
            Request.Method.GET, "$url?code=$authCode", null,
            { response ->
                Log.d(
                    "$TAG: ",
                    "somePostRequest Response : $response"
                )
                if(checkResponseSuccess(response)){
                    AuthorizeManager.getInstance()?.setAccessToken(response.getString("access_token"))
                    listener.getResult(true)
                }
                else{
                    listener.getError("Error")
                    listener.getResult(false)
                }
            }
        ) { error ->
            if (null != error.networkResponse) {
                Log.d(
                    "$TAG: ",
                    "Error Response code: " + error.networkResponse.statusCode
                )
                listener.getResult(false)
            }
        }
        requestQueue.add(request)
    }

    companion object {
        private const val TAG = "NetworkManager"
        private var instance: NetworkManager? = null
        private const val authURL = "https://noteappauth.k-paas.org/"
        private const val ocrURL = "https://noteappocr.k-paas.org/"
        private const val dbURL = "https://noteappnoteapi.k-paas.org/"
        @Synchronized
        fun getInstance(context: Context): NetworkManager? {
            if (instance == null) instance = NetworkManager(context)
            return instance
        }

        //this is so you don't need to pass context each time
        @Synchronized
        fun getInstance(): NetworkManager? {
            checkNotNull(instance) {
                NetworkManager::class.java.simpleName +
                        " is not initialized, call getInstance(...) first"
            }
            return instance
        }
    }
}