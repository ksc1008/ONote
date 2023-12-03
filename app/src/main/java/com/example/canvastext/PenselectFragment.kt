package com.example.canvastext

import android.animation.ArgbEvaluator
import android.animation.TimeAnimator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Interpolator
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.fragment.app.Fragment
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.example.canvastext.databinding.FragmentPenselectBinding


class PenselectFragment : Fragment() {

    private var toolbarOpened = false

    var binding:FragmentPenselectBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPenselectBinding.inflate(layoutInflater)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.toolButton?.setOnClickListener {
            if(!toolbarOpened)
                clickToolButton()
            else
                closeToolButton()}
    }

    fun hideSettingMenu(){
        if(binding == null)
            return

        if(binding!!.penSettingMenu.visibility == View.INVISIBLE)
            return

        val fadeAnimation =  AnimationUtils.loadAnimation(context, R.anim.formula_popout_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        fadeAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                binding!!.penSettingMenu.visibility = View.INVISIBLE

            }

            override fun onAnimationRepeat(p0: Animation?) {
            }
        })
        binding!!.penSettingMenu.startAnimation(fadeAnimation)
    }

    fun showSettingMenu(){
        if(binding == null)
            return

        if(binding!!.penSettingMenu.visibility == View.VISIBLE)
            return

        binding!!.penSettingMenu.isEnabled=true
        val fadeAnimation =  AnimationUtils.loadAnimation(requireActivity(),R.anim.formula_popin_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        binding!!.penSettingMenu.startAnimation(fadeAnimation)
        binding!!.penSettingMenu.visibility = View.VISIBLE
    }
    fun dp2px(dp: Float): Float {
        val resources = this.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun clickToolButton(){
        if(binding?.toolButton == null)
            return

        toolbarOpened = true

        val evaluator = ArgbEvaluator();
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp30 = dp2px(30f)
        val dp15 = dp2px(15f)
        val dp25 = dp2px(25f)
        val dp50 = dp2px(50f)

        animator.duration = 300
        animator.addUpdateListener {
            val fraction = it.animatedFraction
            val params = binding?.toolButton?.layoutParams as ViewGroup.MarginLayoutParams
            binding?.toolButton?.imageTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#454545"),Color.parseColor("#FAFAFA")) as Int)
            binding?.toolButton?.backgroundTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#DDDDDD"),Color.parseColor("#FA4444")) as Int)


            params.marginStart = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.bottomMargin = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.width = (fraction * dp50 + (1-fraction) * dp30).toInt()
            params.height = (fraction * dp50 + (1-fraction) * dp30).toInt()

            binding?.toolButton?.rotation = (1-fraction) * 45
            binding?.toolButton?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animator.start()
        showSettingMenu()
        toolBGAnimator()
    }

    fun closeToolButton(){
        if(binding?.toolButton == null)
            return

        toolbarOpened = false

        val evaluator = ArgbEvaluator();
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp30 = dp2px(30f)
        val dp15 = dp2px(15f)
        val dp25 = dp2px(25f)
        val dp50 = dp2px(50f)
        val dp12 = dp2px(12f)
        val dp5 = dp2px(5f)

        animator.duration = 200
        animator.addUpdateListener {
            val fraction = 1-it.animatedFraction
            val params = binding?.toolButton?.layoutParams as ViewGroup.MarginLayoutParams
            binding?.toolButton?.imageTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#454545"),Color.parseColor("#FAFAFA")) as Int)
            binding?.toolButton?.backgroundTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#DDDDDD"),Color.parseColor("#FA4444")) as Int)


            params.marginStart = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.bottomMargin = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.width = (fraction * dp50 + (1-fraction) * dp30).toInt()
            params.height = (fraction * dp50 + (1-fraction) * dp30).toInt()

            binding?.toolButton?.rotation = (1-fraction) * 45
            binding?.toolButton?.elevation = fraction * dp12 + (1-fraction) * dp5
            binding?.toolButton?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animator.start()
        toolBGCloseAnimator()
    }

    fun toolBGAnimator(){
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp165 = dp2px(165f)
        val dp40 = dp2px(40f)

        animator.duration = 300
        animator.addUpdateListener {
            val fraction = it.animatedFraction
            val params = binding?.toolButtonsBackground?.layoutParams as ViewGroup.MarginLayoutParams

            params.width = ((dp40 * 0.6f) + fraction * dp40 * 0.4f).toInt()
            params.height = (fraction * dp165).toInt()
            binding?.toolButtonsBackground?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUAD_OUT)

        binding?.toolsEraserButton?.startAnimation(AnimationUtils.loadAnimation(context,R.anim.toollist_icon_expand_animation))
        binding?.toolsEraserButton?.visibility = View.VISIBLE
        binding?.toolsPenButton?.startAnimation(AnimationUtils.loadAnimation(context,R.anim.toollist_icon_expand_animation))
        binding?.toolsPenButton?.visibility = View.VISIBLE
        binding?.toolsHighlighterButton?.startAnimation(AnimationUtils.loadAnimation(context,R.anim.toollist_icon_expand_animation))
        binding?.toolsHighlighterButton?.visibility = View.VISIBLE

        binding?.toolButtonsBackground?.visibility = View.VISIBLE
        animator.start()
    }

    fun toolBGCloseAnimator(){
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp165 = dp2px(165f)
        val dp40 = dp2px(40f)

        animator.duration = 150
        animator.addUpdateListener {
            val fraction = 1-it.animatedFraction
            val params = binding?.toolButtonsBackground?.layoutParams as ViewGroup.MarginLayoutParams

            params.width = ((dp40 * 0.6f) + fraction * dp40 * 0.4f).toInt()
            params.height = (fraction * dp165).toInt()
            binding?.toolButtonsBackground?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUART_OUT)

        val anim = AnimationUtils.loadAnimation(context,R.anim.toollist_icon_shrink_animation)
        anim.interpolator = EasingInterpolator(Ease.QUART_OUT)


        binding?.toolsEraserButton?.startAnimation(anim)


        binding?.toolsPenButton?.startAnimation(anim)
        anim.setAnimationListener(object:AnimationListener{
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                binding?.toolsEraserButton?.visibility = View.INVISIBLE
                binding?.toolsPenButton?.visibility = View.INVISIBLE
                binding?.toolsHighlighterButton?.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(p0: Animation?) {
            }
        })
        binding?.toolsHighlighterButton?.startAnimation(anim)

        animator.doOnEnd { binding?.toolButtonsBackground?.visibility = View.INVISIBLE }
        animator.start()
    }
}