package com.cs407.lab5_milestone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter() : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {return 0}

    class NoteViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val noteTitle: TextView = itemView.findViewById(R.id.titleTextView)
        val noteAbstract: TextView = itemView.findViewById(R.id.abstractTextView)
        val noteDate: TextView = itemView.findViewById(R.id.dateTextView)
    }
}
