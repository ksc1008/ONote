package com.ksc.onote

import android.annotation.SuppressLint
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ksc.onote.databinding.FragmentFirstBinding
import com.ksc.onote.databinding.NoteMenuItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray



class FirstFragment : Fragment() {

    private var noteList:Array<String> = arrayOf()
    private var _binding: FragmentFirstBinding? = null
    private var recyclerView:RecyclerView? = null
    private var recyclerViewAdapter:CustomAdapter? = null


    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            requestList()
        }

        recyclerView = binding.noteList
        val linearLayoutManager = LinearLayoutManager(activity)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerViewAdapter = CustomAdapter(noteList)
        recyclerView?.adapter = recyclerViewAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateNoteList(notes:List<String>){
        if(!needUpdate(notes))
            return

        noteList = notes.toTypedArray().clone()
        recyclerViewAdapter?.setDataset(noteList)
        recyclerViewAdapter?.notifyDataSetChanged()
        Log.d(TAG,"Updated List")
    }

    fun needUpdate(notes:List<String>):Boolean{
        if(notes.size != noteList.size)
            return true
        for(i in 0 until notes.size){
            if(notes[i] != noteList[i]){
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requestList(){
        activity?.lifecycleScope?.launch {
            NetworkManager.getInstance()?.getRequestNameList(object:NetworkGetListener<JSONArray>{
                override fun getResult(`object`: JSONArray) {
                    val len = `object`.length()
                    val list = mutableListOf<String>()
                    for(i in 0 until len){
                        Log.d("Get Note List",`object`.getString(i))
                        list.add(`object`.getString(i))
                    }
                    updateNoteList(list)
                }

                override fun getError(message: String) {
                    Log.e("Get Note List",message)

                }

            })?: kotlin.run {
                Log.e(TAG,"No Network Manager")
            }
        }
        val list = mutableListOf<String>()
        for(i in 0 until 10){
            list.add("Item $i")
        }
        updateNoteList(list)
    }

    companion object{
        val TAG:String = "Note List Fragment"
    }
}