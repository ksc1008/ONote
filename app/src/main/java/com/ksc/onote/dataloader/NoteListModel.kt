package com.ksc.onote.dataloader

import android.util.Log
import com.ksc.onote.FirstFragment
import com.ksc.onote.NetworkGetListener
import com.ksc.onote.NetworkManager
import org.json.JSONArray

class NoteListModel {
    interface NoteListListener{
        fun invokeListChange()
    }

    private var noteListListener: NoteListListener? = null

    private var _noteList:MutableList<String> = mutableListOf()

    fun getNote():Array<String> = _noteList.toTypedArray()

    fun setNote(list:List<String>){
        var changed = false
        if(list.size == _noteList.size){
            for(i in list.indices){
                if (list[i] != _noteList[i]) {
                    changed = true
                    break
                }
            }
        }
        if(!changed)
            return
        _noteList = list.toMutableList()
        noteListListener?.invokeListChange()
    }
    fun setNote(list:JSONArray){
        var changed = false
        var len = list.length()
        if(list.length() != _noteList.size){
            changed = true
        }
        val result = mutableListOf<String>()
        for(i in 0 until len){
            result.add(list.getString(i))
            if(list[i] != _noteList.getOrNull(i))
                changed = true
        }
        if(!changed)
            return
        _noteList = result
        noteListListener?.invokeListChange()
    }

    fun setNoteListListener(listener: NoteListListener){
        noteListListener = listener
    }

    fun renewNoteList(){
        NetworkManager.getInstance()?.getRequestNameList(object: NetworkGetListener<JSONArray> {
            override fun getResult(`object`: JSONArray) {
                setNote(`object`)
            }

            override fun getError(message: String) {
                Log.e("Get Note List",message)
            }

        })?: kotlin.run {
            Log.e(FirstFragment.TAG,"No Network Manager")
        }
    }


    companion object {
        private const val TAG = "NetworkManager"
        private var instance: NoteListModel? = null

        @Synchronized
        fun getInstance(): NoteListModel? {
            if (instance == null) instance = NoteListModel()
            return instance
        }
    }
}