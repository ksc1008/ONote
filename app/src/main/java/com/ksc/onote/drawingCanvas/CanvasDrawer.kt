package com.ksc.onote.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.util.DisplayMetrics
import androidx.lifecycle.ViewModel
import com.ksc.onote.canvasViewUI.MyCanvasView
import kotlin.math.max
import kotlin.math.min

class CanvasDrawer: ViewModel() {
    enum class DrawMod{PENDOWN, PENUP, RESET, IDLE, IMAGEDOWN, IMAGEUP}
    var currentDrawMod: DrawMod = DrawMod.IDLE
    var currentPentool: MyCanvasView.DrawingToolMod = MyCanvasView.DrawingToolMod.PEN

    private var placingBitmap: CanvasBitmap? = null
    var scaleFactor = 1f
        set(value) { field = max(0.4f, min(value, 2.0f))}

    private var currentPenWidth:Float = 10f
    private var currentPenColor:Int = Color.BLACK

    private var handHoldPoint= PointF(0f,0f)
    private var handMovePoint= PointF(0f,0f)
    private var viewPoint:PointF = PointF(0f,0f)

    private var viewWidth:Int = 1000
    private var viewHeight:Int = 1000

    val rectangleArea = RectangleArea()

    private fun getAdjustedPoint(pointX:Float, pointY:Float):PointF{
        return PointF(pointX / scaleFactor + viewPoint.x, pointY / scaleFactor + viewPoint.y)
    }

    private fun globalToCanvasCoordinate(point:PointF, canvas:DrawingCanvas?):PointF{
        return PointF(point.x - (canvas?.x?.toFloat()?:0f),point.y-(canvas?.y?.toFloat()?:0f) )
    }

    fun setViewSize(width:Int, height:Int){
        viewWidth = width
        viewHeight = height
    }

    fun updateActiveCanvas(pointX:Float, pointY:Float, canvasViewModel: CanvasViewModel, doNotAdjust:Boolean=false){
        val point = if(doNotAdjust){
            PointF(pointX, pointY)
        } else{
            getAdjustedPoint(pointX, pointY)
        }
        for(c in canvasViewModel.visibleCanvasList.value?: listOf()){
            if(c.y<point.y && (c.y+c.height)>point.y && c.x<point.x && (c.x+ c.width) > point.x){
                canvasViewModel.setActiveCanvas(c)
                return
            }
        }
        canvasViewModel.setActiveCanvas(null)
    }

    fun updateVisibleCanvases(viewHeight:Int, canvasViewModel: CanvasViewModel){
        val y = viewPoint.y
        val h = viewHeight.toFloat() / scaleFactor

        fun getStart(begin:Int):Int{
            if(y > canvasViewModel.canvasList.value?.get(begin)?.y!! + canvasViewModel.canvasList.value?.get(begin)?.height!!){
                for (i in begin+1 until (canvasViewModel.canvasList.value?.size?:0)){
                    if(y<=canvasViewModel.canvasList.value?.get(i)?.y!! + canvasViewModel.canvasList.value?.get(i)?.height!!)
                        return i
                }
                return (canvasViewModel.canvasList.value?.size?:0) - 1
            }
            else {
                for (i in begin-1 downTo 0){
                    if(y > canvasViewModel.canvasList.value?.get(i)?.y!! + canvasViewModel.canvasList.value?.get(i)?.height!!)
                        return i + 1
                }
                return 0
            }
        }

        fun getLast(end:Int):Int{
            if(y + h < canvasViewModel.canvasList.value?.get(end)?.y!!){
                for (i in end-1 downTo 0){
                    if(y + h >= canvasViewModel.canvasList.value?.get(i)?.y!!)
                        return i
                }
                return 0
            }
            else {
                for (i in end+1 until (canvasViewModel.canvasList.value?.size?:0)){
                    if(y + h < canvasViewModel.canvasList.value?.get(i)?.y!!)
                        return i - 1
                }
                return (canvasViewModel.canvasList.value?.size?:0) - 1
            }
        }

        if(canvasViewModel.visibleIdxStart == -1){
            canvasViewModel.setVisibleCanvas(getStart(0),getLast(0))
        }
        else{
            canvasViewModel.setVisibleCanvas(getStart(canvasViewModel.visibleIdxStart),getLast(canvasViewModel.visibleIdxEnd))
        }
    }

    fun penUp(){
        currentDrawMod = DrawMod.PENUP
    }

    fun startPlaceImage(bitmap:Bitmap,moveX:Float, moveY:Float){
        placingBitmap = CanvasBitmap(moveX+viewPoint.x,moveY+viewPoint.y,
            Bitmap.createScaledBitmap(bitmap,(bitmap.width/scaleFactor).toInt(),(bitmap.height/scaleFactor).toInt(),true))
        currentDrawMod = DrawMod.IMAGEDOWN
    }

    fun movePlacingImage(moveX:Float, moveY:Float){
        val movePoint = getAdjustedPoint(moveX,moveY)
        placingBitmap?.move(movePoint.x, movePoint.y)
    }

    fun placeImage(canvasViewModel:CanvasViewModel){
        currentDrawMod = DrawMod.IMAGEUP
        if(placingBitmap==null)
            return
        //val p =getAdjustedPoint(placingBitmap!!.x,placingBitmap!!.y)
        updateActiveCanvas(placingBitmap!!.x,placingBitmap!!.y,canvasViewModel, doNotAdjust = true)
        canvasViewModel.activeCanvas?.addBitmap(placingBitmap!!)
    }

    fun addStroke(strokeX:Float, strokeY:Float, canvasViewModel:CanvasViewModel){
        val point = globalToCanvasCoordinate(getAdjustedPoint(strokeX,strokeY),canvasViewModel.activeCanvas)

        canvasViewModel.activeCanvas?.addStroke(point.x,point.y,currentPentool,currentPenWidth,currentPenColor)
        currentDrawMod = CanvasDrawer.DrawMod.PENDOWN
    }

    fun appendStroke(strokeX:Float, strokeY:Float, canvasViewModel:CanvasViewModel){
        val point = globalToCanvasCoordinate(getAdjustedPoint(strokeX,strokeY),canvasViewModel.activeCanvas)

        canvasViewModel.activeCanvas?.appendStroke(point.x,point.y,currentPentool)
    }

    fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float, canvasViewModel:CanvasViewModel){
        val point = globalToCanvasCoordinate(getAdjustedPoint(eraseX,eraseY),canvasViewModel.activeCanvas)

        val result = canvasViewModel.activeCanvas?.eraseCircle(radius,point.x,point.y)?:false
        if(result){
            currentDrawMod = DrawMod.RESET
        }
    }

    private fun drawVisibleCanvases(canvas:Canvas, canvasViewModel:CanvasViewModel){
        for(c in canvasViewModel.visibleCanvasList.value?: listOf()){
            if(!c.hasCache())
                c.redraw()


            canvas.drawBitmap(c.getCache()!!,c.x.toFloat(),c.y.toFloat(),null)
        }
    }

    private fun drawVisibleCanvasBackgrounds(canvas:Canvas, canvasViewModel:CanvasViewModel){
        for(c in canvasViewModel.visibleCanvasList.value?: listOf()){
            val slice = c.canvasPaper.sliceCnt
            val bg = c.getBackground((viewWidth/scaleFactor).toInt())

            val imgTop = c.y
            val viewTop = viewPoint.y
            val viewBottom = viewPoint.y + (viewHeight/scaleFactor)

            val startIdx = max(((viewTop-imgTop)/c.height * slice).toInt(),0)
            val endIdx = min(((viewBottom) /c.height*slice).toInt(),slice-1)

            for(i in startIdx..endIdx) {
                canvas.drawBitmap(bg[i], c.x.toFloat(), (c.y+(c.height/slice)*i).toFloat(),null)
            }

            //canvas.drawBitmap(bg,c.x.toFloat(),c.y.toFloat(),null)
        }
    }

    private fun canvasTranslateToCanvasView(canvas:Canvas, drawingCanvas: DrawingCanvas?){
        canvas.save()
        canvas.translate((drawingCanvas?.x?.toFloat()?:0f),(drawingCanvas?.y?.toFloat()?:0f))
    }

    fun drawAll(canvas: Canvas?, canvasViewModel:CanvasViewModel){
        if(canvas == null)
            return

        canvas.drawColor(Color.LTGRAY)
        canvas.save()
        canvas.scale(scaleFactor,scaleFactor)
        canvas.translate(-viewPoint.x,-viewPoint.y)
        drawVisibleCanvasBackgrounds(canvas, canvasViewModel)

        when(currentDrawMod){
            DrawMod.PENDOWN -> {
                if(currentPentool== MyCanvasView.DrawingToolMod.HIGHLIGHTER) {
                    canvasTranslateToCanvasView(canvas,canvasViewModel.activeCanvas)
                    canvasViewModel.activeCanvas?.lastHighlighter()?.draw(canvas)
                    canvas.restore()
                }
                drawVisibleCanvases(canvas, canvasViewModel)
                if(currentPentool== MyCanvasView.DrawingToolMod.PEN) {
                    canvasTranslateToCanvasView(canvas,canvasViewModel.activeCanvas)
                    canvasViewModel.activeCanvas?.lastStroke()?.draw(canvas)
                    canvas.restore()
                }
            }
            DrawMod.PENUP -> {
                canvasViewModel.activeCanvas?.redraw()
                drawVisibleCanvases(canvas, canvasViewModel)
                currentDrawMod = DrawMod.IDLE
            }
            DrawMod.RESET -> {
                canvasViewModel.activeCanvas?.redraw()
                drawVisibleCanvases(canvas, canvasViewModel)
                currentDrawMod = DrawMod.IDLE
            }
            DrawMod.IDLE -> {
                drawVisibleCanvases(canvas, canvasViewModel)
            }
            DrawMod.IMAGEDOWN ->{
                drawVisibleCanvases(canvas, canvasViewModel)
                placingBitmap?.draw(canvas,true)
            }
            DrawMod.IMAGEUP ->{
                canvasViewModel.activeCanvas?.redraw()
                drawVisibleCanvases(canvas, canvasViewModel)
                currentDrawMod = DrawMod.IDLE
            }
        }
        canvas.restore()
    }

    fun getAreaPixels(canvasViewModel:CanvasViewModel):Bitmap?{
        if(!rectangleArea.getActive()){
            return null
        }
        val left = rectangleArea.getArea().left
        val top = rectangleArea.getArea().top
        val p = globalToCanvasCoordinate(getAdjustedPoint(left,top),canvasViewModel.activeCanvas)
        val bitmap = canvasViewModel.activeCanvas?.getAreaPixels(p.x.toInt(),p.y.toInt(),(rectangleArea.getArea().width().toInt()/scaleFactor).toInt(),(rectangleArea.getArea().height().toInt()/scaleFactor).toInt())
        rectangleArea.clear()
        return bitmap
    }

    fun clear(canvasViewModel:CanvasViewModel){
        for(c in canvasViewModel.canvasList.value?: listOf()){
            c.clear()
        }

        for(c in canvasViewModel.visibleCanvasList.value?: listOf()){
            c.redraw()
        }
        currentDrawMod = DrawMod.IDLE
    }

    fun setHandHoldPoint(pivot:PointF){
        handHoldPoint = pivot
        handMovePoint = pivot
    }
    fun moveView(newPivot:PointF){
        val d = PointF(handMovePoint.x-newPivot.x,handMovePoint.y-newPivot.y)
        handMovePoint = newPivot
        viewPoint = PointF(viewPoint.x + d.x / scaleFactor, viewPoint.y + d.y / scaleFactor)
    }

    fun centerHoriz(width:Int){
        viewPoint = PointF(-width.toFloat()/2,viewPoint.y)
    }

    fun setPenWidth(width:Float){
        currentPenWidth = width * 2
    }

    fun setPenColor(color:Int){
        currentPenColor = color
    }
}