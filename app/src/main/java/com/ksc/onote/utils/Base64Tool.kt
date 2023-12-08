package com.ksc.onote.utils

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.Base64

object Base64Tool {
    fun encodeImage(bm: Bitmap, format:Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(format, 90, baos)
        val b = baos.toByteArray()
        val encoder = Base64.getEncoder()

        return encoder.encodeToString(b)
    }

    fun decodeImage(encoded:String):Bitmap?{
        return try {
            val decoder = Base64.getDecoder()
            val encodeByte: ByteArray = decoder.decode(encoded)
            BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
        } catch(e: Exception){
            Log.e(ContentValues.TAG,e.toString())
            null
        }
    }
}