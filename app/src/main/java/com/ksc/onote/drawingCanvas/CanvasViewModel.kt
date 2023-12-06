package com.ksc.onote.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.lifecycle.ViewModel
import com.ksc.onote.canvasViewUI.MyCanvasView
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class CanvasViewModel: ViewModel() {
    var width:Int = 0
    var height:Int = 0

    enum class DrawMod{PENDOWN, PENUP, RESET, IDLE, IMAGEDOWN, IMAGEUP}
    var currentDrawMod: DrawMod = DrawMod.IDLE
    var currentPentool: MyCanvasView.DrawingToolMod = MyCanvasView.DrawingToolMod.PEN

    private var bitmapCache: Bitmap
    val piecewiseCanvas: PiecewiseCanvas = PiecewiseCanvas()
    private val strokeList: LinkedList<CanvasStroke> = LinkedList()
    private val highlighterList: LinkedList<CanvasHighlighter> = LinkedList()
    private val bitmapList: LinkedList<CanvasBitmap> = LinkedList()
    private var _bitmap: Bitmap
    private var canvasTemp: Canvas
    private var placingBitmap: CanvasBitmap? = null
    private var _scaleFactor = 1f

    private var currentPenWidth:Float = 10f
    private var currentPenColor:Int = Color.BLACK


    @JvmField
    var handHoldPoint= PointF(0f,0f)
    @JvmField
    var handMovePoint= PointF(0f,0f)
    var viewPoint:PointF = PointF(0f,0f)

    val rectangleArea = RectangleArea()

    class PiecewiseCanvas{
        private val pathPoints:MutableList<MutableList<MutableList<CanvasStroke.StrokePoint>>> = mutableListOf()

        lateinit var bgBitmap:Bitmap

        private var rows:Int = 0
        private var cols:Int = 0

        private var _width:Int = 0
        private var _height:Int = 0

        fun changeCache(newRows:Int, newCols:Int, width:Int, height:Int){
            this._width = width
            this._height = height

            for (i: Int in 1..newRows) {
                pathPoints.add(mutableListOf())
                for (j: Int in 1..newCols) {
                    pathPoints.last().add(mutableListOf())
                }
            }

            cols = newCols
            rows = newRows

            bgBitmap = Bitmap.createBitmap(_width,_height,Bitmap.Config.ARGB_8888)
            bgBitmap.eraseColor(Color.WHITE)
        }

        fun getWidth():Int{
            return _width
        }

        fun getHeight():Int{
            return _height
        }
        fun getIndex(x:Int, y:Int):Pair<Int,Int>{
            return Pair((x.toFloat() / (_width.toFloat()/cols)).toInt(),(x.toFloat() / (_height.toFloat()/rows)).toInt())
        }

        fun addPoint(point: CanvasStroke.StrokePoint){
            val idx = getIndex(point.pX,point.pY)
            if(idx.first<0 || idx.first>=cols || idx.second<0 || idx.second>=rows){
                throw ArrayIndexOutOfBoundsException("Can't add point at point (${point.pX}, ${point.pY})")
            }
            pathPoints[idx.second][idx.first].add(point)
            point.addToPiecewiseCanvas(pathPoints[idx.second][idx.first])
        }

        fun checkOverlap(x:Int, y:Int):MutableList<CanvasStroke.StrokePoint>{
            val idx = getIndex(x,y)
            val list:MutableList<CanvasStroke.StrokePoint> = mutableListOf()

            for(p in pathPoints[idx.second][idx.first]){
                if(p.pX == x && p.pY == y)
                    list.add(p)
            }
            return list
        }

        fun removePathPoint(p: CanvasStroke.StrokePoint){
            val idx = getIndex(p.pX,p.pY)
            pathPoints[idx.second][idx.first].remove(p)
        }
        fun dispose(){
            pathPoints.clear()
        }

    }

    fun resize(){

    }

    fun getScaleFactor():Float{return _scaleFactor}
    fun setScaleFactor(value:Float){
        _scaleFactor = max(0.4f, min(value, 2.0f))}

    fun clear(){
        for(s in strokeList){
            s.removeStroke()
        }
        for(h in highlighterList){
            h.removeStroke()
        }
        bitmapList.clear()
        strokeList.clear()
        highlighterList.clear()
        currentDrawMod = DrawMod.RESET
    }
    fun addStroke(strokeX:Float, strokeY:Float){
        val strokePaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = currentPenWidth
            color = currentPenColor
        }
        if (strokeX + viewPoint.x < 0 || strokeX + viewPoint.x >= piecewiseCanvas.getWidth() || strokeY + viewPoint.y < 0 || strokeY + viewPoint.y >= piecewiseCanvas.getHeight())
            return

        if(currentPentool == MyCanvasView.DrawingToolMod.PEN) {
            val s = CanvasStroke(
                strokeX + viewPoint.x,
                strokeY + viewPoint.y,
                piecewiseCanvas,
                strokePaint
            )
            strokeList.add(s)
        }
        else{
            strokePaint.strokeWidth = strokePaint.strokeWidth * 5
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.strokeJoin = Paint.Join.MITER
            strokePaint.color = Color.argb(100,currentPenColor.red,currentPenColor.green,currentPenColor.blue)
            val s = CanvasHighlighter(
                strokeX + viewPoint.x,
                strokeY + viewPoint.y,
                piecewiseCanvas,
                strokePaint
            )
            highlighterList.add(s)
        }

        currentDrawMod = DrawMod.PENDOWN
    }


    fun appendStroke(strokeX:Float, strokeY:Float){
        if(currentPentool == MyCanvasView.DrawingToolMod.PEN) {
            if (strokeList.isNotEmpty())
                strokeList.last().appendStroke(strokeX + viewPoint.x, strokeY + viewPoint.y)
        }
        else{
            if (highlighterList.isNotEmpty())
                highlighterList.last().appendStroke(strokeX + viewPoint.x, strokeY + viewPoint.y)
        }
    }

    fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float){
        val viewEraseX = eraseX + viewPoint.x
        val viewEraseY = eraseY + viewPoint.y
        fun feasible(i:Int, j:Int):Boolean{
            if((viewEraseX-j).pow(2) + (viewEraseY-i).pow(2) > radius.pow(2))
                return false
            if(j<0 || j>=piecewiseCanvas.getWidth() || i<0 || i>=piecewiseCanvas.getHeight())
                return false
            return true
        }

        var erased = false

        for(j:Int in (viewEraseX-radius).toInt()..(viewEraseX+radius).toInt()){
            for(i:Int in (viewEraseY-radius).toInt() .. (viewEraseY+radius).toInt()){
                if(!feasible(i,j))
                    continue
                val t = piecewiseCanvas.checkOverlap(j,i)
                for(st in t){
                    val stroke = st.path
                    if(!stroke.removed){
                        stroke.removeStroke()
                        if(stroke is CanvasHighlighter)
                            highlighterList.remove(stroke)
                        else
                            strokeList.remove(stroke)
                    }
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

    fun drawCurrentPen(canvas:Canvas?){
        if(currentPentool== MyCanvasView.DrawingToolMod.PEN)
            strokeList.lastOrNull()?.draw(canvas)
        else
            highlighterList.lastOrNull()?.draw(canvas)
    }

    fun drawAll(canvas: Canvas?){
        if(canvas == null)
            return

        canvas.drawColor(Color.LTGRAY)
        canvas.save()
        canvas.scale(_scaleFactor,_scaleFactor)
        canvas.translate(-viewPoint.x,-viewPoint.y)
        canvas.drawBitmap(piecewiseCanvas.bgBitmap,0f,0f, null)

        canvasTemp.density = canvas.density
        canvasTemp.drawFilter = canvas.drawFilter

        when(currentDrawMod){
            DrawMod.PENDOWN -> {
                if(currentPentool== MyCanvasView.DrawingToolMod.HIGHLIGHTER)
                    highlighterList.lastOrNull()?.draw(canvas)
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                if(currentPentool== MyCanvasView.DrawingToolMod.PEN)
                    strokeList.lastOrNull()?.draw(canvas)
            }
            DrawMod.PENUP -> {
                canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                if(currentPentool== MyCanvasView.DrawingToolMod.HIGHLIGHTER)
                    highlighterList.lastOrNull()?.draw(canvasTemp)
                canvasTemp.drawBitmap(bitmapCache, 0f, 0f, null)
                if(currentPentool== MyCanvasView.DrawingToolMod.PEN)
                    strokeList.lastOrNull()?.draw(canvasTemp)

                //canvas.setBitmap(bitmapCache)
                bitmapCache = _bitmap.copy(_bitmap.config,false)
                canvas.drawBitmap(bitmapCache, 0f, 0f, null)
                currentDrawMod = DrawMod.IDLE

            }
            DrawMod.RESET -> {
                canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                for (highlight in highlighterList) {
                    highlight.draw(canvasTemp)
                }
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

    fun setPenWidth(width:Float){
        currentPenWidth = width * 2

    }

    fun setPenColor(color:Int){
        currentPenColor = color
    }

    init{
        width = 2000
        height = 2000

        piecewiseCanvas.changeCache(100,100,height,width)
        _bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888,true)
        canvasTemp = Canvas(_bitmap)

        bitmapCache = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888,true)
    }

    override fun onCleared() {
        piecewiseCanvas.dispose()
        strokeList.clear()
        super.onCleared()
    }
}