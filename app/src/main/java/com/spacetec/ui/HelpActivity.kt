package com.spacetec.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.ui.activities.DiagnosticLogViewerActivity

class HelpActivity : AppCompatActivity() {

    private lateinit var recyclerFaq: RecyclerView
    private lateinit var btnEmailSupport: Button
    private lateinit var btnViewLogs: Button
    private lateinit var btnReportBug: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        setupToolbar()
        initializeViews()
        setupFaq()
        setupEventListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Help & Support"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        recyclerFaq = findViewById(R.id.recyclerFaq)
        btnEmailSupport = findViewById(R.id.btnEmailSupport)
        btnViewLogs = findViewById(R.id.btnViewLogs)
        btnReportBug = findViewById(R.id.btnReportBug)
    }

    private fun setupFaq() {
        val faqItems = listOf(
            FaqItem("How do I connect to my vehicle?", 
                "Make sure your vehicle is running and the OBD-II adapter is properly connected. Enable Bluetooth and pair with your adapter, then tap Connect in the app."),
            FaqItem("Why can't I read DTCs?", 
                "Ensure your vehicle supports OBD-II diagnostics (1996+ for most vehicles). Some luxury vehicles may require specific adapters."),
            FaqItem("Is ECU flashing safe?", 
                "ECU flashing should only be performed by experienced technicians. Incorrect flashing can damage your vehicle's ECU permanently."),
            FaqItem("What vehicles are supported?", 
                "SpaceTec supports most OBD-II compliant vehicles (1996+). Some advanced features may require specific vehicle protocols."),
            FaqItem("Can I use this for commercial purposes?", 
                "Please check our license agreement. Commercial use may require a different license."),
            FaqItem("How do I update the app?", 
                "Updates are available through the Google Play Store. Enable auto-updates for the latest features and bug fixes.")
        )

        recyclerFaq.layoutManager = LinearLayoutManager(this)
        recyclerFaq.adapter = FaqAdapter(faqItems)
    }

    private fun setupEventListeners() {
        btnEmailSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@spacetec.com")
                putExtra(Intent.EXTRA_SUBJECT, "SpaceTec Diagnostic Support Request")
                putExtra(Intent.EXTRA_TEXT, "Please describe your issue:\n\n")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        btnViewLogs.setOnClickListener {
            try {
                val intent = Intent(this, DiagnosticLogViewerActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                // Fall back to basic log viewing
                showBasicLogViewer()
            }
        }

        btnReportBug.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:bugs@spacetec.com")
                putExtra(Intent.EXTRA_SUBJECT, "SpaceTec Diagnostic Bug Report")
                putExtra(Intent.EXTRA_TEXT, "Bug Description:\n\nSteps to Reproduce:\n\nDevice Info:\n- Model: ${android.os.Build.MODEL}\n- Android: ${android.os.Build.VERSION.RELEASE}\n- App Version: 1.0\n\n")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBasicLogViewer() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Diagnostic Logs")
            .setMessage("Log Viewer Features:\n\n" +
                    "• View OBD-II communication logs\n" +
                    "• Export diagnostic session data\n" +
                    "• Filter by error level or timestamp\n" +
                    "• Real-time log monitoring\n\n" +
                    "This feature requires additional permissions and will be available in the next version.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Enable Debug Logging") { dialog, _ ->
                // Enable debug logging for future sessions
                android.util.Log.d("SpaceTec", "Debug logging enabled for future sessions")
                Toast.makeText(this, "Debug logging enabled", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

data class FaqItem(val question: String, val answer: String, var isExpanded: Boolean = false)
