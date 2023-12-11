package com.ksc.onote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ksc.onote.databinding.NoteMenuItemBinding

class CustomAdapter(private var dataSet: Array<String>) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
    lateinit var binding: NoteMenuItemBinding

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val removeButton: Button

        init {
            // Define click listener for the ViewHolder's View.
            removeButton = view.findViewById(R.id.remove_button)
            textView = view.findViewById(R.id.note_name)
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
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = dataSet[position]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    fun setDataset(data:Array<String>){
        dataSet = data
    }

}