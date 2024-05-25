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

        val davResource = sardine.list(url).firstOrNull() ?: throw Exception("No resource found at $url")


        Log.i(tangshgTAG, "getNoteAPI: $davResource")

        // 获取标题和内容
        val title = davResource.displayName ?: throw Exception("No display name for the resource")
        val content = "Not available from DavResource, fetch it separately" // 假设需要通过其他方式获取内容
        val id = extractIdFromUrl(url)
        val etag = davResource.etag // 假设 DavResource 有一个 etag 属性
        val category = "defaultCategory" // 需要一个实际的逻辑来确定或获取分类
        val favorite = false // 默认值，可能需要从元数据中获取或设置
        val modified = davResource.modified?.time ?: 0L // 如果 lastModified 为 null，默认值为 0
        val readOnly = false // 由于 DavResource 类中没有 isReadOnly 方法，这里假设默认值

        return WebdavNote(id, etag, content, title, category, favorite, modified, readOnly)
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

        return TODO()
    }
}
