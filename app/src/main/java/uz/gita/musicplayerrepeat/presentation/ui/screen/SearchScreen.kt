package uz.gita.musicplayerrepeat.presentation.ui.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.data.ActionEnum
import uz.gita.musicplayerrepeat.data.MusicData
import uz.gita.musicplayerrepeat.databinding.ScreenSearchBinding
import uz.gita.musicplayerrepeat.presentation.service.MyMusicService
import uz.gita.musicplayerrepeat.presentation.ui.adapter.SearchAdapter
import uz.gita.musicplayerrepeat.utils.MyAppManager
import uz.gita.musicplayerrepeat.utils.collapseKeyboard
import uz.gita.musicplayerrepeat.utils.getMusicCursor
import uz.gita.musicplayerrepeat.utils.getMusicDataByPosition


class SearchScreen : Fragment(R.layout.screen_search) {
    private val binding by viewBinding(ScreenSearchBinding::bind)
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private var job: Job? = null
    private val list = ArrayList<MusicData>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SearchAdapter()
        binding.searchRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecycler.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty()) {
                    adapter.submitList(emptyList())
                    return true
                }
                handler.removeCallbacksAndMessages(null)
                job?.cancel()
                job = requireContext().getMusicCursor().onEach { cursor ->
                    list.clear()
                    for (i in 0 until cursor.count) {
                        cursor.moveToPosition(i)
                        if (cursor.getString(2).lowercase()
                                .contains(query.lowercase()) || cursor.getString(1).lowercase()
                                .contains(query.lowercase())
                        ) {
                            list.add(cursor.getMusicDataByPosition(i))
                        }
                    }
                    adapter.submitList(list)
//                    adapter.notifyDataSetChanged()
                }.launchIn(lifecycleScope)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) return true

                handler.postDelayed({
                    job?.cancel()
                    job = requireContext().getMusicCursor().onEach { cursor ->
                        list.clear()
                        for (i in 0 until cursor.count) {
                            cursor.moveToPosition(i)
                            if (cursor.getString(2).lowercase()
                                    .contains(newText.lowercase()) || cursor.getString(1)
                                    .lowercase()
                                    .contains(newText.lowercase())
                            ) {
                                list.add(cursor.getMusicDataByPosition(i))
                            }
                        }
                        adapter.submitList(list)
                        adapter.notifyDataSetChanged()
                    }.launchIn(lifecycleScope)
                }, 200)
                return true
            }
        })

        adapter.setOnClickItemListener {
            MyAppManager.currentTime = 0
            MyAppManager.selectPosition = it
            startMyMusicService(ActionEnum.PLAY)
        }

        val closeButton: ImageView =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setOnClickListener {
            binding.searchView.setQuery("", true)
            list.clear()
            adapter.submitList(list)
            collapseKeyboard()
            binding.searchView.isFocusable = false
            adapter.notifyDataSetChanged()
        }

        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun startMyMusicService(command: ActionEnum) {
        val intent = Intent(requireContext(), MyMusicService::class.java)
        intent.putExtra("COMMAND", command)
        if (Build.VERSION.SDK_INT >= 26) {
            requireActivity().startForegroundService(intent)
        } else requireActivity().startService(intent)
    }
}