package com.example.lab7_monstarlabs.activity

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab7_monstarlabs.R
import com.example.lab7_monstarlabs.adapter.AdapterListAudio
import com.example.lab7_monstarlabs.databinding.ActivityHomeBinding
import com.example.lab7_monstarlabs.helper.CommunicationHome
import com.example.lab7_monstarlabs.model.MyAudio
import com.example.lab7_monstarlabs.service.ServiceAudio
import kotlinx.coroutines.*
import java.util.*

class Home : AppCompatActivity(), CommunicationHome {

    private val binding: ActivityHomeBinding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private lateinit var serviceAudio: ServiceAudio
    private var isServiceConnected = false
    private lateinit var audios: MutableList<MyAudio>
    private lateinit var audiosBackup: MutableList<MyAudio>
    private lateinit var adapterListAudio: AdapterListAudio
    private var idItemSelected: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.title = "Home"
        handingRecyclerView()
        Intent(this, ServiceAudio::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        binding.layoutMinimize.setOnClickListener {
            Intent(this, PlayingAudioActivity::class.java).also {
                startActivity(it)
            }
        }

        binding.btnPlayMinimize.setOnClickListener {
            serviceAudio.onPauseAudio()
        }

        binding.btnCloseMinimize.setOnClickListener {
            serviceAudio.onCloseMediaPlayer()
            adapterListAudio.setPositionSelected(-1)
            CoroutineScope(Dispatchers.IO).launch {
                for ((index, audio) in audiosBackup.withIndex()) {
                    if (audio.idAudio == idItemSelected) {
                        withContext(Dispatchers.Main) {
                            adapterListAudio.notifyItemChanged(index)
                        }
                        idItemSelected = -1
                    }
                }
            }
            binding.layoutMinimize.visibility = View.GONE
        }

        binding.edtSearch.addTextChangedListener {
            CoroutineScope(Dispatchers.IO).launch {
                val searchPattern = it.toString()
                delay(500)
                audios.clear()
                for (audio in audiosBackup) {
                    when {
                        audio.titleAudio.toLowerCase(Locale.ROOT).contains(
                            searchPattern.toLowerCase(
                                Locale.ROOT
                            )
                        ) -> {
                            audios.add(audio)
                        }
                        searchPattern == "" -> {
                            audios.addAll(audiosBackup)
                            break
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    adapterListAudio.notifyDataSetChanged()
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val iBinder: ServiceAudio.BinderAudio = service as ServiceAudio.BinderAudio
            serviceAudio = iBinder.getService()
            isServiceConnected = true
            serviceAudio.getListAudio().observe(this@Home, {
                audios.addAll(it)
                audiosBackup.addAll(it)
                adapterListAudio.notifyDataSetChanged()
            })
            serviceAudio.getAudioPlaying().observe(this@Home,{
                binding.layoutMinimize.visibility = View.VISIBLE
                binding.tvNameAudioMinimize.text = it.titleAudio
                adapterListAudio.setPositionSelected(it.idAudio)
                adapterListAudio.notifyDataSetChanged()
            })
            serviceAudio.getIsPlayingAudio().observe(this@Home,{
                when(it){
                    true -> binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_pause_24)
                    else -> binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceConnected = false
        }
    }

    private fun handingRecyclerView() {
        audios = mutableListOf()
        audiosBackup = mutableListOf()
        adapterListAudio = AdapterListAudio(this, audios)
        binding.lvAudio.apply {
            adapter = adapterListAudio
            layoutManager = LinearLayoutManager(this@Home)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        isServiceConnected = false
    }

    override fun setItemOnClick(idAudio: Long) {
        idItemSelected = idAudio
        serviceAudio.onPlayAudioById(idAudio)
    }
}