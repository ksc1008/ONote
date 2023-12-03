package com.example.canvastext.graphViewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.example.canvastext.OnImageCopiedListener
import com.example.canvastext.R
import com.example.canvastext.databinding.FragmentGraphBinding
import com.example.canvastext.formulaViewer.FormulaViewModel
import com.example.canvastext.formulaViewer.FormulaViewer
import java.util.Base64


class GraphFragment : Fragment() {
    private var axisEnabled = true
    private var gridEnabled = true

    private val viewModel: FormulaViewModel by activityViewModels()
    private val viewInActivity: FragmentContainerView by lazy{
        requireActivity().findViewById(R.id.graphFragmentContainer)
    }

    private var _onImageCopiedListener: OnImageCopiedListener? = null
    private var binding: FragmentGraphBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.enableSlowWholeDocumentDraw()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGraphBinding.inflate(layoutInflater)
        return binding?.root
    }

    fun setOnImageCopiedListener(listener: OnImageCopiedListener){
        _onImageCopiedListener = listener
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val formulaObserver = Observer<String> { value ->
            binding?.graphDisplay?.setLatexCode(value)
        }
        viewModel.outputString.observe(viewLifecycleOwner, formulaObserver)
        binding?.closeButton?.setOnClickListener{
            hide()
        }

        binding?.btn1?.setOnClickListener {
            gridEnabled = !gridEnabled
            binding?.graphDisplay?.setGrid(gridEnabled)
        }
        binding?.btn2?.setOnClickListener {
            axisEnabled = !axisEnabled
            binding?.graphDisplay?.setAxis(axisEnabled)
        }

        binding?.btn3?.setOnClickListener {
            binding?.graphDisplay?.screenshot()
        }

        binding?.graphDisplay?.setLongTouchListener(object: FormulaViewer.FormulaLongTouchListener{
            override fun invokeTouchDown() {
            }

            override fun invokeTouchUp() {
            }

            override fun invokeLongTouch() {
                _onImageCopiedListener?.invoke()
                hide()
            }

        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun getGraphImage(): Bitmap?{
        return binding?.graphDisplay?.getGraphImage()
    }

    fun hide(){
        if(viewInActivity.visibility == View.INVISIBLE)
            return

        viewInActivity.isEnabled = false
        binding?.graphDisplay?.isEnabled=false
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
            binding?.graphDisplay?.isEnabled=true
            val fadeAnimation =  AnimationUtils.loadAnimation(requireActivity(),R.anim.formula_popin_animation)
            fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
            viewInActivity.startAnimation(fadeAnimation)
            viewInActivity.visibility = View.VISIBLE
            viewInActivity.isEnabled = true
        }
    }
}