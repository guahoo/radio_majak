package com.radio_majak

import android.R
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import java.io.IOException


class BackgroundSoundService : Service() {
    var player: MediaPlayer? = null
    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        player = MediaPlayer()
        try {
            //MainActivity.player.setDataSource(MEDIA_SOURCE_LINK_LINK)

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        player!!.prepareAsync()
        player!!.isLooping = true // Set looping
        player!!.setVolume(100f, 100f)

    }

   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        player!!.start()
        return Service.START_STICKY
    }

    override fun onStart(intent: Intent?, startId: Int) {
        // TO DO
    }

    fun onUnBind(arg0: Intent?): IBinder? {
        // TO DO Auto-generated method
        return null
    }

    fun onStop() {}
    fun onPause() {}
    override fun onDestroy() {
        player!!.stop()
        player!!.release()
    }

    override fun onLowMemory() {}

    companion object {
        private val TAG: String? = null
    }
}