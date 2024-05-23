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
import org.qosp.notes.data.sync.core.Unauthorized
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


    //TODO 设置 URL，保存到仓库
    fun setURL(url: String) = viewModelScope.launch {
        if (!URLUtil.isHttpsUrl(url)) return@launch

        val url = if (url.endsWith("/")) url else "$url/"

        //将网址加密后保存到仓库
        preferenceRepository.putEncryptedStrings(
            PreferenceRepository.WEBDAV_INSTANCE_URL to url,
        )

        //TODO 这里仅仅作为测试，通过 log 查看保存的网址是否正确
        val flow = preferenceRepository.getEncryptedString(
            PreferenceRepository.WEBDAV_INSTANCE_URL
        )
        Log.i("tangshg", "webdavViewModel 当前保存的网址是" + flow.first())
    }


    // authenticate 中文：进行身份确认
    //TODO 这里需要改进，要使用 syncManager.authenticate 进行身份确认
    suspend fun authenticate(username: String, password: String): BaseResult {

        Log.i("tangshg","WebdavViewModel 开始身份信息验证 2")
        // 创建 WebdavConfig 实例，配置认证需要的参数
        val config = WebdavConfig(
            username = username,
            password = password,
            // 从偏好仓库中获取加密的 WebDAV 实例URL，并解密使用
            remoteAddress = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_INSTANCE_URL).first()
        )
        //
        Log.i("tangshg","WebdavViewModel  得到配置文件 3")

        //获取 webdav 的网址
        val url = preferenceRepository.getEncryptedString(PreferenceRepository.WEBDAV_INSTANCE_URL).first()
        //TODO 当前仓库中的网址为
        Log.i("tangshg", "现在进行身份验证，当前连接的网址为$url  4")

        //开始连接操作
        //TODO 对异常进行捕捉
        val sardine: Sardine = OkHttpSardine()
        sardine.setCredentials(username, password)

        val resources = sardine.list(url)

        val response: BaseResult = withContext(Dispatchers.IO) {
            // 执行同步管理器的认证操作

            sardine.list(url)

            Log.i("tangshg","即将进入 syncManager.authenticate，这里传入的是配置文件  5")
            val loginResult = syncManager.authenticate(config)

            // 如果认证成功，则检查服务器是否兼容；否则，直接返回认证结果。
            //取消检查服务器的兼容检查
            if (loginResult == Success) {
                syncManager.isServerCompatible(config)
            } else
                loginResult
        }

        Log.i("tangshg", "$resources")

        if (resources.isNotEmpty()) {
            // 认证成功
            return Success

        } else {
            //认证失败
            return Unauthorized
        }

        //连接成功后，需要存储账号密码

    }

    /**
     * 异步认证函数，用于通过用户名和密码对用户进行认证。
     *
     * @param username 用户名。
     * @param password 密码。
     * @return 返回认证结果，封装在 BaseResult 类型中。
     */
    suspend fun webdavAuthenticate(username: String, password: String): BaseResult {
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
        //
        //这段代码使用了Kotlin的withContext函数，它用于在指定的上下文中执行一个block，
        // 并返回block的执行结果。这里指定的上下文是Dispatchers.IO，表示在IO线程池中执行block。
        // withContext函数会阻塞当前线程，直到block执行完成并返回结果。
        // 这段代码的目的是在IO线程池中执行某个操作，并将操作结果赋值给response变量。

        val response: BaseResult = withContext(Dispatchers.IO) {
            // 执行同步管理器的认证操作
            val loginResult = syncManager.authenticate(config)

            // 如果认证成功，则检查服务器是否兼容；否则，直接返回认证结果。
            //取消检查服务器的兼容检查
            if (loginResult == Success) {
                syncManager.isServerCompatible(config)
            } else
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
