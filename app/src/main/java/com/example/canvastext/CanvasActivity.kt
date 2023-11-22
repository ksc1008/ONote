package com.example.canvastext

import android.graphics.RectF
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.activity.viewModels
import com.example.canvastext.databinding.ActivityCanvasBinding
import com.example.canvastext.drawingCanvas.CanvasViewModel

class CanvasActivity : AppCompatActivity() {
    private val TOOLBAR_DEACTIVATE_TRANSPARENCY:Float = 0.1f
    private val TOOLBAR_ACTIVATE_TRANSPARENCY:Float = 0.6f
    enum class Toolbar(var id:Int) {Pen(0),Rectangle(1)}

    private val binding:ActivityCanvasBinding by lazy { ActivityCanvasBinding.inflate(layoutInflater) }
    private val formulaViewModel:FormulaViewModel by viewModels()

    lateinit var buttons:ArrayList<ImageButton>
    private val model: CanvasViewModel by viewModels()

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
        buttons = arrayListOf(binding.toolbarPenButton,binding.toolbarRectangleButton)

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
                showFormulaFragment()
            }
        })


        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
            }
        }
    }

    fun showFormulaFragment(){
        if(binding.formulaFragmentContainer.getFragment<FormulaFragment?>()==null) {
            binding.formulaFragmentContainer.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .add(binding.formulaFragmentContainer.id, FormulaFragment()).commit()
            formulaViewModel.setFormula("$$ x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a} $$")
        }
        else{
            formulaViewModel.setFormula("$$ x = \\frac{-a \\pm \\sqrt{a^3-4bd}}{2a} $$")
        }
    }
}