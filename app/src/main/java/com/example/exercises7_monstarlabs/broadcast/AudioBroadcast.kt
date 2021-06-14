package com.example.exercises7_monstarlabs.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_NEXT
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PAUSE
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PLAY
import com.example.exercises7_monstarlabs.helper.Utils.ACTION_PREVIOUS
import com.example.exercises7_monstarlabs.helper.Utils.MY_PATH

class AudioBroadcast:BroadcastReceiver() {


    private var mediaPlayer: MediaPlayer? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val path = intent?.getStringExtra(MY_PATH)
        val uri: Uri? = path?.toUri()
        Log.i("test123","${intent?.action}")
        when(intent?.action){
            ACTION_PLAY -> {

            }

            ACTION_PAUSE -> {
            }
        }

    }


}