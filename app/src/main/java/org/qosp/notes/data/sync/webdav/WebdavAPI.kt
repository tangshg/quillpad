package org.qosp.notes.data.sync.webdav

import com.google.android.exoplayer2.util.Log
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.qosp.notes.data.sync.webdav.model.WebdavNote

/**
 * 定义与 webdav 笔记应用API交互的接口。
 * 共有5个 API
 *
 * 1. getNotesAPI
 * 2. getNoteAPI
 * 3. createNoteAPI
 * 4. updateNoteAPI
 * 5. deleteNoteAPI
 */
interface WebdavAPI {

    //获取所有的笔记列表
    suspend fun getNotesAPI(
        url: String,
        sardine: Sardine
    ): List<WebdavNote>

    //获取指定的笔记
    suspend fun getNoteAPI(
        url: String,
        sardine: Sardine
    ): WebdavNote


    //创建一个笔记
    suspend fun createNoteAPI(
        note: WebdavNote,
        url: String,
        sardine: Sardine
    ): WebdavNote

    //更新一个笔记
    suspend fun updateNoteAPI(
        note: WebdavNote,
        url: String,
        etag: String,
        sardine: Sardine
    ): WebdavNote

    //删除一个笔记
    suspend fun deleteNoteAPI(
        url: String,
        sardine: Sardine
    )


}
