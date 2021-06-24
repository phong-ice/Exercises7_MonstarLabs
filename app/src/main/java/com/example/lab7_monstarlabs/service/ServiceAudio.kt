package com.example.lab7_monstarlabs.service

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.lab7_monstarlabs.R
import com.example.lab7_monstarlabs.helper.MyApplication.Companion.CHANNEL_ID
import com.example.lab7_monstarlabs.helper.Utils.ACTION_NEXT
import com.example.lab7_monstarlabs.helper.Utils.ACTION_PAUSE
import com.example.lab7_monstarlabs.helper.Utils.ACTION_PREVIOUS
import com.example.lab7_monstarlabs.helper.Utils.CLOSE_SERVICE
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_NORMAL
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_REPEAT_ALL
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_REPEAT_ONE
import com.example.lab7_monstarlabs.helper.Utils.PLAY_MODE_SHUFFLE
import com.example.lab7_monstarlabs.model.MyAudio
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

class ServiceAudio : Service() {

    inner class BinderAudio : Binder() {
        fun getService(): ServiceAudio = this@ServiceAudio
    }

    private val mIBinder = BinderAudio()
    private lateinit var audios: MutableList<MyAudio>
    private lateinit var audiosLiveData: MutableLiveData<MutableList<MyAudio>>
    private lateinit var audioLiveData: MutableLiveData<MyAudio>
    private lateinit var isPlayingAudioLiveData: MutableLiveData<Boolean>
    private var playMode: String = PLAY_MODE_NORMAL
    private var mediaPlayer: MediaPlayer? = null
    private var drawablePlay: Int = R.drawable.ic_baseline_pause_24
    private var titlePlay: String = "Pause"
    private var positionPlaying = -1
    private var audioPlaying: MyAudio? = null
    private var coroutineSendBroadcast: Job? = null
    private val pendingIntentPrevious: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    private val pendingIntentPlay: PendingIntent by lazy {
        PendingIntent.getBroadcast(this, 1, Intent(ACTION_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private val pendingIntentNext: PendingIntent by lazy {
        PendingIntent.getBroadcast(this, 1, Intent(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT)
    }


    @DelicateCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        audios = mutableListOf()
        audiosLiveData = MutableLiveData()
        audioLiveData = MutableLiveData()
        isPlayingAudioLiveData = MutableLiveData()
        registerReceiver(myBroadcastReceiver, IntentFilter(ACTION_PAUSE))
        registerReceiver(myBroadcastReceiver, IntentFilter(ACTION_NEXT))
        registerReceiver(myBroadcastReceiver, IntentFilter(ACTION_PREVIOUS))
        registerReceiver(myBroadcastReceiver, IntentFilter(CLOSE_SERVICE))
        importAllFileAudio()
    }

    override fun onBind(intent: Intent?): IBinder {
        return mIBinder
    }

    fun onPlayAudioById(idAudio: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            for ((index, audio) in audios.withIndex()) {
                if (audio.idAudio == idAudio) {
                    audioPlaying = audio
                    positionPlaying = index
                    withContext(Dispatchers.Main) {
                        audioLiveData.value = audioPlaying
                    }
                    onPlayAudio()
                }
            }
        }
    }

    private fun onPlayAudio() {
        coroutineSendBroadcast?.cancel(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayer = MediaPlayer.create(this, audioPlaying!!.uriAudio)
        drawablePlay = R.drawable.ic_baseline_pause_circle_filled_24
        createNotification(audioPlaying!!.titleAudio)
        CoroutineScope(Dispatchers.Main).launch {
            audioPlaying = audios[positionPlaying]
            audioLiveData.value = audioPlaying
            isPlayingAudioLiveData.value = mediaPlayer?.isPlaying
        }
        mediaPlayer?.start()
        checkEndAudio()
    }

    fun onNexAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            if (positionPlaying < audios.size - 1) {
                positionPlaying++
                audioPlaying = audios[positionPlaying]
                onPlayAudio()
            } else {
                withContext(Dispatchers.Main) {
                    isPlayingAudioLiveData.value = mediaPlayer?.isPlaying
                }
                drawablePlay = R.drawable.ic_baseline_play_circle_filled_24
                createNotification(audioPlaying!!.titleAudio)
                coroutineSendBroadcast?.cancel(null)
            }
        }
    }

    fun onPreviousAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            if (positionPlaying > 0) {
                positionPlaying--
                audioPlaying = audios[positionPlaying]
                onPlayAudio()
            }
        }
    }

    fun onPauseAudio() {
        when {
            mediaPlayer!!.isPlaying -> {
                mediaPlayer?.pause()
                CoroutineScope(Dispatchers.Main).launch {
                    isPlayingAudioLiveData.value = mediaPlayer?.isPlaying
                    drawablePlay = R.drawable.ic_baseline_play_circle_filled_24
                    createNotification(audioPlaying!!.titleAudio)
                    coroutineSendBroadcast?.cancel(null)
                }
            }
            else -> {
                mediaPlayer?.start()
                isPlayingAudioLiveData.value = mediaPlayer?.isPlaying
                drawablePlay = R.drawable.ic_baseline_pause_circle_filled_24
                createNotification(audioPlaying!!.titleAudio)
                checkEndAudio()
            }
        }
    }

    fun getListAudio(): MutableLiveData<MutableList<MyAudio>> = audiosLiveData
    fun getAudioPlaying(): LiveData<MyAudio> = audioLiveData
    fun getIsPlayingAudio(): LiveData<Boolean> = isPlayingAudioLiveData
    fun getDurationAudio(): Int? = mediaPlayer?.duration
    fun getCurrentPositionAudio(): Int? = mediaPlayer?.currentPosition
    fun setCurrentPositionAudio(progress: Int) = mediaPlayer?.seekTo(progress)
    fun getPlayMode(): String = playMode
    fun setPlayMode(model: String) {
        playMode = model
    }

    private fun checkEndAudio() {
        coroutineSendBroadcast = null
        coroutineSendBroadcast = CoroutineScope(Dispatchers.IO).launch {
            repeat(mediaPlayer!!.duration - mediaPlayer!!.currentPosition) {
                if (mediaPlayer!!.duration < (mediaPlayer!!.currentPosition + 1000)) {
                    when (playMode) {
                        PLAY_MODE_NORMAL -> onNexAudio()
                        PLAY_MODE_SHUFFLE -> {
                            positionPlaying = Random.nextInt(0, audios.size)
                            audioPlaying = audios[positionPlaying]
                            onPlayAudio()
                        }
                        PLAY_MODE_REPEAT_ONE -> onPlayAudio()
                        PLAY_MODE_REPEAT_ALL -> {
                            if (positionPlaying == (audios.size - 1)) {
                                positionPlaying = -1
                            }
                            onNexAudio()
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE -> onPauseAudio()
                ACTION_PREVIOUS -> onPreviousAudio()
                ACTION_NEXT -> onNexAudio()
                CLOSE_SERVICE -> onCloseMediaPlayer()
            }
        }
    }

    private fun createNotification(name: String) {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.itunes)
        val notification =
            androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .addAction(
                    R.drawable.ic_baseline_skip_previous_24,
                    "Previous",
                    pendingIntentPrevious
                ) // #0
                .addAction(drawablePlay, titlePlay, pendingIntentPlay)
                .addAction(R.drawable.ic_baseline_skip_next_24, "Next", pendingIntentNext)
                .addAction(
                    R.drawable.ic_baseline_close_24_black, "Close", PendingIntent.getBroadcast(
                        this, 5,
                        Intent(CLOSE_SERVICE), PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                .setContentTitle("Your Audio")
                .setContentText(name)
                .setLargeIcon(bitmap)
                .setSound(null)
                .build()
        startForeground(1, notification)
    }

    fun onCloseMediaPlayer() {
        coroutineSendBroadcast?.cancel(null)
        coroutineSendBroadcast = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
    }

    @DelicateCoroutinesApi
    private fun importAllFileAudio() {
        val collection = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
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
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1)
                    val size = cursor.getInt(2)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val myAudio = MyAudio(id, name, uri, size)
                    audios.add(myAudio)
                    withContext(Dispatchers.Main) {
                        audiosLiveData.value = audios
                    }
                }
            }
        }
    }

}