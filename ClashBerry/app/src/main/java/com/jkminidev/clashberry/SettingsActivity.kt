package com.jkminidev.clashberry

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.jkminidev.clashberry.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = getSharedPreferences("clashberry_prefs", Context.MODE_PRIVATE)
        
        setupUI()
        setupClickListeners()
        loadAppVersion()
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupClickListeners() {
        // Theme setting
        binding.themeOption.setOnClickListener {
            showThemeDialog()
        }
        
        // Update setting
        binding.updateOption.setOnClickListener {
            checkForUpdates()
        }
        
        // Notifications setting
        binding.notificationsOption.setOnClickListener {
            toggleNotifications()
        }
        
        // Social links
        binding.githubIcon.setOnClickListener {
            openUrl("https://github.com") // Update with your GitHub link
        }
        
        binding.discordIcon.setOnClickListener {
            openUrl("https://discord.com") // Update with your Discord link
        }
        
        binding.patreonIcon.setOnClickListener {
            openUrl("https://patreon.com") // Update with your Patreon link
        }
    }
    
    private fun showThemeDialog() {
        val themes = arrayOf("Dark", "Light", "System Default")
        val currentTheme = preferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val selectedIndex = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            else -> 2
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Theme")
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val newTheme = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_YES
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                
                preferences.edit().putInt("theme", newTheme).apply()
                AppCompatDelegate.setDefaultNightMode(newTheme)
                dialog.dismiss()
            }
            .show()
    }
    
    private fun checkForUpdates() {
        // Implement update checking logic
        Toast.makeText(this, getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleNotifications() {
        val notificationsEnabled = preferences.getBoolean("notifications_enabled", false)
        val newState = !notificationsEnabled
        preferences.edit().putBoolean("notifications_enabled", newState).apply()
        
        val message = if (newState) {
            getString(R.string.notification_enabled)
        } else {
            getString(R.string.notification_disabled)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    
    private fun loadAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val version = "${packageInfo.versionName} (${packageInfo.versionCode})"
            binding.appVersion.text = "Version $version"
        } catch (e: Exception) {
            binding.appVersion.text = "Version 1.0"
        }
    }
}