package com.example.canvastext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.canvastext.drawingCanvas.DrawingCanvas
import java.util.LinkedList
import kotlin.math.abs

class MyCanvas(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    var canvas:DrawingCanvas = DrawingCanvas(2000,2000)

    enum class ToolMod{PEN, ERASER}

    var currentTool:ToolMod = ToolMod.PEN
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


    fun changeErase(){
        currentTool = ToolMod.ERASER
    }

    fun changePen(){
        currentTool= ToolMod.PEN
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null)
            return super.onTouchEvent(null)

        penX = event.x
        penY = event.y

        when(event.action){
            MotionEvent.ACTION_DOWN->{
                isPenDown = true
                when(currentTool) {
                    ToolMod.PEN-> {
                        canvas.addStroke(penX,penY)
                        invalidate()
                    }

                    ToolMod.ERASER -> {
                        canvas.eraseCircle(20f,event.x,event.y)
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                invalidate()
            }

            MotionEvent.ACTION_MOVE->{
                when(currentTool){
                    ToolMod.PEN->{
                        canvas.appendStroke(penX,penY)
                        invalidate()
                    }

                    ToolMod.ERASER->{
                        canvas.eraseCircle(20f,event.x,event.y)
                        invalidate()
                    }
                }
            }
        }


        return true
    }

    fun clearCanvas(){
        canvas.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        this.canvas.drawAll(canvas)
        if((currentTool == ToolMod.ERASER) && isPenDown){
            canvas?.drawCircle(penX,penY,20f,erasorIndicator)
        }
        super.onDraw(canvas)
    }
}