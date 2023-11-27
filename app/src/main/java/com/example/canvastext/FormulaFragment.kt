package com.example.canvastext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.canvastext.databinding.FragmentFormulaBinding

class FormulaFragment : Fragment() {

    interface OnViewDestroyedListener{
        fun invoke()
    }

    interface OnBtnClickedListener{
        fun invokeButton1()
        fun invokeButton2()
        fun invokeButton3()
    }

    companion object {
        fun newInstance() = FormulaFragment()
    }

    private val viewModel: FormulaViewModel by activityViewModels()

    private var binding:FragmentFormulaBinding? = null

    private var _onButtonClickedListener:OnBtnClickedListener? = null

    private var _onViewDestroyedListener:OnViewDestroyedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFormulaBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _onViewDestroyedListener?.invoke()
        binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setOnViewDestroyedListener(listener: OnViewDestroyedListener){
        _onViewDestroyedListener = listener
    }

    fun setOnButtonClickedListener(listener: OnBtnClickedListener){
        _onButtonClickedListener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val formulaObserver = Observer<String> { value ->
            binding?.formulaDisplay?.setDisplayText(value)
        }
        viewModel.outputString.observe(viewLifecycleOwner, formulaObserver)
        binding?.closeButton?.setOnClickListener{
            close()
        }

        binding?.btn1?.setOnClickListener{
            _onButtonClickedListener?.invokeButton1()
        }
        binding?.btn2?.setOnClickListener{
            _onButtonClickedListener?.invokeButton2()
        }
        binding?.btn3?.setOnClickListener{
            _onButtonClickedListener?.invokeButton3()
        }

        binding?.formulaDisplay?.setOnLongClickListener {
            close()
            _onButtonClickedListener?.invokeButton1()
            Log.d("Longclick","Long Click")
            true
        }
    }

    private fun close(){
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    fun getFormulaImage():Bitmap{
        val bit:Bitmap = Bitmap.createBitmap(binding?.formulaDisplay?.width?:0,binding?.formulaDisplay?.height?:0, Bitmap.Config.ARGB_8888,true)
        val canvas:Canvas = Canvas(bit)
        binding?.formulaDisplay?.draw(canvas)
        return bit
    }
}