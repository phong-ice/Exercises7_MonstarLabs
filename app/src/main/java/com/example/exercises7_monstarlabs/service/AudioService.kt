package com.example.exercises7_monstarlabs.service

import android.app.Notification
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.media.app.NotificationCompat
import com.example.exercises7_monstarlabs.R
import com.example.exercises7_monstarlabs.helper.MyApplication.Companion.CHANNEL_ID
import com.example.exercises7_monstarlabs.helper.Utils
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_NAME
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_NEXT
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PAUSE
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PLAY
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PREVIOUS
import com.example.exercises7_monstarlabs.helper.Utils.AUDIO_BROADCAST
import com.example.exercises7_monstarlabs.helper.Utils.MY_PATH


class AudioService:Service() {

    private lateinit var pendingIntentPrevious:PendingIntent
    private lateinit var pendingIntentNext:PendingIntent
    private lateinit var pendingIntentPlay:PendingIntent
    private var drawablePlay:Int = -1
    private var titlePlay = "Pause"


    override fun onBind(intent: Intent?): IBinder? {
         return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bundle = intent?.extras
        val name = bundle?.getString("nameAudio")
        Log.i("test123","${bundle?.getBoolean("isPlay")}")
        when(bundle?.getBoolean("isPlay")){
            true -> {
                drawablePlay = R.drawable.ic_baseline_pause_24
                val intentPause = Intent(ACTION_PAUSE)
                titlePlay = "Pause"
                pendingIntentPlay = PendingIntent.getBroadcast(applicationContext,4,intentPause,PendingIntent.FLAG_UPDATE_CURRENT)
            }
            false -> {
                drawablePlay = R.drawable.ic_baseline_play_arrow_24
                val intentPlay = Intent(ACTION_PLAY)
                titlePlay = "Play"
                pendingIntentPlay = PendingIntent.getBroadcast(applicationContext,3,intentPlay,PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }
        handingSendBroadcast()
        createNotification(name!!)
        return START_NOT_STICKY
    }



    private fun createNotification(name:String) {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.avatar_item)
        var notification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_audiotrack_24)
            .addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", pendingIntentPrevious) // #0
            .addAction(drawablePlay, titlePlay, pendingIntentPlay) // #1
            .addAction(R.drawable.ic_baseline_skip_next_24, "Next", pendingIntentNext) // #2
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setContentTitle("Your Audio")
            .setContentText(name)
            .setLargeIcon(bitmap)
            .build()
        startForeground(1,notification)
    }

    private fun handingSendBroadcast(){
        val intentNext = Intent(ACTION_NEXT)
        pendingIntentNext = PendingIntent.getBroadcast(applicationContext,1,intentNext,PendingIntent.FLAG_UPDATE_CURRENT)

        val intentPrevious = Intent(ACTION_PREVIOUS)
        pendingIntentPrevious = PendingIntent.getBroadcast(applicationContext,2,intentPrevious,PendingIntent.FLAG_UPDATE_CURRENT)

    }

    override fun onDestroy() {
        super.onDestroy()
            Log.i("test123","onDestroy")
        }
    }
