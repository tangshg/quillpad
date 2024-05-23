package org.qosp.notes.data.sync.webdav.model

import org.qosp.notes.data.model.Note

/**
 * 将 Note 对象转换为 WebdavNote 对象。
 * 该函数用于将本地笔记数据格式转换为用于WebDAV服务的笔记格式。
 *
 * @param id  笔记的唯一标识符。
 * @param category  笔记所属的类别。
 * @return 转换后的 WebdavNote 对象。
 */
fun Note.asWebdavNote(id: Long, category: String): WebdavNote = WebdavNote(
    id = id,
    title = title,
    content = if (isList) taskListToMd() else content, // 如果笔记是任务列表，则将其转换为Markdown格式
    category = category,
    favorite = isPinned, // 将笔记的置顶状态转换为收藏状态
    modified = modifiedDate
)

/**
 * 将 WebdavNote 对象转换为新的 Note 对象。
 * 该函数用于将从WebDAV服务获取的笔记数据转换为本地笔记格式。
 *
 * @param id  笔记的唯一标识符。
 * @param notebookId  笔记所属笔记本的标识符，可能为空。
 * @return 转换后的 Note 对象。
 */
fun WebdavNote.asNewLocalNote(id: Long, notebookId: Long?) = Note(
    id = id,
    title = title,
    content = content,
    isPinned = favorite, // 将WebDAV笔记的收藏状态转换为本地笔记的置顶状态
    modifiedDate = modified,
    notebookId = notebookId
)
