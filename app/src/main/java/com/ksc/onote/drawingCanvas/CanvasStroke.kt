package com.ksc.onote.drawingCanvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import java.util.Stack
import kotlin.math.abs
import kotlin.math.pow

open class CanvasStroke(private var x:Float, private var y:Float, private val container: DrawingCanvas.CanvasPaper, private val paint: Paint){
    open class StrokePoint(val pX:Int, val pY:Int, val path: CanvasStroke, val includeDrawing:Boolean = true){
        private lateinit var container:MutableList<StrokePoint>
        fun addToPiecewiseCanvas(cont:MutableList<StrokePoint>){
            container = cont
        }
    }

    private val drawThreshold = 2f

    private val pathPoints: Stack<StrokePoint> = Stack()
    var removed = false
    var path: Path = Path()
    init{
        path.moveTo(x,y)
        path.lineTo(x,y)
        addStrokePath(x,y)
    }

    private fun addStrokePath(newX:Float, newY:Float){
        path.lineTo(newX,newY)
        x = newX
        y = newY
        val sp = StrokePoint(newX.toInt(), newY.toInt(), this)

        addDividedPathPoint(sp)
        pathPoints.push(sp)
        container.addPoint(sp)
    }

    private fun addDividedPathPoint(sp: StrokePoint){
        val DIVIDE_LEN = 100
        if(pathPoints.isNotEmpty()){
            val sqrlen = (pathPoints.peek().pX.toFloat() - sp.pX).pow(2) + (pathPoints.peek().pY.toFloat() - sp.pY).pow(2)
            if( sqrlen >DIVIDE_LEN * DIVIDE_LEN){
                val cnt = (sqrlen/(DIVIDE_LEN * DIVIDE_LEN)).toInt()
                val normal = Pair((sp.pX.toFloat()-pathPoints.peek().pX)/(cnt+1), (sp.pY.toFloat()-pathPoints.peek().pY)/(cnt+1))
                val first = Pair(pathPoints.lastElement().pX,pathPoints.lastElement().pY)
                for(i in 1..cnt){
                    val newSp = StrokePoint(first.first + (normal.first * i).toInt(), first.second + (normal.second * i).toInt(), this, false)
                    pathPoints.push(newSp)
                    container.addPoint(newSp)
                }
            }
        }
    }

    open fun appendStroke(newX:Float, newY:Float){
        if(newX<0 || newX >= container.getWidth() || newY<0 || newY>= container.getHeight())
            return
        if(abs(newX-x) + abs(newY-x) < drawThreshold)
            return
        addStrokePath(newX,newY)
    }


    open fun removeStroke(){
        removed = true
        while(pathPoints.isNotEmpty()){
            container.removePathPoint(pathPoints.last())
            pathPoints.pop()
        }
    }

    fun draw(viewCanvas: Canvas?){
        viewCanvas?.drawPath(path,paint)
    }

    companion object{
        fun serialize(canvasStroke:CanvasStroke):StrokeData{
            val l:MutableList<StrokePointData> = mutableListOf()
            for(p in canvasStroke.pathPoints){
                if(p.includeDrawing){
                    l.add(StrokePointData(p.pX,p.pX))
                }
            }
            return StrokeData(canvasStroke.x,canvasStroke.y, canvasStroke.paint.strokeWidth,canvasStroke.paint.color,l)
        }
    }
}