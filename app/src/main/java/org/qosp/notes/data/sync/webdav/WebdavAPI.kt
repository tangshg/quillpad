package org.qosp.notes.data.sync.webdav

import org.qosp.notes.data.sync.nextcloud.NextcloudConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.ResponseBody
import org.qosp.notes.data.sync.nextcloud.NextcloudAPI
import org.qosp.notes.data.sync.nextcloud.model.NextcloudCapabilities
import org.qosp.notes.data.sync.nextcloud.model.NextcloudNote
import org.qosp.notes.data.sync.webdav.model.WebdavNote
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Url

const val baseURL = "index.php/apps/notes/api/v1/"

/**
 * 定义与Nextcloud笔记应用API交互的接口。
 */
interface WebdavAPI {

    //获取所有的笔记列表
    suspend fun getNotesAPI(
        url: String,
    ): List<WebdavNote>

    //获取指定的笔记
    suspend fun getNoteAPI(
        url: String,
    ): WebdavNote

    //创建一个笔记
    suspend fun createNoteAPI(
        note: WebdavNote,
        url: String,
    ): WebdavNote

    //更新一个笔记
    suspend fun updateNoteAPI(
        note: WebdavNote,
        url: String,
        etag: String
    ): WebdavNote

    //删除一个笔记
    suspend fun deleteNoteAPI(
        url: String,
    )


}//上面是定义的接口

/**
 * 这段代码使用了Kotlin的特性：
 * 挂起函数 (suspend 关键字)：suspend 关键字表明这个函数是协程中的挂起函数，它可以在执行过程中暂停，
 * 并在稍后恢复而不会阻塞线程。挂起函数只能在协程上下文中被调用。
 * 函数签名：getNotes 是函数名，它接受一个 WebdavConfig 类型的参数并返回一个 List<WebdavNote>。
 * 调用其他函数：函数体中调用了 getNotesAPI 函数，传入了两个参数：
 * url 参数是通过组合 config.remoteAddress、baseURL 和 "notes" 字符串得到的。
 * auth 参数是 config.credentials，看起来是用来进行身份验证的信息。
 * 类型安全：由于 getNotesAPI 返回 List<WebdavNote>，整个挂起函数也返回同样的类型，这保证了类型一致性。
 * 这个函数的功能是在给定的 WebDAV 配置下获取 Webdav 笔记，并返回一个笔记列表。
 * 具体实现细节（如HTTP请求、网络操作等）则隐藏在 getNotesAPI 函数内部。
**/
suspend fun WebdavAPI.getNotes(config: WebdavConfig): List<WebdavNote> {
    return getNotesAPI(
        // 构建获取笔记列表的完整URL
        //TODO webdav 不需要添加 baseURL
        url = config.remoteAddress + baseURL + "notes",
        //Webdav 不需要认证信息
        //auth = config.credentials,
    )
}

//获取笔记
suspend fun WebdavAPI.getNote(config: WebdavConfig, noteId: Long): WebdavNote {
    return getNoteAPI(
        url = config.remoteAddress + baseURL + "notes/$noteId",
        //Webdav 不需要认证信息
        //auth = config.credentials,
    )
}

//删除笔记文件
suspend fun WebdavAPI.deleteNote(note: WebdavNote, config: WebdavConfig) {
    deleteNoteAPI(
        url = config.remoteAddress + baseURL + "notes/${note.id}",
        //auth = config.credentials,
    )
}

//升级笔记
suspend fun WebdavAPI.updateNote(note: WebdavNote, etag: String, config: WebdavConfig): WebdavNote {
    return updateNoteAPI(
        note = note,
        url = config.remoteAddress + org.qosp.notes.data.sync.nextcloud.baseURL + "notes/${note.id}",
        etag = "\"$etag\"",
        //auth = config.credentials,
    )
}

//创建新笔记
suspend fun WebdavAPI.createNote(note: WebdavNote, config: WebdavConfig): WebdavNote {

    return createNoteAPI (
        note = note,
        url = config.remoteAddress + baseURL + "notes",
        //auth = config.credentials,
    )
}

//test Credentials 中文翻译 测试证书
suspend fun WebdavAPI.testCredentials(config: WebdavConfig) {
    getNotesAPI(
        url = config.remoteAddress + org.qosp.notes.data.sync.nextcloud.baseURL + "notes",
        //auth = config.credentials,
    )
}


