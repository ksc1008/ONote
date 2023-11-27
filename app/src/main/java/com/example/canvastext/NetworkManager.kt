package com.example.canvastext

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
        val url = prefixURL + "this/request/suffix"
        val jsonParams: MutableMap<String?, Any?> = HashMap()
        jsonParams["param1"] = param1
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
        private const val prefixURL = "https://noteapp.k-paas.org/api/mathpix"
        @Synchronized
        fun getInstance(context: Context): NetworkManager? {
            if (null == instance) instance = NetworkManager(context)
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