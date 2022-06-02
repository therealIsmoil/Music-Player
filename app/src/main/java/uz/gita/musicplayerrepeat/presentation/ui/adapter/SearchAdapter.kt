package uz.gita.musicplayerrepeat.presentation.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.gita.musicplayerrepeat.R
import uz.gita.musicplayerrepeat.data.MusicData
import uz.gita.musicplayerrepeat.databinding.ItemMusicBinding

class SearchAdapter() : ListAdapter<MusicData, SearchAdapter.SearchViewHolder>(SearchDiffUtil) {
    private var onClickItemListener: ((Int) -> Unit)? = null
//    var cursor: Cursor? = null
//    var query: String? = null

    object SearchDiffUtil : DiffUtil.ItemCallback<MusicData>() {
        override fun areItemsTheSame(oldItem: MusicData, newItem: MusicData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicData, newItem: MusicData): Boolean {
            return oldItem == newItem
        }
    }

    inner class SearchViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                onClickItemListener?.invoke(getItem(absoluteAdapterPosition).id)
            }
        }

        fun bind() {
            binding.musicName.text = getItem(absoluteAdapterPosition).title
            binding.musicArtist.text = getItem(absoluteAdapterPosition).artist

//                it.image?.let { image ->
//                    Glide.with(binding.root).load(getAlbumImage(image))
//                        .placeholder(R.drawable.ic_music_disk).into(binding.imageMusic)
//                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return SearchViewHolder(ItemMusicBinding.bind(view))
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind()
    }


    fun setOnClickItemListener(block: (Int) -> Unit) {
        onClickItemListener = block
    }

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
}