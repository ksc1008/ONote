package com.ksc.onote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import com.ksc.onote.drawingCanvas.CanvasViewModel

interface OnAreaAssignedListener{
    fun invoke(area: RectF)
}

class MyCanvasView(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    lateinit var canvas:CanvasViewModel

    enum class DrawingToolMod{PEN, ERASER, IMAGE, HIGHLIGHTER}

    private var _tempBitmap:Bitmap? = null
    private var placingImage:Boolean = false
    private var currentDrawingTool: DrawingToolMod = DrawingToolMod.PEN
    private var _onAreaAssignedListener: OnAreaAssignedListener? = null
    var currentTool: CanvasActivity.Toolbar = CanvasActivity.Toolbar.Pen
    private var isPenDown = false
    private var penX:Float = 0f
    private var penY:Float = 0f

    private var imagePlaceOffsetX = 0f
    private var imagePlaceOffsetY = 0f
    var exclusionRects = listOf(Rect(0,top,10,bottom),Rect(10,top,20,bottom),Rect(20,top,30,bottom),Rect(40,top,50,bottom),Rect(50,top,60,bottom),Rect(60,top,70,bottom))

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

    fun injectViewModel(viewModel:CanvasViewModel){
        canvas = viewModel
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
                    DrawingToolMod.HIGHLIGHTER -> canvas.addStroke(penX, penY)
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                when(currentDrawingTool){
                    DrawingToolMod.PEN -> canvas.saveToBitmap()
                    DrawingToolMod.ERASER ->{}
                    DrawingToolMod.IMAGE ->{
                        canvas.placeImage()
                        changePen()
                    }
                    DrawingToolMod.HIGHLIGHTER -> canvas.saveToBitmap()
                }
            }

            MotionEvent.ACTION_MOVE->{
                when (currentDrawingTool) {
                    DrawingToolMod.PEN -> canvas.appendStroke(penX, penY)
                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f, event.x, event.y)
                    DrawingToolMod.IMAGE -> canvas.movePlacingImage(penX + imagePlaceOffsetX,penY + imagePlaceOffsetY)
                    DrawingToolMod.HIGHLIGHTER -> canvas.appendStroke(penX, penY)
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

    fun scaleEvent(scaleFactorMultiplier:Float){
        canvas.setScaleFactor(canvas.getScaleFactor() * scaleFactorMultiplier)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        for(r in exclusionRects){
            r.bottom = 500
        }

        super.onSizeChanged(w, h, oldw, oldh)

    }

    override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets {
        return super.onApplyWindowInsets(insets)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}