package com.ksc.onote.calculator

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel


class CalculatorViewModel: ViewModel() {

    class JsBridge(){
        private var resultCallback:OnCalculationResult? = null
        @JavascriptInterface
        fun onResult(result:String){
            if(result == null || result == "" || result == "error"){
                resultCallback?.invokeFail()
            }
            else{
                resultCallback?.invokeSuccess(result)
            }
        }

        fun setOnResultListener(listener:OnCalculationResult){
            resultCallback = listener
        }
    }

    @SuppressLint("StaticFieldLeak")
    private var webView:WebView? = null

    val bridge = JsBridge()
    interface OnCalculationResult{
        fun invokeSuccess(result:String)
        fun invokeFail()
    }
    fun runAgent(activityContext: Context){
        webView = WebView(activityContext)
        addConsoleCallback()
        configurationSettingWebView()
        webView?.addJavascriptInterface(bridge,"Native")
        webView?.loadDataWithBaseURL("null",getHtml(),"text/html","UTF-8","about:blank")


    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun configurationSettingWebView() {
        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.allowFileAccess = true
        webView?.settings?.allowContentAccess = true
        webView?.settings?.domStorageEnabled = true
    }

    private fun addConsoleCallback(){
        val webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                if(cm==null)
                    return true
                Log.d("MyApplication", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId() );
                return super.onConsoleMessage(cm)
            }
        }

        val webViewClient = object: WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                Log.d("dd","Finished")
                webView?.evaluateJavascript("selectCalc(\"$1+1\");"){
                    Log.d("dd",it)
                }
            }
        }
        webView?.webChromeClient = webChromeClient
        webView?.webViewClient = webViewClient

    }

    private fun getHtml():String{
        val html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <title>desmos api</title>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <script type=\"text/javascript\" src=\"file:///android_asset/desmos.js\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<script>\n" +
                "    function waitForCondition(conditionFunction) {\n" +
                "        return new Promise(async resolve => {\n" +
                "            while (!conditionFunction()) {\n" +
                "                await new Promise(innerResolve => setTimeout(innerResolve, 100));\n" +
                "            }\n" +
                "            resolve();\n" +
                "        });\n" +
                "    }\n" +
                "    const latex = \"\\\\sqrt{2}\"\n" +
                "    function selectCalc(latex) {\n" +
                "        const calculator = Desmos.GraphingCalculator();\n" +
                "        var result = 0;\n" +
                "        calculator.setExpression({id:1, latex:latex});\n" +
                "        waitForCondition(() => calculator.expressionAnalysis[1] !== undefined).then(() => {\n" +
                "            result = calculator.expressionAnalysis[1];\n" +
                "            if(result.isError) {\n" +
                "                console.log(\"Error\");\n" +
                "                Native.onResult ('error')\n" +
                "            }\n" +
                "            else {\n" +
                "                console.log(result.evaluation.value);       // calcalate 결과\n" +
                "                Native.onResult(result.evaluation.value);       // calcalate 결과\n" +
                "            }\n" +
                "        });\n" +
                "    }\n" +
                "    selectCalc(latex);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>"

        return html
    }

    fun calculate(formula:String):Boolean {
        formula.replace("\\","\\\\")
        if(webView==null){
            return false
        }

        webView?.evaluateJavascript("selectCalc(\"${formula.replace("\\","\\\\")}\");",null)
        return true
    }

}