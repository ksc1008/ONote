package com.example.canvastext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.canvastext.drawingCanvas.CanvasViewModel
import com.example.canvastext.drawingCanvas.DrawingCanvas

class MyCanvasView(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    lateinit var canvas:CanvasViewModel

    enum class DrawingToolMod{PEN, ERASER}

    private var currentDrawingTool:DrawingToolMod = DrawingToolMod.PEN
    var currentTool: CanvasActivity.Toolbar = CanvasActivity.Toolbar.Pen
    private var isPenDown = false
    private var penX:Float = 0f
    private var penY:Float = 0f

    private var erasorIndicator= Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f
    }

    fun injectViewModel(owner:ViewModelStoreOwner){
        canvas = ViewModelProvider(owner).get(CanvasViewModel::class.java)
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
        }

        return true
    }

    fun rectangleEvent(event:MotionEvent){
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                canvas.rectangleArea.setP1(penX,penY)
            }
            MotionEvent.ACTION_UP->{
                canvas.rectangleArea.clear()
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
                when(currentDrawingTool) {
                    DrawingToolMod.PEN-> canvas.addStroke(penX,penY)

                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f,event.x,event.y)
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                when(currentDrawingTool){
                    DrawingToolMod.PEN -> canvas.saveToBitmap()

                    DrawingToolMod.ERASER->{}
                }
            }

            MotionEvent.ACTION_MOVE->{
                when(currentDrawingTool){
                    DrawingToolMod.PEN -> canvas.appendStroke(penX,penY)

                    DrawingToolMod.ERASER -> canvas.eraseCircle(20f,event.x,event.y)
                }
            }
        }
        invalidate()
    }

    fun clearCanvas(){
        canvas.clear()
        invalidate()
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
}