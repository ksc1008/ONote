package com.ksc.onote.drawingCanvas

import android.graphics.Paint
import java.util.Stack
import kotlin.math.pow

class CanvasHighlighter(private var x:Float, private var y:Float, private val container: CanvasViewModel.PiecewiseCanvas, private val paint: Paint):
    CanvasStroke(x,y,container,paint){


    private val pathPoints: Stack<StrokePoint> = Stack()
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

    companion object{
        fun serialize(canvasStroke:CanvasHighlighter):StrokeData{
            return StrokeData(canvasStroke.paint.strokeWidth,canvasStroke.paint.color,canvasStroke.getPathPointsData())
        }
    }
}