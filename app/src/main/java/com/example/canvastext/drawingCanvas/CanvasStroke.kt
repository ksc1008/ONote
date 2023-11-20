package com.example.canvastext.drawingCanvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import java.util.Stack
import kotlin.math.abs

class CanvasStroke(private var x:Float, private var y:Float, private val container: DrawingCanvas, private val paint: Paint){
    class StrokePoint(val pX:Int, val pY:Int, val path: CanvasStroke){
        private lateinit var container:MutableList<StrokePoint>
        fun addToPiecewiseCanvas(cont:MutableList<StrokePoint>){
            container = cont
        }
    }

    val drawThreshold = 2f

    private val pathPoints: Stack<StrokePoint> = Stack()
    var path: Path = Path()
    init{
        Log.d("Point Message","($x,$y)")
        path.moveTo(x,y)
        path.lineTo(x,y)
        addStrokePath(x,y)
    }

    private fun addStrokePath(newX:Float, newY:Float){
        path.lineTo(newX,newY)
        val pointX = (x+newX)/2
        val pointY = (y+newY)/2
        x = newX
        y = newY
        val sp = StrokePoint(pointX.toInt(), pointY.toInt(), this)
        pathPoints.push(sp)
        container.piecewiseCanvas.addPoint(sp.pX,sp.pY,sp)
    }

    fun appendStroke(newX:Float, newY:Float){
        if(newX<0 || newX >= container.piecewiseCanvas.cols || newY<0 || newY>= container.piecewiseCanvas.rows)
            return
        if(abs(newX-x) + abs(newY-x) < drawThreshold)
            return
        addStrokePath(newX,newY)
    }

    private fun removePathPoint(p:StrokePoint){
        container.piecewiseCanvas.pathPoints[p.pY][p.pX].remove(p)
    }

    fun removeStroke(removeFromContainer: Boolean =true){
        while(pathPoints.isNotEmpty()){
            removePathPoint(pathPoints.last())
            pathPoints.pop()
        }
        if(removeFromContainer)
            container.removeCanvasStroke(this)
    }

    fun draw(viewCanvas: Canvas?){
        //for(p in pathPoints){
        //    viewCanvas?.drawCircle(p.pX.toFloat(),p.pY.toFloat(),3f,paint)
        //}
        viewCanvas?.drawPath(path,paint)
    }
}