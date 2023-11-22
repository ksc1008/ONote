package com.example.canvastext

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.canvastext.databinding.ActivityCanvasBinding
import com.example.canvastext.databinding.ActivityMainBinding
import com.example.canvastext.drawingCanvas.CanvasViewModel
import com.example.canvastext.drawingCanvas.DrawingCanvas

class CanvasActivity : AppCompatActivity() {
    val TOOLBAR_DEACTIVATE_TRANSPARENCY:Float = 0.1f
    val TOOLBAR_ACTIVATE_TRANSPARENCY:Float = 0.6f
    enum class Toolbar(var id:Int) {Pen(0),Rectangle(1)}

    lateinit var binding:ActivityCanvasBinding
    lateinit var buttons:ArrayList<ImageButton>
    private val model: CanvasViewModel by viewModels()

    fun changeTool(toolbar:Toolbar){
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


        binding = ActivityCanvasBinding.inflate(layoutInflater)


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


        for(i:Int in 0 until buttons.size){
            buttons[i].setOnClickListener {
                changeTool(Toolbar.values()[i])
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }
}