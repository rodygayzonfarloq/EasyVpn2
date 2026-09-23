package com.fastvpnn.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import com.fastvpnn.app.BuildConfig
import com.fastvpnn.app.R
import com.fastvpnn.app.data.AppSettings
import com.fastvpnn.app.databinding.ActivitySettingsBinding
import com.fastvpnn.app.util.applyEdgeToEdgeInsets

/** Regular-user settings only. Server management is handled entirely by the
 *  backend/VPS, so there's no in-app admin surface here. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.root)
        binding.textAppVersion.text = getString(R.string.version_footer_format, BuildConfig.VERSION_NAME)

        settings = AppSettings(this)

        binding.switchAutoConnect.isChecked = settings.autoConnectEnabled
        binding.switchAutoConnect.setOnCheckedChangeListener { _, checked ->
            settings.autoConnectEnabled = checked
        }

        // A real kill switch is Android's VPN lockdown mode. Apps cannot
        // programmatically enable it, so do not expose a local preference that
        // falsely claims the kill switch is active. The button below opens the
        // system VPN settings where the user can enable "Block connections
        // without VPN" for FastVPN.


        // Android only lets the *user* (not the app itself) turn on true lockdown
        // mode; that's deliberate OS security design, not a library limitation.
        // We deep-link straight to the right screen to make it a two-tap job.
        binding.buttonSystemVpnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }

        setUpThemeRadioGroup()
        setUpDnsSection()

        binding.buttonSplitTunneling.setOnClickListener {
            startActivity(Intent(this, SplitTunnelActivity::class.java))
        }

        setUpAboutAndSupportSection()
    }

    /** Privacy Policy and Terms open the same pages published on the backend's
     *  public site (see backend/api/public/{privacy,terms}.html) -- one canonical
     *  copy instead of duplicating legal text inside the app itself. Contact us
     *  opens the user's email app pre-addressed to our support inbox. */
    private fun setUpAboutAndSupportSection() {
        val siteBaseUrl = settings.backendApiUrl.trimEnd('/')

        binding.rowPrivacyPolicy.setOnClickListener {
            openInBrowser("$siteBaseUrl/privacy.html")
        }
        binding.rowTerms.setOnClickListener {
            openInBrowser("$siteBaseUrl/terms.html")
        }
        binding.rowContactUs.setOnClickListener {
            openContactEmail()
        }
    }

    private fun openInBrowser(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser app found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openContactEmail() {
        val email = getString(R.string.contact_email)
        // ACTION_SENDTO with a mailto: Uri (rather than ACTION_SEND) targets only
        // email apps, so this doesn't show up as an option for every share sheet
        // handler on the device.
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "FastVPN Support")
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found — reach us at $email", Toast.LENGTH_LONG).show()
        }
    }

    private fun setUpThemeRadioGroup() {
        val checkedId = when (settings.themeMode) {
            "light" -> R.id.radioThemeLight
            "dark" -> R.id.radioThemeDark
            else -> R.id.radioThemeSystem
        }
        binding.radioGroupTheme.check(checkedId)

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedButtonId ->
            val (mode, nightMode) = when (checkedButtonId) {
                R.id.radioThemeLight -> "light" to AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioThemeDark -> "dark" to AppCompatDelegate.MODE_NIGHT_YES
                else -> "system" to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            settings.themeMode = mode
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    /** "Server default" plus one-tap presets (Google / Cloudflare / AdGuard ad-block)
     *  and a Custom option -- see AppSettings.resolveDns for how this is applied. */
    private fun setUpDnsSection() {
        val checkedId = when (settings.dnsMode) {
            "google" -> R.id.radioDnsGoogle
            "cloudflare" -> R.id.radioDnsCloudflare
            "adblock" -> R.id.radioDnsAdblock
            "custom" -> R.id.radioDnsCustom
            else -> R.id.radioDnsServer
        }
        binding.radioGroupDns.check(checkedId)
        binding.editCustomDns.setText(settings.customDns)
        binding.layoutCustomDns.visibility = if (checkedId == R.id.radioDnsCustom) View.VISIBLE else View.GONE

        binding.radioGroupDns.setOnCheckedChangeListener { _, checkedButtonId ->
            settings.dnsMode = when (checkedButtonId) {
                R.id.radioDnsGoogle -> "google"
                R.id.radioDnsCloudflare -> "cloudflare"
                R.id.radioDnsAdblock -> "adblock"
                R.id.radioDnsCustom -> "custom"
                else -> "server"
            }
            binding.layoutCustomDns.visibility = if (checkedButtonId == R.id.radioDnsCustom) View.VISIBLE else View.GONE
            // The new resolver only takes effect on the next connect -- if a
            // tunnel is already up, flag it so MainActivity reconnects for us
            // instead of the change silently doing nothing until the user
            // happens to toggle the connection themselves.
            settings.dnsChangePendingReconnect = true
        }

        binding.buttonSaveCustomDns.setOnClickListener {
            settings.customDns = binding.editCustomDns.text.toString().trim()
            if (settings.dnsMode == "custom") {
                settings.dnsChangePendingReconnect = true
            }
            Toast.makeText(this, "Custom DNS saved", Toast.LENGTH_SHORT).show()
        }
    }
}
