package org.qosp.notes.data.sync.webdav

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import org.qosp.notes.data.sync.webdav.WebdavAPI
import org.qosp.notes.data.sync.webdav.model.WebdavNote
import java.io.IOException

class WebdavAPIImpl(private val sardine: Sardine) : WebdavAPI {

    // 实现 WebdavAPI 的所有方法
    override suspend fun getNoteAPI(url: String): WebdavNote {
        // 使用 Sardine 获取 WebdavNote 并解析

        val resources = sardine.list(url)

        return  TODO()
    }

    override suspend fun createNoteAPI(note: WebdavNote, url: String): WebdavNote {
        // 使用 Sardine 创建 WebdavNote
        return TODO("提供返回值")

    }

    override suspend fun updateNoteAPI(note: WebdavNote, url: String, etag: String): WebdavNote {
        // 使用 Sardine 更新 WebdavNote
        return TODO("提供返回值")
    }

    override suspend fun deleteNoteAPI(url: String) {
        // 使用 Sardine 删除 WebdavNote
        try {
            sardine.delete(url)//没有返回值
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override suspend fun getNotesAPI(url: String, sardine: Sardine): List<WebdavNote> {
        // 使用 Sardine 获取所有 WebdavNote 并解析
        return TODO("提供返回值")
    }
}
