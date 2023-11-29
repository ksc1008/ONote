package com.example.canvastext.formulaViewer

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import androidx.core.graphics.get
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.example.canvastext.R
import katex.hourglass.`in`.mathlib.MathView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.math.max
import kotlin.math.min

class FormulaViewer(ctx: Context?, attrs: AttributeSet?): MathView(ctx,attrs) {
    var keepTouching:Boolean = false

    var popAnimation:Animation? = null
    val anim2:ObjectAnimator = ObjectAnimator.ofFloat(this,"elevation",10f)

    var alternativeDraw:Boolean = false
    var cachedBitmap:Bitmap? = null

    val originalWidth by lazy{
        layoutParams.width
    }
    val originalHeight by lazy{
        layoutParams.height
    }

    interface FormulaLongTouchListener{
        fun invokeTouchDown()
        fun invokeTouchUp()
        fun invokeLongTouch()
    }

    private var longTouchListener:FormulaLongTouchListener? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if(!isEnabled)
            return false
        when(event?.action){
            null->return super.onTouchEvent(event)

            MotionEvent.ACTION_DOWN->{
                if(!alternativeDraw)
                    findActualDrawRange()

                longTouchListener?.invokeTouchDown()
                keepTouching = true
                popAnimation =  AnimationUtils.loadAnimation(context,R.anim.formula_expand_animation)
                popAnimation?.interpolator = EasingInterpolator(Ease.QUAD_IN)
                popAnimation?.setAnimationListener(object :AnimationListener{
                    override fun onAnimationStart(p0: Animation?) {
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        if(keepTouching)
                            longTouchListener?.invokeLongTouch()
                        keepTouching = false
                    }

                    override fun onAnimationRepeat(p0: Animation?) {
                    }


                })
                startAnimation(popAnimation)


                anim2.interpolator = EasingInterpolator(Ease.QUAD_IN)
                anim2.start()
            }

            MotionEvent.ACTION_UP->{
                longTouchListener?.invokeTouchUp()
                keepTouching = false
                popAnimation?.cancel()
                anim2.end()
                clearAnimation()

                scaleX = 1.0f
                scaleY = 1.0f
                elevation = 0f
                z = 0f
            }
        }

        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return super.onInterceptTouchEvent(ev)
    }

    fun reset(){
        keepTouching = false
        scaleX = 1.0f
        scaleY = 1.0f
        elevation = 0f
        z = 0f
    }

    fun setLongTouchListener(listener: FormulaLongTouchListener){
        longTouchListener = listener
    }

    fun findActualDrawRange() {
        val bitmap: Bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Log.d("Original Size: ","($originalWidth,$originalHeight)")

        draw(canvas)
        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1

        for(i in 0 until height){
            for(j in 0 until width){
                if(bitmap[j,i]!=0){
                    minX = min(j,minX)
                    minY = min(i,minY)
                    maxX = max(j,maxX)
                    maxY = max(i,maxY)
                }
            }
        }

        layoutParams.apply {
            width = maxX-minX + 10
            height = maxY-minY +10
        }
        layoutParams = layoutParams
        Log.d("Actual Draw Range: ","($minX,$minY) - ($maxX,$maxY)")
        cachedBitmap = Bitmap.createBitmap(bitmap,minX,minY,maxX-minX+1,maxY-minY+1)
        background = ColorDrawable(Color.WHITE)
        alternativeDraw = true
    }

    override fun onDraw(canvas: Canvas?) {
        if(!alternativeDraw || cachedBitmap == null) {
            super.onDraw(canvas)
            return
        }
        else{
            canvas?.save()
            canvas?.translate(5f,5f)
            canvas?.drawBitmap(cachedBitmap!!,0f,0f,null)
            canvas?.restore()
        }
    }

    override fun setDisplayText(formulaText:String){
        layoutParams.apply {
            width = originalWidth
            height = originalHeight
        }
        background = ColorDrawable(Color.TRANSPARENT)
        layoutParams = layoutParams
        alternativeDraw = false
        super.setDisplayText(formulaText)
    }

    fun getFormulaImage():Bitmap{
        val bit:Bitmap = Bitmap.createBitmap(layoutParams.width,layoutParams.height, Bitmap.Config.ARGB_8888,true)
        val canvas:Canvas = Canvas(bit)

        val origin = background
        background = ColorDrawable(Color.TRANSPARENT)
        draw(canvas)
        background = origin
        return bit
    }
}
