package com.ksc.onote.drawingCanvas

import androidx.navigation.NavType
import java.io.Serializable

data class ImageData(val img:String, val x:Float, val y:Float)
data class StrokeData(val width:Float, val color:Int, val points:List<StrokePointData>): Serializable
data class CanvasModel(val width:Int, val height:Int, val penStrokes:List<StrokeData>, val highlightStrokes:List<StrokeData>): Serializable {
}

data class StrokePointData(val x:Int, val y:Int):Serializable