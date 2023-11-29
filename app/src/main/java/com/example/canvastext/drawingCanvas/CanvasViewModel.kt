package com.example.canvastext.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.PorterDuff
import android.util.Log
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

    @JvmField
    var handHoldPoint= PointF(0f,0f)
    @JvmField
    var handMovePoint= PointF(0f,0f)
    var viewPoint:PointF = PointF(0f,0f)

    val rectangleArea = RectangleArea()

    class PiecewiseCanvas{
        val pathPoints:MutableList<MutableList<MutableList<CanvasStroke.StrokePoint>>> = mutableListOf()

        lateinit var bgBitmap:Bitmap

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

            bgBitmap = Bitmap.createBitmap(cols,rows,Bitmap.Config.ARGB_8888)
            bgBitmap.eraseColor(Color.WHITE)
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
        bitmapList.clear()
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
        if(strokeX+viewPoint.x<0 || strokeX+viewPoint.x>=piecewiseCanvas.cols || strokeY+viewPoint.y<0 || strokeY+viewPoint.y >= piecewiseCanvas.rows )
            return

        val s:CanvasStroke = CanvasStroke(strokeX+viewPoint.x,strokeY+viewPoint.y,piecewiseCanvas, strokePaint)
        strokeList.add(s)

        currentDrawMod = DrawMod.PENDOWN
    }


    fun appendStroke(strokeX:Float, strokeY:Float){
        if(strokeList.isNotEmpty())
            strokeList.last().appendStroke(strokeX + viewPoint.x,strokeY + viewPoint.y)
    }

    fun removeCanvasStroke(s:CanvasStroke){
        strokeList.remove(s)
    }

    fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float){
        val viewEraseX = eraseX + viewPoint.x
        val viewEraseY = eraseY + viewPoint.y
        fun feasible(i:Int, j:Int):Boolean{
            if((viewEraseX-j).pow(2) + (viewEraseY-i).pow(2) > radius.pow(2))
                return false
            if(j<0 || j>=piecewiseCanvas.cols || i<0 || i>=piecewiseCanvas.rows)
                return false
            return true
        }

        var erased = false

        for(j:Int in (viewEraseX-radius).toInt()..(viewEraseX+radius).toInt()){
            for(i:Int in (viewEraseY-radius).toInt() .. (viewEraseY+radius).toInt()){
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
        placingBitmap = CanvasBitmap(moveX+viewPoint.x,moveY+viewPoint.y,bitmap.copy(bitmap.config,true))
        currentDrawMod = DrawMod.IMAGEDOWN
    }

    fun movePlacingImage(moveX:Float, moveY:Float){
        placingBitmap?.move(moveX+viewPoint.x,moveY+viewPoint.y)
    }

    fun placeImage(){
        if(placingBitmap!=null)
            bitmapList.add(placingBitmap!!)
        currentDrawMod = DrawMod.IMAGEUP
    }

    fun drawAll(canvas: Canvas?){
        if(canvas == null)
            return

        canvas.drawColor(Color.LTGRAY)
        canvas.save()
        canvas.translate(-viewPoint.x,-viewPoint.y)
        canvas.drawBitmap(piecewiseCanvas.bgBitmap,0f,0f, null)

        canvasTemp.density = canvas.density
        canvasTemp.drawFilter = canvas.drawFilter

        when(currentDrawMod){
            DrawMod.PENDOWN -> {
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                strokeList.lastOrNull()?.draw(canvas)
            }
            DrawMod.PENUP -> {
                canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                canvasTemp.drawBitmap(bitmapCache, 0f, 0f, null)
                strokeList.lastOrNull()?.draw(canvasTemp)

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
                canvas.drawBitmap(bitmapCache, 0f,0f, null)
            }
            DrawMod.IMAGEDOWN ->{
                canvas.drawBitmap(bitmapCache, 0f,0f, null)
                placingBitmap?.draw(canvas,true)
            }
            DrawMod.IMAGEUP ->{
                canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                canvasTemp.drawBitmap(bitmapCache, 0f,0f, null)
                placingBitmap?.draw(canvasTemp)
                bitmapCache = _bitmap.copy(_bitmap.config,false)
                canvas.drawBitmap(bitmapCache, 0f,0f, null)
                currentDrawMod = DrawMod.IDLE

            }
        }
        canvas.restore()
        Log.d("",canvasTemp.isHardwareAccelerated.toString())
    }

    fun getAreaPixels():Bitmap?{
        if(!rectangleArea.getActive()){
            return null
        }

        val left = rectangleArea.getArea().left.toInt() + viewPoint.x.toInt()
        val top = rectangleArea.getArea().top.toInt() + viewPoint.y.toInt()

        val bitmap = Bitmap.createBitmap(bitmapCache,left,
            top,
            rectangleArea.getArea().width().toInt(),rectangleArea.getArea().height().toInt())

        rectangleArea.clear()
        return bitmap
    }

    fun setHandHoldPoint(pivot:PointF){
        handHoldPoint = pivot
        handMovePoint = pivot
    }
    fun moveView(newPivot:PointF){
        val d = PointF(handMovePoint.x-newPivot.x,handMovePoint.y-newPivot.y)
        handMovePoint = newPivot
        viewPoint = PointF(viewPoint.x + d.x, viewPoint.y + d.y)
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