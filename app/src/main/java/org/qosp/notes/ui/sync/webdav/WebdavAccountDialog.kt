package org.qosp.notes.ui.sync.webdav

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.qosp.notes.R
import org.qosp.notes.data.sync.core.ApiError
import org.qosp.notes.data.sync.core.GenericError
import org.qosp.notes.data.sync.core.InvalidConfig
import org.qosp.notes.data.sync.core.NoConnectivity
import org.qosp.notes.data.sync.core.OperationNotSupported
import org.qosp.notes.data.sync.core.ServerNotSupported
import org.qosp.notes.data.sync.core.Success
import org.qosp.notes.data.sync.core.SyncManager
import org.qosp.notes.data.sync.core.SyncingNotEnabled
import org.qosp.notes.data.sync.core.Unauthorized
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

        // 从 viewModel 中查询当前存储的账号和密码
        lifecycleScope.launch {
            username = model.username.first()
            password = model.password.first()

            Log.i("tangshg", "WebdavAccountDialog 当前账号和密码是  username: $username, password: $password")

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
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_credentials_cannot_be_blank),
                    Toast.LENGTH_SHORT
                ).show()
                return@setButton
            }

            //如果不为空，提示连接中
            Toast.makeText(requireContext(), getString(R.string.indicator_connecting), Toast.LENGTH_LONG).show()

            //启动协程，开始连接操作
            lifecycleScope.launch {


                //如果连接成功，则提示连接成功，并关闭对话框
                Log.i("tangshg", "WebdavAccountDialog 身份信息验证  1")
                val result = model.authenticate(username, password)
                Log.i("tangshg","WebdavAccountDialog  身份信息验证的结果{$result} 最后一步")

                //返回的消息用于弹出 toast ，进行提示
                val messageResId = when (result) {
                    Success -> R.string.message_logged_in_successfully
                    is ApiError -> TODO()
                    is GenericError -> TODO()
                    InvalidConfig -> TODO()
                    NoConnectivity -> TODO()
                    OperationNotSupported -> TODO()
                    ServerNotSupported -> TODO()
                    SyncingNotEnabled -> TODO()
                    Unauthorized -> TODO()
                }
                if (messageResId != R.string.message_something_went_wrong) { // known error or success
                    Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
                    if (result == Success) dismiss()
                } else { // unknown error
                    val text = getString(R.string.message_something_went_wrong) + "\n" + result.message
                    Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.editTextUsername.requestFocusAndKeyboard()
    }
}
