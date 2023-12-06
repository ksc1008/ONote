package com.ksc.onote.canvasViewUI

import android.animation.ArgbEvaluator
import android.animation.TimeAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.ksc.onote.databinding.FragmentPenselectBinding
import com.ksc.onote.utils.DisplayTool
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener
import com.ksc.onote.R
import com.skydoves.colorpickerview.listeners.ColorListener


class PenselectFragment : Fragment() {
    var selected:Int = -1
    var kept:Int = 1

    interface ToolSelectListener{
        fun invokeHighlighter()
        fun invokePen()
        fun invokeEraser()
    }

    interface OnPenSettingChangedListener{
        fun invokeSliderMove(value:Int)
        fun invokeColorChange(color:Int)
    }

    private var toolbarOpened = false
    private var onToolSelectListener: ToolSelectListener? = null
    private var onPenSettingChangedListener: OnPenSettingChangedListener? = null

    var binding:FragmentPenselectBinding? = null
    val tools by lazy{listOf(binding?.toolsHighlighterButton,binding?.toolsPenButton,binding?.toolsEraserButton)}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPenselectBinding.inflate(layoutInflater)

        return binding?.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.toolButton?.setOnClickListener {
            if(!toolbarOpened)
                clickToolButton()
            else
                closeToolButton()}

        for(i in tools.indices){
            tools[i]?.setOnClickListener {
                changeSelectedItem(i)
                when(i){
                    0-> onToolSelectListener?.invokeHighlighter()
                    1-> onToolSelectListener?.invokePen()
                    2-> onToolSelectListener?.invokeEraser()
                }
            }
        }

        binding?.widthSlider?.addOnChangeListener(object:OnChangeListener{
            @SuppressLint("RestrictedApi")
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                onPenSettingChangedListener?.invokeSliderMove(value.toInt())
                binding?.penWidthIndicator?.text =value.toInt().toString()
            }

        })

        binding?.colorPickerButton?.setOnClickListener {
            if(binding?.colorPickerContainer?.visibility == View.INVISIBLE)
                showColorPalette()
            else
                hideColorPalette()
        }

        binding?.colorPicker?.attachBrightnessSlider(binding?.brightnessSlideBar!!)
        binding?.colorPicker?.setColorListener(object: ColorListener {
            override fun onColorSelected(color: Int, fromUser: Boolean) {
                binding?.colorPickerButton?.backgroundTintList = ColorStateList.valueOf(color)
                onPenSettingChangedListener?.invokeColorChange(color)
            }

        })
        binding?.colorPicker?.pureColor = Color.BLACK
        binding?.brightnessSlideBar?.invalidate()
    }

    fun onOtherScreenSelected(){
        closeToolButton()
        hideSettingMenu()
        hideColorPalette()
    }

    private fun hideSettingMenu(){
        if(binding == null)
            return

        if(binding!!.penSettingMenu.visibility == View.INVISIBLE)
            return

        val fadeAnimation =  AnimationUtils.loadAnimation(context, R.anim.formula_popout_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        fadeAnimation.setAnimationListener(object: AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {binding!!.penSettingMenu.visibility = View.INVISIBLE}
            override fun onAnimationRepeat(p0: Animation?) {}
        })
        binding!!.penSettingMenu.startAnimation(fadeAnimation)
    }

    private fun showSettingMenu(){
        if(binding == null)
            return

        if(binding!!.penSettingMenu.visibility == View.VISIBLE)
            return

        binding!!.penSettingMenu.isEnabled=true
        val fadeAnimation =  AnimationUtils.loadAnimation(requireActivity(),
            R.anim.formula_popin_animation
        )
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        binding!!.penSettingMenu.startAnimation(fadeAnimation)
        binding!!.penSettingMenu.visibility = View.VISIBLE
    }

    private fun showColorPalette(){
        if(binding == null)
            return

        if(binding!!.colorPickerContainer.visibility == View.VISIBLE)
            return

        binding!!.colorPickerContainer.isEnabled=true
        val fadeAnimation =  AnimationUtils.loadAnimation(requireActivity(),
            R.anim.formula_popin_animation
        )
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        binding!!.colorPickerContainer.startAnimation(fadeAnimation)
        binding!!.colorPickerContainer.visibility = View.VISIBLE
    }

    private fun hideColorPalette(){
        if(binding == null)
            return

        if(binding!!.colorPickerContainer.visibility == View.INVISIBLE)
            return

        val fadeAnimation =  AnimationUtils.loadAnimation(context, R.anim.formula_popout_animation)
        fadeAnimation.interpolator = EasingInterpolator(Ease.QUAD_OUT)
        fadeAnimation.setAnimationListener(object: AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {binding!!.colorPickerContainer.visibility = View.INVISIBLE}
            override fun onAnimationRepeat(p0: Animation?) {}
        })
        binding!!.colorPickerContainer.startAnimation(fadeAnimation)
    }


    private fun clickToolButton(){
        if(binding?.toolButton == null)
            return

        toolbarOpened = true

        val evaluator = ArgbEvaluator();
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp45 = DisplayTool.dp2px(45f,this.requireContext())
        val dp15 = DisplayTool.dp2px(15f,this.requireContext())
        val dp25 = DisplayTool.dp2px(25f,this.requireContext())
        val dp65 = DisplayTool.dp2px(60f,this.requireContext())

        animator.duration = 300
        animator.addUpdateListener {
            val fraction = it.animatedFraction
            val params = binding?.toolButton?.layoutParams as ViewGroup.MarginLayoutParams
            binding?.toolButton?.imageTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#454545"),Color.parseColor("#FAFAFA")) as Int)
            binding?.toolButton?.backgroundTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#DDDDDD"),Color.parseColor("#FA4444")) as Int)


            params.marginStart = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.bottomMargin = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.width = (fraction * dp65 + (1-fraction) * dp45).toInt()
            params.height = (fraction * dp65 + (1-fraction) * dp45).toInt()

            binding?.toolButton?.rotation = (1-fraction) * 45
            binding?.toolButton?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animator.start()
        showSettingMenu()
        toolBGAnimator()
    }

    private fun closeToolButton(){
        if(binding?.toolButton == null)
            return

        if(!toolbarOpened)
            return

        toolbarOpened = false


        val dp45 = DisplayTool.dp2px(45f,requireContext())
        val dp15 = DisplayTool.dp2px(15f,requireContext())
        val dp25 = DisplayTool.dp2px(25f,requireContext())
        val dp65 = DisplayTool.dp2px(60f,requireContext())
        val dp12 = DisplayTool.dp2px(12f,requireContext())
        val dp5 = DisplayTool.dp2px(5f,requireContext())

        val evaluator = ArgbEvaluator()
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)
        animator.duration = 200
        animator.addUpdateListener {
            val fraction = 1-it.animatedFraction
            val params = binding?.toolButton?.layoutParams as ViewGroup.MarginLayoutParams
            binding?.toolButton?.imageTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#454545"),Color.parseColor("#FAFAFA")) as Int)
            binding?.toolButton?.backgroundTintList = ColorStateList.valueOf(evaluator.evaluate(fraction, Color.parseColor("#DDDDDD"),Color.parseColor("#FA4444")) as Int)


            params.marginStart = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.bottomMargin = (fraction * dp15 + (1-fraction) * dp25).toInt()
            params.width = (fraction * dp65 + (1-fraction) * dp45).toInt()
            params.height = (fraction * dp65 + (1-fraction) * dp45).toInt()

            binding?.toolButton?.rotation = (1-fraction) * 45
            binding?.toolButton?.elevation = fraction * dp12 + (1-fraction) * dp5
            binding?.toolButton?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animator.start()
        toolBGCloseAnimator()
    }

    private fun toolBGAnimator(){
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp220 = DisplayTool.dp2px(220f,requireContext())
        val dp60 = DisplayTool.dp2px(60f,requireContext())

        animator.duration = 300
        animator.addUpdateListener {
            val fraction = it.animatedFraction
            val params = binding?.toolButtonsBackground?.layoutParams as ViewGroup.MarginLayoutParams

            params.width = ((dp60 * 0.6f) + fraction * dp60 * 0.4f).toInt()
            params.height = (fraction * dp220).toInt()
            binding?.toolButtonsBackground?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUAD_OUT)

        for(tool in tools){
            tool?.startAnimation(AnimationUtils.loadAnimation(context,
                R.anim.toollist_icon_expand_animation
            ))
            tool?.visibility = View.VISIBLE
        }

        binding?.toolButtonsBackground?.visibility = View.VISIBLE
        animator.start()
    }

    private fun toolBGCloseAnimator(){
        val animator = TimeAnimator.ofFloat(0.0f,1.0f)

        val dp220 = DisplayTool.dp2px(220f,requireContext())
        val dp60 = DisplayTool.dp2px(60f,requireContext())

        animator.duration = 150
        animator.addUpdateListener {
            val fraction = 1-it.animatedFraction
            val params = binding?.toolButtonsBackground?.layoutParams as ViewGroup.MarginLayoutParams

            params.width = ((dp60 * 0.6f) + fraction * dp60 * 0.4f).toInt()
            params.height = (fraction * dp220).toInt()
            binding?.toolButtonsBackground?.layoutParams = params
        }

        animator.interpolator = EasingInterpolator(Ease.QUART_OUT)

        val anim = AnimationUtils.loadAnimation(context, R.anim.toollist_icon_shrink_animation)
        anim.interpolator = EasingInterpolator(Ease.QUART_OUT)

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

        for(tool in tools){
            tool?.startAnimation(anim)
        }

        animator.doOnEnd { binding?.toolButtonsBackground?.visibility = View.INVISIBLE }
        animator.start()
    }

    private fun startAnim(item:ImageButton?, isActive:Boolean, doAnimation:Boolean){
        if(item==null)
            return

        val dp3 = DisplayTool.dp2px(3f,requireContext())
        val dp2 = DisplayTool.dp2px(2f,requireContext())

        if(doAnimation) {
            val evaluator = ArgbEvaluator()
            val animator = TimeAnimator.ofFloat(0.0f,1.0f)

            animator.duration = 200
            animator.addUpdateListener {
                val fraction = if (isActive) it.animatedFraction else (1 - it.animatedFraction)
                item.imageTintList = ColorStateList.valueOf(
                    evaluator.evaluate(
                        fraction,
                        Color.parseColor("#000000"),
                        Color.parseColor("#DDDDDD")
                    ) as Int
                )
                item.backgroundTintList = ColorStateList.valueOf(
                    evaluator.evaluate(
                        fraction,
                        Color.parseColor("#DDDDDD"),
                        ResourcesCompat.getColor(resources,R.color.kscPurple,null)
                    ) as Int
                )

                item.elevation = fraction * dp2 + (1 - fraction) * dp3
            }
            animator.start()
        }
        else{
            item.imageTintList = ColorStateList.valueOf(if (!isActive) Color.parseColor("#000000") else Color.parseColor("#DDDDDD"))
            item.backgroundTintList = ColorStateList.valueOf(if (!isActive) Color.parseColor("#DDDDDD") else ResourcesCompat.getColor(resources,R.color.kscPurple,null))
            item.elevation = if (isActive) dp2 else dp3
        }
    }

    fun changeSelectedItem(item:Int){
        if(selected == item)
            return
        if(selected!=-1)
            startAnim(tools[selected],false, toolbarOpened)
        if(item!=-1)
            startAnim(tools[item],true, toolbarOpened)
        selected = item
        if(item!=-1)
            kept = item
    }

    fun setPentoolActive(active:Boolean){
        if(active)
            changeSelectedItem(kept)
        else{
            changeSelectedItem(-1)
        }
    }

    fun setOnToolSelectListener(listener: ToolSelectListener){
        onToolSelectListener = listener
    }

    fun setOnPenSettingChangedListener(listener: OnPenSettingChangedListener){
        onPenSettingChangedListener = listener
    }

}