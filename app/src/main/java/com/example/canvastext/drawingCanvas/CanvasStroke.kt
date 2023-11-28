package com.example.canvastext.drawingCanvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import java.util.Stack
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class CanvasStroke(private var x:Float, private var y:Float, private val container: CanvasViewModel.PiecewiseCanvas, private val paint: Paint){
    class StrokePoint(val pX:Int, val pY:Int, val path: CanvasStroke, val includeDrawing:Boolean = true){
        private lateinit var container:MutableList<StrokePoint>
        fun addToPiecewiseCanvas(cont:MutableList<StrokePoint>){
            container = cont
        }
    }

    val drawThreshold = 2f

    private val pathPoints: Stack<StrokePoint> = Stack()
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
        container.addPoint(sp.pX,sp.pY,sp)
    }

    private fun addDividedPathPoint(sp:StrokePoint){
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
                    container.addPoint(newSp.pX,newSp.pY,newSp)
                }
            }
        }
    }

    fun appendStroke(newX:Float, newY:Float){
        if(newX<0 || newX >= container.cols || newY<0 || newY>= container.rows)
            return
        if(abs(newX-x) + abs(newY-x) < drawThreshold)
            return
        addStrokePath(newX,newY)
    }

    private fun removePathPoint(p:StrokePoint){
        container.pathPoints[p.pY][p.pX].remove(p)
    }

    fun removeStroke(){
        while(pathPoints.isNotEmpty()){
            removePathPoint(pathPoints.last())
            pathPoints.pop()
        }
    }

    fun draw(viewCanvas: Canvas?){
        //for(p in pathPoints){
        //    viewCanvas?.drawCircle(p.pX.toFloat(),p.pY.toFloat(),3f,paint)
        //}
        viewCanvas?.drawPath(path,paint)
    }
}