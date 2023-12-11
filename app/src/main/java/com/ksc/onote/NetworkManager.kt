package com.ksc.onote

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ksc.onote.authorization.AuthorizeManager
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject

interface NetworkGetListener<T> {
    fun getResult(`object`: T)
    fun getError(message: String)
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
        if (key == null) {
            listener.getResult(null)
            return
        }

        jsonParams["img"] = param1
        jsonParams["access_token"] = key

        val request = defaultRequest(url, null, Request.Method.POST, JSONObject(jsonParams),
            { s -> listener.getError(s) },
            { response -> listener.getResult(response.getJSONObject("response").getString("img")) })
        requestQueue.add(request)
    }

    fun getRequestLogin(authCode: String, listener: NetworkGetListener<Boolean>) {
        val url = authURL + "login"
        val request = defaultRequest(url, Pair("code", authCode), Request.Method.GET, null,
            { s ->
                listener.getError(s)
                listener.getResult(false)
            },
            { response ->
                AuthorizeManager.getInstance()?.setAccessToken(response.getString("access_token"))
                listener.getResult(true)
            })
        requestQueue.add(request)

        Log.d(TAG, request.toString())

    }

    fun postUpdateNote(name: String, note: JSONObject, listener: NetworkGetListener<Boolean?>, new:Boolean) {
        Log.d(
            "$TAG: ",
            "Post Update Note"
        )
        val url = dbURL + "api/database/page"
        val jsonParams: MutableMap<String?, Any?> = HashMap()
        val key = AuthorizeManager.getInstance()?.getAccessToken()
        if (key == null) {
            listener.getResult(null)
            return
        }

        jsonParams["old_page_name"] = if(new) null else name
        jsonParams["access_token"] = key
        val page = HashMap<String?, Any?>()
        page["name"] = name
        jsonParams["new_page"] = page
        val obj = JSONObject(jsonParams)
        obj.getJSONObject("new_page").put("data",note)

        Log.d(TAG,obj.toString())

        val request = defaultRequest(url, null, Request.Method.POST, obj,
            { s -> listener.getError(s); listener.getResult(false) },
            { _ -> listener.getResult(true) })
        requestQueue.add(request)
    }

    fun getRequestNameList(listener: NetworkGetListener<JSONArray>) {
        val url = dbURL + "api/database/namelist"
        val key = AuthorizeManager.getInstance()?.getAccessToken()

        if (key == null) {
            Log.e(TAG, "No Key")
            listener.getResult(JSONArray())
            return
        }
        val request = defaultRequest(url, Pair("access_token", key), Request.Method.GET, null,
            { s -> listener.getError(s) },
            { obj -> listener.getResult(obj.getJSONObject("response").getJSONArray("namelist")) })
        requestQueue.add(request)
    }

    fun deleteRequest(name: String, listener: NetworkGetListener<Boolean>) {
        val url = dbURL + "api/database/page"
        val key = AuthorizeManager.getInstance()?.getAccessToken()
        if (key == null) {
            listener.getResult(false)
            return
        }

        val request = defaultRequest(url,
            listOf(Pair("access_token", key), Pair("page_name", name)),
            Request.Method.DELETE,null,
            { s -> listener.getError(s); listener.getResult(false) },
            { _ -> listener.getResult(true) })
        requestQueue.add(request)
    }

    fun readRequest(name:String, listener: NetworkGetListener<JSONObject?>){
        val url = dbURL + "api/database/page"
        val key = AuthorizeManager.getInstance()?.getAccessToken()
        if (key == null) {
            listener.getResult(null)
            return
        }

        val request = defaultRequest(url,
            listOf(Pair("access_token", key), Pair("page_name", name)),
            Request.Method.GET, null,
            {s -> listener.getError(s); listener.getResult(null)} ,
            {response -> listener.getResult(response)})
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

    private fun defaultRequest(
        url: String,
        header: Pair<String, String>,
        method: Int,
        body: JSONObject?,
        errorListener: (String) -> Unit,
        successListener: ((JSONObject) -> Unit)?
    ): JsonObjectRequest =
        defaultRequest(url, listOf(header), method, body, errorListener, successListener)

    private fun defaultRequest(
        url: String,
        header: List<Pair<String, String>>?,
        method: Int,
        body: JSONObject?,
        errorListener: (String) -> Unit,
        successListener: ((JSONObject) -> Unit)?
    ): JsonObjectRequest {
        var newUrl = url
        if(header!=null){
            newUrl += "?"
            for(h in header){
                newUrl += "${h.first}=${h.second}&"
            }
            newUrl = newUrl.dropLast(1)
        }

        return JsonObjectRequest(
            method, newUrl, body ?: JSONObject(),
            { response ->
                if (defaultProcess(response) { s -> errorListener(s) })
                    successListener?.invoke(response)
            }, {
                if (it.networkResponse != null) {
                    Log.d(
                        "$TAG: ",
                        "Error Response code: " + it.networkResponse.statusCode
                    )
                    errorListener("HTTP Error. Error code: " + it.networkResponse.statusCode)
                } else {
                    Log.d(
                        "$TAG: ",
                        "Unknown Error :" + (it?.message ?: "")
                    )
                    errorListener("Unknown Error. Message: " + (it?.message ?: ""))
                }
            })
    }

    private fun defaultProcess(response: JSONObject, errorListener: (String) -> Unit): Boolean {
        Log.d(
            "$TAG: ",
            "somePostRequest Response : $response"
        )
        return if (checkResponseSuccess(response)) {
            AuthorizeManager.getInstance()?.setAccessToken(response.getString("access_token"))
            true
        } else {
            errorListener(response.getJSONObject("response").getString("message"))
            false
        }
    }

    private fun checkResponseSuccess(json: JSONObject): Boolean {
        if (!json.has("success")) {
            Log.e(TAG, "Invalid response.")
            return false
        }

        if (!json.getBoolean("success")) {
            Log.e(TAG, "Request Failed.")
            Log.e(TAG, json.getJSONObject("response").getString("message"))
            return false
        }

        return true
    }
}