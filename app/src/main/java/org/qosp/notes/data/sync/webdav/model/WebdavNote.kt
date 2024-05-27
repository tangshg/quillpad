package org.qosp.notes.data.sync.webdav.model

import kotlinx.serialization.Serializable

@Serializable
data class WebdavNote(
    val id: Long,//ID
    val etag: String? = null,
    val content: String,//内容
    val title: String,//标题
    val category: String,//分类
    val favorite: Boolean,//收藏
    val modified: Long,//修改
    val readOnly: Boolean? = null,
)
