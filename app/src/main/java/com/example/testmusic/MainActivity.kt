package com.example.testmusic

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnPlayPause: ImageButton
    private lateinit var vlSeekbar: SeekBar
    private lateinit var btnVLUP: Button
    private lateinit var btnVLDOWN: Button
    private lateinit var musicSeekbar: SeekBar
    private lateinit var musicService: MusicService
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            updateSeekBar()
            updatePlayPauseIcon()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        val vlSeekbar = findViewById<SeekBar>(R.id.vlSeekbar)
        val btnVLUP = findViewById<ImageButton>(R.id.btnVLUP)
        val btnVLDOWN = findViewById<ImageButton>(R.id.btnVLDOWN)

        btnPlayPause.setOnClickListener {
            if (isBound) musicService.playPauseMusic()
            updatePlayPauseIcon()
        }

        vlSeekbar.max = 100
        vlSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100f
                musicService?.setVolume(volume)
                Log.e("MainActivity", "onProgressChanged: Volume set to $volume")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnVLUP.setOnClickListener {
            val newVolume = (vlSeekbar.progress + 10).coerceAtMost(100)
            vlSeekbar.progress = newVolume
        }

        btnVLDOWN.setOnClickListener {
            val newVolume = (vlSeekbar.progress - 10).coerceAtLeast(0)
            vlSeekbar.progress = newVolume
        }

        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun updateSeekBar() {
        val musicSeekbar = findViewById<SeekBar>(R.id.musicSeekbar)
        musicSeekbar.max = musicService.getDuration()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isBound) {
                    musicSeekbar.progress = musicService.getCurrentPosition()
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }

    private fun updatePlayPauseIcon() {
        if (musicService.isPlaying()) { // Kiểm tra trạng thái của MediaPlayer
            val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
            btnPlayPause.setImageResource(R.drawable.ic_pause) // Sử dụng icon Pause
        } else {
            val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
            btnPlayPause.setImageResource(R.drawable.ic_play) // Sử dụng icon Play
        }
    }
}


