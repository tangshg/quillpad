package org.qosp.notes.data.sync.core

import com.google.android.exoplayer2.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.sync.nextcloud.NextcloudConfig
import org.qosp.notes.data.sync.webdav.WebdavConfig
import org.qosp.notes.preferences.CloudService
import org.qosp.notes.preferences.CloudService.DISABLED
import org.qosp.notes.preferences.CloudService.NEXTCLOUD
import org.qosp.notes.preferences.CloudService.WEBDAV
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.preferences.SyncMode
import org.qosp.notes.ui.utils.ConnectionManager

/**
 * 同步管理器类，负责处理笔记数据的同步逻辑。
 *
 * @param preferenceRepository 偏好仓库，用于获取用户配置。
 * @param idMappingRepository ID映射仓库，用于处理本地ID和云端ID的映射。
 * @param connectionManager 连接管理器，用于检查网络连接状态。
 * @param cloudManager 云服务管理器，实现与服务器的交互。
 * @param syncingScope 异步操作的范围，用于管理协程。
 *
 * TODO 使用 SyncManager 时，一定要注意 传入的那个 cloudManager 对象
 */
 class SyncManager(
    private val preferenceRepository: PreferenceRepository,
    private val idMappingRepository: IdMappingRepository,
    val connectionManager: ConnectionManager,
    private val cloudManager: SyncProvider, //TODO 这里有更改，原先为 cloudManager: NextcloudManager
    val syncingScope: CoroutineScope,
) {
    
    private val tangshgTAG = "tangshgSyncManager"

    val syncManager = cloudManager

    /**
     * 根据用户偏好设置获取同步配置的Flow。
     *
     * @return Flow<SyncPrefs> 包含同步状态、提供者、模式和配置的流。
     */
    //@OptIn(ExperimentalCoroutinesApi::class) 是一个 Kotlin 注解，
    // 用于显式启用实验性的协程库 API。这表明代码中使用了一些可能尚未稳定、未来版本可能会改变的特性。
    // 在给定的代码片段中，添加这个注解表示开发者知道他们正在使用 ExperimentalCoroutinesApi 中的实验性功能，并愿意承担可能的更改风险
    @OptIn(ExperimentalCoroutinesApi::class)

    // 定义了一个 Flow<SyncPrefs> 类型的变量 prefs，用于存储同步配置信息。
    // 通过 preferenceRepository.getAll() 方法获取用户偏好设置
    // 这里注意：getAll 的返回值是 Flow<AppPreferences> 类型，表示用户偏好设置。
    // 所以需要转换成 Flow<SyncPrefs> 类型。通过 flatMapLatest 操作符 来实现
    // flatMapLatest 操作符将每个设置转换为 Flow<SyncPrefs>。
    val prefs: Flow<SyncPrefs> = preferenceRepository.getAll().flatMapLatest {

        prefs ->

        // 根据用户选择的云服务类型获取相应的配置
        // 这里只是获取同步的各种配置，包括是否启用同步、同步模式和云服务类型
        //TODO 在哪里用？

        when (prefs.cloudService) {

            DISABLED ->
                // 如果用户禁用了同步，则返回一个 Flow<SyncPrefs>，其中启用同步为 false，配置为 null
                flowOf(
                    SyncPrefs(false, null, prefs.syncMode, null)
                )

            NEXTCLOUD -> {
                // 如果用户选择了 Nextcloud 服务，则返回一个 Flow<SyncPrefs>，其中启用同步为 true，配置为 NextcloudConfig
                //NextcloudConfig.fromPreferences 是一个静态方法，返回值是 Flow<NextcloudConfig?>
                //使用 map 操作符将 NextcloudConfig 转换为 SyncPrefs
                NextcloudConfig.fromPreferences(preferenceRepository).map { config ->
                    SyncPrefs(true, cloudManager, prefs.syncMode, config)
                }
            }

            WEBDAV ->{
                // 如果用户选择了 WebDAV 服务，则返回一个 Flow<SyncPrefs>，其中启用同步为 true，配置为 WebdavConfig
                //云服务提供商为 WEBDAV
                WebdavConfig.fromPreferences(preferenceRepository).map { config ->
                    Log.i(tangshgTAG,"当前的配置 $config")
                    SyncPrefs(true, cloudManager, prefs.syncMode, config)
                }

            }
        }
    }

    /**
     * 共享的同步配置状态。
     *
     * @return Flow<ProviderConfig?> 包含当前活动的同步提供者的配置。
     *
     * 得到获取的信息，
     */

    //定义一个变量，config ，从 prefs 中获取 config 字段
    //TODO 这里 map 函数
    val config = prefs.map { prefs -> prefs.config }
        .stateIn(syncingScope, SharingStarted.WhileSubscribed(5000), null)


    /**
     * 消息处理器，用于处理来自UI或其他组件的消息，触发相应的同步操作。
     */
    @OptIn(ObsoleteCoroutinesApi::class)

    //actor是Kotlin Coroutines库中的一个函数，用于创建一个并发actor，它处理来自其他协程的消息。
    // 这个actor在自己的协程上下文中运行，接收到消息后会依次处理，提供了一种线程安全的通信方式。
    // 通常用于构建反应式系统或实现并发控制。

    // 定义一个 actor，用于处理来自 UI 或其他组件的消息，触发相应的同步操作。
    private val actor = syncingScope.actor<Message> {


        //遍历消息队列
        for (msg in channel) {
            // 根据消息类型，调用相应的同步方法
            Log.i(tangshgTAG,"收到的消息是：${msg}")

            with(msg) {

                Log.i(tangshgTAG,"当前使用的 config：${config}")
                Log.i(tangshgTAG,"当前使用的 provider：${provider}")

                val result = when (this ) {


                    //如果消息是 CreateNote 消息，则调用 provider.createNote 方法，并将结果返回给调用方。
                    is CreateNote -> provider.createNote(note, config)

                    //如果消息是 DeleteNote 消息，则调用 provider.deleteNote 方法
                    is DeleteNote -> provider.deleteNote(note, config)
                    //如果消息是 MoveNoteToBin 消息，则调用 provider.moveNoteToBin 方法
                    is MoveNoteToBin -> provider.moveNoteToBin(note, config)

                    //如果消息是 MoveNoteToBin 消息，则调用 provider.moveNoteToBin 方法
                    is RestoreNote -> provider.restoreNote(note, config)

                    //如果消息是 Sync 消息，则调用 provider.sync 方法
                    is Sync -> provider.sync(config)

                    //如果消息是 UpdateNote 消息，则调用 provider.updateNote 方法
                    is UpdateNote -> provider.updateNote(note, config)

                    //如果消息是 Authenticate 消息，则调用 provider.authenticate 方法
                    is Authenticate -> provider.authenticate(config)

                    //如果消息是 IsServerCompatible 消息，则调用 provider.isServerCompatible 方法
                    is IsServerCompatible -> provider.isServerCompatible(config)

                    //如果消息是 UpdateOrCreate 消息，则根据是否存在本地ID和云端ID的映射关系，调用 provider.updateNote 或 provider.createNote 方法
                    is UpdateOrCreate -> {
                        val exists = idMappingRepository.getByLocalIdAndProvider(note.id, config.provider) != null
                        if (exists) provider.updateNote(note, config) else provider.createNote(note, config)
                    }
                }

                Log.i(tangshgTAG,"收到执行验证的结果 $result")

                deferred.complete(result)
            }
        }
    }

    /**
     * 在进行同步操作前的条件检查。
     *
     * @param customConfig 自定义的同步提供者配置。
     * @param fallback 当条件不满足时执行的回退操作。
     * @param block 满足条件时执行的操作。
     * @return BaseResult 操作的结果。
     */

    //一个内联类
    suspend inline fun ifSyncing(
        customConfig: ProviderConfig? = null,
        fallback: () -> Unit = {},
        block: (SyncProvider, ProviderConfig) -> BaseResult,
    ): BaseResult {
        val (isEnabled, provider, mode, prefConfig) = prefs.first()
        val config = customConfig ?: prefConfig
        return when {
            !isEnabled -> SyncingNotEnabled.also { fallback() }
            provider == null || config == null -> InvalidConfig.also { fallback() }
            !connectionManager.isConnectionAvailable(mode) -> NoConnectivity.also { fallback() }
            else -> block(provider, config)
        }
    }

    /**
     * 发送消息到消息处理器。
     *
     * @param customConfig 自定义的同步提供者配置。
     * @param block 消息处理逻辑。
     * @return BaseResult 操作的结果。
     */
    //这个函数是一个kotlin函数，它的功能是在一个同步环境下发送消息。
    //customConfig是一个可选的ProviderConfig参数，用于提供自定义配置。
    //block是一个lambda表达式，用于创建一个Message对象，并且这个Message对象会被发送到一个actor。
    //函数内部会先检查是否正在同步，并且会传入provider和config给block来创建一个Message对象。
    //然后，这个Message对象会被发送到一个actor。
    //最后，会等待这个Message对象的deferred完成。
    //函数返回一个BaseResult对象，表示消息发送的结果。
    private suspend inline fun sendMessage(
        customConfig: ProviderConfig? = null,
        crossinline block: suspend (SyncProvider, ProviderConfig) -> Message,
    ): BaseResult {

        Log.i(tangshgTAG,"进入 syncManager.sendMessage 1")

        return ifSyncing(customConfig) { provider, config ->
            val message = block(provider, config)
            actor.send(message)
            message.deferred.await()
        }
    }

    /**
     * 验证同步提供者的身份验证信息。
     *
     * @param customConfig 自定义的同步提供者配置。
     */
    suspend fun authenticate(customConfig: ProviderConfig? = null) = sendMessage(customConfig)
    {
            provider, config ->
        //TODO 这里注入错了，如何换成 provider?已经成功注入了 240525
        Log.i(tangshgTAG," 已经进入 syncManager.authenticate 6")
        Log.i(tangshgTAG,"这里得到的provider 是 ：${provider}")
        Log.i(tangshgTAG,"这里得到的config 是 ：${config}")
        Authenticate(provider, config)
    }
    /**
     * 触发同步操作。
     */
    suspend fun sync() = sendMessage { provider, config -> Sync(provider, config) }

    /**
     * 创建笔记。
     *
     * @param note 要创建的笔记。
     */
    suspend fun createNote(note: Note) = sendMessage { provider, config -> CreateNote(note, provider, config) }

    /**
     * 删除笔记。
     *
     * @param note 要删除的笔记。
     */
    suspend fun deleteNote(note: Note) = sendMessage { provider, config -> DeleteNote(note, provider, config) }

    /**
     * 将笔记移至回收站。
     *
     * @param note 要移动的笔记。
     */
    suspend fun moveNoteToBin(note: Note) = sendMessage { provider, config -> MoveNoteToBin(note, provider, config) }

    /**
     * 恢复回收站中的笔记。
     *
     * @param note 要恢复的笔记。
     */
    suspend fun restoreNote(note: Note) = sendMessage { provider, config -> RestoreNote(note, provider, config) }

    /**
     * 更新笔记。
     *
     * @param note 要更新的笔记。
     */
    suspend fun updateNote(note: Note) = sendMessage { provider, config -> UpdateNote(note, provider, config) }

    /**
     * 更新或创建笔记。
     *
     * @param note 要更新或创建的笔记。
     */
    suspend fun updateOrCreate(note: Note) = sendMessage { provider, config -> UpdateOrCreate(note, provider, config) }

    /**
     * 检查服务器兼容性。
     *
     * @param customConfig 自定义的同步提供者配置。
     */
    suspend fun isServerCompatible(customConfig: ProviderConfig? = null) = sendMessage(customConfig) { provider, config ->
        IsServerCompatible(provider, config)
    }


}

/**
 * 包含同步设置的数据类。
 * 
 * @param isEnabled 是否启用同步。
 * @param provider 使用的同步提供者。
 * @param mode 同步模式，可以是WIFI和移动网络。
 * @param config 同步提供者配置。
 */
data class SyncPrefs(
    val isEnabled: Boolean,
    val provider: SyncProvider?,
    val mode: SyncMode,
    val config: ProviderConfig?,
)


/**
 * 消息类，用于actor内部通信，封装了同步操作的各种请求。
 *
 * 定义一个密闭类Message，用于actor内部通信，封装了同步操作的各种请求。
 *
 * @property provider 同步提供者。
 * @property config 同步提供者配置。
 *
 * @property deferred 用于异步操作的CompletableDeferred对象。
 */
private sealed class Message(val provider: SyncProvider, val config: ProviderConfig) {
    val deferred: CompletableDeferred<BaseResult> = CompletableDeferred()
}

/**
 * 验证身份验证信息的消息。8
 */
private class Authenticate(provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 创建笔记的消息。1
 */
private class CreateNote(val note: Note, provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 更新笔记的消息。2
 */
private class UpdateNote(val note: Note, provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 更新或创建笔记的消息。3
 */
private class UpdateOrCreate(val note: Note, provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 删除笔记的消息。4
 */
private class DeleteNote(val note: Note, provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 恢复笔记的消息。5
 */
private class RestoreNote(val note: Note, provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 移动笔记到回收站的消息。6
 */
private class MoveNoteToBin(val note: Note, provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
/**
 * 触发同步的消息。7
 */
private class Sync(provider: SyncProvider, config: ProviderConfig) : Message(provider, config)

/**
 * 检查服务器兼容性的消息。9
 */
private class IsServerCompatible(provider: SyncProvider, config: ProviderConfig) : Message(provider, config)
