package uz.gita.musicplayerrepeat.utils

import android.database.Cursor
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import uz.gita.musicplayerrepeat.data.MusicData

object MyAppManager {
    var cursor: Cursor? = null
    var selectPosition: Int = 0
    var repeatPos: Int = 0

    val isPlayingLiveData = MutableLiveData<Boolean>()
    val playMusicLiveData = MutableLiveData<MusicData>()

    val emptyLiveData = MutableLiveData<Unit>()
    val notEmptyLiveData = MutableLiveData<Unit>()

    var isShuffle = false
    var isRepeat = false

    val openMusicListScreenLiveData = MutableLiveData<Unit>()

    val currentTimeLiveData = MutableLiveData<Int>()
    var currentTime: Int = 0
    var fullTime: Int = 0
}