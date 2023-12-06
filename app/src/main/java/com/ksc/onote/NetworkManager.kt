package com.ksc.onote

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

interface NetworkGetListener<T> {
    fun getResult(`object`: T)
}

class NetworkManager private constructor(context: Context) {
    //for Volley API
    var requestQueue: RequestQueue

    init {
        requestQueue = Volley.newRequestQueue(context.applicationContext)
    }

    fun somePostRequestReturningString(param1: Any?, listener: NetworkGetListener<String?>) {
        val url = prefixURL + "api/mathpix"
        val jsonParams: MutableMap<String?, Any?> = HashMap()
        jsonParams["img"] = param1
        val request = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(jsonParams),
            { response:JSONObject ->
                Log.d(
                    "$TAG: ",
                    "somePostRequest Response : $response"
                )
                if(response.has("latex_styled"))
                    listener.getResult(response.getString("latex_styled"))
                else{
                    listener.getResult("")
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

    fun postRequestToMathPix(param1: Any?, listener: NetworkGetListener<String?>) {
        val url = prefixURL2 + "v3/text"
        val jsonParams: MutableMap<String?, Any?> = HashMap()
        jsonParams["src"] = "data:image/jpeg;base64, $param1"
        val request = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(jsonParams),
            { response ->
                Log.d(
                    "$TAG: ",
                    "somePostRequest Response : $response"
                )
                listener.getResult(response.toString())
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

    companion object {
        private const val TAG = "NetworkManager"
        private var instance: NetworkManager? = null
        private const val prefixURL = "https://noteapp.k-paas.org/"
        private const val prefixURL2 = "https://api.mathpix.com/"
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