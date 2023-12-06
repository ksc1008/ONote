package com.ksc.onote.drawingCanvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

class RectangleArea (){
    private val dashEffect = DashPathEffect(floatArrayOf(15f,15f),2f)
    private val paint:Paint = Paint().apply {
        this.color = Color.parseColor("#FF6C22")
        this.style = Paint.Style.STROKE
        this.pathEffect = dashEffect
        this.strokeWidth = 6f
    }

    private var y1:Float = 0f
    private var x1:Float = 0f

    private var y2:Float = 0f
    private var x2:Float = 0f

    private var active:Boolean = false

    fun setP1(x:Float, y:Float){
        active = true
        x1 = x
        y1 = y
        x2 = x
        y2 = y
    }

    fun drag(x:Float, y:Float){
        x2 = x
        y2 = y
    }

    fun clear(){
        active = false
    }

    fun drawRectangle(canvas: Canvas?){
        if(active) {
            canvas?.drawLine(x1,y1,x2,y1,paint)
            canvas?.drawLine(x1,y1,x1,y2,paint)
            canvas?.drawLine(x1,y2,x2,y2,paint)
            canvas?.drawLine(x2,y1,x2,y2,paint)
        }
    }

    fun getArea(): RectF {
        return RectF(min(x1,x2),min(y1,y2),max(x1,x2),max(y1,y2))
    }

    fun getActive():Boolean{
        return active
    }
}