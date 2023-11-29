package com.example.canvastext.formulaViewer

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.example.canvastext.R
import com.example.canvastext.ServerRequestViewModel
import com.example.canvastext.databinding.FragmentFormulaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FormulaFragment : Fragment() {

    interface OnImageCopiedListener{
        fun invoke()
    }
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
    private val serverModel: ServerRequestViewModel by activityViewModels()
    private val viewInActivity: FragmentContainerView by lazy{
        requireActivity().findViewById(R.id.formulaFragmentContainer)
    }

    private var binding:FragmentFormulaBinding? = null

    private var _onButtonClickedListener: OnBtnClickedListener? = null

    private var _onViewDestroyedListener: OnViewDestroyedListener? = null
    private var _onImageCopiedListener: OnImageCopiedListener? = null

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

    fun setOnImageCopiedListener(listener: OnImageCopiedListener){
        _onImageCopiedListener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val formulaObserver = Observer<String> { value ->
            binding?.formulaDisplay?.setDisplayText(value)
        }
        viewModel.outputString.observe(viewLifecycleOwner, formulaObserver)
        binding?.closeButton?.setOnClickListener{
            //close()
            hide()
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

        binding?.formulaDisplay?.setLongTouchListener(object:FormulaViewer.FormulaLongTouchListener{
            override fun invokeTouchDown() {
            }

            override fun invokeTouchUp() {
            }

            override fun invokeLongTouch() {
                _onImageCopiedListener?.invoke()
                hide()
            }

        })
        viewModel.setFormula("$$ a x+b=c $$")
    }

    private fun close(){
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    fun getFormulaImage():Bitmap?{
        return binding?.formulaDisplay?.getFormulaImage()
    }

    fun getCalculate(){
        GlobalScope.launch(Dispatchers.Main) {
            serverModel.getFormulaFromServer(Bitmap.createBitmap(0,0,Bitmap.Config.ARGB_8888))
        }
    }

    fun hide(){
        Log.d("","View Destroyed")

        viewInActivity.isEnabled = false
        binding?.formulaDisplay?.isEnabled=false
        val fadeAnimation =  AnimationUtils.loadAnimation(context, R.anim.formula_popout_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        fadeAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                viewInActivity.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(p0: Animation?) {
            }
        })
        viewInActivity.startAnimation(fadeAnimation)
    }

    fun show(){
        viewInActivity.setLayerType(View.LAYER_TYPE_HARDWARE,null)
        if(viewInActivity.visibility == View.INVISIBLE) {
            binding?.formulaDisplay?.isEnabled=true
            binding?.formulaDisplay?.reset()
            val fadeAnimation =  AnimationUtils.loadAnimation(requireActivity(),R.anim.formula_popin_animation)
            fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
            viewInActivity.startAnimation(fadeAnimation)
            viewInActivity.visibility = View.VISIBLE
            viewInActivity.isEnabled = true
        }
    }
}