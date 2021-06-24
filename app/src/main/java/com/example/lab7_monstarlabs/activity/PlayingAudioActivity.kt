package com.example.lab7_monstarlabs.activity

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.SeekBar
import com.example.lab7_monstarlabs.R
import com.example.lab7_monstarlabs.databinding.ActivityPlayingAudioBinding
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_NORMAL
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_REPEAT_ALL
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_REPEAT_ONE
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_SHUFFLE
import com.example.lab7_monstarlabs.service.ServiceAudio
import kotlinx.coroutines.*

class PlayingAudioActivity : AppCompatActivity() {

    private val binding: ActivityPlayingAudioBinding by lazy {
        ActivityPlayingAudioBinding.inflate(layoutInflater)
    }
    private lateinit var audioService: ServiceAudio
    private var coroutineGetData: Job? = null
    private var isServiceConnection: Boolean = false
    private var duration = 0
    private var current = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.title = "Playing"
        Intent(this, ServiceAudio::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        binding.btnNextAudio.setOnClickListener {
            audioService.onNexAudio()
            getDurationAndCurrentPosition()

        }
        binding.btnPreviousAudio.setOnClickListener {
            audioService.onPreviousAudio()
            getDurationAndCurrentPosition()
        }
        binding.btnPlayAudio.setOnClickListener {
            audioService.onPauseAudio()
        }

        binding.seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvCurrentAudio.text = when {
                    (progress / 1000) >= 60 && (progress / 1000) % 60 < 10 -> "${(progress / 1000) / 60}:0${(progress / 1000) % 60}"
                    (progress / 1000) >= 60 -> "${(progress / 1000) / 60}:${(progress / 1000) % 60}"
                    (progress / 1000) < 10 -> "0:0${(progress / 1000)}"
                    else -> "0:${(progress / 1000)}"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                coroutineGetData?.cancel(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioService.setCurrentPositionAudio(seekBar!!.progress)
                current = seekBar.progress
                changeProgressSeekBar()
            }

        })
        binding.btnRepeat.setOnClickListener {
            when (audioService.getPlayMode()) {
                PLAY_MODE_NORMAL -> {
                    audioService.setPlayMode(PLAY_MODE_REPEAT_ALL)
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24_selected)
                }
                PLAY_MODE_REPEAT_ALL -> {
                    audioService.setPlayMode(PLAY_MODE_REPEAT_ONE)
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_one_24)
                }
                PLAY_MODE_REPEAT_ONE -> {
                    audioService.setPlayMode(PLAY_MODE_NORMAL)
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
                }
                PLAY_MODE_SHUFFLE -> {
                    audioService.setPlayMode(PLAY_MODE_REPEAT_ALL)
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24_selected)
                    binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
                }
            }
        }
        binding.btnShuffle.setOnClickListener {
            when (audioService.getPlayMode()) {
                PLAY_MODE_SHUFFLE -> {
                    audioService.setPlayMode(PLAY_MODE_NORMAL)
                    binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
                }
                else -> {
                    audioService.setPlayMode(PLAY_MODE_SHUFFLE)
                    binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24_selected)
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
                }
            }
        }
    }

    private fun changeProgressSeekBar() {
        coroutineGetData = null
        coroutineGetData = CoroutineScope(Dispatchers.IO).launch {
            repeat(duration - current) {
                withContext(Dispatchers.Main) {
                    binding.seekBarAudio.progress = current
                }
                current += 1000
                delay(1000)
            }
        }
    }

    private fun getDurationAndCurrentPosition() {
        audioService.getDurationAudio()?.let {
            binding.seekBarAudio.max = it
            duration = it
        }
        audioService.getCurrentPositionAudio()?.let {
            current = it
            binding.seekBarAudio.progress = it
        }
        binding.tvDurationAudio.text = when {
            (duration / 1000) >= 60 && (duration / 1000) % 60 < 10 -> "${(duration / 1000) / 60}:0${(duration / 1000) % 60}"
            (duration / 1000) >= 60 -> "${(duration / 1000) / 60}:${(duration / 1000) % 60}"
            (duration / 1000) < 10 -> "0:0${(duration / 1000)}"
            else -> "0:${(duration / 1000)}"
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val iBinder: ServiceAudio.BinderAudio = service as ServiceAudio.BinderAudio
            audioService = iBinder.getService()
            isServiceConnection = true
            audioService.getAudioPlaying().observe(this@PlayingAudioActivity, {
                binding.tvNameAudio.text = it.titleAudio
                getDurationAndCurrentPosition()
            })
            audioService.getIsPlayingAudio().observe(this@PlayingAudioActivity, {
                when (it) {
                    true -> {
                        binding.btnPlayAudio.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24)
                        coroutineGetData?.cancel(null)
                        Log.i("test123","!231231111")
                        changeProgressSeekBar()
                    }
                    else -> {
                        binding.btnPlayAudio.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
                        coroutineGetData?.cancel(null)

                    }
                }
            })
            when (audioService.getPlayMode()) {
                PLAY_MODE_SHUFFLE -> binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24_selected)
                PLAY_MODE_REPEAT_ONE -> binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_one_24)
                PLAY_MODE_REPEAT_ALL -> binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24_selected)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceConnection = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        isServiceConnection = false
        coroutineGetData?.cancel(null)
    }
}