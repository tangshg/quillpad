package org.qosp.notes.data.sync.webdav

import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.serialization.json.Json
import org.qosp.notes.data.sync.webdav.WebdavAPI
import org.qosp.notes.data.sync.webdav.model.WebdavNote
import java.io.IOException
import kotlinx.serialization.decodeFromString

class WebdavAPIImpl(private val sardine: Sardine) : WebdavAPI {

    private val tangshgTAG = "tangshgWebdavAPIImpl"

    // 实现 WebdavAPI 的所有方法
    override suspend fun getNoteAPI(url: String, sardine: Sardine): WebdavNote {
        val webdavNote =WebdavNote(
            1,
            "etg",
            "content",
            "title",
            "category",
            false,
            11,
            false,)

        return webdavNote
    }

    private fun extractIdFromUrl(url: String): Long {
        // 这里需要实现一个函数来从URL中提取ID
        // 这仅仅是一个示例，实际实现取决于URL的格式
        val pathSegments = url.split("/").dropWhile { it.isEmpty() }
        return pathSegments.last().toLong()
    }

    override suspend fun createNoteAPI(note: WebdavNote, url: String, sardine: Sardine): WebdavNote {
        // 使用 Sardine 创建 WebdavNote
        return TODO("提供返回值")

    }

    override suspend fun updateNoteAPI(note: WebdavNote, url: String, etag: String, sardine: Sardine): WebdavNote {
        // 使用 Sardine 更新 WebdavNote
        return TODO("提供返回值")
    }

    override suspend fun deleteNoteAPI(url: String, sardine: Sardine) {
        // 使用 Sardine 删除 WebdavNote
        try {
            sardine.delete(url)//没有返回值
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override suspend fun getNotesAPI(url: String, sardine: Sardine): List<WebdavNote> {
        // 使用 Sardine 获取所有 WebdavNote 并解析

        val webdavNote =WebdavNote(
            1,
            "etg",
            "content",
            "title",
            "category",
            false,
            11,
            false,)

        val list = listOf(webdavNote)

        return list
    }
}
