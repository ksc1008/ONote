package com.ksc.onote.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.CornerRadius
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

    fun move(newX:Float, newY:Float){
        x = newX
        y = newY
    }

    fun eraseArea(radius: Float, eraseX:Float, eraseY:Float){
        val canvas = Canvas(bitmap)
        val eraser = Paint()

        eraser.alpha = 0
        eraser.isAntiAlias = true
        eraser.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        eraser.style = Paint.Style.FILL
        canvas.drawCircle(eraseX-x,eraseY-y,radius,eraser)
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