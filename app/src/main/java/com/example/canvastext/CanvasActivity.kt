package com.example.canvastext

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.example.canvastext.databinding.ActivityCanvasBinding
import com.example.canvastext.drawingCanvas.CanvasViewModel
import com.example.canvastext.formulaViewer.FormulaFragment
import com.example.canvastext.formulaViewer.FormulaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.abs


class CanvasActivity : AppCompatActivity() {
    private val TOOLBAR_DEACTIVATE_TRANSPARENCY:Float = 0.1f
    private val TOOLBAR_ACTIVATE_TRANSPARENCY:Float = 0.6f
    val scope = CoroutineScope(Job() + Dispatchers.Main)


    enum class Toolbar(var id:Int) {Pen(0),Rectangle(1),Hand(2)}

    private val binding:ActivityCanvasBinding by lazy { ActivityCanvasBinding.inflate(layoutInflater) }
    private val formulaViewModel: FormulaViewModel by viewModels()
    private val serverRequestViewModel:ServerRequestViewModel by viewModels()

    lateinit var buttons:ArrayList<ImageButton>
    private val canvasViewModel: CanvasViewModel by viewModels()

    private var lastPointerX = 0f
    private var lastPointerY = 0f
    private var draggingImage:Boolean = false
    private var canvasX = 0
    private var canvasY = 0

    private fun changeTool(toolbar:Toolbar){

        Log.d("toolbar log","change tool to ${toolbar.name}")
        for(i:Int in 0 until buttons.size){
            if(i == toolbar.id)
                buttons[i].alpha = TOOLBAR_ACTIVATE_TRANSPARENCY
            else
                buttons[i].alpha = TOOLBAR_DEACTIVATE_TRANSPARENCY
        }
        binding.canvas.changeTool(toolbar)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(binding.root)
        binding.canvas.injectViewModel(this)
        NetworkManager.getInstance(this)
        buttons = arrayListOf(binding.toolbarPenButton,binding.toolbarRectangleButton, binding.toolbarHandButton)

        binding.ClearButton.setOnClickListener{
            binding.canvas.clearCanvas()
        }

        binding.EraserButton.setOnClickListener {
            binding.toolbarPenButton.setImageResource(R.drawable.eraser)
            binding.canvas.changeErase()
        }

        binding.penButton.setOnClickListener {
            binding.toolbarPenButton.setImageResource(R.drawable.pen)
            binding.canvas.changePen()
        }


        binding.canvas.setOnAreaAssignedListener(object : OnAreaAssignedListener {
            override fun invoke(area: RectF) {
                if(abs(area.height() * area.width()) > 10)
                    showFormulaFragment()
            }
        })


        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
            }
        }

        supportFragmentManager.beginTransaction()
            .add(binding.formulaFragmentContainer.id, FormulaFragment().apply {
                setOnImageCopiedListener(object: FormulaFragment.OnImageCopiedListener{
                    override fun invoke() {
                        val img = getFormulaImage()
                        if(img != null) {
                            Log.d("Capture Log", "(${img.width},${img.height})")
                            binding.canvas.addBitmapToCanvas(img)
                            changeTool(Toolbar.Pen)
                            binding.canvas.dispatchTouchEvent(MotionEvent.obtain(0,0, MotionEvent.ACTION_DOWN,
                                lastPointerX-canvasX,lastPointerY-canvasY,0))
                            draggingImage = true

                        }
                    }
                })
            }).commit()

        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
            }
        }
        binding.toolbarFunctionButton.setOnClickListener{
            popInFormula()
        }

        binding.canvas.viewTreeObserver.addOnGlobalLayoutListener {
            val location = IntArray(2)
            binding.canvas.getLocationOnScreen(location)
            canvasX = location[0]
            canvasY = location[1]
        }
    }

    fun showFormulaFragment(){
        val bitmap = binding.canvas.getAreaBitmap()
        if(bitmap != null)
            getFromServer(bitmap)
    }

    private fun popInFormula(){
        if(!formulaViewModel.hasFormula())
            return
        binding.formulaFragmentContainer.getFragment<FormulaFragment?>()?.show()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        lastPointerX = ev?.x?:0f
        lastPointerY = ev?.y?:0f
        if(draggingImage){
            var ev2:MotionEvent? = null
            if(ev!=null) {
                ev2 = MotionEvent.obtain(
                    ev.downTime,
                    ev.eventTime,
                    ev.action,
                    ev.x + binding.canvas.x - canvasX,
                    ev.y + binding.canvas.y - canvasY,
                    0
                )
            }
            binding.canvas.dispatchTouchEvent(ev2)
            if(ev?.action == MotionEvent.ACTION_UP)
                draggingImage = false
        }
        return super.dispatchTouchEvent(ev)
    }

    fun getFromServer(bitmap: Bitmap){
        scope.launch(Dispatchers.Main) {
            val result = scope.async {
                serverRequestViewModel.getFormulaFromServer(bitmap)
            }.await()
            Log.d("result:",result.toString())

            if(result.second) {
                formulaViewModel.setFormula("$$ ${result.first} $$")
                popInFormula()
            }
        }
    }

}
