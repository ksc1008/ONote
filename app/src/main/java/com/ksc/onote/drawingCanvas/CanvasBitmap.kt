package com.ksc.onote.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.ksc.onote.utils.Base64Tool

class CanvasBitmap(_x:Float, _y:Float, private val bitmap:Bitmap) {
    val width:Int by lazy{
        bitmap.width
    }

    val height:Int by lazy{
        bitmap.height
    }


    var x:Float
        private set
    var y:Float
        private set


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
        y = newY
    }

    init {
        x = _x
        y = _y
    }

    companion object{
        fun serialize(canvasBitmap:CanvasBitmap):ImageData{
            return ImageData(Base64Tool.encodeImage(canvasBitmap.bitmap)?:"",canvasBitmap.x,canvasBitmap.y)
        }
    }
}