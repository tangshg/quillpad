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
import org.qosp.notes.data.sync.nextcloud.NextcloudAPI
import org.qosp.notes.data.sync.nextcloud.NextcloudConfig
import org.qosp.notes.data.sync.nextcloud.createNote
import org.qosp.notes.data.sync.nextcloud.deleteNote
import org.qosp.notes.data.sync.nextcloud.getNotes
import org.qosp.notes.data.sync.nextcloud.getNotesCapabilities
import org.qosp.notes.data.sync.nextcloud.model.NextcloudNote
import org.qosp.notes.data.sync.nextcloud.model.asNewLocalNote
import org.qosp.notes.data.sync.nextcloud.model.asNextcloudNote
import org.qosp.notes.data.sync.nextcloud.testCredentials
import org.qosp.notes.data.sync.nextcloud.updateNote
import org.qosp.notes.preferences.CloudService
import retrofit2.HttpException

/**
 * NextcloudManager 类负责与Nextcloud服务器进行同步操作，包括创建、删除、更新笔记。
 *
 * @param nextcloudAPI 用于与Nextcloud服务器通信的API客户端。
 * @param noteRepository 本地笔记数据的存储库。
 * @param notebookRepository 本地笔记本数据的存储库。
 * @param idMappingRepository 本地和远程笔记ID映射的存储库。
 */
class WebdavManager(
    private val nextcloudAPI: WebdavAPI, // Nextcloud API客户端，用于与服务器交互
    private val noteRepository: NoteRepository, // 笔记数据仓库
    private val notebookRepository: NotebookRepository, // 笔记本数据仓库
    private val idMappingRepository: IdMappingRepository, // ID映射数据仓库，记录本地与远程笔记ID对应关系
) : SyncProvider {
    /**
     * 在Nextcloud服务器上创建新的笔记。
     *
     * @param note 要创建的笔记。
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 创建笔记的结果。
     */
    override suspend fun createNote(
        note: Note,
        config: ProviderConfig
    ): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        val nextcloudNote = note.asNextcloudNote()

        if (nextcloudNote.id != 0L) return GenericError("Cannot create note that already exists")

        return tryCalling {
            val savedNote = nextcloudAPI.createNote(nextcloudNote, config)
            idMappingRepository.assignProviderToNote(
                IdMapping(
                    localNoteId = note.id,
                    remoteNoteId = savedNote.id,
                    provider = CloudService.NEXTCLOUD,
                    extras = savedNote.etag,
                    isDeletedLocally = false,
                ),
            )
        }
    }

    /**
     * 删除Nextcloud服务器上的笔记。
     *
     * @param note 要删除的笔记。
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 删除笔记的结果。
     */
    override suspend fun deleteNote(
        note: Note,
        config: ProviderConfig
    ): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        val nextcloudNote = note.asNextcloudNote()

        if (nextcloudNote.id == 0L) return GenericError("Cannot delete note that does not exist.")

        return tryCalling {
            nextcloudAPI.deleteNote(nextcloudNote, config)
            idMappingRepository.deleteByRemoteId(CloudService.NEXTCLOUD, nextcloudNote.id)
        }
    }

    /**
     * 将笔记移至回收站。
     *
     * @param note 要移动的笔记。
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 移动笔记的结果。
     */
    override suspend fun moveNoteToBin(note: Note, config: ProviderConfig): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        val nextcloudNote = note.asNextcloudNote()

        if (nextcloudNote.id == 0L) return GenericError("Cannot delete note that does not exist.")

        return tryCalling {
            nextcloudAPI.deleteNote(nextcloudNote, config)
            idMappingRepository.unassignProviderFromNote(CloudService.NEXTCLOUD, note.id)
        }
    }

    /**
     * 从回收站中恢复笔记。
     *
     * @param note 要恢复的笔记。
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 恢复笔记的结果。
     */
    override suspend fun restoreNote(note: Note, config: ProviderConfig) = createNote(note, config)


    /**
     * 更新Nextcloud服务器上的笔记。
     *
     * @param note 要更新的笔记。
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 更新笔记的结果。
     */
    override suspend fun updateNote(
        note: Note,
        config: ProviderConfig
    ): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        val nextcloudNote = note.asNextcloudNote()

        if (nextcloudNote.id == 0L) return GenericError("Cannot update note that does not exist.")

        return tryCalling {
            updateNoteWithEtag(note, nextcloudNote, null, config)
        }
    }

    /**
     * 验证配置信息的有效性并进行身份验证。
     *
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 验证结果。
     */
    override suspend fun authenticate(config: ProviderConfig): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        return tryCalling {
            nextcloudAPI.testCredentials(config)
        }
    }

    /**
     * 检查服务器是否兼容Nextcloud服务。
     *
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 兼容性检查结果。
     */
    override suspend fun isServerCompatible(config: ProviderConfig): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        return tryCalling {
            val capabilities = nextcloudAPI.getNotesCapabilities(config)!!
            val maxServerVersion = capabilities.apiVersion.last().toFloat()

            if (MIN_SUPPORTED_VERSION.toFloat() > maxServerVersion) throw ServerNotSupportedException
        }
    }

    /**
     * 与Nextcloud服务器进行同步。
     *
     * @param config 配置信息，必须是NextcloudConfig类型。
     * @return 同步结果。
     */
    override suspend fun sync(config: ProviderConfig): BaseResult {
        if (config !is NextcloudConfig) return InvalidConfig

        suspend fun handleConflict(local: Note, remote: NextcloudNote, mapping: IdMapping) {
            if (mapping.isDeletedLocally) return

            if (remote.modified < local.modifiedDate) {
                // Remote version is outdated
                updateNoteWithEtag(local, remote, mapping.extras, config)

                // Nextcloud does not update change the modification date when a note is starred
            } else if (remote.modified > local.modifiedDate || remote.favorite != local.isPinned) {
                // Local version is outdated
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
            val nextcloudNotes = nextcloudAPI.getNotes(config)

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
            for (remoteNote in nextcloudNotes) {
                idsInUse.add(remoteNote.id)

                when (val mapping = idMappingRepository.getByRemoteId(remoteNote.id, CloudService.NEXTCLOUD)) {
                    null -> {
                        // New note, we have to create it locally
                        val localNote = remoteNote.asNewLocalNote()
                        val localId = noteRepository.insertNote(localNote, shouldSync = false)
                        idMappingRepository.insert(
                            IdMapping(
                                localNoteId = localId,
                                remoteNoteId = remoteNote.id,
                                provider = CloudService.NEXTCLOUD,
                                isDeletedLocally = false,
                                extras = remoteNote.etag
                            )
                        )
                    }
                    else -> {
                        if (mapping.isDeletedLocally && mapping.remoteNoteId != null) {
                            nextcloudAPI.deleteNote(remoteNote, config)
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
            noteRepository.moveRemotelyDeletedNotesToBin(idsInUse, CloudService.NEXTCLOUD)
            idMappingRepository.unassignProviderFromRemotelyDeletedNotes(idsInUse, CloudService.NEXTCLOUD)

            // Finally, upload any new local notes that are not mapped to any remote id
            val newLocalNotes = noteRepository.getNonRemoteNotes(CloudService.NEXTCLOUD).first()
            newLocalNotes.forEach {
                val newRemoteNote = nextcloudAPI.createNote(it.asNextcloudNote(), config)
                idMappingRepository.assignProviderToNote(
                    IdMapping(
                        localNoteId = it.id,
                        remoteNoteId = newRemoteNote.id,
                        provider = CloudService.NEXTCLOUD,
                        isDeletedLocally = false,
                        extras = newRemoteNote.etag,
                    )
                )
            }
        }
    }


    // 使用etag更新笔记
    private suspend fun updateNoteWithEtag(
        note: Note,
        nextcloudNote: NextcloudNote,
        etag: String? = null,
        config: NextcloudConfig
    ) {
        // 更新笔记并维护映射表中的etag
        val cloudId = idMappingRepository.getByRemoteId(nextcloudNote.id, CloudService.NEXTCLOUD) ?: return
        val etag = etag ?: cloudId.extras
        val newNote = nextcloudAPI.updateNote(
            note.asNextcloudNote(nextcloudNote.id),
            etag.toString(),
            config,
        )

        idMappingRepository.update(
            cloudId.copy(extras = newNote.etag, isBeingUpdated = false)
        )
    }

    // 本地笔记转Nextcloud格式
    private suspend fun Note.asNextcloudNote(newId: Long? = null): NextcloudNote {
        val id = newId ?: idMappingRepository.getByLocalIdAndProvider(id, CloudService.NEXTCLOUD)?.remoteNoteId
        val notebookName = notebookId?.let { notebookRepository.getById(it).first()?.name }
        return asNextcloudNote(id = id ?: 0L, category = notebookName ?: "")
    }

    // Nextcloud笔记转本地格式（更新）
    private suspend fun NextcloudNote.asUpdatedLocalNote(note: Note) = note.copy(
        title = title,
        taskList = if (note.isList) note.mdToTaskList(content) else listOf(),
        content = content,
        isPinned = favorite,
        modifiedDate = modified,
        notebookId = getNotebookIdForCategory(category)
    )


    // Nextcloud笔记转本地格式（新笔记）
    private suspend fun NextcloudNote.asNewLocalNote(newId: Long? = null): Note {
        val id = newId ?: idMappingRepository.getByRemoteId(id, CloudService.NEXTCLOUD)?.localNoteId
        val notebookId = getNotebookIdForCategory(category)
        return asNewLocalNote(id = id ?: 0L, notebookId = notebookId)
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
