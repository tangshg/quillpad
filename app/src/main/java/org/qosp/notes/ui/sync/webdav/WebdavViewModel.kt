package org.qosp.notes.ui.sync.webdav

import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.qosp.notes.data.sync.core.BaseResult
import org.qosp.notes.data.sync.core.Success
import org.qosp.notes.data.sync.core.SyncManager
import org.qosp.notes.data.sync.nextcloud.NextcloudConfig
import org.qosp.notes.data.sync.webdav.WebdavConfig
import org.qosp.notes.preferences.PreferenceRepository
import javax.inject.Inject

@HiltViewModel
class WebdavViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val username = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_USERNAME)
    val password = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_PASSWORD)


    //设置 URL，保存到偏好仓库
    fun setURL(url: String) = viewModelScope.launch {
        if (!URLUtil.isHttpsUrl(url)) return@launch

        val url = if (url.endsWith("/")) url else "$url/"
        preferenceRepository.putEncryptedStrings(
            PreferenceRepository.WEBDAV_INSTANCE_URL to url,
        )
    }




    suspend fun WebdavAuthenticate(username: String,password: String) = withContext(Dispatchers.IO) {
        //获取 webdav 的网址
        val url = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_INSTANCE_URL).first()
        //TODO 当前仓库中的网址为
        Log.i("tangshg",url)

        //开始连接操作
        val sardine: Sardine = OkHttpSardine() //实例化
        sardine.setCredentials(username, password)

        //TODO 这里写死了连接地址，后续需要改成配置文件
        val resources = sardine.list(url)


        Log.i("tangshg","$resources")

        if (resources.isNotEmpty()) {
            // 认证成功
            return@withContext Success
        } else {
            // 认证失败

        }
    }

    /**
     * 异步认证函数，用于通过用户名和密码对用户进行认证。
     *
     * @param username 用户名。
     * @param password 密码。
     * @return 返回认证结果，封装在 BaseResult 类型中。
     */
    suspend fun authenticate(username: String, password: String): BaseResult {
        // 创建 NextcloudConfig 实例，配置认证需要的参数
        val config = WebdavConfig(
            username = username,
            password = password,
            // 从偏好仓库中获取加密的 WebDAV 实例URL，并解密使用
            remoteAddress = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_INSTANCE_URL).first()
        )

        /**
         * 在后台线程中执行认证和服务器兼容性检查流程。
         *
         * @return [BaseResult] 表示认证和兼容性检查的结果。如果认证成功，则进一步检查服务器兼容性；
         *         如果认证失败，则直接返回认证结果。
         */

        val response: BaseResult = withContext(Dispatchers.IO) {
            // 执行同步管理器的认证操作
            val loginResult = syncManager.authenticate(config)

            // 如果认证成功，则检查服务器是否兼容；否则，直接返回认证结果。
            //取消检查服务器的兼容检查
            if (loginResult == Success) {
                syncManager.isServerCompatible(config)
            }
            else
                loginResult
        }

        //存储账号密码
        return response.also {
            if (it == Success) {
                preferenceRepository.putEncryptedStrings(
                    PreferenceRepository.WEBDAV_USERNAME to username,
                    PreferenceRepository.WEBDAV_PASSWORD to password,
                )
            }
        }
    }
}
