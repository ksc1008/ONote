package com.ksc.onote.authorization

import android.content.Context
import android.content.SharedPreferences
import com.android.volley.toolbox.Volley
import com.ksc.onote.NetworkManager

class AuthorizeManager private constructor(context: Context) {
    private var authorized = false
    private val key = "AccessToken"


    private var sharedPreferences:SharedPreferences? = null
    fun getAccessToken():String?{
        authorized = false
        return sharedPreferences?.getString(key,null)
    }

    fun peekAccessToken():String?{
        return sharedPreferences?.getString(key,null)
    }

    fun setAccessToken(token:String){
        authorized = true
        sharedPreferences?.edit()?.putString(key,token)?.apply()
    }

    fun isAuthorized():Boolean{
        return authorized
    }

    init {
        sharedPreferences = context.getSharedPreferences("ows",Context.MODE_PRIVATE)
    }
    companion object{
        private const val TAG = "NetworkManager"
        private var instance: AuthorizeManager? = null
        @Synchronized
        fun getInstance(context: Context): AuthorizeManager? {
            if (instance == null) instance = AuthorizeManager(context)
            return instance
        }

        //this is so you don't need to pass context each time
        @Synchronized
        fun getInstance(): AuthorizeManager? {
            checkNotNull(instance) {
                NetworkManager::class.java.simpleName +
                        " is not initialized, call getInstance(...) first"
            }
            return instance
        }
    }
}