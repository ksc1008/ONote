package com.ksc.onote.drawingCanvas

import android.os.Parcelable
import androidx.navigation.NavType
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.io.Serializable

@Parcelize
data class ImageData(val img:String, val x:Float, val y:Float):Parcelable
@Parcelize
data class StrokeData(val x:Float, val y:Float, val width:Float, val color:Int,
                      val points:List<StrokePointData>):Parcelable
@Parcelize
data class CanvasModel(val width:Int, val height:Int, val penStrokes:List<StrokeData>,
                       val highlightStrokes:List<StrokeData>, val images:List<ImageData>,
                       val hasBG:Boolean, val bg:String): Parcelable

@Parcelize
data class NoteModel(val name:String, val data1:String, val data2:String, val canvases:List<CanvasModel>):Parcelable

@Parcelize
data class StrokePointData(val x:Int, val y:Int):Parcelable

object CanvasSerializer{
        fun toJson(note:NoteModel):String{
            return Gson().toJson(note)
        }
}