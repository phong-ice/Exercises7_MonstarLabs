package com.example.exercises7_monstarlabs.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exercises7_monstarlabs.R
import com.example.exercises7_monstarlabs.adapter.AdapterListAudio
import com.example.exercises7_monstarlabs.databinding.ActivityHomeBinding
import com.example.exercises7_monstarlabs.fragment.PlayAudioFragment
import com.example.exercises7_monstarlabs.helper.CommunicationAdapterAudio
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
import com.example.exercises7_monstarlabs.model.MyAudio
import com.example.exercises7_monstarlabs.service.AudioService
import kotlinx.coroutines.*
import kotlin.random.Random

class Home : AppCompatActivity(), CommunicationAdapterAudio {

    private val binding: ActivityHomeBinding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val READ_STORAGE_REQUEST_CODE = 1
    private lateinit var storagePermission: Array<String>
    private lateinit var audios: MutableList<MyAudio>
    private lateinit var audiosBackup: MutableList<MyAudio>
    private lateinit var adapterAudio: AdapterListAudio
    private var mediaPlayer: MediaPlayer? = null
    private var _position = -1
    private lateinit var localMyAudio: MyAudio
    private lateinit var coroutineSendBroadcast: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()
        audios = mutableListOf()
        audiosBackup = mutableListOf()
        storagePermission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        when (checkPermissionReadStorage()) {
            false -> requestPermission()
            else -> importAllFileAudio()
        }
        addFragment(PlayAudioFragment(), PlayAudioFragment.FRAGMENT_NAME)
        handingRecyclerView()
        registerReceiver(broadcast, IntentFilter(ACTION_NEXT))
        registerReceiver(broadcast, IntentFilter(ACTION_PREVIOUS))
        registerReceiver(broadcast, IntentFilter(ACTION_PAUSE))
        registerReceiver(broadcast, IntentFilter(ACTION_PLAY))
        registerReceiver(broadcast, IntentFilter(ACTION_CHANGED_PROGRESS))
        binding.minimize.setOnClickListener {
            binding.minimize.visibility = View.GONE
            binding.fragmentLayout.visibility = View.VISIBLE
            binding.constraintLayout.visibility = View.GONE
        }
        binding.btnPlayAll.setOnClickListener {
            val lastPosition =  _position
           _position = 0
            localMyAudio = audios[_position]
            adapterAudio.getItemSelected(localMyAudio.id)
            adapterAudio.notifyItemChanged(lastPosition)
            adapterAudio.notifyItemChanged(_position)
            onPlayAudio()
        }
        binding.btnDown.setOnClickListener {
            binding.minimize.visibility = View.VISIBLE
            binding.fragmentLayout.visibility = View.GONE
            binding.constraintLayout.visibility = View.VISIBLE
        }
        binding.btnCloseMinimize.setOnClickListener {
            mediaPlayer?.stop()
            binding.minimize.visibility = View.GONE
            coroutineSendBroadcast.cancel(null)
            stopService(Intent(this@Home, AudioService::class.java))
            adapterAudio.getItemSelected(-1)
            adapterAudio.notifyItemChanged(_position)
        }
        binding.btnNextMinimize.setOnClickListener {
            onNextAudio()
        }
        binding.btnPreMinimize.setOnClickListener {
            onPreviousAudio()
        }
        binding.btnPlayMinimize.setOnClickListener {
            when (mediaPlayer?.isPlaying) {
                true -> {
                    onPauseAudio()
                    binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                }
                else -> {
                    rePlayAudio()
                    binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_pause_24)
                }
            }
        }
        binding.edtSearch.addTextChangedListener {
            CoroutineScope(Dispatchers.IO).launch {
                val pattern = it.toString()
                Log.i("test123","$pattern - before")
                delay(500)
                Log.i("test123","$pattern - after")
                audios.clear()
                for (audio in audiosBackup){
                    when{
                        audio.name.contains(pattern)  -> {
                            audios.add(audio)
                           withContext(Dispatchers.Main){
                               adapterAudio.notifyDataSetChanged()
                           }
                        }
                        pattern == "" -> {
                            audios.addAll(audiosBackup)
                            withContext(Dispatchers.Main){
                                adapterAudio.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handingRecyclerView() {
        adapterAudio = AdapterListAudio(this, audios)
        binding.lvAudio.apply {
            adapter = adapterAudio
            layoutManager = LinearLayoutManager(this@Home)
        }
    }

    private fun addFragment(fr: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_layout, fr)
        transaction.addToBackStack(tag)
        transaction.commit()
    }


    override fun setOnClickItem(position: Int) {
        _position = position
        localMyAudio = audios[position]
        binding.minimize.visibility = View.VISIBLE
        onPlayAudio()
    }

    private fun onPlayAudio() {
        mediaPlayer?.stop()
        mediaPlayer = MediaPlayer.create(this@Home, localMyAudio.uri.toUri())
        binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_pause_24)
        mediaPlayer?.start()
        sendMyBroadcast()
        sendNotification()
    }

    private fun onPauseAudio() {
        mediaPlayer?.pause()
        binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            coroutineSendBroadcast.cancel(null)
        }
        sendNotification()
    }

    private fun rePlayAudio() {
        mediaPlayer?.start()
        binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_pause_24)
        sendMyBroadcast()
        sendNotification()
    }

    private fun onNextAudio() {
        when {
            _position < audios.size - 1 -> {
                _position++
                coroutineSendBroadcast.cancel(null)
                localMyAudio = audios[_position]
                onPlayAudio()
                adapterAudio.getItemSelected(localMyAudio.id)
                adapterAudio.notifyItemChanged(_position -1 )
                adapterAudio.notifyItemChanged(_position)
                binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_pause_24)
            }
            else -> {
                when(PLAY_MODE_MY_AUDIO){
                    PLAY_MODE_REPEAT_ALL -> {
                        _position = 0
                        localMyAudio = audios[_position]
                        adapterAudio.getItemSelected(localMyAudio.id)
                        adapterAudio.notifyItemChanged(_position)
                        adapterAudio.notifyItemChanged(audios.size - 1)
                        onPlayAudio()
                    }
                    PLAY_MODE_NORMAL -> {
                        onPauseAudio()
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(1000)
                            coroutineSendBroadcast.cancel(null)
                        }
                    }
                }
            }
        }

    }

    private fun onPreviousAudio() {

        when {
            _position > 0 -> {
                _position--
                coroutineSendBroadcast.cancel(null)
                localMyAudio = audios[_position]
                onPlayAudio()
                adapterAudio.getItemSelected(localMyAudio.id)
                adapterAudio.notifyItemChanged(_position + 1)
                adapterAudio.notifyItemChanged(_position)
                binding.btnPlayMinimize.setImageResource(R.drawable.ic_baseline_pause_24)
            }
            else -> binding.btnPreMinimize.visibility = View.GONE
        }
    }


    private val broadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> rePlayAudio()
                ACTION_PAUSE -> onPauseAudio()
                ACTION_NEXT -> onNextAudio()
                ACTION_PREVIOUS -> onPreviousAudio()
                ACTION_CHANGED_PROGRESS -> {
                    val process = intent.getIntExtra(MY_PROGRESS, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        coroutineSendBroadcast.cancel(null)
                        mediaPlayer?.seekTo((process*1000).toLong(),MediaPlayer.SEEK_CLOSEST)
                        sendMyBroadcast()
                    }else{
                        coroutineSendBroadcast.cancel(null)
                        mediaPlayer?.seekTo(process)
                        sendMyBroadcast()
                    }

                }
            }

        }

    }

    private fun sendNotification() {
        val intent = Intent(this, AudioService::class.java)
        val bundle = Bundle()
        bundle.putBoolean("isPlay", mediaPlayer?.isPlaying!!)
        bundle.putString("nameAudio", localMyAudio.name)
        intent.putExtras(bundle)
        startService(intent)
    }

    private fun sendMyBroadcast() {
        coroutineSendBroadcast = CoroutineScope(Dispatchers.IO).launch {
            repeat((mediaPlayer!!.duration / 1000) - (mediaPlayer!!.currentPosition / 1000) + 2) {
                delay(1000)
                val intent = Intent(AUDIO_BROADCAST)
                intent.putExtra(NAME_AUDIO, localMyAudio.name)
                intent.putExtra(CURRENT_AUDIO, mediaPlayer!!.currentPosition / 1000)
                intent.putExtra(DURATION_AUDIO, mediaPlayer!!.duration / 1000)
                intent.putExtra("isPlay",mediaPlayer?.isPlaying)
                if ((mediaPlayer!!.currentPosition / 1000) == (mediaPlayer!!.duration / 1000)){
                    when(PLAY_MODE_MY_AUDIO){
                        PLAY_MODE_SHUFFLE -> {
                            val lastPosition = _position
                            _position = Random.nextInt(0,audios.size)
                            localMyAudio = audios[_position]
                            withContext(Dispatchers.Main){
                                adapterAudio.getItemSelected(localMyAudio.id)
                                adapterAudio.notifyItemChanged(_position)
                                adapterAudio.notifyItemChanged(lastPosition)
                            }
                            onPlayAudio()
                        }
                        PLAY_MODE_REPEAT_ONE -> {
                            coroutineSendBroadcast.cancel(null)
                            onPlayAudio()
                        }
                        else -> {
                           withContext(Dispatchers.Main){
                               onNextAudio()
                           }
                        }
                    }
                }
                sendBroadcast(intent)
            }
        }
    }

    private fun importAllFileAudio() {
        val collection = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        )
        GlobalScope.launch(Dispatchers.IO) {
            applicationContext.contentResolver.query(
                collection,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                audios.clear()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val name = cursor.getString(1)
                    val duration = cursor.getString(2)
                    val size = cursor.getString(3)
                    val path = cursor.getString(4)
                    val myAudio = MyAudio(id.toInt(), name, duration, path)
                    audios.add(myAudio)
                    audiosBackup.add(myAudio)
                    withContext(Dispatchers.Main) {
                        adapterAudio.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun checkPermissionReadStorage(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, storagePermission, READ_STORAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importAllFileAudio()
                Log.i("test123", "Request permission success!")
            } else {
                Log.i("test123", "Request permission failed!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this@Home, AudioService::class.java))
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }
}