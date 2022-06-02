package uz.gita.musicplayerrepeat.presentation.ui.screen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.data.ActionEnum
import uz.gita.musicplayerrepeat.data.MusicData
import uz.gita.musicplayerrepeat.databinding.ScreenMusicListBinding
import uz.gita.musicplayerrepeat.presentation.service.MyMusicService
import uz.gita.musicplayerrepeat.presentation.ui.adapter.MusicListCursorAdapter
import uz.gita.musicplayerrepeat.utils.MyAppManager

class MusicListScreen : Fragment(R.layout.screen_music_list) {
    private val binding by viewBinding(ScreenMusicListBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MusicListCursorAdapter()
        binding.musicList.layoutManager = LinearLayoutManager(requireContext())
        adapter.cursor = MyAppManager.cursor
        binding.musicList.adapter = adapter

        adapter.setSelectMusicItemListener {
            MyAppManager.selectPosition = it
            MyAppManager.repeatPos = it
            MyAppManager.currentTime = 0
            startMyMusicService(ActionEnum.PLAY)
        }
        binding.search.setOnClickListener {
            findNavController().navigate(R.id.action_musicListScreen_to_searchScreen)
        }
        binding.manage.setOnClickListener {
            startMyMusicService(ActionEnum.MANAGE)
        }
        binding.next.setOnClickListener {
            startMyMusicService(ActionEnum.NEXT)
        }
        binding.prev.setOnClickListener {
            startMyMusicService(ActionEnum.PREV)
        }
        binding.bottomPart.setOnClickListener {
            findNavController().navigate(R.id.action_musicListScreen_to_songScreen)
        }

        MyAppManager.isPlayingLiveData.observe(viewLifecycleOwner, isPlayingObserver)
        MyAppManager.playMusicLiveData.observe(viewLifecycleOwner, playMusicObserver)
        MyAppManager.emptyLiveData.observe(viewLifecycleOwner, emptyObserver)
        MyAppManager.notEmptyLiveData.observe(viewLifecycleOwner, notEmptyObserver)
    }

    private val isPlayingObserver = Observer<Boolean> {
        if (it) {
            binding.manage.setImageResource(R.drawable.ic_pause_max)
        } else {
            binding.manage.setImageResource(R.drawable.ic_play_max)
        }
    }
    private val playMusicObserver = Observer<MusicData> {
        binding.bottomMusicName.text = it.title
        binding.bottomMusicArtist.text = it.artist
        it.image?.let { image ->
            Glide.with(binding.root).load(getAlbumImage(image))
                .placeholder(R.drawable.logo).into(binding.image)
        }
    }

    private fun startMyMusicService(command: ActionEnum) {
        val intent = Intent(requireContext(), MyMusicService::class.java)
        intent.putExtra("COMMAND", command)
        if (Build.VERSION.SDK_INT >= 26) {
            requireActivity().startForegroundService(intent)
        } else requireActivity().startService(intent)
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

    private val emptyObserver = Observer<Unit> {
        binding.manage.isEnabled = false
        binding.prev.isEnabled = false
        binding.next.isEnabled = false
    }
    private val notEmptyObserver = Observer<Unit> {
        binding.manage.isEnabled = true
        binding.prev.isEnabled = true
        binding.next.isEnabled = true
    }
}