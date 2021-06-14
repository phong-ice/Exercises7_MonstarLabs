package com.example.exercises7_monstarlabs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.exercises7_monstarlabs.databinding.ItemListAudioBinding
import com.example.exercises7_monstarlabs.databinding.ItemListAudioSelectedBinding
import com.example.exercises7_monstarlabs.helper.CommunicationAdapterAudio
import com.example.exercises7_monstarlabs.model.MyAudio

class AdapterListAudio(
    private val listener: CommunicationAdapterAudio,
    private val audios: MutableList<MyAudio>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var idSelected = -1
    private val VIEW_TYPE = 1
    private val VIEW_TYPE_SELECTED = 2

    class ViewHolder(private val binding: ItemListAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tvName = binding.tvNameAudio
        val tvDuration = binding.tvArtistAudio
        val tvStt= binding.tvSttAudio
    }

    class ViewHolder2(private val binding: ItemListAudioSelectedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tvName = binding.tvNameAudio
        val tvDuration = binding.tvArtistAudio
        val btnPlay = binding.btnPlay
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SELECTED -> ViewHolder2(
                ItemListAudioSelectedBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            else -> ViewHolder(
                ItemListAudioBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE -> {
                val holderNormal = holder as ViewHolder
                val myAudio = audios[position]
                holderNormal.tvName.text = myAudio.name
                holderNormal.tvDuration.text = myAudio.duration
                holderNormal.tvStt.text = myAudio.id.toString()
                holderNormal.itemView.setOnClickListener {
                    getItemSelected(myAudio.id)
                    notifyItemChanged(position)
                    listener.setOnClickItem(position)
                }
            }

            VIEW_TYPE_SELECTED -> {
                val holderSelected = holder as ViewHolder2
                val myAudio = audios[position]
                holderSelected.tvName.text = myAudio.name
                holderSelected.tvDuration.text = myAudio.duration
                holderSelected.btnPlay.setOnClickListener {

                }
            }
        }
    }

    override fun getItemCount(): Int {
        return audios.size
    }

    override fun getItemViewType(position: Int): Int {
        val myAudio = audios[position]
        return when (myAudio.id) {
            idSelected -> VIEW_TYPE_SELECTED
            else -> VIEW_TYPE
        }
    }

    fun getItemSelected(id: Int) {
        idSelected = id
    }

}