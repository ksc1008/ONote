package com.example.canvastext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.LinkedList
import kotlin.math.abs

class MyCanvas(ctx: Context?, attrs: AttributeSet?): View(ctx,attrs) {
    internal val strokeList: LinkedList<Stroke> = LinkedList()

    init{
        PiecewisedCanvas.changeCache(2000,2000)
    }

    enum class ToolMod{PEN, ERASER}

    var currentTool:ToolMod = ToolMod.PEN
    private var isPenDown = false
    private var penX:Float = 0f
    private var penY:Float = 0f



    class StrokePoint(val pX:Float, val pY:Float, public val path: Stroke){
        private var removed:Boolean = false
        private lateinit var container:MutableList<StrokePoint>
        fun addToPiecewiseCanvas(cont:MutableList<StrokePoint>){
            container = cont
        }

        fun removeFromCanvas(){
            if(removed)
                return
            container.remove(this)
            removed = true;
        }

        fun removeStroke(){
            if(removed)
                return
            path.removeStroke()
            removed = true;
        }
    }

    class Stroke(private var x:Float, private var y:Float, private val container:LinkedList<Stroke>){
        val drawThreshold = 2f

        private val pathPoints:MutableList<StrokePoint> = mutableListOf()
        var path:Path = Path()
        init{
            Log.d("Point Message","($x,$y)")
            path.moveTo(x,y)
        }

        fun addStrokePath(newX:Float, newY:Float){
            if(newX<0 || newX >= PiecewisedCanvas.cols || newY<0 || newY>=PiecewisedCanvas.rows)
                return
            if(abs(newX-x) + abs(newY-x) < drawThreshold)
                return
            //path.quadTo(x,y,newX,newY)
            path.lineTo(newX,newY)
            val pointX = (x+newX)/2
            val pointY = (y+newY)/2
            x = newX
            y = newY
            val sp = StrokePoint(pointX,pointY,this)
            pathPoints.add(sp)
            PiecewisedCanvas.addPoint(x.toInt(),y.toInt(),sp)
        }

        fun removeStroke(removeFromContainer: Boolean =true){
            for(p in pathPoints){
                p.removeFromCanvas()
            }
            if(removeFromContainer)
                container.remove(this)
        }
    }

    internal object PiecewisedCanvas{
        val pathPoints:MutableList<MutableList<MutableList<StrokePoint>>> = mutableListOf()

        var rows:Int = 0
        var cols:Int = 0

        fun changeCache(newRows:Int, newCols:Int){
            val dRow = newRows - rows
            val dCol = newCols - cols

            for (i:Int in 1..dRow){
                pathPoints.add(mutableListOf())
                for(j:Int in 1..dCol){
                    pathPoints.last().add(mutableListOf())
                }
            }

            cols = newCols
            rows = newRows
        }

        fun addPoint(x:Int, y:Int, point:StrokePoint){
            if(x<0 || x>=cols || y<0 || y>=rows){
                return
            }
            pathPoints[y][x].add(point)
            point.addToPiecewiseCanvas(pathPoints[y][x])
        }

        fun checkOverlap(x:Int, y:Int):MutableList<StrokePoint>{
            return pathPoints[y][x]
        }
    }


    private val strokePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
    }

    private var erasorIndicator= Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f
    }

    fun clearCanvas(){
        for(s in strokeList){
            s.removeStroke(false)
        }
        strokeList.clear()
        invalidate()
    }

    private fun addStroke(strokeX:Float, strokeY:Float){
        val s:Stroke = Stroke(strokeX,strokeY,strokeList)
        strokeList.add(s)
    }

    private fun eraseCircle(radius:Float, eraseX:Float, eraseY:Float){
        Log.d("Erase Event","erasing ($eraseX,$eraseY)")
        for(j:Int in (eraseX-radius).toInt()..(eraseX+radius).toInt()){
            for(i:Int in (eraseY-radius).toInt() .. (eraseY+radius).toInt()){
                if(((eraseX-j)*(eraseX-j)+(eraseY-i)*(eraseY-i))<(radius*radius)){
                    if(j>=0 && j<PiecewisedCanvas.cols && i>=0 && i < PiecewisedCanvas.rows){
                        val t = PiecewisedCanvas.checkOverlap(j,i)
                        while(t.isNotEmpty()){
                            t[0].removeStroke()
                        }
                    }
                }
            }
        }
    }

    fun changeErase(){
        currentTool = ToolMod.ERASER
    }

    fun changePen(){
        currentTool= ToolMod.PEN
    }

    fun drawCircle(){

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null)
            return super.onTouchEvent(null)

        when(event.action){
            MotionEvent.ACTION_DOWN->{
                penX = event.x
                penY = event.y
                isPenDown = true
                return when(currentTool) {
                    ToolMod.PEN-> {
                        Log.d("Point Message", "Down! (${event.x},${event.y})")
                        invalidate()

                        addStroke(event.x, event.y)
                        true
                    }

                    ToolMod.ERASER -> {
                        eraseCircle(100f,event.x,event.y)
                        invalidate()
                        true
                    }
                }
            }

            MotionEvent.ACTION_UP->{
                isPenDown = false
                invalidate()
            }

            MotionEvent.ACTION_MOVE->{
                penX = event.x
                penY = event.y
                return when(currentTool){
                    ToolMod.PEN->{
                        strokeList.last().addStrokePath(event.x,event.y)
                        invalidate()

                        true
                    }

                    ToolMod.ERASER->{
                        eraseCircle(100f,event.x,event.y)
                        invalidate()
                        true
                    }
                }
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        for(s in strokeList){
            canvas?.drawPath(s.path,strokePaint)
        }
        if((currentTool == ToolMod.ERASER) && isPenDown){
            canvas?.drawCircle(penX,penY,100f,erasorIndicator)
        }
    }
}