package com.ksc.onote.drawingCanvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.Reader


class BitmapCacheManager(private val ctx:Context) {
    enum class CachingState{DONE, CACHING, NOTCACHED}
    val scope = CoroutineScope(Job()+ Dispatchers.IO)

    private val encodedImageCachingState:MutableMap<Int,CachingState> = HashMap()
    private val canvasCachingState:MutableMap<Int,CachingState> = HashMap()
    private val cachingState:MutableMap<Int,CachingState> = HashMap()
    private val cacheMap:MutableMap<Int,Int> = HashMap()
    private val encodedImageMap:MutableMap<Int,String> = HashMap()
    private val canvasCacheMap:MutableMap<Int,String> = HashMap()
    private fun getCacheDir():File? = ctx.externalCacheDir

    private val currentCacheDir:File? by lazy{getCacheDir()}

    fun putPageToCache(page:Int, bitmaps:List<Bitmap>):Boolean{
        cachingState[page] = CachingState.CACHING
        if(cacheMap.containsKey(page)){
            Log.d(TAG,"Already cached. stopped caching.")
            for (i in bitmaps.indices) {
                bitmaps[i].recycle()
            }
        }
        else {
            if (currentCacheDir == null) {
                Log.e(TAG, "Failed to cache bitmap")
                return false
            }
            for (i in bitmaps.indices) {
                val fName = "cache_${page}_$i"
                val f = File(currentCacheDir, fName)
                if(!saveFile(bitmaps[i],f)) {
                    Log.e(TAG, "Failed to cache bitmap")
                    return false
                }
                bitmaps[i].recycle()
            }
        }
        cacheMap[page] = bitmaps.size
        cachingState[page] = CachingState.DONE
        return true
    }

    fun putEncodedBackgroundToCache(page:Int, background:String):Boolean{
        encodedImageCachingState[page] = CachingState.CACHING
        if(encodedImageMap.containsKey(page)){
            Log.d(TAG,"Already cached. stopped caching.")
        }
        else {
            if (currentCacheDir == null) {
                Log.e(TAG, "Failed to cache bitmap")
                return false
            }
            val fName = "cache2_${page}"
            val f = File(currentCacheDir, fName)
            if(!saveFile(background,f)) {
                Log.e(TAG, "Failed to cache bitmap")
                return false
            }
            encodedImageMap[page] = fName
        }
        encodedImageCachingState[page] = CachingState.DONE
        return true
    }

    fun putCanvasToCache(page:Int, canvas:CanvasModel):Boolean{
        canvasCachingState[page] = CachingState.CACHING
        if(canvasCacheMap.containsKey(page)){
            Log.d(TAG,"Already cached canvas. renewing cache.")
        }
        if (currentCacheDir == null) {
            Log.e(TAG, "Failed to cache canvas")
            return false
        }
        val fName = "cache0_${page}"
        val f = File(currentCacheDir, fName)
        if(!saveFile(canvas,f)){
            Log.e(TAG, "Failed to cache canvas")
            return false
        }
        canvasCacheMap[page] = fName
        canvasCachingState[page] = CachingState.DONE
        return true
    }

    fun getPageFromCache(page:Int):List<Bitmap>{
        val result:MutableList<Bitmap> = mutableListOf()
        if(!cacheMap.containsKey(page)){
            Log.e(TAG,"No cached data in disk. stopped retrieving.")
            return listOf()
        }
        if (currentCacheDir == null) {
            Log.e(TAG, "Failed to retrieving bitmap")
            return listOf()
        }
        for (i in 0 until cacheMap[page]!!){
            val fName = "cache_${page}_$i"
            val f = File(currentCacheDir, fName)
            val bitmap = getFile(f)
            if(bitmap == null){
                Log.e(TAG, "Failed to retrieving bitmap")
                return listOf()
            }
            result.add(bitmap)
        }
        return result
    }

    fun getCanvasFromCache(page: Int): CanvasModel? {
        if (!canvasCacheMap.containsKey(page)) {
            Log.e(TAG, "No cached data in disk. stopped retrieving.")
            return null
        }
        if (currentCacheDir == null) {
            Log.e(TAG, "Failed to retrieving canvas")
            return null
        }
        val fName = canvasCacheMap[page]
        if(fName == null){
            Log.e(TAG, "Failed to retrieving canvas")
            return null
        }
        val f = File(currentCacheDir, fName)
        val canvas = getCanvasFile(f)
        if (canvas == null) {
            Log.e(TAG, "Failed to retrieving canvas")
            return null
        }
        return canvas
    }

    fun getEncodedBackgroundFromCache(page: Int): String? {
        if (!encodedImageCachingState.containsKey(page)) {
            Log.e(TAG, "No cached data in disk. stopped retrieving.")
            return null
        }
        if (currentCacheDir == null) {
            Log.e(TAG, "Failed to retrieving encoded image")
            return null
        }
        val fName = encodedImageMap[page]
        if(fName == null){
            Log.e(TAG, "Failed to retrieving encoded image")
            return null
        }
        val f = File(currentCacheDir, fName)
        val encoded = getEncodedImage(f)
        if (encoded == null) {
            Log.e(TAG, "Failed to retrieve encoded image")
            return null
        }
        return encoded
    }

    suspend fun getEncodedImageAsync(page: Int): String? {
        if ((encodedImageCachingState[page] ?: CachingState.NOTCACHED) == CachingState.NOTCACHED) {
            return null
        }
        if (encodedImageCachingState[page] == CachingState.CACHING) {
            val waitlimit = 100
            var i = 0
            while (i++ < waitlimit && encodedImageCachingState[page] != CachingState.DONE) {
                delay(100)
            }
            if (encodedImageCachingState[page] != CachingState.DONE) {
                return null
            }
        }
        return getEncodedBackgroundFromCache(page)
    }

    fun getPageAsync(page:Int, callback:(List<Bitmap>?)->Unit){
        scope.launch {
            if((cachingState[page] ?: CachingState.NOTCACHED) == CachingState.NOTCACHED){
                callback(null)
                return@launch
            }
            if(cachingState[page] == CachingState.CACHING){
                val waitlimit = 100
                var i = 0
                while(i++ < waitlimit && cachingState[page] != CachingState.DONE){
                    delay(100)
                }
                if(cachingState[page] != CachingState.DONE){
                    callback(null)
                    return@launch
                }
            }
            val result = getPageFromCache(page)
            if(result.isEmpty())
                callback(null)
            else
                callback(result)
        }
    }

    fun getCanvasAsync(page:Int, callback:(CanvasModel?)->Unit){
        scope.launch {
            if((canvasCachingState[page] ?: CachingState.NOTCACHED) == CachingState.NOTCACHED){
                callback(null)
                return@launch
            }
            if(canvasCachingState[page] == CachingState.CACHING){
                val waitlimit = 100
                var i = 0
                while(i++ < waitlimit && canvasCachingState[page] != CachingState.DONE){
                    delay(100)
                }
                if(canvasCachingState[page] != CachingState.DONE){
                    callback(null)
                    return@launch
                }
            }
            val result = getCanvasFromCache(page)
            callback(result)
        }
    }

    fun clearDisk(){
        if(currentCacheDir == null)
            return
        var cnt = 0
        for (file in ((currentCacheDir!!.listFiles())?: arrayOf<File>())) {
            if (!file.isDirectory) {
                file.delete()
                cnt++
            }
        }
        cachingState.clear()
        canvasCacheMap.clear()
        cacheMap.clear()
        canvasCachingState.clear()
        encodedImageMap.clear()
        encodedImageCachingState.clear()
        Log.d(TAG,"Cleared $cnt temp files")
    }

    private fun getFile(f: File): Bitmap? {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(f)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
        return BitmapFactory.decodeStream(fis,null,BitmapFactory.Options().apply {
            this.inMutable = false
            this.inPreferredConfig = Bitmap.Config.HARDWARE
        })
    }

    private fun getCanvasFile(f: File): CanvasModel? {
        return try {
            val reader:Reader = FileReader(f)
            val gson = Gson()
            gson.fromJson(reader,CanvasModel::class.java)
        }catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun getEncodedImage(f: File): String? {
        var content:String? = null
        try {
            FileReader(f).use{
                val chars = CharArray(f.length().toInt())
                it.read(chars)
                content = String(chars)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        } catch (e:IOException){
            e.printStackTrace()
            return null
        }
        return content
    }

    private fun saveFile(bitmap: Bitmap, file:File):Boolean{
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos)
            fos.flush()
            fos.close()
        }
        catch(e:IOException){
            e.printStackTrace()
            return false
        }
        return true
    }
    private fun saveFile(canvas: CanvasModel, file:File):Boolean{
        try {
            val gson = Gson()
            val fw = FileWriter(file)
            gson.toJson(canvas, fw)
            fw.flush()
            fw.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private fun saveFile(encodedImage: String, file:File):Boolean{
        try {
            file.printWriter().use { out ->
                out.write(encodedImage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun initiateNote(pages:Int){
        for(i in 0 until pages){
            cachingState[i] = CachingState.CACHING
            canvasCachingState[i] = CachingState.CACHING
            encodedImageCachingState[i] = CachingState.CACHING
        }
    }

    companion object {
        private const val TAG = "Bitmap Cache Manager"
        @SuppressLint("StaticFieldLeak")
        private var instance: BitmapCacheManager? = null

        @Synchronized
        fun getInstance(context: Context): BitmapCacheManager? {
            if (instance == null) instance = BitmapCacheManager(context)
            return instance
        }

        //this is so you don't need to pass context each time
        @Synchronized
        fun getInstance(): BitmapCacheManager? {
            checkNotNull(instance) {
                BitmapCacheManager::class.java.simpleName +
                        " is not initialized, call getInstance(...) first"
            }
            return instance
        }
    }
}