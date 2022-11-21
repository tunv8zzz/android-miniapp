package com.rakuten.tech.mobile.testapp.ui.input

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.webkit.URLUtil
import androidx.databinding.DataBindingUtil
import com.rakuten.tech.mobile.miniapp.testapp.R
import com.rakuten.tech.mobile.miniapp.testapp.databinding.MiniAppInputActivityBinding
import com.rakuten.tech.mobile.testapp.helper.isValidMiniAppUrl
import com.rakuten.tech.mobile.testapp.ui.base.BaseActivity
import com.rakuten.tech.mobile.testapp.ui.deeplink.SchemeByUrlActivity
import com.rakuten.tech.mobile.testapp.ui.display.MiniAppDisplayActivity
import com.rakuten.tech.mobile.testapp.ui.display.preload.PreloadMiniAppWindow

class MiniAppByUrlActivity : BaseActivity(), PreloadMiniAppWindow.PreloadMiniAppLaunchListener {

    override val pageName: String = this::class.simpleName ?: ""
    override val siteSection: String = this::class.simpleName ?: ""

    private lateinit var binding: MiniAppInputActivityBinding

    sealed class InputDisplay(val input: String) {
        class Url(input: String) : InputDisplay(input)
        class MiniAppUrl(input: String) : InputDisplay(input)
        class None : InputDisplay("")
    }

    private var display: InputDisplay = InputDisplay.None()
    private var miniappCameraPermissionCallback: (isGranted: Boolean) -> Unit = {
        if (it) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(display.input.trim()))
            startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.mini_app_input_activity)

        binding.inputLayout.hint = getString(R.string.lb_app_url)

        validateInput(binding.edtAppId.text.toString().trim())
        binding.edtAppId.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                //intended
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //intended
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateInput(s.toString().trim())
            }
        })

        binding.btnDisplayAppId.setOnClickListener {
            raceExecutor.run { displayMiniApp() }
        }
        binding.btnDisplayList.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun isValidAppUrl(input: String): Boolean = URLUtil.isValidUrl(input)
            && Patterns.WEB_URL.matcher(input).matches()

    fun validateInput(input: String) {
        if (input.isBlank())
            binding.btnDisplayAppId.isEnabled = false
        else {
            display =
                when {
                    (isValidAppUrl(input)) -> {
                        onValidUI()
                        InputDisplay.Url(input)
                    }
                    isValidMiniAppUrl(input) -> {
                        onValidUI()
                        InputDisplay.MiniAppUrl(input)
                    }
                    else -> {
                        onInvalidUI()
                        InputDisplay.None()
                    }
                }
        }
    }

    private fun onInvalidUI() {
        binding.edtAppId.error = getString(R.string.error_invalid_input)
        binding.btnDisplayAppId.isEnabled = false
    }

    private fun onValidUI() {
        binding.edtAppId.error = null
        binding.btnDisplayAppId.isEnabled = true
    }

    private fun displayMiniApp() = when (display) {
        is InputDisplay.Url -> {
            MiniAppDisplayActivity.startUrl(
                this,
                display.input.trim(),
            )
        }
        is InputDisplay.MiniAppUrl -> {
            SchemeByUrlActivity.start(
                this,
                display.input.trim()
            )
        }
        is InputDisplay.None -> {
            //intended
        }
    }

    override fun onPreloadMiniAppResponse(isAccepted: Boolean) {
        if (isAccepted)
            MiniAppDisplayActivity.start(this, display.input)
    }
}
