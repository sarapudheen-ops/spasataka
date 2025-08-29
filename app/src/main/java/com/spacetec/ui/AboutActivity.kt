package com.spacetec.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.spacetec.R

class AboutActivity : AppCompatActivity() {

    private lateinit var tvVersion: TextView
    private lateinit var tvBuildInfo: TextView
    private lateinit var btnWebsite: Button
    private lateinit var btnContact: Button
    private lateinit var btnLicense: Button
    private lateinit var btnPrivacy: Button
    private lateinit var btnOpenSource: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setupToolbar()
        initializeViews()
        setupVersionInfo()
        setupEventListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "About"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        tvVersion = findViewById(R.id.tvVersion)
        tvBuildInfo = findViewById(R.id.tvBuildInfo)
        btnWebsite = findViewById(R.id.btnWebsite)
        btnContact = findViewById(R.id.btnContact)
        btnLicense = findViewById(R.id.btnLicense)
        btnPrivacy = findViewById(R.id.btnPrivacy)
        btnOpenSource = findViewById(R.id.btnOpenSource)
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            tvVersion.text = "Version $versionName"
            
            val buildInfo = """
                Build: $versionName ($versionCode)
                Build Date: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}
                Target SDK: ${packageInfo.applicationInfo.targetSdkVersion}
                Min SDK: 24
                Package: $packageName
            """.trimIndent()
            
            tvBuildInfo.text = buildInfo
            
        } catch (e: PackageManager.NameNotFoundException) {
            tvVersion.text = "Version 1.0.0"
            tvBuildInfo.text = "Build information unavailable"
        }
    }

    private fun setupEventListeners() {
        btnWebsite.setOnClickListener {
            openUrl("https://spacetec.com")
        }

        btnContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:info@spacetec.com")
                putExtra(Intent.EXTRA_SUBJECT, "SpaceTec Diagnostic Inquiry")
                putExtra(Intent.EXTRA_TEXT, "Hello SpaceTec Team,\n\n")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        btnLicense.setOnClickListener {
            showLicenseDialog()
        }

        btnPrivacy.setOnClickListener {
            openUrl("https://spacetec.com/privacy")
        }

        btnOpenSource.setOnClickListener {
            showOpenSourceDialog()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLicenseDialog() {
        val licenseText = """
            SpaceTec Diagnostic License Agreement
            
            Copyright (c) 2024 SpaceTec Solutions
            
            This software is licensed for use with compatible OBD-II diagnostic hardware only.
            
            IMPORTANT: ECU flashing and programming features should only be used by qualified automotive technicians. Improper use may damage vehicle electronics.
            
            The software is provided "AS IS" without warranty of any kind. Use at your own risk.
            
            For full license terms, visit: https://spacetec.com/license
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("License Agreement")
            .setMessage(licenseText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showOpenSourceDialog() {
        val openSourceText = """
            This application uses the following open source libraries:
            
            • Android Jetpack Libraries (Apache 2.0)
            • Material Design Components (Apache 2.0)
            • Kotlin Coroutines (Apache 2.0)
            • OkHttp (Apache 2.0)
            • Gson (Apache 2.0)
            
            Full license texts are available at:
            https://spacetec.com/opensource
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Open Source Licenses")
            .setMessage(openSourceText)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
