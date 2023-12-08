package com.ksc.onote.canvasViewUI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import com.ksc.onote.CanvasActivity
import com.ksc.onote.drawingCanvas.CanvasDrawer
import com.ksc.onote.drawingCanvas.CanvasViewModel

interface OnAreaAssignedListener{
    fun invoke(area: RectF)
}

class MyCanvasView(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    lateinit var canvas:CanvasDrawer
    lateinit var canvasViewModel:CanvasViewModel

    enum class DrawingToolMod{PEN, ERASER, IMAGE, HIGHLIGHTER}

    private var _tempBitmap:Bitmap? = null
    private var currentDrawingTool: DrawingToolMod = DrawingToolMod.PEN
    private var _onAreaAssignedListener: OnAreaAssignedListener? = null
    var currentTool: CanvasActivity.Toolbar = CanvasActivity.Toolbar.Pen
    private var isPenDown = false
    private var penX:Float = 0f
    private var penY:Float = 0f

    private var imagePlaceOffsetX = 0f
    private var imagePlaceOffsetY = 0f

    fun setOnAreaAssignedListener(listener: OnAreaAssignedListener){
        _onAreaAssignedListener = listener
    }

    private var erasorIndicator= Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f
    }

    fun injectViewModel(drawer:CanvasDrawer, viewModel: CanvasViewModel){
        canvas = drawer
        canvasViewModel = viewModel
    }

    fun changeErase(){
        currentDrawingTool = DrawingToolMod.ERASER
        canvas.currentPentool = DrawingToolMod.ERASER
    }

    fun changePen(){
        currentDrawingTool= DrawingToolMod.PEN
        canvas.currentPentool = DrawingToolMod.PEN
    }

    fun changeHighlighter(){
        currentDrawingTool= DrawingToolMod.HIGHLIGHTER
        canvas.currentPentool = DrawingToolMod.HIGHLIGHTER
    }

    fun changeTool(tool: CanvasActivity.Toolbar){
        currentTool = tool
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null)
            return super.onTouchEvent(null)

        penX = event.x
        penY = event.y


        when (currentTool){
            CanvasActivity.Toolbar.Pen -> penEvent(event)
            CanvasActivity.Toolbar.Rectangle -> rectangleEvent(event)
            CanvasActivity.Toolbar.Hand -> handEvent(event)
        }

        return true
    }

    fun handEvent(event: MotionEvent){
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                canvas.setHandHoldPoint(PointF(penX,penY))
            }
            MotionEvent.ACTION_UP->{
            }
            MotionEvent.ACTION_MOVE->{
                canvas.moveView(PointF(penX,penY))
                canvas.updateVisibleCanvases(height,canvasViewModel)
            }
        }
        invalidate()
    }
    fun rectangleEvent(event:MotionEvent){
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                canvas.rectangleArea.setP1(penX,penY)
            }
            MotionEvent.ACTION_UP->{
                _onAreaAssignedListener?.invoke(canvas.rectangleArea.getArea())
            }
            MotionEvent.ACTION_MOVE->{
                canvas.rectangleArea.drag(penX,penY)
            }
        }
        invalidate()
    }
    private fun penEvent(event: MotionEvent){
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                isPenDown = true
                canvas.updateActiveCanvas(penX,penY,canvasViewModel)
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvas.addStroke(penX, penY, canvasViewModel)
                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f, event.x, event.y, canvasViewModel)
                    DrawingToolMod.IMAGE -> {
                        Log.d("Place Image Log","Adding Bitmap to Canvas. (${_tempBitmap?.width?:0},${_tempBitmap?.height?:0})")
                        canvas.startPlaceImage(_tempBitmap?: Bitmap.createBitmap(0,0,Bitmap.Config.ARGB_8888),penX + imagePlaceOffsetX, penY + imagePlaceOffsetY)
                        //_tempBitmap = null
                    }
                    DrawingToolMod.HIGHLIGHTER -> canvas.addStroke(penX, penY, canvasViewModel)
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                when(currentDrawingTool){
                    DrawingToolMod.PEN -> canvas.penUp()
                    DrawingToolMod.ERASER ->{}
                    DrawingToolMod.IMAGE ->{
                        canvas.placeImage(canvasViewModel)
                        changePen()
                    }
                    DrawingToolMod.HIGHLIGHTER -> canvas.penUp()
                }
            }

            MotionEvent.ACTION_MOVE->{
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvas.appendStroke(penX, penY, canvasViewModel)
                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f, event.x, event.y, canvasViewModel)
                    DrawingToolMod.IMAGE -> canvas.movePlacingImage(penX + imagePlaceOffsetX,penY + imagePlaceOffsetY)
                    DrawingToolMod.HIGHLIGHTER -> canvas.appendStroke(penX, penY, canvasViewModel)
                }
            }
        }
        invalidate()
    }


    fun clearCanvas(){
        canvas.clear(canvasViewModel)
        invalidate()
    }

    fun addBitmapToCanvas(bitmap:Bitmap){
        currentDrawingTool = DrawingToolMod.IMAGE
        Log.d("Place Image Log","Call Add Bitmap Method. (${bitmap.width},${bitmap.height})")
        imagePlaceOffsetX = -bitmap.width.toFloat()/2
        imagePlaceOffsetY = -bitmap.height.toFloat()/2
        _tempBitmap = bitmap
    }

    override fun onDraw(canvas: Canvas?) {
        this.canvas.drawAll(canvas, canvasViewModel)
        if(currentTool == CanvasActivity.Toolbar.Rectangle){
            this.canvas.rectangleArea.drawRectangle(canvas)
        }
        else if(currentTool == CanvasActivity.Toolbar.Pen){
            if((currentDrawingTool == DrawingToolMod.ERASER) && isPenDown){
                canvas?.drawCircle(penX,penY,20f,erasorIndicator)
            }
        }
        super.onDraw(canvas)
    }

    fun getAreaBitmap():Bitmap?{
        return canvas.getAreaPixels(canvasViewModel)
    }

    fun scaleEvent(scaleFactorMultiplier:Float){
        if(currentTool == CanvasActivity.Toolbar.Hand)
            canvas.scaleFactor = canvas.scaleFactor * scaleFactorMultiplier
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        canvas.centerHoriz(w)
        canvas.updateVisibleCanvases(h,canvasViewModel)
        canvas.setViewSize(w,h)
        super.onSizeChanged(w, h, oldw, oldh)
    }
}