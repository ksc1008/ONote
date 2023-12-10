package com.ksc.onote.drawingCanvas

import android.os.Parcelable
import androidx.navigation.NavType
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import kotlinx.serialization.Serializable

@Serializable
data class ImageData(val img:String, val x:Float, val y:Float)
@Serializable
data class StrokeData(val x:Float, val y:Float, val width:Float, val color:Int,
                      val points:List<StrokePointData>)
@Serializable
data class CanvasModel(val width:Int, val height:Int, val penStrokes:List<StrokeData>,
                       val highlightStrokes:List<StrokeData>, val images:List<ImageData>,
                       val hasBG:Boolean, val bg:String)

@Serializable
data class NoteModel(val name:String, val data1:String, val data2:String, val canvases:List<CanvasModel>)

@Serializable
data class StrokePointData(val x:Int, val y:Int)

object CanvasSerializer{
}