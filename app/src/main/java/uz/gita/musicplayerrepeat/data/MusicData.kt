package uz.gita.musicplayerrepeat.data

data class MusicData(
    val id: Int,
    val artist: String,
    val title: String,
    val data: String,
    val duration: Long,
    val image: String?
)
