package com.ksc.onote.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.ksc.onote.canvasViewUI.MyCanvasView
import com.ksc.onote.utils.Base64Tool
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DrawingCanvas(val width:Int, val height:Int,background: Bitmap? = null) {
    var x = 0
        private set
    var y = 0
        private set

    enum class DrawType{STROKE, HIGHLIGHTER, BITMAP}

    private val canvasPaper: CanvasPaper = CanvasPaper(100,100,width,height, background)
    private val strokeList: LinkedList<CanvasStroke> = LinkedList()
    private val highlighterList: LinkedList<CanvasHighlighter> = LinkedList()
    private val bitmapList: LinkedList<CanvasBitmap> = LinkedList()
    private var bitmapCache: Bitmap? = null

    class CanvasPaper(private val rows:Int, private val cols:Int, private val _width:Int, private val _height:Int, background:Bitmap?){
        val mipmapLevel:Int
        private val pathPoints:MutableList<MutableList<MutableList<CanvasStroke.StrokePoint>>> = mutableListOf()
        val hasBackground:Boolean

        var bgBitmap: Bitmap
        val mipmap:MutableList<Bitmap> = mutableListOf()

        fun getWidth():Int{
            return _width
        }

        fun getHeight():Int{
            return _height
        }
        private fun getIndex(x:Int, y:Int):Pair<Int,Int>{
            return Pair((x.toFloat() / (_width.toFloat()/(cols-1))).toInt(),(x.toFloat() / (_height.toFloat()/(rows-1))).toInt())
        }

        fun addPoint(point: CanvasStroke.StrokePoint){
            val idx = getIndex(point.pX,point.pY)
            if(idx.first<0 || idx.first>=cols || idx.second<0 || idx.second>=rows){
                throw ArrayIndexOutOfBoundsException("Can't add point at point (${point.pX}, ${point.pY})")
            }
            pathPoints[idx.second][idx.first].add(point)
            point.addToPiecewiseCanvas(pathPoints[idx.second][idx.first])
        }

        fun checkOverlap(x:Int, y:Int):MutableList<CanvasStroke.StrokePoint>{
            val idx = getIndex(x,y)
            val list:MutableList<CanvasStroke.StrokePoint> = mutableListOf()

            for(p in pathPoints[idx.second][idx.first]){
                if(p.pX == x && p.pY == y)
                    list.add(p)
            }
            return list
        }

        fun removePathPoint(p: CanvasStroke.StrokePoint){
            val idx = getIndex(p.pX,p.pY)
            pathPoints[idx.second][idx.first].remove(p)
        }
        fun dispose(){
            pathPoints.clear()
        }

        init{
            for (i: Int in 1..rows) {
                pathPoints.add(mutableListOf())
                for (j: Int in 1..cols) {
                    pathPoints.last().add(mutableListOf())
                }
            }

            if(background!=null){
                bgBitmap = background
                bgBitmap.density = DisplayMetrics.DENSITY_LOW
                var i = 2
                while(bgBitmap.width/i > 128){
                    mipmap.add(Bitmap.createScaledBitmap(bgBitmap,bgBitmap.width/i,bgBitmap.height/i,true))
                    i *= 2
                }
                hasBackground =true
            }
            else{
                bgBitmap = Bitmap.createBitmap(_width,_height, Bitmap.Config.ARGB_8888)
                bgBitmap.eraseColor(Color.WHITE)
                hasBackground = false
            }

            bgBitmap.setHasMipMap(true)
            mipmapLevel = mipmap.size
            Log.d("Mipmap Build","Mipmap Level : $mipmapLevel")
            Log.d("Mipmap Build","Has Mipmap : ${bgBitmap.hasMipMap()}")
        }
    }

    fun lastStroke():CanvasStroke?{
        return strokeList.lastOrNull()
    }

    fun lastHighlighter():CanvasHighlighter?{
        return highlighterList.lastOrNull()
    }

    fun redraw(){
        bitmapCache = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvasTemp = Canvas(bitmapCache!!)
        canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        for (highlight in highlighterList) {
            highlight.draw(canvasTemp)
        }
        for (stroke in strokeList) {
            stroke.draw(canvasTemp)
        }
        for(bitmap in bitmapList){
            bitmap.draw(canvasTemp)
        }
    }

    fun clear(){
        for(s in strokeList){
            s.removeStroke()
        }
        for(h in highlighterList){
            h.removeStroke()
        }
        bitmapList.clear()
        strokeList.clear()
        highlighterList.clear()
    }

    fun addStroke(strokeX:Float, strokeY:Float, penType:MyCanvasView.DrawingToolMod, penWidth:Float, penColor:Int){
        val strokePaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = penWidth
            color = penColor
        }
        if (strokeX < 0 || strokeX >= canvasPaper.getWidth() || strokeY < 0 || strokeY >= canvasPaper.getHeight())
            return

        if(penType == MyCanvasView.DrawingToolMod.PEN) {
            val s = CanvasStroke(
                strokeX,
                strokeY,
                canvasPaper,
                strokePaint
            )
            strokeList.add(s)
        }
        else{
            strokePaint.strokeWidth = strokePaint.strokeWidth * 5
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.strokeJoin = Paint.Join.MITER
            strokePaint.color = Color.argb(100,penColor.red,penColor.green,penColor.blue)
            val s = CanvasHighlighter(
                strokeX,
                strokeY,
                canvasPaper,
                strokePaint
            )
            highlighterList.add(s)
        }
    }

    fun appendStroke(strokeX:Float, strokeY:Float, penType:MyCanvasView.DrawingToolMod){
        if(penType == MyCanvasView.DrawingToolMod.PEN) {
            if (strokeList.isNotEmpty())
                strokeList.last().appendStroke(strokeX, strokeY)
        }
        else{
            if (highlighterList.isNotEmpty())
                highlighterList.last().appendStroke(strokeX, strokeY)
        }
    }

    fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float):Boolean{
        fun feasible(i:Int, j:Int):Boolean{
            if((eraseX-j).pow(2) + (eraseY-i).pow(2) > radius.pow(2))
                return false
            if(j<0 || j>=canvasPaper.getWidth() || i<0 || i>=canvasPaper.getHeight())
                return false
            return true
        }

        var erased = false

        for(j:Int in (eraseX-radius).toInt()..(eraseX+radius).toInt()){
            for(i:Int in (eraseY-radius).toInt() .. (eraseY+radius).toInt()){
                if(!feasible(i,j))
                    continue
                val t = canvasPaper.checkOverlap(j,i)
                for(st in t){
                    val stroke = st.path
                    if(!stroke.removed){
                        stroke.removeStroke()
                        if(stroke is CanvasHighlighter)
                            highlighterList.remove(stroke)
                        else
                            strokeList.remove(stroke)
                    }
                    erased = true
                }
            }
        }

        if(erased)
            return true
        return false
    }

    fun getAreaPixels(left: Int, top: Int, width: Int, height: Int): Bitmap? {
        if (bitmapCache == null)
            return null
        Log.d("area pixel rect","x:$left, y:$top, width:$width, height:$height")
        return Bitmap.createBitmap(bitmapCache!!, left, top, width, height)
    }

    fun hasCache():Boolean{
        return bitmapCache!=null
    }

    fun deactivate(){
        Log.d("Deactivation","Deactivated Canvas")
        bitmapCache = null
    }

    fun getCache():Bitmap?{
        return bitmapCache
    }


    fun getBackground(scaledWidth:Int):Bitmap{
        if(scaledWidth/2<canvasPaper.bgBitmap.width || canvasPaper.mipmapLevel == 0)
            return canvasPaper.bgBitmap
        return canvasPaper.mipmap[min(scaledWidth/canvasPaper.bgBitmap.width/2,canvasPaper.mipmapLevel)-1]
    }

    fun addBitmap(bitmap:CanvasBitmap){
        bitmap.move(bitmap.x-x.toFloat(),bitmap.y-y.toFloat())
        bitmapList.add(bitmap)
    }

    fun setCanvasPosition(x:Int, y:Int){
        this.x = x
        this.y = y
    }

    companion object{
        fun deserialize(data:CanvasModel):DrawingCanvas{
            val bg:Bitmap? = if(data.hasBG) null
                else Base64Tool.decodeImage(data.bg)

            val canvas = DrawingCanvas(data.width,data.height, bg)
            for(p in data.penStrokes){
                canvas.addStroke(p.x,p.y,MyCanvasView.DrawingToolMod.PEN,p.width,p.color)
                for(sp in p.points){
                    canvas.appendStroke(p.x,p.y,MyCanvasView.DrawingToolMod.PEN)
                }
            }

            for(h in data.highlightStrokes){
                canvas.addStroke(h.x,h.y,MyCanvasView.DrawingToolMod.HIGHLIGHTER,h.width,h.color)
                for(sp in h.points){
                    canvas.appendStroke(h.x,h.y,MyCanvasView.DrawingToolMod.HIGHLIGHTER)
                }
            }

            for(b in data.images){
                val bitmap = Base64Tool.decodeImage(b.img)
                if(bitmap != null)
                    canvas.addBitmap(CanvasBitmap(b.x,b.y,bitmap))
            }

            return canvas
        }

        fun serialize(canvas:DrawingCanvas):CanvasModel{
            val pList = mutableListOf<StrokeData>()
            val hList = mutableListOf<StrokeData>()
            val bList = mutableListOf<ImageData>()
            for(p in canvas.strokeList){
                pList.add(CanvasStroke.serialize(p))
            }
            for(h in canvas.highlighterList){
                hList.add(CanvasHighlighter.serialize(h))
            }
            for(b in canvas.bitmapList){
                bList.add(CanvasBitmap.serialize(b))
            }
            val bg:String = if(canvas.canvasPaper.hasBackground)
                Base64Tool.encodeImage(canvas.canvasPaper.bgBitmap,Bitmap.CompressFormat.PNG)?:""
            else
                ""

            return CanvasModel(canvas.width,canvas.height,pList,hList,bList,canvas.canvasPaper.hasBackground,bg)
        }
    }
}