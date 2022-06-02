package uz.gita.musicplayerrepeat.presentation.ui.adapter

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.databinding.ItemMusicBinding
import uz.gita.musicplayerrepeat.utils.getMusicDataByPosition

class MusicListCursorAdapter() : RecyclerView.Adapter<MusicListCursorAdapter.MyViewHolder>() {
    var cursor: Cursor? = null
    private var selectMusicItemListener: ((Int) -> Unit)? = null

    inner class MyViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                selectMusicItemListener?.invoke(absoluteAdapterPosition)
            }
        }

        fun bind() {
            val music = cursor?.getMusicDataByPosition(absoluteAdapterPosition)
            music?.let {
                binding.musicName.text = it.title
                binding.musicArtist.text = it.artist

//                it.image?.let { image ->
//                    Glide.with(binding.root).load(getAlbumImage(image))
//                        .placeholder(R.drawable.ic_music_disk).into(binding.imageMusic)
//                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return MyViewHolder(ItemMusicBinding.bind(view))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = cursor?.count ?: 0

    //getMusicImageFunction
    fun getAlbumImage(path: String): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)
        val data: ByteArray? = mmr.embeddedPicture
        return when {
            data != null -> BitmapFactory.decodeByteArray(data, 0, data.size)
            else -> null
        }
    }

    fun setSelectMusicItemListener(block: (Int) -> Unit) {
        selectMusicItemListener = block
    }
}