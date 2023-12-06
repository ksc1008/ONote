package com.ksc.onote

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ksc.onote.canvasViewUI.OnAreaAssignedListener
import com.ksc.onote.canvasViewUI.PenselectFragment
import com.ksc.onote.databinding.ActivityCanvasBinding
import com.ksc.onote.drawingCanvas.CanvasViewModel
import com.ksc.onote.formulaViewer.FormulaFragment
import com.ksc.onote.formulaViewer.FormulaViewModel
import com.ksc.onote.graphViewer.GraphFragment
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
    private val serverRequestViewModel: ServerRequestViewModel by viewModels()
    private val penSelectFragment: PenselectFragment? by lazy{
        binding.penSelectFragmentContainer.getFragment<PenselectFragment?>()?.apply {
            setOnToolSelectListener(object:
                PenselectFragment.ToolSelectListener {
                override fun invokeHighlighter() {
                    changeTool(Toolbar.Pen)
                    this@CanvasActivity.binding.toolbarPenButton.setImageResource(R.drawable.highlighter2)
                    this@CanvasActivity.binding.canvas.changeHighlighter()
                }

                override fun invokePen() {
                    changeTool(Toolbar.Pen)
                    this@CanvasActivity.binding.toolbarPenButton.setImageResource(R.drawable.pen2)
                    this@CanvasActivity.binding.canvas.changePen()
                }

                override fun invokeEraser() {
                    changeTool(Toolbar.Pen)
                    this@CanvasActivity.binding.toolbarPenButton.setImageResource(R.drawable.eraser2)
                    this@CanvasActivity.binding.canvas.changeErase()
                }

            })

            changeSelectedItem(1)

            setOnPenSettingChangedListener(object:
                PenselectFragment.OnPenSettingChangedListener {
                override fun invokeSliderMove(value: Int) {
                    canvasViewModel.setPenWidth(value.toFloat())
                }

                override fun invokeColorChange(color: Int) {
                    canvasViewModel.setPenColor(color)
                }

            })
        }
    }

    lateinit var buttons:ArrayList<ImageButton>
    private val canvasViewModel: CanvasViewModel by viewModels()
    lateinit var scaleDetector: ScaleGestureDetector
    private var lastPointerX = 0f
    private var lastPointerY = 0f
    private var draggingImage:Boolean = false
    private var canvasX = 0
    private var canvasY = 0

    private fun changeTool(toolbar: Toolbar){
        for(i:Int in 0 until buttons.size){
            if(i == toolbar.id)
                buttons[i].alpha = TOOLBAR_ACTIVATE_TRANSPARENCY
            else
                buttons[i].alpha = TOOLBAR_DEACTIVATE_TRANSPARENCY
        }
        binding.canvas.changeTool(toolbar)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        binding.canvas.injectViewModel(canvasViewModel)
        NetworkManager.getInstance(this)
        buttons = arrayListOf(binding.toolbarPenButton,binding.toolbarRectangleButton, binding.toolbarHandButton)

        binding.ClearButton.setOnClickListener{
            binding.canvas.clearCanvas()
        }

        binding.canvas.setOnAreaAssignedListener(object :
            OnAreaAssignedListener {
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
                setOnImageCopiedListener(object:
                    OnImageCopiedListener {
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
                        binding.graphFragmentContainer.getFragment<GraphFragment?>()?.hide()
                    }
                })

                setOnButtonClickedListener(object: FormulaFragment.OnBtnClickedListener{
                    override fun invokeButton1() {
                    }

                    override fun invokeButton2() {
                        binding.graphFragmentContainer.getFragment<GraphFragment?>()?.show()
                    }

                    override fun invokeButton3() {
                    }

                })
            }).commit()


        supportFragmentManager.beginTransaction()
            .add(binding.graphFragmentContainer.id, GraphFragment().apply {
                setOnImageCopiedListener(object:
                    OnImageCopiedListener {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun invoke() {
                        val img = getGraphImage()
                        if(img != null) {
                            Log.d("Capture Log", "(${img.width},${img.height})")
                            binding.canvas.addBitmapToCanvas(img)
                            changeTool(Toolbar.Pen)
                            binding.canvas.dispatchTouchEvent(MotionEvent.obtain(0,0, MotionEvent.ACTION_DOWN,
                                lastPointerX-canvasX,lastPointerY-canvasY,0))
                            draggingImage = true

                        }
                        binding.formulaFragmentContainer.getFragment<FormulaFragment?>()?.hide()
                    }
                })
            }).commit()

        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
                if(Toolbar.values()[i]!= Toolbar.Pen){
                    penSelectFragment?.setPentoolActive(false)
                }
                else{
                    penSelectFragment?.setPentoolActive(true)
                }
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

        scaleDetector = ScaleGestureDetector(this, object: SimpleOnScaleGestureListener() {
            override fun onScale(p0: ScaleGestureDetector): Boolean {
                Log.d("scale Event:","${p0.scaleFactor}")
                binding.canvas.scaleEvent(p0.scaleFactor)
                return true
            }
        })

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {

            }
        })

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.canvas.setOnTouchListener { view, motionEvent ->
            view.onTouchEvent(motionEvent)
            if(motionEvent.action==MotionEvent.ACTION_DOWN){
                penSelectFragment?.onOtherScreenSelected()
            }
            true
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
        if(ev!=null)
            scaleDetector.onTouchEvent(ev)
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

    private fun getFromServer(bitmap: Bitmap){
        scope.launch(Dispatchers.Main) {
            val result = scope.async {
                serverRequestViewModel.getFormulaFromServer(bitmap)
            }.await()
            Log.d("result:",result.toString())

            if(result.second) {
                formulaViewModel.setFormula(result.first)
                popInFormula()
                changeTool(Toolbar.Hand)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        hideSystemUI()
        super.onWindowFocusChanged(hasFocus)
    }

    private fun hideSystemUI() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            val controller = window.insetsController

            if(controller != null){
                controller.hide(WindowInsets.Type.systemBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

}
