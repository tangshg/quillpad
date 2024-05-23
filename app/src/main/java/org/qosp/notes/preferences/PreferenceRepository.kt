package org.qosp.notes.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.msoul.datastore.EnumPreference
import me.msoul.datastore.getEnum
import me.msoul.datastore.setEnum
import java.io.IOException

/**
 * 偏好设置仓库类，用于管理应用的偏好设置数据。
 *
 * @param dataStore DataStore对象，用于持久化存储偏好设置。
 * @param sharedPreferences FlowSharedPreferences对象，用于管理Flow类型的SharedPreferences。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceRepository(
    val dataStore: DataStore<Preferences>,
    private val sharedPreferences: FlowSharedPreferences,
) {
    /**
     * 获取指定键对应的加密字符串。
     *
     * @param key 配置项的键。
     * @return Flow类型，代表配置项的值。
     */
    fun getEncryptedString(key: String): Flow<String> {
        return sharedPreferences.getString(key, "").asFlow()
    }

    /**
     * 获取应用的所有偏好设置。
     *
     * @return Flow类型，包含应用的所有偏好设置 [AppPreferences]。
     */
    fun getAll(): Flow<AppPreferences> {
        return dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences()) // 发出空偏好设置，处理数据存储异常。
                } else {
                    throw it // 重新抛出非IOException类型的异常。
                }
            }
            .map { prefs ->
                AppPreferences(
                    // 设置布局模式，根据用户偏好设置应用的布局展示方式
                    layoutMode = prefs.getEnum(),

                    // 设置主题模式，允许用户自定义应用的主题（如浅色、深色等）
                    themeMode = prefs.getEnum(),

                    // 设置暗色主题模式，提供额外的暗色主题选项以适配不同环境
                    darkThemeMode = prefs.getEnum(),

                    // 选择颜色方案，决定应用的色彩搭配和视觉风格
                    colorScheme = prefs.getEnum(),

                    // 设定排序方法，用于组织和排列笔记或其他数据的逻辑
                    sortMethod = prefs.getEnum(),

                    // 配置备份策略，定义数据备份的时机和条件
                    backupStrategy = prefs.getEnum(),

                    // 自定义笔记删除时间规则，管理数据清理行为
                    noteDeletionTime = prefs.getEnum(),

                    // 选定日期格式，个性化日期显示样式
                    dateFormat = prefs.getEnum(),

                    // 选定时间格式，调整时间显示的方式
                    timeFormat = prefs.getEnum(),

                    // 指定媒体打开方式，控制图片、视频等媒体文件的默认打开程序或应用内体验
                    openMediaIn = prefs.getEnum(),

                    // 开关显示日期功能，允许用户决定是否在界面中显示日期信息
                    showDate = prefs.getEnum(),

                    // 调整编辑器字体大小，提升文本编辑的可读性和舒适度
                    editorFontSize = prefs.getEnum(),

                    // 控制是否显示切换模式的Floating Action Button，优化用户界面操作
                    showFabChangeMode = prefs.getEnum(),

                    // 选项以决定是否将无笔记本归属的笔记进行分组处理
                    groupNotesWithoutNotebook = prefs.getEnum(),

                    // 定义检查项（如待办事项）的移动行为，提升任务管理效率
                    moveCheckedItems = prefs.getEnum(),

                    // 选择云服务提供商，配置云同步服务的基础
                    cloudService = prefs.getEnum(),

                    // 确定同步模式，自动化数据同步的策略和条件
                    syncMode = prefs.getEnum(),

                    // 启用或禁用背景同步，平衡数据更新的即时性和资源消耗
                    backgroundSync = prefs.getEnum(),

                    // 设置新创建笔记的默认同步状态，便于跨设备访问
                    newNotesSyncable = prefs.getEnum(),

                    )
            }
    }

    /**
     * 获取指定枚举类型的偏好设置。
     *
     * @param T 枚举类型，必须实现 [EnumPreference] 接口。
     * @return Flow类型，代表指定枚举类型的偏好设置。
     */
    inline fun <reified T> get(): Flow<T> where T : Enum<T>, T : EnumPreference {
        return dataStore.getEnum()
    }

    /**
     * 同时设置多个加密字符串键值对。
     *
     * @param pairs 键值对数组，每个键值对包含一个键和对应的值。
     */
    suspend fun putEncryptedStrings(vararg pairs: Pair<String, String>) {
        pairs.forEach { (key, value) ->
            sharedPreferences.getString(key).setAndCommit(value)
        }
    }

    /**
     * 设置指定枚举类型的偏好设置。
     *
     * @param preference 需要设置的偏好设置枚举项，必须实现 [EnumPreference] 接口。
     */
    suspend fun <T> set(preference: T) where T : Enum<T>, T : EnumPreference {
        dataStore.setEnum(preference)
    }

    /**
     * 仓库类的伴生对象，定义了一些常量，用于云服务和WebDAV服务的配置。
     */
    companion object {
        const val NEXTCLOUD_INSTANCE_URL = "NEXTCLOUD_INSTANCE_URL"
        const val NEXTCLOUD_USERNAME = "NEXTCLOUD_USERNAME"
        const val NEXTCLOUD_PASSWORD = "NEXTCLOUD_PASSWORD"

        const val WEBDAV_INSTANCE_URL = "WEBDAV_INSTANCE_URL"
        const val WEBDAV_USERNAME = "WEBDAV_USERNAME"
        const val WEBDAV_PASSWORD = "WEBDAV_PASSWORD"
    }
}
