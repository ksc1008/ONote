package com.example.canvastext

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.activity.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
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
                setOnViewDestroyedListener(object: FormulaFragment.OnViewDestroyedListener{
                    override fun invoke() {
                        Log.d("","View Destroyed")

                        val fadeAnimation =  AnimationUtils.loadAnimation(context,R.anim.formula_popout_animation)
                        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
                        fadeAnimation.setAnimationListener(object:AnimationListener{
                            override fun onAnimationStart(p0: Animation?) {
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                binding.formulaFragmentContainer.visibility = View.INVISIBLE
                            }

                            override fun onAnimationRepeat(p0: Animation?) {
                            }
                        })
                        binding.formulaFragmentContainer.startAnimation(fadeAnimation)
                    }
                })
                setOnButtonClickedListener(object: FormulaFragment.OnBtnClickedListener{
                    override fun invokeButton1() {
                        val img = getFormulaImage()
                        Log.d("Capture Log","(${img.width},${img.height})")
                        binding.canvas.addBitmapToCanvas(img)

                    }

                    override fun invokeButton2() {
                    }

                    override fun invokeButton3() {
                    }
                })
            }).commit()

        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
            }
        }
    }

    fun showFormulaFragment(){
        val bitmap = binding.canvas.getAreaBitmap()
        if(bitmap != null)
            getFromServer(bitmap)
    }

    fun popInFormula(){
        if(binding.formulaFragmentContainer.visibility == View.INVISIBLE) {
            //formulaViewModel.setFormula("$$ x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a} $$")
            val fadeAnimation =  AnimationUtils.loadAnimation(this,R.anim.formula_popin_animation)
            fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
            binding.formulaFragmentContainer.startAnimation(fadeAnimation)
            binding.formulaFragmentContainer.visibility = View.VISIBLE
            binding.formulaFragmentContainer.isEnabled = true
        }

    }

    fun getFromServer(bitmap: Bitmap){
        scope.launch(Dispatchers.Main) {
            val result = scope.async {
                serverRequestViewModel.getFormulaFromServer(bitmap)
            }.await()
            Log.d("result:",result.toString())
            formulaViewModel.setFormula("$$ ${result.first} $$")
            popInFormula()
            //
        }
    }
}
