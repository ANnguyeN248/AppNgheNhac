package com.example.testmusic


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private val binder = MusicBinder()
    private lateinit var mediaPlayer: MediaPlayer
    private val handler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                showNotification()
                handler.postDelayed(this, 1000) // Cập nhật mỗi giây
            }
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.traidatommatroi)
        mediaPlayer.isLooping = true

        handler.post(updateProgressRunnable)
    }

    fun playPauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            Log.d("MusicService", "playPauseMusic: Music paused")
        } else {
            mediaPlayer.start()
        }
        showNotification()
    }

    fun setVolume(volume: Float) {
        mediaPlayer.setVolume(volume, volume)
        Log.d("MusicService", "setVolume: Volume set to $volume")
    }

    private var volumeLevel = 0.5f

    fun increaseVolume() {
        if (volumeLevel < 1.0f) {
            volumeLevel += 0.1f
            if (volumeLevel > 1.0f) volumeLevel = 1.0f
            mediaPlayer.setVolume(volumeLevel, volumeLevel)
            showNotification() // Cập nhật lại notification
        }
    }

    fun decreaseVolume() {
        if (volumeLevel > 0.0f) {
            volumeLevel -= 0.1f
            if (volumeLevel < 0.0f) volumeLevel = 0.0f
            mediaPlayer.setVolume(volumeLevel, volumeLevel)
            showNotification() // Cập nhật lại notification
        }
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer.currentPosition
    }

    fun getDuration(): Int {
        return mediaPlayer.duration
    }

    private fun showNotification() {
        val remoteViews = RemoteViews(packageName, R.layout.custom_notification)

        // Gán hành động cho các nút
        remoteViews.setOnClickPendingIntent(
            R.id.btn_play_pause,
            getPendingIntent("ACTION_PLAY_PAUSE")
        )
        remoteViews.setOnClickPendingIntent(
            R.id.btn_volume_up,
            getPendingIntent("ACTION_VOLUME_UP")
        )
        remoteViews.setOnClickPendingIntent(
            R.id.btn_volume_down,
            getPendingIntent("ACTION_VOLUME_DOWN")
        )

        // Cập nhật icon Play/Pause tùy theo trạng thái
        if (mediaPlayer.isPlaying) {
            remoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_pause)
        } else {
            remoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play)
        }

        val duration = mediaPlayer.duration
        val currentPosition = mediaPlayer.currentPosition
        val progress = if (duration > 0) (currentPosition * 100 / duration) else 0
        remoteViews.setProgressBar(R.id.progress_timeline, 100, progress, false)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Music Player")
            .setContentText("Playing music")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Sử dụng Custom Style
            .setCustomContentView(remoteViews) // Đặt RemoteViews cho notification
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY_PAUSE" -> {
                playPauseMusic()
                showNotification()
            }
            "ACTION_VOLUME_UP" -> increaseVolume()
            "ACTION_VOLUME_DOWN" -> decreaseVolume()
        }
        Log.e("MusicService", "onStartCommand: " + intent?.action)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        mediaPlayer.release()
        Log.e("MusicService", "onDestroy: MediaPlayer released")
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying

    }

    companion object {
        private const val CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 1
    }
}
