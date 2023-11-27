package com.example.canvastext.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.lifecycle.ViewModel
import java.util.LinkedList
import kotlin.math.pow

class CanvasViewModel: ViewModel() {
    var width:Int = 0
    var height:Int = 0

    enum class DrawMod{PENDOWN, PENUP, RESET, IDLE, IMAGEDOWN, IMAGEUP}
    var currentDrawMod:DrawMod = DrawMod.IDLE

    private var bitmapCache: Bitmap
    val piecewiseCanvas:PiecewiseCanvas = PiecewiseCanvas()
    private val strokeList: LinkedList<CanvasStroke> = LinkedList()
    private val bitmapList: LinkedList<CanvasBitmap> = LinkedList()
    private var _bitmap: Bitmap
    private var canvasTemp: Canvas
    private var placingBitmap:CanvasBitmap? = null

    val rectangleArea = RectangleArea()

    class PiecewiseCanvas{
        val pathPoints:MutableList<MutableList<MutableList<CanvasStroke.StrokePoint>>> = mutableListOf()

        var rows:Int = 0
        var cols:Int = 0

        fun changeCache(newRows:Int, newCols:Int){
            val dRow = newRows - rows
            val dCol = newCols - cols

            for (i:Int in 1..dRow){
                pathPoints.add(mutableListOf())
                for(j:Int in 1..dCol){
                    pathPoints.last().add(mutableListOf())
                }
            }

            cols = newCols
            rows = newRows
        }

        fun addPoint(x:Int, y:Int, point: CanvasStroke.StrokePoint){
            if(x<0 || x>=cols || y<0 || y>=rows){
                return
            }
            pathPoints[y][x].add(point)
            point.addToPiecewiseCanvas(pathPoints[y][x])
        }

        fun checkOverlap(x:Int, y:Int):MutableList<CanvasStroke.StrokePoint>{
            return pathPoints[y][x]
        }

    }

    fun resize(){

    }

    fun clear(){
        for(s in strokeList){
            s.removeStroke()
        }
        strokeList.clear()
        currentDrawMod = DrawMod.RESET
    }
    fun addStroke(strokeX:Float, strokeY:Float){

        val strokePaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 10f
        }
        val s:CanvasStroke = CanvasStroke(strokeX,strokeY,piecewiseCanvas, strokePaint)
        strokeList.add(s)

        currentDrawMod = DrawMod.PENDOWN
    }


    fun appendStroke(strokeX:Float, strokeY:Float){
        if(strokeList.isNotEmpty())
            strokeList.last().appendStroke(strokeX,strokeY)
    }

    fun removeCanvasStroke(s:CanvasStroke){
        strokeList.remove(s)
    }

    fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float){
        fun feasible(i:Int, j:Int):Boolean{
            if((eraseX-j).pow(2) + (eraseY-i).pow(2) > radius.pow(2))
                return false
            if(j<0 || j>=piecewiseCanvas.cols || i<0 || i>=piecewiseCanvas.rows)
                return false
            return true
        }

        var erased = false

        for(j:Int in (eraseX-radius).toInt()..(eraseX+radius).toInt()){
            for(i:Int in (eraseY-radius).toInt() .. (eraseY+radius).toInt()){
                if(!feasible(i,j))
                    continue
                val t = piecewiseCanvas.checkOverlap(j,i)
                while(t.isNotEmpty()){
                    val stroke = t.last().path
                    stroke.removeStroke()
                    strokeList.remove(stroke)
                    erased = true
                }
            }
        }

        if(erased)
            currentDrawMod = DrawMod.RESET
    }

    fun saveToBitmap(){
        currentDrawMod = DrawMod.PENUP
    }

    fun startPlaceImage(bitmap:Bitmap,moveX:Float, moveY:Float){
        placingBitmap = CanvasBitmap(moveX,moveY,bitmap.copy(bitmap.config,true))
        currentDrawMod = DrawMod.IMAGEDOWN
    }

    fun movePlacingImage(moveX:Float, moveY:Float){
        placingBitmap?.move(moveX,moveY)
    }

    fun placeImage(){
        if(placingBitmap!=null)
            bitmapList.add(placingBitmap!!)
        currentDrawMod = DrawMod.IMAGEUP
    }

    fun drawAll(canvas: Canvas?){
        if(canvas == null)
            return

        when(currentDrawMod){
            DrawMod.PENDOWN -> {
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                strokeList.last().draw(canvas)
            }
            DrawMod.PENUP -> {
                canvasTemp.drawBitmap(bitmapCache, 0f, 0f, null)
                strokeList.last().draw(canvasTemp)

                //canvas.setBitmap(bitmapCache)
                bitmapCache = _bitmap.copy(_bitmap.config,false)
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                currentDrawMod = DrawMod.IDLE

            }
            DrawMod.RESET -> {
                canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                for (stroke in strokeList) {
                    stroke.draw(canvasTemp)
                }
                for(bitmap in bitmapList){
                    bitmap.draw(canvasTemp)
                }
                bitmapCache = _bitmap.copy(_bitmap.config,false)
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                currentDrawMod = DrawMod.IDLE
            }
            DrawMod.IDLE -> {
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
            }
            DrawMod.IMAGEDOWN ->{
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                placingBitmap?.draw(canvas,true)
            }
            DrawMod.IMAGEUP ->{
                canvasTemp.drawBitmap(bitmapCache, 0f, 0f, null)
                placingBitmap?.draw(canvasTemp)
                bitmapCache = _bitmap.copy(_bitmap.config,false)
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                currentDrawMod = DrawMod.IDLE

            }
        }
    }

    init{
        width = 2000
        height = 2000

        piecewiseCanvas.changeCache(height,width)
        _bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888,true)
        canvasTemp = Canvas(_bitmap)

        bitmapCache = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888,true)
    }
}