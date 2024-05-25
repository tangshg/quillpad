package org.qosp.notes.data.sync.webdav

import android.util.Base64
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.qosp.notes.data.sync.core.ProviderConfig
import org.qosp.notes.preferences.CloudService
import org.qosp.notes.preferences.PreferenceRepository

/**
 * WebDAV配置类，继承自ProviderConfig，用于存储和提供WebDAV服务的配置信息。
 */
data class WebdavConfig(
    override val remoteAddress: String, // 远程地址
    override val username: String, // 用户名
    val password: String, // 密码
) : ProviderConfig {

    //在这里加上 sardine 实例
    val sardine: Sardine = OkHttpSardine()
    init {
        sardine.setCredentials(username, password)
    }


    //sardine.setCredentials(username, password)
    //val resources = sardine.list(url)

    //TODO 后期删掉，webdav 不需要认证头
    // 计算基础认证信息头，用于HTTP认证
    val credentials = ("Basic " + Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)).trim()

    override val provider: CloudService = CloudService.WEBDAV // 服务提供商类型
    override val authenticationHeaders: Map<String, String> // 认证所需的HTTP头
        get() = mapOf("Authorization" to credentials)

    /**
     * 从偏好设置中构建 WebdavConfig 的 Flow。
     * @param preferenceRepository 偏好仓库，用于获取加密的配置信息。
     * @return Flow<WebdavConfig?>，可能为null的情况是当所需的配置信息不完整时。
     */
    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        fun fromPreferences(preferenceRepository: PreferenceRepository): Flow<WebdavConfig?> {
            // 从偏好设置获取URL、用户名和密码
            val url = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_INSTANCE_URL)
            val username = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_USERNAME)
            val password = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_PASSWORD)

            // 构建Flow，确保只有当所有必要信息都非空时才构建WebdavConfig实例
            return url.flatMapLatest { url ->
                username.flatMapLatest { username ->
                    password.map { password ->
                        WebdavConfig(url, username, password)
                            .takeUnless { url.isBlank() or username.isBlank() or password.isBlank() }
                    }
                }
            }
        }
    }
}
