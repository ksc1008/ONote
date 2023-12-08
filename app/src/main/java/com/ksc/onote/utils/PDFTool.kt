package com.ksc.onote.utils

import android.R.attr
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File


object PDFTool {
    fun read_from_file(file: File, densityDpi:Int):List<Bitmap>{
        val bitmaps:MutableList<Bitmap> = mutableListOf()

        try {
            val renderer: PdfRenderer =
                PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))

            val pageCnt = renderer.pageCount
            for(i in 0 until pageCnt){
                val page = renderer.openPage(i)

                val width = densityDpi / 72 * page.width
                val height = densityDpi / 72 * page.height

                val bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)

                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)

                page.close()
            }
            renderer.close()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
        return bitmaps
    }

    fun read_from_pdf_uri(uri: Uri, context: Context):List<Bitmap>{
        val bitmaps:MutableList<Bitmap> = mutableListOf()

        val resolver:ContentResolver = context.contentResolver

        try {
            val descriptor = resolver.openFileDescriptor(uri, "r") ?: return bitmaps
            val renderer: PdfRenderer =
                PdfRenderer(descriptor)

            val pageCnt = renderer.pageCount
            for(i in 0 until pageCnt){
                val page = renderer.openPage(i)
                val dpi = context.resources.displayMetrics.densityDpi

                val width = dpi / 72 * page.width
                val height = dpi / 72 * page.height

                val bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)


                bitmaps.add(bitmap.copy(Bitmap.Config.HARDWARE,false))

                page.close()
            }
            descriptor.close()
            renderer.close()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
        return bitmaps
    }
}