package com.example.lab7_monstarlabs.model

import android.net.Uri

data class MyAudio(
    var idAudio:Long,
    var titleAudio:String,
    var uriAudio:Uri,
    var size:Int,
)