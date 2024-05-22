package org.qosp.notes.data.sync.webdav.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 表示WebDAV功能的类，包含API版本和 WebDAV的版本信息。
 */
@Serializable
data class WebdavCapabilities(
    /**
     * API版本列表，序列化时字段名为"api_version"。
     */
    @SerialName("api_version")
    val apiVersion: List<String>,

    /**
     * WebDAV的版本字符串。
     */
    val version: String,
)
