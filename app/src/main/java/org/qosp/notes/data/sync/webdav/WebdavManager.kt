package org.qosp.notes.data.sync.webdav

import kotlinx.coroutines.flow.first
import org.qosp.notes.data.model.IdMapping
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.Notebook
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NotebookRepository
import org.qosp.notes.data.sync.core.ApiError
import org.qosp.notes.data.sync.core.BaseResult
import org.qosp.notes.data.sync.core.GenericError
import org.qosp.notes.data.sync.core.InvalidConfig
import org.qosp.notes.data.sync.core.ProviderConfig
import org.qosp.notes.data.sync.core.ServerNotSupported
import org.qosp.notes.data.sync.core.ServerNotSupportedException
import org.qosp.notes.data.sync.core.Success
import org.qosp.notes.data.sync.core.SyncProvider
import org.qosp.notes.data.sync.core.Unauthorized
import org.qosp.notes.data.sync.webdav.model.WebdavNote
import org.qosp.notes.data.sync.webdav.model.asWebdavNote
import org.qosp.notes.preferences.CloudService
import retrofit2.HttpException

/**
 * WebdavManager 类负责与 webdav 服务器进行同步操作，包括创建、删除、更新笔记。
 *
 * @param webdavAPI 用于与 webdav 服务器通信的API客户端。
 * @param noteRepository 本地笔记数据的存储库。
 * @param notebookRepository 本地笔记本数据的存储库。
 * @param idMappingRepository 本地和远程笔记ID映射的存储库。
 */

//在哪里调用它？
class WebdavManager(
    private val webdavAPI:WebdavAPI, // Webdav API客户端，用于与服务器交互
    private val noteRepository: NoteRepository, // 笔记数据仓库
    private val notebookRepository: NotebookRepository, // 笔记本数据仓库
    private val idMappingRepository: IdMappingRepository, // ID映射数据仓库，记录本地与远程笔记ID对应关系
) : SyncProvider {

    //这里是 provider 接口的实现
    override suspend fun authenticate(config: ProviderConfig): BaseResult {
        //如果接收的不是 webdavConfig 类型，返回无效配置
        if (config !is WebdavConfig) return InvalidConfig

        //如果是 webdavConfig 类型，就调用 webdavAPI 的 testCredentials 方法进行身份验证
        return tryCalling {
            webdavAPI.testCredentials(config)
        }
    }

    //继承了这个接口，就要实现这个接口的所有方法
    /**
     * 在 webdav 服务器上创建新的笔记。
     * @param note 要创建的笔记。
     * @param config 配置信息，必须是WebdavConfig类型。
     * @return 创建笔记的结果。
     * 只是创建一个远程笔记，并没有同步
     */
    override suspend fun createNote(
        note: Note,
        config: ProviderConfig
    ): BaseResult {

        //如果不是 webdav 的配置，返回无效配置
        if (config !is WebdavConfig) return InvalidConfig

        //获取笔记的 WebdavNote 对象
        val webdavNote = note.asWebdavNote()

        //如果笔记的ID不为0，则说明笔记已经存在，返回 能创建笔记，因为笔记已经存在
        if (webdavNote.id != 0L) return GenericError("Cannot create note that already exists")


        //前置检查没错，就开始新建笔记

        //这段代码定义了一个kotlin函数，函数名为tryCalling，其接受一个lambda表达式作为参数。
        // 函数的功能是尝试执行lambda表达式中提供的代码块，并返回执行结果。如果在执行过程中发生异常，则捕获异常并返回null。
        return tryCalling {

            //调用 webdavAPI 中 createNote ，创建一个新笔记
            val savedNote = webdavAPI.createNote(webdavNote, config)

            // 将新创建的笔记与远程笔记 ID 进行关联
            idMappingRepository.assignProviderToNote(
                IdMapping(
                    localNoteId = note.id,
                    remoteNoteId = savedNote.id,
                    provider = CloudService.WEBDAV,
                    extras = savedNote.etag,
                    isDeletedLocally = false,
                ),
            )
        }

    }

    /**
     * 删除 webdav 服务器上的笔记。
     *
     * @param note 要删除的笔记。
     * @param config 配置信息，必须是 WebdavConfig类型。
     * @return 删除笔记的结果。
     */
    override suspend fun deleteNote(
        note: Note,
        config: ProviderConfig
    ): BaseResult {
        if (config !is WebdavConfig) return InvalidConfig

        val webdavNote = note.asWebdavNote()

        if (webdavNote.id == 0L) return GenericError("Cannot delete note that does not exist.")

        return tryCalling {
            webdavAPI.deleteNote(webdavNote, config)
            //TODO 对 ID 映射库的处理
            idMappingRepository.deleteByRemoteId(CloudService.WEBDAV, webdavNote.id)
        }
    }

    /**
     * 将笔记移至回收站。
     *
     * @param note 要移动的笔记。
     * @param config 配置信息，必须是WebdavConfig类型。
     * @return 移动笔记的结果。
     */
    override suspend fun moveNoteToBin(note: Note, config: ProviderConfig): BaseResult {

        if (config !is WebdavConfig) return InvalidConfig

        val webdavNote = note.asWebdavNote()

        if (webdavNote.id == 0L) return GenericError("Cannot delete note that does not exist.")

        return tryCalling {
            webdavAPI.deleteNote(webdavNote, config)
            idMappingRepository.unassignProviderFromNote(CloudService.WEBDAV, note.id)
        }
    }

    /**
     * 从回收站中恢复笔记。
     *
     * @param note 要恢复的笔记。
     * @param config 配置信息，必须是WebdavConfig类型。
     * @return 恢复笔记的结果。
     */
    override suspend fun restoreNote(note: Note, config: ProviderConfig) = createNote(note, config)

    /**
     * 更新Webdav服务器上的笔记。
     *
     * @param note 要更新的笔记。
     * @param config 配置信息，必须是WebdavConfig类型。
     * @return 更新笔记的结果。
     */
    override suspend fun updateNote(
        note: Note,
        config: ProviderConfig
    ): BaseResult {
        if (config !is WebdavConfig) return InvalidConfig

        val webdavNote = note.asWebdavNote()

        if (webdavNote.id == 0L) return GenericError("Cannot update note that does not exist.")

        return tryCalling {
            updateNoteWithEtag(note, webdavNote, null, config)
        }
    }

    /**
     * 验证配置信息的有效性并进行身份验证。
     *
     * @param config 配置信息，必须是 WebdavConfig类型。
     * @return 验证结果。
     */



    /**
     * 检查服务器是否兼容Webdav服务。
     *
     * @param config 配置信息，必须是WebdavConfig类型。
     * @return 兼容性检查结果。
     */
    override suspend fun isServerCompatible(config: ProviderConfig): BaseResult {
        return InvalidConfig


//        if (config !is NextcloudConfig) return InvalidConfig
//
//        return tryCalling {
//            val capabilities = webdavAPI.getNotesCapabilities(config)!!
//            val maxServerVersion = capabilities.apiVersion.last().toFloat()
//
//            if (MIN_SUPPORTED_VERSION.toFloat() > maxServerVersion) throw ServerNotSupportedException
//        }
    }

    /**
     * 与Webdav服务器进行同步。
     *
     * @param config 配置信息，必须是WebdavConfig类型。
     * @return 同步结果。
     */
    override suspend fun sync(config: ProviderConfig): BaseResult {

        //如果配置信息不是 WebdavConfig 类型，则返回InvalidConfig，说明是无效的配置
        if (config !is WebdavConfig) return InvalidConfig

        /**
         * 处理本地和远程笔记之间的冲突。
         *
         * 此函数首先检查笔记是否已在本地被删除，如果是，则不执行任何操作。
         * 接着，根据远程和本地笔记的修改时间以及是否被标记为收藏来决定如何解决冲突。
         * 如果远程笔记的修改时间早于本地笔记，则使用远程笔记的元数据更新本地笔记。
         * 如果远程笔记的修改时间晚于或等于本地笔记，并且存在收藏状态的改变，则更新本地笔记和映射关系中的信息。
         *
         * @param local 本地笔记对象。
         * @param remote 远程笔记对象。
         * @param mapping 笔记的本地ID与远程ID的映射关系。
         */
        suspend fun handleConflict(local: Note, remote: WebdavNote, mapping: IdMapping) {
            // 检查笔记是否已在本地被删除
            if (mapping.isDeletedLocally) return

            // 比较远程和本地笔记的修改时间
            if (remote.modified < local.modifiedDate) {
                // 远程版本过时，用远程笔记的元数据更新本地笔记
                updateNoteWithEtag(local, remote, mapping.extras, config)

                // 由于Webdav在标记笔记为收藏时不会更新修改日期，此处无需操作
            } else if (remote.modified > local.modifiedDate || remote.favorite != local.isPinned) {
                // 本地版本过时，更新本地笔记和映射关系
                noteRepository.updateNotes(remote.asUpdatedLocalNote(local))
                idMappingRepository.update(
                    mapping.copy(
                        extras = remote.etag,
                    )
                )
            }
        }


        return tryCalling {
            // Fetch notes from the cloud
            val webdavNotes = webdavAPI.getNotes(config)

            val localNoteIds = noteRepository
                .getAll()
                .first()
                .map { it.id }

            val localNotes = noteRepository
                .getNonDeleted()
                .first()
                .filterNot { it.isLocalOnly }

            val idsInUse = mutableListOf<Long>()

            // Remove id mappings for notes that do not exist
            idMappingRepository.deleteIfLocalIdNotIn(localNoteIds)

            // Handle conflicting notes
            for (remoteNote in webdavNotes) {
                idsInUse.add(remoteNote.id)

                when (val mapping = idMappingRepository.getByRemoteId(remoteNote.id, CloudService.WEBDAV)) {
                    null -> {
                        // New note, we have to create it locally
                        val localNote = remoteNote.asNewLocalNote()
                        val localId = noteRepository.insertNote(localNote, shouldSync = false)
                        idMappingRepository.insert(
                            IdMapping(
                                localNoteId = localId,
                                remoteNoteId = remoteNote.id,
                                provider = CloudService.WEBDAV,
                                isDeletedLocally = false,
                                extras = remoteNote.etag
                            )
                        )
                    }
                    else -> {
                        if (mapping.isDeletedLocally && mapping.remoteNoteId != null) {
                            webdavAPI.deleteNote(remoteNote, config)
                            continue
                        }

                        if (mapping.isBeingUpdated) continue

                        val localNote = localNotes.find { it.id == mapping.localNoteId }
                        if (localNote != null) handleConflict(
                            local = localNote,
                            remote = remoteNote,
                            mapping = mapping,
                        )
                    }
                }
            }

            // Delete notes that have been deleted remotely
            noteRepository.moveRemotelyDeletedNotesToBin(idsInUse, CloudService.WEBDAV)
            idMappingRepository.unassignProviderFromRemotelyDeletedNotes(idsInUse, CloudService.WEBDAV)

            // Finally, upload any new local notes that are not mapped to any remote id
            val newLocalNotes = noteRepository.getNonRemoteNotes(CloudService.WEBDAV).first()
            newLocalNotes.forEach {
                val newRemoteNote = webdavAPI.createNote(it.asWebdavNote(), config)
                idMappingRepository.assignProviderToNote(
                    IdMapping(
                        localNoteId = it.id,
                        remoteNoteId = newRemoteNote.id,
                        provider = CloudService.WEBDAV,
                        isDeletedLocally = false,
                        extras = newRemoteNote.etag,
                    )
                )
            }
        }
    }



    // 更新笔记属性
    private suspend fun updateNoteWithEtag(
        note: Note,
        webdavNote: WebdavNote,
        etag: String? = null,
        config: WebdavConfig
    ) {

        val cloudId = idMappingRepository.getByRemoteId(webdavNote.id, CloudService.NEXTCLOUD) ?: return

        val etag = etag ?: cloudId.extras

        val newNote = webdavAPI.updateNote(
            note.asWebdavNote(webdavNote.id),
            etag.toString(),
            config,
        )

        idMappingRepository.update(
            cloudId.copy(extras = newNote.etag, isBeingUpdated = false)
        )
    }
    //本地笔记转 webdav 格式
    private suspend fun Note.asWebdavNote(newId: Long? = null): WebdavNote{
        val id = newId ?: idMappingRepository.getByLocalIdAndProvider(id,CloudService.WEBDAV)?.remoteNoteId
        val notebookName = notebookId?.let { notebookRepository.getById(it).first()?.name }
        return asWebdavNote(id=id ?: 0L,category = notebookName ?: "")
    }


    // webdav 笔记转本地格式（更新）
    private suspend fun WebdavNote.asUpdatedLocalNote(note: Note) = note.copy(
        title = title,
        taskList = if (note.isList) note.mdToTaskList(content) else listOf(),
        content = content,
        isPinned = favorite,
        modifiedDate = modified,
        notebookId = getNotebookIdForCategory(category)
    )


    // Webdav 笔记转本地格式（新笔记）
    //TODO 格式需要更新

    private suspend fun WebdavNote.asNewLocalNote(newId: Long? = null): Note {
        val id = newId ?: idMappingRepository.getByRemoteId(id, CloudService.WEBDAV)?.localNoteId
        val notebookId = getNotebookIdForCategory(category)

        return asNewLocalNote(id, )
    }


    // 根据类别获取或创建笔记本ID
    private suspend fun getNotebookIdForCategory(category: String): Long? {
        return category
            .takeUnless { it.isBlank() }
            ?.let {
                notebookRepository.getByName(it).first()?.id ?: notebookRepository.insert(Notebook(name = category))
            }
    }

    // 尝试执行并处理异常的通用函数
    private inline fun tryCalling(block: () -> Unit): BaseResult {
        return try {
            block()
            Success
        } catch (e: Exception) {
            when (e) {
                ServerNotSupportedException -> ServerNotSupported
                is HttpException -> {
                    when (e.code()) {
                        401 -> Unauthorized
                        else -> ApiError(e.message(), e.code())
                    }
                }
                else -> GenericError(e.message.toString())
            }
        }
    }

    companion object {
        const val MIN_SUPPORTED_VERSION = 1
    }
}
