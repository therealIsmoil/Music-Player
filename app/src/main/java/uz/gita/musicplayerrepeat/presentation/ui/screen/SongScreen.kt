package uz.gita.musicplayerrepeat.presentation.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.data.ActionEnum
import uz.gita.musicplayerrepeat.data.MusicData
import uz.gita.musicplayerrepeat.databinding.ScreenSongBinding
import uz.gita.musicplayerrepeat.presentation.service.MyMusicService
import uz.gita.musicplayerrepeat.utils.MyAppManager

class SongScreen : Fragment(R.layout.screen_song) {
    private val binding by viewBinding(ScreenSongBinding::bind)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var boolNext = false
    private var boolPrev = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.color4)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MyAppManager.isPlayingLiveData.observe(viewLifecycleOwner, isPlayingObserver)
        MyAppManager.playMusicLiveData.observe(viewLifecycleOwner, playMusicObserver)
        MyAppManager.currentTimeLiveData.observe(viewLifecycleOwner, currentTimeObserver)
        MyAppManager.emptyLiveData.observe(viewLifecycleOwner, emptyObserver)
        MyAppManager.notEmptyLiveData.observe(viewLifecycleOwner, notEmptyObserver)

        if (MyAppManager.isShuffle) {
            binding.shuffle.setImageResource(R.drawable.ic_shuffter)
        } else {
            binding.shuffle.setImageResource(R.drawable.ic_shuffter_sel)
        }

        if (MyAppManager.isRepeat) {
            binding.repeat.setImageResource(R.drawable.ic_repeat_current)
        } else {
            binding.repeat.setImageResource(R.drawable.ic_auto_next_n_repeatqueue)
        }

        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    MyAppManager.currentTimeLiveData.value = progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                MyAppManager.currentTime = seekBar.progress
                startMyMusicService(ActionEnum.SEEKBAR)
            }
        })

        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.manageSongScreen.setOnClickListener {
            startMyMusicService(ActionEnum.MANAGE)
        }
        binding.nextSongScreen.setOnClickListener {
            if (!boolNext) {
                boolNext = true
                binding.seekBar.progress = 0
                startMyMusicService(ActionEnum.NEXT)

                scope.launch {
                    delay(300)
                    boolNext = false
                    cancel()
                }
            }
        }
        binding.prevSongScreen.setOnClickListener {
            if (!boolPrev) {
                boolPrev = true
                binding.seekBar.progress = 0
                startMyMusicService(ActionEnum.PREV)

                scope.launch {
                    delay(300)
                    boolPrev = false
                    cancel()
                }
            }
        }
        binding.shuffle.setOnClickListener {
            if (MyAppManager.isShuffle) {
                MyAppManager.isShuffle = false
                binding.shuffle.setImageResource(R.drawable.ic_shuffter_sel)
            } else {
                MyAppManager.isShuffle = true
                binding.shuffle.setImageResource(R.drawable.ic_shuffter)
            }
        }
        binding.repeat.setOnClickListener {
            if (MyAppManager.isRepeat) {
                MyAppManager.isRepeat = false
                binding.repeat.setImageResource(R.drawable.ic_auto_next_n_repeatqueue)
            } else {
                MyAppManager.isRepeat = true
                binding.repeat.setImageResource(R.drawable.ic_repeat_current)
            }
        }
        binding.currentTime.text = getTime(0)
        binding.totalTime.text = getTime(0)
    }

    private val isPlayingObserver = Observer<Boolean> {
        if (it) {
            binding.manageSongScreen.setImageResource(R.drawable.ic_pause_max)
        } else {
            binding.manageSongScreen.setImageResource(R.drawable.ic_play_max)
        }
    }

    @SuppressLint("SetTextI18n")
    private val playMusicObserver = Observer<MusicData> {
        binding.songArtistName.text = it.artist
        binding.songMusicName.text = it.title
        it.image?.let { image ->
            Glide.with(binding.root).load(getAlbumImage(image))
                .placeholder(R.drawable.logo).into(binding.songImage)
        }
        binding.seekBar.max = (it.duration / 1000).toInt()
        binding.totalTime.text = getTime((it.duration / 1000).toInt())
        binding.seekBar.progress = MyAppManager.currentTime
    }

    private fun startMyMusicService(command: ActionEnum) {
        val intent = Intent(requireContext(), MyMusicService::class.java)
        intent.putExtra("COMMAND", command)
        if (Build.VERSION.SDK_INT >= 26) {
            requireActivity().startForegroundService(intent)
        } else requireActivity().startService(intent)
    }

    private val currentTimeObserver = Observer<Int> {
        binding.seekBar.progress = it
        binding.currentTime.text = getTime(it)
    }

    private val emptyObserver = Observer<Unit> {
        binding.manageSongScreen.isEnabled = false
        binding.nextSongScreen.isEnabled = false
        binding.prevSongScreen.isEnabled = false
    }
    private val notEmptyObserver = Observer<Unit> {
        binding.manageSongScreen.isEnabled = true
        binding.nextSongScreen.isEnabled = true
        binding.prevSongScreen.isEnabled = true
    }

    private fun getAlbumImage(path: String): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)
        val data: ByteArray? = mmr.embeddedPicture
        return when {
            data != null -> BitmapFactory.decodeByteArray(data, 0, data.size)
            else -> null
        }
    }

    private fun getTime(time: Int): String {
        val hour = time / 3600
        val minute = (time % 3600) / 60
        val second = time % 60

        val hourText = if (hour > 0) {
            if (hour < 10) "0$hour:"
            else "$hour:"
        } else ""

        val minuteText = if (minute < 10) "0$minute:"
        else "$minute:"

        val secondText = if (second < 10) "0$second"
        else "$second"

        return "$hourText$minuteText$secondText"
    }
}