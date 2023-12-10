package com.ksc.onote.utils

import android.R.attr
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File


object PDFTool {
    fun read_from_pdf_uri(uri: Uri, context: Context):Pair<Pair<PdfRenderer,ParcelFileDescriptor>?,List<Point>?>{

        val resolver:ContentResolver = context.contentResolver
        val descriptor:ParcelFileDescriptor
        val renderer:PdfRenderer
        val sizes:MutableList<Point> = mutableListOf()

        try {
            descriptor = resolver.openFileDescriptor(uri, "r") ?: return Pair(null,null)
            renderer = PdfRenderer(descriptor)

            val pageCnt = renderer.pageCount
            val dpi = context.resources.displayMetrics.densityDpi
            for(i in 0 until pageCnt){
                val page = renderer.openPage(i)

                val width = dpi / 72 * page.width
                val height = dpi / 72 * page.height

                sizes.add(Point(width,height))

                page.close()
            }
        } catch (ex:Exception){
            ex.printStackTrace()
            return Pair(null,sizes)
        }
        return Pair(Pair(renderer,descriptor),sizes)
    }
}