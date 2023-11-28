package com.example.canvastext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.canvastext.drawingCanvas.CanvasViewModel

interface OnAreaAssignedListener{
    fun invoke(area: RectF)
}

class MyCanvasView(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    lateinit var canvas:CanvasViewModel

    enum class DrawingToolMod{PEN, ERASER, IMAGE}

    private var _tempBitmap:Bitmap? = null
    private var placingImage:Boolean = false
    private var currentDrawingTool:DrawingToolMod = DrawingToolMod.PEN
    private var _onAreaAssignedListener:OnAreaAssignedListener? = null
    var currentTool: CanvasActivity.Toolbar = CanvasActivity.Toolbar.Pen
    private var isPenDown = false
    private var penX:Float = 0f
    private var penY:Float = 0f

    private var imagePlaceOffsetX = 0f
    private var imagePlaceOffsetY = 0f

    fun setOnAreaAssignedListener(listener:OnAreaAssignedListener){
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

    fun injectViewModel(owner:ViewModelStoreOwner){
        canvas = ViewModelProvider(owner)[CanvasViewModel::class.java]
    }

    fun changeErase(){
        currentDrawingTool = DrawingToolMod.ERASER
    }

    fun changePen(){
        currentDrawingTool= DrawingToolMod.PEN
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
            CanvasActivity.Toolbar.Hand-> handEvent(event)
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
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvas.addStroke(penX, penY)
                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f, event.x, event.y)
                    DrawingToolMod.IMAGE -> {
                        Log.d("Place Image Log","Adding Bitmap to Canvas. (${_tempBitmap?.width?:0},${_tempBitmap?.height?:0})")
                        canvas.startPlaceImage(_tempBitmap?: Bitmap.createBitmap(0,0,Bitmap.Config.ARGB_8888),penX + imagePlaceOffsetX, penY + imagePlaceOffsetY)
                        //_tempBitmap = null
                    }
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                when(currentDrawingTool){
                    DrawingToolMod.PEN -> canvas.saveToBitmap()
                    DrawingToolMod.ERASER->{}
                    DrawingToolMod.IMAGE->{
                        canvas.placeImage()
                        changePen()
                    }
                }
            }

            MotionEvent.ACTION_MOVE->{
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvas.appendStroke(penX, penY)
                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f, event.x, event.y)
                    DrawingToolMod.IMAGE -> canvas.movePlacingImage(penX + imagePlaceOffsetX,penY + imagePlaceOffsetY)
                }
            }
        }
        invalidate()
    }

    fun clearCanvas(){
        canvas.clear()
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
        this.canvas.drawAll(canvas)
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
        return canvas.getAreaPixels()
    }
}