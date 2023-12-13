package com.ksc.onote

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ksc.onote.databinding.FragmentFirstBinding
import com.ksc.onote.dataloader.JsonLoader
import com.ksc.onote.dataloader.NoteListModel
import org.json.JSONObject


class FirstFragment : Fragment() {

    private var noteList:Array<String> = arrayOf()
    private var _binding: FragmentFirstBinding? = null
    private var recyclerView:RecyclerView? = null
    private var recyclerViewAdapter:CustomAdapter? = null
    private var deleteRequestCount = 0

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

        recyclerView = binding.noteList
        val linearLayoutManager = LinearLayoutManager(activity)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerViewAdapter = CustomAdapter()
        recyclerView?.adapter = recyclerViewAdapter



        NoteListModel.getInstance()?.setNoteListListener(object: NoteListModel.NoteListListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun invokeListChange() {
                recyclerViewAdapter?.setDataset(NoteListModel.getInstance()!!.getNote())
                recyclerViewAdapter?.notifyDataSetChanged()
            }
        })

        recyclerViewAdapter?.setOnItemClickListener(object: CustomAdapter.OnItemClickListener{
            override fun invokeRemoveClick(name: String) {
                deleteRequestCount++
                NetworkManager.getInstance()?.deleteRequest(name,object:NetworkGetListener<Boolean>{

                    override fun getResult(`object`: Boolean) {
                        NoteListModel.getInstance()?.getNote()
                        deleteRequestCount--
                    }
                    override fun getError(message: String) {
                        Log.e("Delete Note",message)
                        deleteRequestCount--
                    }


                })
                recyclerViewAdapter?.removeFromList(name)
            }

            override fun invokeOpenClick(name: String) {
                NetworkManager.getInstance()?.readRequest(name, object:NetworkGetListener<JSONObject?>{
                    override fun getResult(`object`: JSONObject?) {
                        val switchActivityIntent = Intent(requireContext(), CanvasActivity::class.java)


                        val obj = `object`?.getJSONObject("response")?.getJSONObject("page")?.getJSONObject("data")
                        if(obj == null){
                            Toast.makeText(requireContext(),"Invalid Data",Toast.LENGTH_LONG).show()
                            return
                        }
                        switchActivityIntent.putExtra("Type","from_json")
                        JsonLoader.getInstance(requireContext())?.putData(obj)

                        switchActivityIntent.putExtra("Name",name)
                        startActivity(switchActivityIntent)
                    }

                    override fun getError(message: String) {
                        Toast.makeText(requireContext(),"Failed to open note.",Toast.LENGTH_LONG).show()
                    }

                })
            }
        })

        NoteListModel.getInstance()?.renewNoteList()
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

    companion object{
        val TAG:String = "Note List Fragment"
    }
}