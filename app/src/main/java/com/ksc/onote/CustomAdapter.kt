package com.ksc.onote

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ksc.onote.databinding.NoteMenuItemBinding
import com.ksc.onote.dataloader.NoteListModel

class CustomAdapter() :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    private var dataSet: Array<String> = NoteListModel.getInstance()!!.getNote()

    interface OnItemClickListener{
        fun invokeRemoveClick(name:String)
        fun invokeOpenClick(name:String)
    }

    lateinit var binding: NoteMenuItemBinding

    private var mItemClickListener:OnItemClickListener? = null

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, listener:OnItemClickListener?) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        private val removeButton: Button
        val openButton: Button
        var noteName:String = ""

        init {
            // Define click listener for the ViewHolder's View.
            removeButton = view.findViewById(R.id.remove_button)
            openButton = view.findViewById(R.id.open_button)
            textView = view.findViewById(R.id.note_name)

            openButton.setOnClickListener {
                listener?.invokeOpenClick(noteName)
            }

            removeButton.setOnClickListener {
                listener?.invokeRemoveClick(noteName)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        binding = NoteMenuItemBinding.inflate(LayoutInflater.from(viewGroup.context),null,false)

        val view = binding.root
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.setLayoutParams(lp)
        return ViewHolder(view,mItemClickListener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = dataSet[position]
        viewHolder.noteName = dataSet[position]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    fun setDataset(data:Array<String>){
        dataSet = data
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        mItemClickListener = listener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeFromList(name:String){
        val ml = dataSet.toMutableList()
        ml.remove(name)
        dataSet = ml.toTypedArray()
        notifyDataSetChanged()
    }
}