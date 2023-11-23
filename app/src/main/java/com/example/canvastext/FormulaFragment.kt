package com.example.canvastext

import android.graphics.RectF
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.canvastext.databinding.FragmentFormulaBinding

class FormulaFragment : Fragment() {

    interface OnViewDestroyedListener{
        fun invoke()
    }
    companion object {
        fun newInstance() = FormulaFragment()
    }

    private val viewModel: FormulaViewModel by activityViewModels()

    private var binding:FragmentFormulaBinding? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val formulaObserver = Observer<String> { value ->
            binding?.formulaDisplay?.setDisplayText(value)
        }
        viewModel.outputString.observe(viewLifecycleOwner, formulaObserver)
        binding?.closeButton?.setOnClickListener{
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }
    }
}