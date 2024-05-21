package org.qosp.notes.data.sync.webdav.model

import kotlinx.serialization.Serializable

@Serializable
data class WebdavNote(
    val id: Long,
    val etag: String? = null,
    val content: String,
    val title: String,
    val category: String,
    val favorite: Boolean,
    val modified: Long,
    val readOnly: Boolean? = null,
)
