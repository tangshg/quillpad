package org.qosp.notes.ui.sync.webdav

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.qosp.notes.R
import org.qosp.notes.data.sync.core.SyncManager
import org.qosp.notes.databinding.DialogWebdavAccountBinding
import org.qosp.notes.ui.common.BaseDialog
import org.qosp.notes.ui.common.setButton
import org.qosp.notes.ui.utils.requestFocusAndKeyboard
import javax.inject.Inject


@AndroidEntryPoint
class WebdavAccountDialog : BaseDialog<DialogWebdavAccountBinding>() {
    private val model: WebdavViewModel by activityViewModels()

    private var username = ""
    private var password = ""

    @Inject
    lateinit var syncManager: SyncManager

    override fun createBinding(inflater: LayoutInflater) = DialogWebdavAccountBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog.setTitle(getString(R.string.preferences_webdav_account))

        // viewModel 中读数并设置输入框
        lifecycleScope.launch {
            username = model.username.first()
            password = model.password.first()

            if (username.isNotBlank() && password.isNotBlank()) {
                binding.editTextUsername.setText(username)
                binding.editTextPassword.setText(password)
            }
        }

        //当按下保存的时候，对输入框进行校验
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_save), this@WebdavAccountDialog) {
            //获取输入框的内容
            username = binding.editTextUsername.text.toString()
            password = binding.editTextPassword.text.toString()

            //判空操作，弹出 Toast 进行提示
            if (username.isBlank() or password.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.message_credentials_cannot_be_blank), Toast.LENGTH_SHORT).show()
                return@setButton
            }

            //如果不为空，提示连接中
            Toast.makeText(requireContext(), getString(R.string.indicator_connecting), Toast.LENGTH_LONG).show()

            //启动协程，开始连接操作
            lifecycleScope.launch {


                model.WebdavAuthenticate(username, password)


                /*
                //如果连接成功，则提示连接成功，并关闭对话框
                //首先对账号进行检验
                //TODO authenticate 函数
                val result = model.authenticate(username, password)

                val messageResId = when (result) {
                    NoConnectivity -> R.string.message_internet_not_available
                    ServerNotSupported -> R.string.message_server_not_compatible
                    Success -> R.string.message_logged_in_successfully
                    Unauthorized -> R.string.message_invalid_credentials
                    else -> R.string.message_something_went_wrong
                }
                if (messageResId != R.string.message_something_went_wrong) { // known error or success
                    Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
                    if (result == Success) dismiss()
                } else { // unknown error
                    val text = getString(R.string.message_something_went_wrong) + "\n" + result.message
                    Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
                }

                 */
            }
        }

        binding.editTextUsername.requestFocusAndKeyboard()
    }
}
