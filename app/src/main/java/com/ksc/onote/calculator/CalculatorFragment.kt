package com.ksc.onote.formulaViewer

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.ksc.onote.OnImageCopiedListener
import com.ksc.onote.R
import com.ksc.onote.ServerRequestViewModel
import com.ksc.onote.databinding.FragmentCalculatorBinding
import com.ksc.onote.databinding.FragmentFormulaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CalculatorFragment : Fragment() {

    interface OnViewDestroyedListener{
        fun invoke()
    }

    interface OnBtnClickedListener{
        fun invoke()
    }

    private val viewModel: FormulaViewModel by activityViewModels()
    private val viewInActivity: FragmentContainerView by lazy{
        requireActivity().findViewById(R.id.calculationFragmentContainer)
    }

    private var binding:FragmentCalculatorBinding? = null

    private var _onButtonClickedListener: OnBtnClickedListener? = null

    private var _onViewDestroyedListener: OnViewDestroyedListener? = null
    private var _onImageCopiedListener: OnImageCopiedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCalculatorBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _onViewDestroyedListener?.invoke()
        binding = null
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
            binding?.formulaDisplay?.setDisplayText("$$ = $value $$")
        }
        viewModel.calculationResult.observe(viewLifecycleOwner, formulaObserver)
        binding?.closeButton?.setOnClickListener{
            hide()
        }

        binding?.btn1?.setOnClickListener{
            _onButtonClickedListener?.invoke()
        }

        binding?.formulaDisplay?.setLongTouchListener(object:
            FormulaViewer.FormulaLongTouchListener {
            override fun invokeTouchDown() {
            }

            override fun invokeTouchUp() {
            }

            override fun invokeLongTouch() {
                _onImageCopiedListener?.invoke()
                hide()
            }

        })
        binding?.formulaDisplay?.setDisplaySizeDp(8f)
    }

    fun getFormulaImage():Bitmap?{
        return binding?.formulaDisplay?.getFormulaImage()
    }

    fun hide(instant:Boolean = false){
        if(viewInActivity.visibility == View.INVISIBLE)
            return

        viewInActivity.isEnabled = false
        binding?.formulaDisplay?.isEnabled=false
        val fadeAnimation =  AnimationUtils.loadAnimation(context, R.anim.formula_popout_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)

        if(instant)
            fadeAnimation.duration = 1

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

    fun show(instant:Boolean = false){
        viewInActivity.setLayerType(View.LAYER_TYPE_HARDWARE,null)
        if(viewInActivity.visibility != View.INVISIBLE)
            return

        binding?.formulaDisplay?.isEnabled=true
        binding?.formulaDisplay?.reset()
        val fadeAnimation =  AnimationUtils.loadAnimation(requireActivity(),R.anim.formula_popin_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)

        if(instant)
            fadeAnimation.duration = 1

        viewInActivity.startAnimation(fadeAnimation)
        viewInActivity.visibility = View.VISIBLE
        viewInActivity.isEnabled = true
    }
}