package com.example.lab7_monstarlabs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lab7_monstarlabs.databinding.ItemListAudioFocusBinding
import com.example.lab7_monstarlabs.databinding.ItemListAudioNomalBinding
import com.example.lab7_monstarlabs.helper.CommunicationHome
import com.example.lab7_monstarlabs.model.MyAudio

class AdapterListAudio(val listener:CommunicationHome,private val audios: MutableList<MyAudio>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewType1 = 1
    private val viewType2 = 2
    private var idItemSelected:Long = -1

    class ViewHolderNormal(binding: ItemListAudioNomalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imgAudio = binding.imgAudio
        val tvNameAudio = binding.tvNameAudio
        val tvSizeAudio = binding.tvSizeAudio
    }

    class ViewHolderSelected(binding: ItemListAudioFocusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imgAudio = binding.imgAudio
        val tvNameAudio = binding.tvNameAudio
        val tvSizeAudio = binding.tvSizeAudio
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            viewType2 -> ViewHolderSelected(ItemListAudioFocusBinding.inflate(LayoutInflater.from(parent.context),parent,false))
            else -> ViewHolderNormal(ItemListAudioNomalBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val myAudio = audios[position]
        when(holder.itemViewType){
            viewType2 -> {
                val holderSelected = holder as ViewHolderSelected
                holderSelected.tvNameAudio.text = myAudio.titleAudio
                holderSelected.tvSizeAudio.text = "${myAudio.size}"
            }
            else -> {
                val holderNormal = holder as ViewHolderNormal
                holderNormal.tvNameAudio.text = myAudio.titleAudio
                holderNormal.tvSizeAudio.text = "${myAudio.size}"
                holder.itemView.setOnClickListener {
                    listener.setItemOnClick(myAudio.idAudio)
                    idItemSelected = myAudio.idAudio
                    notifyItemChanged(position)
                }
            }
        }
    }

    fun setPositionSelected(idAudio: Long){
         idItemSelected = idAudio
    }

    override fun getItemCount(): Int {
        return audios.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (audios[position].idAudio) {
            idItemSelected -> viewType2
            else -> viewType1
        }
    }
}