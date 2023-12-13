package com.ksc.onote.drawingCanvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.disklrucache.DiskLruCache
import com.ksc.onote.utils.Base64Tool
import com.ksc.onote.utils.PDFTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.min

class CanvasViewModel:ViewModel() {
    var noteName:String = ""

    var isNew:Boolean = true

    var canvasList: MutableLiveData<MutableList<DrawingCanvas>>
    = MutableLiveData<MutableList<DrawingCanvas>>(mutableListOf())
        private set
    var visibleCanvasList:MutableLiveData<List<DrawingCanvas>>
    = MutableLiveData<List<DrawingCanvas>>(listOf())
        private set
    var activeCanvas:DrawingCanvas? = null
        private set

    var borderHorizontal:Int = 0
        private set
    var borderBottom:Int = 0
        private set

    var visibleIdxStart = -1
        private set
    var visibleIdxEnd = -1
        private set

    var started = false

    suspend fun toDataAsync():NoteModel?{
        if(!started){
            return null
        }
        val list:MutableList<CanvasModel> = mutableListOf()
        for(c in canvasList.value!!){
            list.add(DrawingCanvas.serialize(c))
        }

        return NoteModel("NoName2","","",list)
    }

    fun createEmpty(canvasWidth:Int, canvasHeight:Int){
        clearData()
        canvasList.value?.add(DrawingCanvas(canvasWidth,canvasHeight))
        canvasList.value?.last()?.sleep()
        started = true
        calculateCanvasPositions(50)
    }

    fun createFromData(note:NoteModel){
        clearData()
        for(i in note.canvases.indices){
            canvasList.value?.add(DrawingCanvas.deserialize(note.canvases[i]))
            canvasList.value?.last()?.initiateCanvas(i,note.canvases[i])
        }
        viewModelScope.launch(Dispatchers.IO) {

            for(i in 0 until note.canvases.size) {
                val data = canvasList.value?.getOrNull(i) ?: break
                if(note.canvases[i].hasBG){
                    data.canvasPaper.initiateBackground(i,note.canvases[i].bg)
                }
                else{
                    data.canvasPaper.page = i
                }
                viewModelScope.launch(Dispatchers.Default) {
                    data.canvasPaper.makeEncodedString(note.canvases[i].bg)
                }
            }
            started = true
        }
        calculateCanvasPositions(50)
    }

    fun createFromPdfUri(pdfUri: Uri, context: Context){
        clearData()
        val result = PDFTool.read_from_pdf_uri(pdfUri,context)
        if(result.first == null){
            createEmpty(2000,2000)
            return
        }

        for((i,r) in result.second!!.withIndex()){
            if(min(r.x,r.y) > min(context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels)*1.2){
                val rescaleFactor = min(context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels) * 1.2 / min(r.x,r.y)
                canvasList.value?.add(DrawingCanvas((r.x * rescaleFactor).toInt(),(r.y * rescaleFactor).toInt(),true))
            }
            else {
                canvasList.value?.add(DrawingCanvas(r.x, r.y, true))
            }
            canvasList.value?.last()?.initiateCanvas(i)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val descriptor = result.first!!.second
            val renderer = result.first!!.first

            val pageCnt = renderer.pageCount
            val dpi = context.resources.displayMetrics.densityDpi
            for (i in 0 until pageCnt) {
                val page = renderer.openPage(i)

                val width = dpi / 72 * page.width
                val height = dpi / 72 * page.height

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                Log.d("PDF Load:","loaded page $i")

                page.close()

                val newBit:Bitmap = if(min(bitmap.width,bitmap.height) > min(context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels)*1.2){
                    val rescaleFactor = min(context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels) * 1.2 / min(bitmap.width,bitmap.height)
                    Bitmap.createScaledBitmap(bitmap.copy(Bitmap.Config.HARDWARE,false),
                        (bitmap.width * rescaleFactor).toInt(),
                        (bitmap.height * rescaleFactor).toInt(), true)
                } else{
                    bitmap.copy(Bitmap.Config.HARDWARE,false)
                }

                bitmap.recycle()

                canvasList.value?.get(i)?.canvasPaper?.initiateBackground(i,newBit)
                canvasList.value?.get(i)?.sleep()
                viewModelScope.launch(Dispatchers.Default) {
                    canvasList.value?.get(i)?.canvasPaper?.makeEncodedString()
                }

            }
            descriptor.close()
            renderer.close()
            started = true
        }
        calculateCanvasPositions(50)
    }

    private fun clearData(){
        activeCanvas = null
        visibleCanvasList.value = listOf()
        canvasList.value = mutableListOf()
    }

    fun setActiveCanvas(canvas:DrawingCanvas?){
        activeCanvas = canvas
    }

    fun setVisibleCanvas(from:Int, to:Int){
        if(visibleIdxStart!=-1){
            for(i in visibleIdxStart until from){
                canvasList.value?.get(i)?.deactivate()
            }
            for(i in to+1 until visibleIdxEnd){
                canvasList.value?.get(i)?.deactivate()
            }
        }
        visibleCanvasList.value = canvasList.value?.subList(from,to+1)
        visibleIdxStart = from
        visibleIdxEnd = to
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        GlobalScope.launch(Dispatchers.IO) {
            BitmapCacheManager.getInstance()?.clearDisk()
        }

        Log.d("Canvas View Model","View model cleared")
        super.onCleared()
    }

    fun calculateCanvasPositions(spacing:Int){
        var minX = 0
        var lastY = 0
        for(c in canvasList.value?: listOf()){
            c.setCanvasPosition(-c.width/2,lastY)
            if(c.x<minX)
                minX = c.x
            lastY += c.height + spacing
        }

        borderHorizontal = abs(minX)
        borderBottom = lastY
    }
}