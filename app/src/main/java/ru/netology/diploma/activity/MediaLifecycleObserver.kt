package ru.netology.diploma.activity

import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class MediaLifecycleObserver : LifecycleEventObserver {

    var player: MediaPlayer? = MediaPlayer()

    fun play(url: String) {
        player?.setDataSource(url)
        player?.setOnPreparedListener {
            it.start()
        }
        player?.prepareAsync()
    }

    fun stop() {
        player?.stop()
        player?.reset()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> player?.pause()
            Lifecycle.Event.ON_STOP -> {
                player?.release()
                player = null
            }

            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            else -> Unit
        }
    }
}