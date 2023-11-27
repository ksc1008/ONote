package com.example.canvastext.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

class CanvasBitmap(private var x:Float, private var y:Float, private val bitmap:Bitmap) {
    val width:Int by lazy{
        bitmap.width
    }

    val height:Int by lazy{
        bitmap.height
    }

    fun draw(viewCanvas: Canvas?, semiTransparent:Boolean = false){
        val paint = Paint()
        if(semiTransparent){
            paint.alpha = 100
        }
        viewCanvas?.drawBitmap(bitmap,x,y,paint)
    }

    fun checkOverlap(checkX:Int, checkY:Int):Boolean{
        return (checkX>=x && checkX<=(x+width)) && (checkY>=y && checkY<=(y+height))
    }

    fun move(newX:Float, newY:Float){
        x = newX
        y=newY
    }
}