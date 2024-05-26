package org.qosp.notes.data.sync.webdav

import com.google.android.exoplayer2.util.Log
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.qosp.notes.data.sync.webdav.model.WebdavNote

/**
 * 定义与 webdav 笔记应用API交互的接口。
 * 如果有继承这个接口，需要实现这个接口的全部方法+
 * 一共是五个接口，分别是
 * getNotesAPI
 * getNoteAPI
 * createNoteAPI
 * updateNoteAPI
 * deleteNoteAPI
 */
interface WebdavAPI {

    //获取所有的笔记列表
    //笔记列表不需要做任何转换，只需要读到列表就好
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


}//上面是定义的接口

//test Credentials 中文翻译 测试证书
//定义的一个函数，函数名为 testCredentials，接收一个参数 config，返回值为 Unit。
suspend fun WebdavAPI.testCredentials(config: WebdavConfig) {

}


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
        url = config.remoteAddress  + "notes",
        sardine = config.sardine
    )
}

//获取笔记
suspend fun WebdavAPI.getNote(config: WebdavConfig, noteId: Long): WebdavNote{
    return getNoteAPI(
        url = config.remoteAddress  + "notes/$noteId",
        sardine = config.sardine
    )
}

//删除笔记文件
suspend fun WebdavAPI.deleteNote(note: WebdavNote, config: WebdavConfig) {
    deleteNoteAPI(
        url = config.remoteAddress  + "notes/${note.id}",
        sardine = config.sardine
    )
}

//升级笔记
suspend fun WebdavAPI.updateNote(note: WebdavNote, etag: String, config: WebdavConfig): WebdavNote {
    return updateNoteAPI(
        note = note,
        url = config.remoteAddress  + "notes/${note.id}",
        etag = "\"$etag\"",
        sardine = config.sardine
    )
}

//创建新笔记
suspend fun WebdavAPI.createNote(note: WebdavNote, config: WebdavConfig): WebdavNote {

    return createNoteAPI (
        note = note,
        url = config.remoteAddress  + "notes",
        sardine = config.sardine
    )
}



