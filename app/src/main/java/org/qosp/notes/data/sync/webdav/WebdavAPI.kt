package org.qosp.notes.data.sync.webdav

import org.qosp.notes.data.sync.nextcloud.NextcloudConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.ResponseBody
import org.qosp.notes.data.sync.nextcloud.model.NextcloudCapabilities
import org.qosp.notes.data.sync.nextcloud.model.NextcloudNote
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
    /**
     * 获取所有笔记的API。
     * @param url 请求的完整URL。
     * @param auth 授权信息。
     * @return 笔记列表。
     */
    @GET
    suspend fun getNotesAPI(
        @Url url: String,
        @Header("Authorization") auth: String,
    ): List<NextcloudNote>

    /**
     * 获取指定笔记的API。
     * @param url 请求的完整URL。
     * @param auth 授权信息。
     * @return 指定的笔记。
     */
    @GET
    suspend fun getNoteAPI(
        @Url url: String,
        @Header("Authorization") auth: String,
    ): NextcloudNote

    /**
     * 创建新笔记的API。
     * @param note 要创建的笔记内容。
     * @param url 请求的完整URL。
     * @param auth 授权信息。
     * @return 创建的笔记。
     */
    @POST
    suspend fun createNoteAPI(
        @Body note: NextcloudNote,
        @Url url: String,
        @Header("Authorization") auth: String,
    ): NextcloudNote

    /**
     * 更新指定笔记的API。
     * @param note 要更新的笔记内容。
     * @param etag 该笔记的Etag，用于实现乐观并发控制。
     * @param url 请求的完整URL。
     * @param auth 授权信息。
     * @return 更新后的笔记。
     */
    @PUT
    suspend fun updateNoteAPI(
        @Body note: NextcloudNote,
        @Url url: String,
        @Header("If-Match") etag: String,
        @Header("Authorization") auth: String,
    ): NextcloudNote

    /**
     * 删除指定笔记的API。
     * @param url 请求的完整URL。
     * @param auth 授权信息。
     */
    @DELETE
    suspend fun deleteNoteAPI(
        @Url url: String,
        @Header("Authorization") auth: String,
    )

    /**
     * 获取所有能力的API，用于了解服务器支持的功能。
     * @param url 请求的完整URL。
     * @param auth 授权信息。
     * @return 服务器能力的响应体。
     */
    @Headers(
        "OCS-APIRequest: true",
        "Accept: application/json"
    )
    @GET
    suspend fun getAllCapabilitiesAPI(
        @Url url: String,
        @Header("Authorization") auth: String,
    ): ResponseBody
}

/**
 * 通过 API 获取 Nextcloud 笔记应用的能力信息。
 * @param config 包含Nextcloud的配置信息。
 * @return Nextcloud笔记应用的能力信息。
 */
//TODO 这里不需要，删除
suspend fun WebdavAPI.getNotesCapabilities(config: NextcloudConfig): NextcloudCapabilities? {
    // 构建获取能力信息的完整URL
    val endpoint = "ocs/v2.php/cloud/capabilities"
    val fullUrl = config.remoteAddress + endpoint

    // 发送请求并解析响应
    val response = withContext(Dispatchers.IO) {
        getAllCapabilitiesAPI(url = fullUrl, auth = config.credentials).string()
    }

    // 从响应中提取笔记应用的能力信息
    val element = Json
        .parseToJsonElement(response).jsonObject["ocs"]?.jsonObject
        ?.get("data")?.jsonObject
        ?.get("capabilities")?.jsonObject
        ?.get("notes")
    return element?.let { Json.decodeFromJsonElement<NextcloudCapabilities>(it) }
}

/**
 * 通过API删除指定的笔记。
 * @param note 要删除的笔记。
 * @param config 包含Nextcloud的配置信息。
 */
suspend fun WebdavAPI.deleteNote(note: NextcloudNote, config: NextcloudConfig) {
    deleteNoteAPI(
        url = config.remoteAddress + baseURL + "notes/${note.id}",
        auth = config.credentials,
    )
}

/**
 * 通过API更新指定的笔记。
 * @param note 要更新的笔记。
 * @param etag 该笔记的Etag。
 * @param config 包含Nextcloud的配置信息。
 * @return 更新后的笔记。
 */
suspend fun WebdavAPI.updateNote(note: NextcloudNote, etag: String, config: NextcloudConfig): NextcloudNote {
    return updateNoteAPI(
        note = note,
        url = config.remoteAddress + baseURL + "notes/${note.id}",
        etag = "\"$etag\"",
        auth = config.credentials,
    )
}

/**
 * 通过API创建新笔记。
 * @param note 要创建的笔记。
 * @param config 包含Nextcloud的配置信息。
 * @return 创建的笔记。
 */
suspend fun WebdavAPI.createNote(note: NextcloudNote, config: NextcloudConfig): NextcloudNote {
    return createNoteAPI(
        note = note,
        url = config.remoteAddress + baseURL + "notes",
        auth = config.credentials,
    )
}

/**
 * 通过API获取所有笔记。
 * @param config 包含Nextcloud的配置信息。
 * @return 笔记列表。
 */
suspend fun WebdavAPI.getNotes(config: NextcloudConfig): List<NextcloudNote> {
    return getNotesAPI(
        url = config.remoteAddress + baseURL + "notes",
        auth = config.credentials,
    )
}

/**
 * 测试配置的凭证是否有效。
 * @param config 包含Nextcloud的配置信息。
 */
suspend fun WebdavAPI.testCredentials(config: NextcloudConfig) {
    getNotesAPI(
        url = config.remoteAddress + baseURL + "notes",
        auth = config.credentials,
    )
}
