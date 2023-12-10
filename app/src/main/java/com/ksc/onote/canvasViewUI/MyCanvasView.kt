package com.ksc.onote.canvasViewUI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import com.ksc.onote.CanvasActivity
import com.ksc.onote.drawingCanvas.CanvasDrawer
import com.ksc.onote.drawingCanvas.CanvasViewModel
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

interface OnAreaAssignedListener{
    fun invoke(area: RectF)
}

class MyCanvasView(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    lateinit var canvasDrawer:CanvasDrawer
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
        canvasDrawer = drawer
        canvasViewModel = viewModel
    }

    fun changeErase(){
        currentDrawingTool = DrawingToolMod.ERASER
        canvasDrawer.currentPentool = DrawingToolMod.ERASER
    }

    fun changePen(){
        currentDrawingTool= DrawingToolMod.PEN
        canvasDrawer.currentPentool = DrawingToolMod.PEN
    }

    fun changeHighlighter(){
        currentDrawingTool= DrawingToolMod.HIGHLIGHTER
        canvasDrawer.currentPentool = DrawingToolMod.HIGHLIGHTER
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
                canvasDrawer.setHandHoldPoint(PointF(penX,penY))
            }
            MotionEvent.ACTION_UP->{
            }
            MotionEvent.ACTION_MOVE->{
                canvasDrawer.moveView(PointF(penX,penY))
                canvasDrawer.updateVisibleCanvases(height,canvasViewModel)
            }
        }
        invalidate()
    }
    fun rectangleEvent(event:MotionEvent){
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                canvasDrawer.rectangleArea.setP1(penX,penY)
            }
            MotionEvent.ACTION_UP->{
                _onAreaAssignedListener?.invoke(canvasDrawer.rectangleArea.getArea())
            }
            MotionEvent.ACTION_MOVE->{
                canvasDrawer.rectangleArea.drag(penX,penY)
            }
        }
        invalidate()
    }
    private fun penEvent(event: MotionEvent){
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                isPenDown = true
                canvasDrawer.updateActiveCanvas(penX,penY,canvasViewModel)
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvasDrawer.addStroke(penX, penY, canvasViewModel)
                    DrawingToolMod.ERASER -> canvasDrawer.eraseCircle(20f, event.x, event.y, canvasViewModel)
                    DrawingToolMod.IMAGE -> {
                        Log.d("Place Image Log","Adding Bitmap to Canvas. (${_tempBitmap?.width?:0},${_tempBitmap?.height?:0})")
                        canvasDrawer.startPlaceImage(_tempBitmap?: Bitmap.createBitmap(0,0,Bitmap.Config.ARGB_8888),penX + imagePlaceOffsetX, penY + imagePlaceOffsetY)
                        //_tempBitmap = null
                    }
                    DrawingToolMod.HIGHLIGHTER -> canvasDrawer.addStroke(penX, penY, canvasViewModel)
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                when(currentDrawingTool){
                    DrawingToolMod.PEN -> canvasDrawer.penUp()
                    DrawingToolMod.ERASER ->{}
                    DrawingToolMod.IMAGE ->{
                        canvasDrawer.placeImage(canvasViewModel)
                        changePen()
                    }
                    DrawingToolMod.HIGHLIGHTER -> canvasDrawer.penUp()
                }
            }

            MotionEvent.ACTION_MOVE->{
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvasDrawer.appendStroke(penX, penY, canvasViewModel)
                    DrawingToolMod.ERASER -> canvasDrawer.eraseCircle(20f, event.x, event.y, canvasViewModel)
                    DrawingToolMod.IMAGE -> canvasDrawer.movePlacingImage(penX + imagePlaceOffsetX,penY + imagePlaceOffsetY)
                    DrawingToolMod.HIGHLIGHTER -> canvasDrawer.appendStroke(penX, penY, canvasViewModel)
                }
            }
        }
        invalidate()
    }


    fun clearCanvas(){
        canvasDrawer.clear(canvasViewModel)
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
        this.canvasDrawer.drawAll(canvas, canvasViewModel)
        if(currentTool == CanvasActivity.Toolbar.Rectangle){
            this.canvasDrawer.rectangleArea.drawRectangle(canvas)
        }
        else if(currentTool == CanvasActivity.Toolbar.Pen){
            if((currentDrawingTool == DrawingToolMod.ERASER) && isPenDown){
                canvas?.drawCircle(penX,penY,20f,erasorIndicator)
            }
        }
        super.onDraw(canvas)
    }

    fun getAreaBitmap():Bitmap?{
        return canvasDrawer.getAreaPixels(canvasViewModel)
    }

    fun scaleEvent(scaleFactorMultiplier:Float){
        if(currentTool == CanvasActivity.Toolbar.Hand)
            canvasDrawer.scaleFactor = canvasDrawer.scaleFactor * scaleFactorMultiplier
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        canvasDrawer.centerHoriz(w)
        canvasDrawer.updateVisibleCanvases(h,canvasViewModel)
        canvasDrawer.setViewSize(w,h)
        super.onSizeChanged(w, h, oldw, oldh)
    }
}