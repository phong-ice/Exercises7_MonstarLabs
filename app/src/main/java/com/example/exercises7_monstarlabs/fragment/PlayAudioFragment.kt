package com.example.exercises7_monstarlabs.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.example.exercises7_monstarlabs.R
import com.example.exercises7_monstarlabs.databinding.FragmentPlayAudioBinding
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_CHANGED_PROGRESS
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_NEXT
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PAUSE
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PLAY
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PREVIOUS
import com.example.exercises7_monstarlabs.helper.Utils.AUDIO_BROADCAST
import com.example.exercises7_monstarlabs.helper.Utils.CURRENT_AUDIO
import com.example.exercises7_monstarlabs.helper.Utils.DURATION_AUDIO
import com.example.exercises7_monstarlabs.helper.Utils.MY_PROGRESS
import com.example.exercises7_monstarlabs.helper.Utils.NAME_AUDIO
import com.example.exercises7_monstarlabs.helper.Utils.PLAY_MODE_MY_AUDIO
import com.example.exercises7_monstarlabs.helper.Utils.PLAY_MODE_NORMAL
import com.example.exercises7_monstarlabs.helper.Utils.PLAY_MODE_REPEAT_ALL
import com.example.exercises7_monstarlabs.helper.Utils.PLAY_MODE_REPEAT_ONE
import com.example.exercises7_monstarlabs.helper.Utils.PLAY_MODE_SHUFFLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayAudioFragment : Fragment() {

    companion object {
        const val FRAGMENT_NAME = "play_audio_fragment"
    }

    private var isPlay: Boolean = true
    private var _progress = -1

    private val binding: FragmentPlayAudioBinding by lazy {
        FragmentPlayAudioBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.registerReceiver(broadcast, IntentFilter(AUDIO_BROADCAST))
        binding.btnNext.setOnClickListener {
            onNextAudio()
        }
        binding.btnPrevious.setOnClickListener {
            onPreviousAudio()
        }
        binding.btnPlay.setOnClickListener {
            isPlay = !isPlay
            when (isPlay) {
                true -> {
                    onPlayAudio()
                    binding.btnPlay.setImageResource(R.drawable.ic_baseline_pause_24)
                }
                false -> {
                    onPauseAudio()
                    binding.btnPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                }
            }


        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                _progress = progress
                binding.tvCurrent.text = when {
                    progress >= 60 && progress % 60 < 10 -> "${progress / 60}:0${progress % 60}"
                    progress >= 60 -> "${progress / 60}:${progress % 60}"
                    progress < 10 -> "0:0$progress"
                    else -> "0:$progress"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                context?.unregisterReceiver(broadcast)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val intent = Intent(ACTION_CHANGED_PROGRESS)
                intent.putExtra(MY_PROGRESS, _progress)
                context?.sendBroadcast(intent)
                context?.registerReceiver(broadcast, IntentFilter(AUDIO_BROADCAST))
            }

        })

        binding.btnRepeat.setOnClickListener {
            when (PLAY_MODE_MY_AUDIO) {
                PLAY_MODE_NORMAL -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24_color)
                    PLAY_MODE_MY_AUDIO = PLAY_MODE_REPEAT_ALL
                }
                PLAY_MODE_REPEAT_ALL -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_one_24)
                    PLAY_MODE_MY_AUDIO = PLAY_MODE_REPEAT_ONE
                }
                PLAY_MODE_REPEAT_ONE -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
                    PLAY_MODE_MY_AUDIO = PLAY_MODE_NORMAL
                }
                PLAY_MODE_SHUFFLE -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24_color)
                    PLAY_MODE_MY_AUDIO = PLAY_MODE_REPEAT_ALL
                    binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
                }
            }
        }

        binding.btnShuffle.setOnClickListener {
            when (PLAY_MODE_MY_AUDIO) {
                PLAY_MODE_SHUFFLE -> {
                    binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
                    PLAY_MODE_MY_AUDIO = PLAY_MODE_NORMAL
                }
                else -> {
                    binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24_color)
                    binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
                    PLAY_MODE_MY_AUDIO = PLAY_MODE_SHUFFLE
                }
            }
        }
    }

    private fun onNextAudio() {
        val intent = Intent(ACTION_NEXT)
        context?.sendBroadcast(intent)
    }

    private fun onPreviousAudio() {
        val intent = Intent(ACTION_PREVIOUS)
        context?.sendBroadcast(intent)
    }

    private fun onPauseAudio() {
        val intent = Intent(ACTION_PAUSE)
        context?.sendBroadcast(intent)
    }

    private fun onPlayAudio() {
        val intent = Intent(ACTION_PLAY)
        context?.sendBroadcast(intent)
    }

    private val broadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            CoroutineScope(Dispatchers.IO).launch {
                val name = intent?.getStringExtra(NAME_AUDIO)
                val current = intent?.getIntExtra(CURRENT_AUDIO, 0)
                val duration = intent?.getIntExtra(DURATION_AUDIO, 0)
                isPlay = intent?.getBooleanExtra("isPlay", true)!!
                withContext(Dispatchers.Main) {
                    binding.tvNameAudio.text = name
                    binding.seekBar.max = duration!!
                    binding.seekBar.progress = current!!
                    binding.btnPlay.setImageResource(
                        when {
                            isPlay -> R.drawable.ic_baseline_pause_24
                            else -> R.drawable.ic_baseline_play_arrow_24
                        }
                    )
                    binding.tvCurrent.text = when {
                        current >= 60 && current % 60 < 10 -> "${current / 60}:0${current % 60}"
                        current >= 60 -> "${current / 60}:${current % 60}"
                        current < 10 -> "0:0$current"
                        else -> "0:$current"
                    }
                    binding.tvDuration.text = when {
                        duration > 60 -> "${duration / 60}:${duration % 60}"
                        duration < 10 -> "0:0$duration"
                        else -> "0:$duration"
                    }
                }
            }
        }

    }

}