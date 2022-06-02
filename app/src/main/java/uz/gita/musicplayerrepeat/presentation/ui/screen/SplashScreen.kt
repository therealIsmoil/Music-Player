package uz.gita.musicplayerrepeat.presentation.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.utils.MyAppManager
import uz.gita.musicplayerrepeat.utils.checkPermissions
import uz.gita.musicplayerrepeat.utils.getMusicCursor

@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment(R.layout.screen_splash) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.black)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().checkPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requireContext().getMusicCursor().onEach { cursor ->
                if (cursor.count == 0) {
                    MyAppManager.emptyLiveData.value = Unit
                } else {
                    MyAppManager.notEmptyLiveData.value = Unit
                }
                MyAppManager.cursor = cursor
                delay(1000)
                MyAppManager.openMusicListScreenLiveData.value = Unit
            }.launchIn(lifecycleScope)
        }
        MyAppManager.openMusicListScreenLiveData.observe(
            viewLifecycleOwner,
            openMusicListScreenObserver
        )
    }

    private val openMusicListScreenObserver = Observer<Unit> {
        findNavController().navigate(R.id.action_splashScreen_to_musicListScreen)
    }
}