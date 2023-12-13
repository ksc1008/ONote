package com.ksc.onote.dataloader

import android.content.Context
import org.json.JSONObject

class JsonLoader(context: Context) {
    private var data:JSONObject? = null

    fun loadData():JSONObject?{
        return data
    }

    fun releaseData(){
        data = null
    }

    fun putData(newData:JSONObject){
        data = newData
    }

    fun hasData():Boolean = (data!=null)

    companion object {
        private const val TAG = "JsonLoader"
        private var instance: JsonLoader? = null

        @Synchronized
        fun getInstance(context: Context): JsonLoader? {
            if (instance == null) instance = JsonLoader(context)
            return instance
        }

        //this is so you don't need to pass context each time
        @Synchronized
        fun getInstance(): JsonLoader? {
            checkNotNull(instance) {
                JsonLoader::class.java.simpleName +
                        " is not initialized, call getInstance(...) first"
            }
            return instance
        }
    }
}