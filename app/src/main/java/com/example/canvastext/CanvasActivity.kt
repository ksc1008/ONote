package com.example.canvastext

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.canvastext.databinding.ActivityCanvasBinding
import com.example.canvastext.databinding.ActivityMainBinding

class CanvasActivity : AppCompatActivity() {
    lateinit var binding:ActivityCanvasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCanvasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ClearButton.setOnClickListener{
            binding.canvas.clearCanvas()
        }

        binding.EraserButton.setOnClickListener {
            binding.canvas.changeErase()
        }

        binding.penButton.setOnClickListener {
            binding.canvas.changePen()
        }
    }
}