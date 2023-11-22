package com.example.canvastext

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.agog.mathdisplay.MTFontManager
import com.agog.mathdisplay.MTMathView
import com.agog.mathdisplay.render.MTFont
import com.example.canvastext.databinding.FragmentFormulaBinding

class FormulaFragment : Fragment() {

    companion object {
        fun newInstance() = FormulaFragment()
    }

    private val viewModel: FormulaViewModel by activityViewModels()

    private var binding:FragmentFormulaBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFormulaBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("","Creating View")
        binding?.formulaDisplay?.setText("123123")
        val formulaObserver = Observer<String> { value ->
            binding?.formulaDisplay?.setText(value)
            Log.d("","latex Value changed ${binding?.formulaDisplay?.getText()}")
        }
        viewModel.outputString.observe(viewLifecycleOwner, formulaObserver)
    }
}