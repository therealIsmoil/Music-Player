package uz.gita.musicplayerrepeat.presentation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import uz.gita.musicplayerrepeat.BuildConfig
import uz.gita.musicplayerrepeat.MainActivity
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.data.ActionEnum
import uz.gita.musicplayerrepeat.utils.MyAppManager
import uz.gita.musicplayerrepeat.utils.getMusicDataByPosition
import java.io.File
import kotlin.random.Random

class MyMusicService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null
    private val CHANNEL_ID = "DEMO"
    private lateinit var mediaPlayer: MediaPlayer
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var job: Job? = null
    private var pendingIntent: PendingIntent? = null
//    private val intent by lazy {
//        Intent(this, MyMusicService::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//    }

    override fun onCreate() {
        mediaPlayer = MediaPlayer()
        createChannel()
        createForeGroundService()
//        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
    }

    private fun createForeGroundService() {
        val notifyIntent = Intent(this, MainActivity::class.java)
        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Music player")
            .setChannelId(CHANNEL_ID)
            .setContent(createRemoteView())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, "PLAYER", NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(null, null)
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
        }
    }

    private fun createRemoteView(): RemoteViews {
        val musicData = MyAppManager.cursor?.getMusicDataByPosition(MyAppManager.selectPosition)
        val view = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.remote_viewmain)
        view.setTextViewText(R.id.remoteMusicName, musicData?.artist)
        view.setTextViewText(R.id.remoteMusicArtist, musicData?.title)

        if (mediaPlayer.isPlaying) {
            view.setImageViewResource(R.id.remoteManage, R.drawable.ic_pause_max)
        } else {
            view.setImageViewResource(R.id.remoteManage, R.drawable.ic_play_max)
        }
        view.setOnClickPendingIntent(R.id.remotePrev, createPendingIntent(ActionEnum.PREV))
        view.setOnClickPendingIntent(R.id.remoteNext, createPendingIntent(ActionEnum.NEXT))
        view.setOnClickPendingIntent(R.id.remoteCancel, createPendingIntent(ActionEnum.CANCEL))
        view.setOnClickPendingIntent(R.id.remoteManage, createPendingIntent(ActionEnum.MANAGE))
        return view
    }

    private fun createPendingIntent(command: ActionEnum): PendingIntent {
        val intent = Intent(this, MyMusicService::class.java)
        val flag = (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        intent.putExtra("COMMAND", command)
        return PendingIntent.getService(
            this,
            command.pos,
            intent,
            flag
        )
    }

    private fun doneCommand(command: ActionEnum) {
        val musicData = MyAppManager.cursor?.getMusicDataByPosition(MyAppManager.selectPosition)
        when (command) {
            ActionEnum.MANAGE -> {
                if (mediaPlayer.isPlaying) {
                    doneCommand(ActionEnum.PAUSE)
                } else {
                    doneCommand(ActionEnum.PLAY)
                }
            }
            ActionEnum.PLAY -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.reset()
                }
                musicData?.let {
                    MyAppManager.fullTime = (it.duration / 1000).toInt()
                }
                MyAppManager.isPlayingLiveData.value = true
                MyAppManager.playMusicLiveData.value = musicData
                mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(musicData?.data.toString())))
                mediaPlayer.start()
                mediaPlayer.seekTo(MyAppManager.currentTime * 1000)
                job?.cancel()
                job = scope.launch {
                    changeSeekbar().collectLatest {
                        MyAppManager.currentTimeLiveData.postValue(it)
                        MyAppManager.currentTime = it
                    }
                }
                mediaPlayer.setOnCompletionListener {
                    doneCommand(ActionEnum.NEXT)
                    createForeGroundService()
                }
                createForeGroundService()
            }
            ActionEnum.PAUSE -> {
                job?.cancel()
                MyAppManager.isPlayingLiveData.value = false
                mediaPlayer.reset()
            }
            ActionEnum.NEXT -> {
                MyAppManager.currentTime = 0
                when {
                    MyAppManager.isRepeat -> {
                        MyAppManager.selectPosition = MyAppManager.repeatPos
                    }
                    MyAppManager.isShuffle -> {
                        MyAppManager.selectPosition =
                            Random.nextInt(0, MyAppManager.cursor?.count!!)
                    }
                    (MyAppManager.selectPosition + 1) == MyAppManager.cursor?.count -> MyAppManager.selectPosition =
                        0
                    else -> MyAppManager.selectPosition++
                }
                if (!MyAppManager.isRepeat) {
                    MyAppManager.repeatPos = MyAppManager.selectPosition
                }
                doneCommand(ActionEnum.PLAY)
            }
            ActionEnum.PREV -> {
                MyAppManager.currentTime = 0
                when {
                    MyAppManager.isRepeat -> {
                        MyAppManager.selectPosition = MyAppManager.repeatPos
                    }
                    MyAppManager.isShuffle -> {
                        MyAppManager.selectPosition =
                            Random.nextInt(0, MyAppManager.cursor?.count!!)
                    }
                    MyAppManager.selectPosition == 0 -> MyAppManager.selectPosition =
                        MyAppManager.cursor?.count?.minus(1) ?: 0
                    else -> MyAppManager.selectPosition--
                }
                if (!MyAppManager.isRepeat) {
                    MyAppManager.repeatPos = MyAppManager.selectPosition
                }
                doneCommand(ActionEnum.PLAY)
            }
            ActionEnum.CANCEL -> {
                MyAppManager.isPlayingLiveData.value = false
                MyAppManager.currentTime = 0
                mediaPlayer.reset()
                stopSelf()
            }
            ActionEnum.SEEKBAR -> {
                if (mediaPlayer.isPlaying)
                    mediaPlayer.seekTo((MyAppManager.currentTime * 1000))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.extras?.getSerializable("COMMAND") as ActionEnum
        doneCommand(command)
        createForeGroundService()
        return START_NOT_STICKY
    }

    private fun changeSeekbar(): Flow<Int> = flow {
        for (i in MyAppManager.currentTime until MyAppManager.fullTime) {
            emit(MyAppManager.currentTime)
            MyAppManager.currentTime++
            delay(1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}