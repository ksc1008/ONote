package com.ksc.onote.graphViewer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.ksc.onote.R
import com.ksc.onote.formulaViewer.FormulaViewer
import com.ksc.onote.utils.Base64Tool
import kotlin.math.abs


class GraphView(ctx: Context, attrs: AttributeSet?): WebView(ctx,attrs) {

    var latex:String = ""
    var keepTouching:Boolean = false
    var popAnimation:Animation? = null
    val elevationAnimator: ObjectAnimator = ObjectAnimator.ofFloat(this,"elevation",10f)
    var longTouchPoint:PointF = PointF(0f,0f)
    var screenshot:Bitmap? = null




    private fun getHtml(latex:String, grid:Boolean = true, axis:Boolean = true):String{
        val html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>desmos api</title>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <script type=\"text/javascript\" src=\"file:///android_asset/desmos.js\"></script>\n" +
                "    <style>\n" +
                "        html, body {\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            overflow: hidden;\n" +
                "            background-color: transparent;\n" +
                "        }\n" +
                "\n" +
                "        #calculator {\n" +
                "            position: absolute;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"calculator\"></div>\n" +
                "    <script>\n" +
                "        const elt = document.getElementById('calculator');\n" +
                "        var calculator = null;\n" +
                "    \n" +
                "        function Graphing(latex) {\n" +
                "            var options = { \n" +
                "            border: false};\n" +
                "            calculator = Desmos.GraphingCalculator(elt,options);\n" +
                "            calculator.setExpression({id:1, latex:latex});\n" +
                "        calculator.updateSettings({\n" +
                "        showGrid:${if(grid) "true" else "false"},\n "+
                "        showXAxis:${if(axis) "true" else "false"}, showYAxis:${if(axis) "true" else "false"},\n"+
                "            expressionsCollapsed: true,\n" +
                "            autosize:false\n" +
                "        });"+
                "        }\n" +
                "        Graphing(\"$latex\");\n" +
                "    \n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>"

        return html
    }

    private fun loadData(grid: Boolean= true, axis: Boolean=true){
        this.loadDataWithBaseURL("null",getHtml(latex,grid,axis),"text/html","UTF-8","about:blank")
    }

    fun setGrid(enable:Boolean){
        loadUrl("javascript:calculator.updateSettings({showGrid:${if(enable) "true" else "false"}})")
    }

    fun setAxis(enable:Boolean){
        loadUrl("javascript:calculator.updateSettings({showXAxis:${if(enable) "true" else "false"}, showYAxis:${if(enable) "true" else "false"}})")
    }
    fun setLatexCode(latex:String,grid: Boolean = true, axis: Boolean = true){
        this.latex = latex.replace("\\","\\\\")
        loadData(grid,axis)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurationSettingWebView(enable_zoom_in_controls: Boolean) {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.loadsImagesAutomatically =true
        settings.allowContentAccess = true
        settings.displayZoomControls = enable_zoom_in_controls
        settings.domStorageEnabled = true
        settings.builtInZoomControls = enable_zoom_in_controls
        settings.setSupportZoom(enable_zoom_in_controls)

        setLayerType(LAYER_TYPE_HARDWARE, null)
        settings.loadWithOverviewMode = true
        settings.useWideViewPort= true

        this.isVerticalScrollBarEnabled = enable_zoom_in_controls
        this.isHorizontalScrollBarEnabled = enable_zoom_in_controls

        isEnabled = true
        isContextClickable = true
        isClickable = true


        webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                if(cm==null)
                    return true
                Log.d("MyApplication", cm.message() + " -- From line "
                + cm.lineNumber() + " of "
                + cm.sourceId() );
                return super.onConsoleMessage(cm)
            }
        }

        setBackgroundColor(Color.TRANSPARENT)
    }

    private var longTouchListener: FormulaViewer.FormulaLongTouchListener? = null
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if(!isEnabled)
            return false
        when(event?.action){
            null->return super.onTouchEvent(event)

            MotionEvent.ACTION_DOWN->{
                screenshot()
                longTouchPoint = PointF(event.x,event.y)

                longTouchListener?.invokeTouchDown()
                keepTouching = true
                popAnimation =  AnimationUtils.loadAnimation(context, R.anim.formula_expand_animation)
                popAnimation?.startOffset=500
                popAnimation?.interpolator = EasingInterpolator(Ease.QUAD_IN)
                popAnimation?.setAnimationListener(object : Animation.AnimationListener {
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


                elevationAnimator.interpolator = EasingInterpolator(Ease.QUAD_IN)
                elevationAnimator.startDelay=500
                elevationAnimator.start()
            }

            MotionEvent.ACTION_MOVE->{
                if( keepTouching && ((abs(event.x-longTouchPoint.x) + abs(event.y-longTouchPoint.y))) > 100){
                    longTouchListener?.invokeTouchUp()
                    cancelLongTouch()
                }
            }

            MotionEvent.ACTION_UP->{
                if(keepTouching) {
                    longTouchListener?.invokeTouchUp()
                    cancelLongTouch()
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun cancelLongTouch(){
        keepTouching = false
        popAnimation?.cancel()
        elevationAnimator.end()
        clearAnimation()

        scaleX = 1.0f
        scaleY = 1.0f
        elevation = 0f
        z = 0f

    }

    fun setLongTouchListener(listener: FormulaViewer.FormulaLongTouchListener){
        longTouchListener = listener
    }

    fun getGraphImage(): Bitmap {
        if(screenshot == null){
            return Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        }
        else {
            Bitmap.createScaledBitmap(screenshot!!,width,height,true)
            return Bitmap.createScaledBitmap(screenshot!!,width,height,true)
        }
    }

    init {
        configurationSettingWebView(false)
        enableSlowWholeDocumentDraw()
        loadData()
    }

    fun screenshot(){
        evaluateJavascript("calculator.screenshot({targetPixelRatio: 2});"){
            Log.d(TAG,it)
            screenshot = Base64Tool.decodeImage(it.split('\"')[1].substring("data:image/png;base64,".length))
        }
    }
}