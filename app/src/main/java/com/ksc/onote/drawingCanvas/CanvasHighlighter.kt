package com.ksc.onote.drawingCanvas

import android.graphics.Paint
import android.util.Log
import java.util.Stack
import kotlin.math.pow

class CanvasHighlighter(private var x:Float, private var y:Float, private val container: DrawingCanvas.CanvasPaper, private val paint: Paint):
    CanvasStroke(x,y,container,paint){
    init{
        path.moveTo(x,y)
        path.lineTo(x,y)
        addStrokePath(x,y)
    }

    companion object{
        fun serialize(highlight:CanvasHighlighter):StrokeData{
            val l:MutableList<StrokePointData> = mutableListOf()
            for(p in highlight.pathPoints){
                if(p.includeDrawing){
                    l.add(0,StrokePointData(p.pX,p.pY))
                    Log.d("test Stroke Data save trace","${p.pY}")
                }
            }

            return StrokeData(highlight.x, highlight.y ,highlight.paint.strokeWidth/5,highlight.paint.color,l)
        }
    }
}