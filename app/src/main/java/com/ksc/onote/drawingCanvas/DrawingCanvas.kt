package com.ksc.onote.drawingCanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.ksc.onote.canvasViewUI.MyCanvasView
import com.ksc.onote.utils.Base64Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DrawingCanvas(val width:Int, val height:Int,hasBackground:Boolean = false) {
    var x = 0
        private set
    var y = 0
        private set


    var readyToDrawPath = false
    var canvasLoading = false

    var canvasPaper: CanvasPaper = CanvasPaper(ROW,COL,width,height, hasBackground)
        private set
    private val strokeList: LinkedList<CanvasStroke> = LinkedList()
    private val highlighterList: LinkedList<CanvasHighlighter> = LinkedList()
    private val bitmapList: LinkedList<CanvasBitmap> = LinkedList()
    private var bitmapCache: Bitmap? = null

    class CanvasPaper(private val rows:Int, private val cols:Int, private val _width:Int, private val _height:Int, var hasBackground: Boolean = false){
        var page:Int = 0
        var bgLoading = false

        val sliceCnt:Int
        private var pathPoints:MutableList<MutableList<MutableList<CanvasStroke.StrokePoint>>> = mutableListOf()
        var documentSaving: Deferred<Unit>? = null
            private set

        lateinit var bgBitmap: Bitmap
            private set

        var bgBitmapFraction: List<Bitmap> = listOf()
            private set

        var bgLoaded = false
            private set

        var readyToSave = false
            private set

        fun getWidth():Int{
            return _width
        }

        fun getHeight():Int{
            return _height
        }
        private fun getIndex(x:Float, y:Float):Pair<Int,Int>{
            return Pair((x / (_width.toFloat()/(cols-1))).toInt(),(y / (_height.toFloat()/(rows-1))).toInt())
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
            val idx = getIndex(x.toFloat(),y.toFloat())
            val list:MutableList<CanvasStroke.StrokePoint> = mutableListOf()

            for(p in pathPoints[idx.second][idx.first]){
                if(p.pX.toInt() == x && p.pY.toInt() == y)
                    list.add(p)
            }
            return list
        }

        fun removePathPoint(p: CanvasStroke.StrokePoint){
            val idx = getIndex(p.pX,p.pY)
            pathPoints[idx.second][idx.first].remove(p)
        }

        fun initiateBackground(pg:Int, bitmap:Bitmap){
            page = pg
            bgBitmap = bitmap
            bgBitmap.setHasMipMap(true)
            val l:MutableList<Bitmap> = mutableListOf()
            for(i in 0 until sliceCnt-1){
                l.add(Bitmap.createBitmap(bgBitmap,0,bgBitmap.height/sliceCnt*i,bgBitmap.width,bgBitmap.height/sliceCnt))
                l.last().setHasMipMap(true)
            }
            l.add(Bitmap.createBitmap(bgBitmap,0,bgBitmap.height/sliceCnt*(sliceCnt-1),bgBitmap.width,bgBitmap.height-(bgBitmap.height/sliceCnt)*(sliceCnt-1)))
            BitmapCacheManager.getInstance()?.putPageToCache(page,l)
            bgLoaded = false
        }

        suspend fun initiateBackground(pg:Int,encodedBitmap: String){
            page = pg
            val decoded = Base64Tool.decodeImage(encodedBitmap)
            Log.d("Canvas Decode",encodedBitmap)
            if(decoded==null) {
                Log.e("Canvas","Failed to load initial background")
                hasBackground = false
                bgLoaded = false
                readyToSave = true
                return
            }

            bgBitmap = decoded
            bgBitmap.setHasMipMap(true)
            val l: MutableList<Bitmap> = mutableListOf()
            for(i in 0 until sliceCnt-1){
                l.add(Bitmap.createBitmap(bgBitmap,0,bgBitmap.height/sliceCnt*i,bgBitmap.width,bgBitmap.height/sliceCnt))
                l.last().setHasMipMap(true)
            }
            l.add(Bitmap.createBitmap(bgBitmap,0,bgBitmap.height/sliceCnt*(sliceCnt-1),bgBitmap.width,bgBitmap.height-(bgBitmap.height/sliceCnt)*(sliceCnt-1)))

            CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default) {
                documentSaving = async {
                    BitmapCacheManager.getInstance()?.putPageToCache(page,l)
                    BitmapCacheManager.getInstance()?.putEncodedBackgroundToCache(page,encodedBitmap)
                    readyToSave = true
                    bgBitmap.recycle()
                }
            }

            hasBackground = true
            readyToSave = true
        }

        fun loadBackground(){
            bgLoading = true
            BitmapCacheManager.getInstance()?.getPageAsync(page) {
                bgLoading = false
                if (it == null) {
                    return@getPageAsync
                }
                setBackgroundList(it)
            }
        }

        private fun setBackgroundList(list:List<Bitmap>){
            bgBitmapFraction = list
            bgLoaded = true
        }

        fun clearBackgroundList(){
            for(b in bgBitmapFraction){
                b.recycle()
            }
            bgLoaded =false
            bgBitmapFraction = listOf()
        }

        fun clearPathList(){
            pathPoints = mutableListOf()
        }

        fun resetPathList(){
            for (i: Int in 1..rows) {
                pathPoints.add(mutableListOf())
                for (j: Int in 1..cols) {
                    pathPoints.last().add(mutableListOf())
                }
            }
        }

        init{
            sliceCnt = _height / 300
            if(!hasBackground){
                bgLoaded = false
            }
        }

        suspend fun makeEncodedString(preEncoded:String?= null){
            if(readyToSave || !hasBackground) {
                readyToSave = true
                return
            }
            CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default) {
                if(preEncoded!=null){
                    documentSaving = async {
                        BitmapCacheManager.getInstance()?.putEncodedBackgroundToCache(page,preEncoded?:"")
                        readyToSave = true
                        bgBitmap.recycle()
                    }
                }
                else {
                    documentSaving = async {
                        val encoded = Base64Tool.encodeImage(bgBitmap)
                        BitmapCacheManager.getInstance()
                            ?.putEncodedBackgroundToCache(page, encoded ?: "")
                        readyToSave = true
                        bgBitmap.recycle()
                    }
                }
            }
        }
    }

    fun initiateCanvas(pg:Int){
        Log.d("Canvas","Initiating canvas.")
        BitmapCacheManager.getInstance()?.putCanvasToCache(pg,serializeNoBG(this))
    }
    fun initiateCanvas(pg:Int, serialized:CanvasModel){
        Log.d("Canvas","Initiating canvas.")
        BitmapCacheManager.getInstance()?.putCanvasToCache(pg,serialized)
        canvasLoading = false
        readyToDrawPath = false
    }

    fun sleep(){
        canvasLoading = false
        readyToDrawPath = false
        BitmapCacheManager.getInstance()?.putCanvasToCache(canvasPaper.page,serializeNoBG(this))
        clear()
        canvasPaper.clearBackgroundList()
        canvasPaper.clearPathList()
    }

    private fun awake(){
        canvasLoading = true
        BitmapCacheManager.getInstance()?.getCanvasAsync(canvasPaper.page) {
            data->

            Log.d("load result","strokes: ${data?.penStrokes?.size}, bitmaps: ${data?.images?.size}")
            if (data == null) {
                Log.e("Canvas","Failed to load. data is null")
                return@getCanvasAsync
            }
            canvasPaper.resetPathList()
            canvasLoading = false
            readyToDrawPath = true
            for(p in data.penStrokes){
                addStroke(p.x,p.y,MyCanvasView.DrawingToolMod.PEN,p.width,p.color)
                for(sp in p.points){
                    appendStroke(sp.x,sp.y,MyCanvasView.DrawingToolMod.PEN)
                }
            }

            for(h in data.highlightStrokes){
                addStroke(h.x,h.y,MyCanvasView.DrawingToolMod.HIGHLIGHTER,h.width,h.color)
                for(sp in h.points){
                    appendStroke(sp.x,sp.y,MyCanvasView.DrawingToolMod.HIGHLIGHTER)
                }
            }

            for(b in data.images){
                val bitmap = Base64Tool.decodeImage(b.img)
                if(bitmap != null){
                    Log.d("Canvas","Added Bitmap.")
                    bitmapList.add(CanvasBitmap(b.x,b.y,bitmap))
                }
            }
            canvasPaper.loadBackground()
            Log.d("load result","Loaded cached canvas")

            redraw()
        }
    }

    fun lastStroke():CanvasStroke?{
        if(!isReady())
            return null
        return strokeList.lastOrNull()
    }

    fun lastHighlighter():CanvasHighlighter?{
        if(!isReady())
            return null
        return highlighterList.lastOrNull()
    }

    fun redraw(){
        if(bitmapCache == null)
            bitmapCache = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        if(!readyToDrawPath) {
            if(!canvasLoading)
                awake()
            return
        }
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

    fun isReady():Boolean{
        if(!readyToDrawPath) {
            if (!canvasLoading)
                awake()
            return false
        }
        return true
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
        if(!isReady())
            return
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
        if(!isReady())
            return
        if(penType == MyCanvasView.DrawingToolMod.PEN) {
            if (strokeList.isNotEmpty())
                strokeList.last().appendStroke(strokeX, strokeY)
        }
        else{
            if (highlighterList.isNotEmpty())
                highlighterList.last().appendStroke(strokeX, strokeY)
        }
    }


    private fun checkBitmapOverlapAndErase(x:Float, y:Float, radius: Float):Boolean{
        var result = false
        for(b in bitmapList){
            val xn = max(b.x,min(x,(b.x+b.width)))
            val yn = max(b.y,min(y,(b.y+b.height)))

            val dx = xn - x
            val dy = yn - y
            if( (dx*dx+dy*dy) <= radius * radius ){
                b.eraseArea(radius,x,y)
                result = true
            }
        }
        return result
    }
    fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float):Boolean{
        if(!isReady())
            return false
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

        erased = erased || checkBitmapOverlapAndErase(eraseX,eraseY, radius)

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
        bitmapCache?.recycle()
        bitmapCache = null
        if(readyToDrawPath)
            sleep()
    }

    fun getCache():Bitmap?{
        return bitmapCache
    }


    fun getBackground():List<Bitmap>{
        if(!canvasPaper.bgLoaded) {
            if(canvasPaper.hasBackground && !canvasPaper.bgLoading){
                canvasPaper.loadBackground()
            }
            return listOf()
        }
        return canvasPaper.bgBitmapFraction
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
        const val ROW = 100
        const val COL = 100

        fun deserialize(data: CanvasModel): DrawingCanvas {
            return DrawingCanvas(data.width, data.height, data.hasBG)
        }

        suspend fun serialize(canvas:DrawingCanvas):CanvasModel{
            if(!canvas.isReady()){
                var timeout = 100
                while(!canvas.isReady() && timeout-- > 0){
                    delay(100)
                }
            }

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
            val bg:String = if(!canvas.canvasPaper.hasBackground)
                ""
            else if(canvas.canvasPaper.readyToSave)
                BitmapCacheManager.getInstance()?.getEncodedImageAsync(canvas.canvasPaper.page)?:""
            else{
                canvas.canvasPaper.documentSaving?.await()
                BitmapCacheManager.getInstance()?.getEncodedImageAsync(canvas.canvasPaper.page)?:""
            }

            return CanvasModel(canvas.width,canvas.height,pList,hList,bList,canvas.canvasPaper.hasBackground && bg.isNotBlank(),bg)
        }

        fun serializeNoBG(canvas:DrawingCanvas):CanvasModel{
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

            Log.d("Serialize result","strokes: ${pList.size}, bitmaps: ${bList.size}")

            return CanvasModel(canvas.width,canvas.height,pList,hList,bList,false,"")
        }
    }
}