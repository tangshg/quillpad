package org.qosp.notes.data.sync.core

import org.qosp.notes.data.model.Note

/**
 * SyncProvider 接口定义了与同步提供者交互所需的方法。
 *
 * 共有八个方法
 *
 * 1. sync: 同步数据。
 * 2. createNote: 创建一个新的笔记。
 * 3. deleteNote: 删除一个笔记。
 * 4. updateNote: 更新现有的笔记。
 * 5. moveNoteToBin:
 * 6. restoreNote
 * 7. authenticate
 * 8. isServerCompatible
 *
 */
interface SyncProvider {

    /**
     * 同步数据。
     * @param config 供应商配置，包含同步所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun sync(config: ProviderConfig): BaseResult

    /**
     * 创建一个新的笔记。
     * @param note 要创建的笔记对象。
     * @param config 供应商配置，包含创建所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun createNote(note: Note, config: ProviderConfig): BaseResult

    /**
     * 删除一个笔记。
     * @param note 要删除的笔记对象。
     * @param config 供应商配置，包含删除所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun deleteNote(note: Note, config: ProviderConfig): BaseResult

    /**
     * 更新一个笔记。
     * @param note 要更新的笔记对象。
     * @param config 供应商配置，包含更新所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun updateNote(note: Note, config: ProviderConfig): BaseResult

    /**
     * 将笔记移动到回收站。
     * @param note 要移动到回收站的笔记对象。
     * @param config 供应商配置，包含移动所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun moveNoteToBin(note: Note, config: ProviderConfig): BaseResult

    /**
     * 从回收站恢复一个笔记。
     * @param note 要恢复的笔记对象。
     * @param config 供应商配置，包含恢复所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun restoreNote(note: Note, config: ProviderConfig): BaseResult

    /**
     * 对用户进行认证。
     * @param config 供应商配置，包含认证所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息。
     */
    suspend fun authenticate(config: ProviderConfig): BaseResult

    /**
     * 检查服务器是否兼容。
     * @param config 供应商配置，包含兼容性检查所需的详细设置。
     * @return 返回操作的结果，成功或失败的信息，以及是否兼容的信息。
     */
    suspend fun isServerCompatible(config: ProviderConfig): BaseResult
}

