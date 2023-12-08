package com.ksc.onote.drawingCanvas

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ksc.onote.utils.PDFTool
import java.io.File
import kotlin.math.abs

class CanvasViewModel:ViewModel() {

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

    fun toData():NoteModel{
        val list:MutableList<CanvasModel> = mutableListOf()
        for(c in canvasList.value!!){
            list.add(DrawingCanvas.serialize(c))
        }

        return NoteModel("NoName","","",list)
    }

    fun createEmpty(canvasWidth:Int, canvasHeight:Int){
        clearData()
        canvasList.value?.add(DrawingCanvas(canvasWidth,canvasHeight))
        calculateCanvasPositions(50)
    }

    fun createFromPdf(pdfFile: File, densityDpi:Int){
        val bitmaps = PDFTool.read_from_file(pdfFile,densityDpi)
        if(bitmaps.isNullOrEmpty()){
            createEmpty(2000,2000)
            return
        }
        clearData()
        for(b in bitmaps){
            canvasList.value?.add(DrawingCanvas(b.width,b.height,b))
        }
        calculateCanvasPositions(50)
    }

    fun createFromPdfUri(pdfUri: Uri, context: Context){
        val bitmaps = PDFTool.read_from_pdf_uri(pdfUri,context)
        if(bitmaps.isNullOrEmpty()){
            createEmpty(2000,2000)
            return
        }
        clearData()
        for(b in bitmaps){
            canvasList.value?.add(DrawingCanvas(b.width,b.height,b))
        }
        calculateCanvasPositions(50)
    }

    fun clearData(){
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

    override fun onCleared() {
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