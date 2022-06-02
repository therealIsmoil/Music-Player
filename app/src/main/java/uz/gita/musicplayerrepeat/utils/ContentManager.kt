package uz.gita.musicplayerrepeat.utils

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uz.gita.musicplayerrepeat.data.MusicData

private val projection = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.DATA,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.DATA
)

fun Context.getMusicCursor(): Flow<Cursor> = flow {
    val sortOrder: String = MediaStore.Audio.Media.TITLE + " ASC"
    val cursor: Cursor = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        MediaStore.Audio.Media.IS_MUSIC + "!=0 and ${MediaStore.Audio.Media.DURATION} > 200",
        null,
        sortOrder
    ) ?: return@flow

    emit(cursor)
}.flowOn(Dispatchers.IO)

fun Cursor.getMusicDataByPosition(pos: Int): MusicData {
    this.moveToPosition(pos)
    return MusicData(
        pos,
        this.getString(1),
        this.getString(2),
        this.getString(3),
        this.getLong(4),
        this.getString(5)
    )
}