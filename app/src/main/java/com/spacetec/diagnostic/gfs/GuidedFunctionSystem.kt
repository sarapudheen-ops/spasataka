package com.spacetec.diagnostic.gfs

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.json.JSONArray

/**
 * Guided Function System (GFS) - Interactive diagnostic workflows
 * Based on MaxiSys GFS implementation
 */
class GuidedFunctionSystem(private val context: Context) {
    
    private val tag = "GFS"
    private var webView: WebView? = null
    
    private val _currentStep = MutableStateFlow<GfsStep?>(null)
    val currentStep: StateFlow<GfsStep?> = _currentStep
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    data class GfsStep(
        val id: String,
        val title: String,
        val description: String,
        val type: StepType,
        val content: String,
        val actions: List<GfsAction>,
        val nextStepId: String? = null,
        val previousStepId: String? = null
    )
    
    data class GfsAction(
        val id: String,
        val label: String,
        val type: ActionType,
        val data: Map<String, Any> = emptyMap()
    )
    
    enum class StepType {
        INSTRUCTION,
        MEASUREMENT,
        SELECTION,
        CONFIRMATION,
        RESULT
    }
    
    enum class ActionType {
        NEXT,
        PREVIOUS,
        MEASURE,
        SELECT,
        CONFIRM,
        CANCEL,
        FINISH
    }
    
    fun initializeWebView(webView: WebView) {
        this.webView = webView
        setupWebView()
    }
    
    private fun setupWebView() {
        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            addJavascriptInterface(GfsJavaScriptInterface(), "GFS")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    _isLoading.value = false
                    Log.d("GFS", "GFS page loaded")
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    _error.value = "WebView error: $description"
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun startGuidedFunction(functionId: String, vehicleInfo: Map<String, String>) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val gfsData = loadGuidedFunction(functionId, vehicleInfo)
            if (gfsData != null) {
                loadGfsContent(gfsData)
            } else {
                _error.value = "Guided function not found: $functionId"
                _isLoading.value = false
            }
        } catch (e: Exception) {
            _error.value = "Failed to start guided function: ${e.message}"
            _isLoading.value = false
        }
    }
    
    private fun loadGuidedFunction(functionId: String, vehicleInfo: Map<String, String>): JSONObject? {
        return when (functionId) {
            "oil_reset" -> createOilResetGfs(vehicleInfo)
            "epb_reset" -> createEpbResetGfs(vehicleInfo)
            "sas_calibration" -> createSasCalibrationGfs(vehicleInfo)
            "dpf_regeneration" -> createDpfRegenerationGfs(vehicleInfo)
            "battery_registration" -> createBatteryRegistrationGfs(vehicleInfo)
            "headlamp_alignment" -> createHeadlampAlignmentGfs(vehicleInfo)
            "throttle_adaptation" -> createThrottleAdaptationGfs(vehicleInfo)
            else -> null
        }
    }
    
    private fun createOilResetGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "oil_reset")
            put("title", "Oil Service Reset")
            put("description", "Reset oil service interval for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Preparation", "Turn ignition ON, engine OFF", "INSTRUCTION"))
                put(createStep("step2", "Navigation", "Navigate to Service menu using steering wheel controls", "INSTRUCTION"))
                put(createStep("step3", "Selection", "Select 'Oil Service' from menu", "SELECTION"))
                put(createStep("step4", "Reset", "Confirm oil service reset", "CONFIRMATION"))
                put(createStep("step5", "Complete", "Oil service reset completed successfully", "RESULT"))
            })
        }
    }
    
    private fun createEpbResetGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "epb_reset")
            put("title", "Electronic Parking Brake Reset")
            put("description", "Reset EPB system for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Safety", "Ensure vehicle is on level ground with wheels chocked", "INSTRUCTION"))
                put(createStep("step2", "Ignition", "Turn ignition ON, engine OFF", "INSTRUCTION"))
                put(createStep("step3", "Brake", "Press and hold brake pedal", "INSTRUCTION"))
                put(createStep("step4", "EPB", "Activate EPB reset procedure", "MEASUREMENT"))
                put(createStep("step5", "Verify", "Verify EPB operation", "CONFIRMATION"))
                put(createStep("step6", "Complete", "EPB reset completed", "RESULT"))
            })
        }
    }
    
    private fun createSasCalibrationGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "sas_calibration")
            put("title", "Steering Angle Sensor Calibration")
            put("description", "Calibrate SAS for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Position", "Position vehicle on level surface", "INSTRUCTION"))
                put(createStep("step2", "Wheels", "Set front wheels straight ahead", "INSTRUCTION"))
                put(createStep("step3", "Ignition", "Turn ignition ON, engine running", "INSTRUCTION"))
                put(createStep("step4", "Calibrate", "Start SAS calibration procedure", "MEASUREMENT"))
                put(createStep("step5", "Test", "Perform steering test", "CONFIRMATION"))
                put(createStep("step6", "Complete", "SAS calibration completed", "RESULT"))
            })
        }
    }
    
    private fun createDpfRegenerationGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "dpf_regeneration")
            put("title", "DPF Regeneration")
            put("description", "Force DPF regeneration for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Conditions", "Check DPF regeneration conditions", "INSTRUCTION"))
                put(createStep("step2", "Temperature", "Ensure engine is at operating temperature", "MEASUREMENT"))
                put(createStep("step3", "Start", "Initiate forced DPF regeneration", "CONFIRMATION"))
                put(createStep("step4", "Monitor", "Monitor regeneration progress", "MEASUREMENT"))
                put(createStep("step5", "Complete", "DPF regeneration completed", "RESULT"))
            })
        }
    }
    
    private fun createBatteryRegistrationGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "battery_registration")
            put("title", "Battery Registration")
            put("description", "Register new battery for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Battery", "Install new battery", "INSTRUCTION"))
                put(createStep("step2", "Specs", "Enter battery specifications", "SELECTION"))
                put(createStep("step3", "Register", "Register battery with ECU", "CONFIRMATION"))
                put(createStep("step4", "Verify", "Verify battery registration", "MEASUREMENT"))
                put(createStep("step5", "Complete", "Battery registration completed", "RESULT"))
            })
        }
    }
    
    private fun createHeadlampAlignmentGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "headlamp_alignment")
            put("title", "Headlamp Alignment")
            put("description", "Align headlamps for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Setup", "Position vehicle 25 feet from wall", "INSTRUCTION"))
                put(createStep("step2", "Load", "Ensure proper vehicle loading", "INSTRUCTION"))
                put(createStep("step3", "Lights", "Turn on headlights", "INSTRUCTION"))
                put(createStep("step4", "Adjust", "Adjust headlamp alignment", "MEASUREMENT"))
                put(createStep("step5", "Verify", "Verify alignment pattern", "CONFIRMATION"))
                put(createStep("step6", "Complete", "Headlamp alignment completed", "RESULT"))
            })
        }
    }
    
    private fun createThrottleAdaptationGfs(vehicleInfo: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("id", "throttle_adaptation")
            put("title", "Throttle Body Adaptation")
            put("description", "Adapt throttle body for ${vehicleInfo["make"]} ${vehicleInfo["model"]}")
            put("steps", JSONArray().apply {
                put(createStep("step1", "Engine", "Engine at operating temperature", "INSTRUCTION"))
                put(createStep("step2", "Idle", "Let engine idle for 2 minutes", "INSTRUCTION"))
                put(createStep("step3", "Throttle", "Start throttle adaptation", "MEASUREMENT"))
                put(createStep("step4", "Learn", "Throttle learning process", "MEASUREMENT"))
                put(createStep("step5", "Test", "Test throttle response", "CONFIRMATION"))
                put(createStep("step6", "Complete", "Throttle adaptation completed", "RESULT"))
            })
        }
    }
    
    private fun createStep(id: String, title: String, description: String, type: String): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("description", description)
            put("type", type)
            put("actions", JSONArray().apply {
                when (type) {
                    "INSTRUCTION" -> {
                        put(createAction("next", "Next", "NEXT"))
                        put(createAction("previous", "Previous", "PREVIOUS"))
                    }
                    "MEASUREMENT" -> {
                        put(createAction("measure", "Start Measurement", "MEASURE"))
                        put(createAction("cancel", "Cancel", "CANCEL"))
                    }
                    "SELECTION" -> {
                        put(createAction("select", "Select", "SELECT"))
                        put(createAction("cancel", "Cancel", "CANCEL"))
                    }
                    "CONFIRMATION" -> {
                        put(createAction("confirm", "Confirm", "CONFIRM"))
                        put(createAction("cancel", "Cancel", "CANCEL"))
                    }
                    "RESULT" -> {
                        put(createAction("finish", "Finish", "FINISH"))
                    }
                }
            })
        }
    }
    
    private fun createAction(id: String, label: String, type: String): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("type", type)
        }
    }
    
    private fun loadGfsContent(gfsData: JSONObject) {
        val htmlContent = generateGfsHtml(gfsData)
        webView?.loadDataWithBaseURL(
            "file:///android_asset/",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }
    
    private fun generateGfsHtml(gfsData: JSONObject): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Guided Function System</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: #333;
                    min-height: 100vh;
                }
                .gfs-container {
                    max-width: 800px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    overflow: hidden;
                }
                .gfs-header {
                    background: linear-gradient(45deg, #1e3c72, #2a5298);
                    color: white;
                    padding: 20px;
                    text-align: center;
                }
                .gfs-title {
                    font-size: 24px;
                    font-weight: bold;
                    margin: 0;
                }
                .gfs-description {
                    font-size: 14px;
                    opacity: 0.9;
                    margin-top: 8px;
                }
                .gfs-content {
                    padding: 30px;
                }
                .step {
                    display: none;
                    animation: fadeIn 0.5s ease-in;
                }
                .step.active {
                    display: block;
                }
                .step-header {
                    border-bottom: 2px solid #e0e0e0;
                    padding-bottom: 15px;
                    margin-bottom: 20px;
                }
                .step-title {
                    font-size: 20px;
                    font-weight: bold;
                    color: #1e3c72;
                    margin: 0;
                }
                .step-description {
                    font-size: 16px;
                    color: #666;
                    margin-top: 8px;
                    line-height: 1.5;
                }
                .step-actions {
                    margin-top: 30px;
                    text-align: center;
                }
                .gfs-button {
                    background: linear-gradient(45deg, #667eea, #764ba2);
                    color: white;
                    border: none;
                    padding: 12px 24px;
                    border-radius: 6px;
                    font-size: 16px;
                    font-weight: bold;
                    cursor: pointer;
                    margin: 0 10px;
                    transition: all 0.3s ease;
                }
                .gfs-button:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 5px 15px rgba(0,0,0,0.2);
                }
                .gfs-button.secondary {
                    background: #6c757d;
                }
                .progress-bar {
                    height: 4px;
                    background: #e0e0e0;
                    margin: 20px 0;
                    border-radius: 2px;
                    overflow: hidden;
                }
                .progress-fill {
                    height: 100%;
                    background: linear-gradient(45deg, #667eea, #764ba2);
                    transition: width 0.3s ease;
                }
                .measurement-panel {
                    background: #f8f9fa;
                    border: 2px solid #dee2e6;
                    border-radius: 8px;
                    padding: 20px;
                    margin: 20px 0;
                    text-align: center;
                }
                .measurement-value {
                    font-size: 24px;
                    font-weight: bold;
                    color: #1e3c72;
                    margin: 10px 0;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
            </style>
        </head>
        <body>
            <div class="gfs-container">
                <div class="gfs-header">
                    <h1 class="gfs-title">${gfsData.getString("title")}</h1>
                    <p class="gfs-description">${gfsData.getString("description")}</p>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" id="progressFill" style="width: 0%"></div>
                </div>
                <div class="gfs-content" id="gfsContent">
                    <!-- Steps will be dynamically loaded here -->
                </div>
            </div>
            
            <script>
                const gfsData = ${gfsData};
                let currentStepIndex = 0;
                
                function loadStep(stepIndex) {
                    const steps = gfsData.steps;
                    if (stepIndex < 0 || stepIndex >= steps.length) return;
                    
                    const step = steps[stepIndex];
                    const progress = ((stepIndex + 1) / steps.length) * 100;
                    
                    document.getElementById('progressFill').style.width = progress + '%';
                    
                    const content = document.getElementById('gfsContent');
                    content.innerHTML = generateStepHtml(step, stepIndex);
                    
                    // Notify Android
                    if (window.GFS) {
                        window.GFS.onStepChanged(JSON.stringify(step));
                    }
                }
                
                function generateStepHtml(step, stepIndex) {
                    let html = '<div class="step active">';
                    html += '<div class="step-header">';
                    html += '<h2 class="step-title">' + step.title + '</h2>';
                    html += '<p class="step-description">' + step.description + '</p>';
                    html += '</div>';
                    
                    if (step.type === 'MEASUREMENT') {
                        html += '<div class="measurement-panel">';
                        html += '<div>Measurement in progress...</div>';
                        html += '<div class="measurement-value" id="measurementValue">--</div>';
                        html += '</div>';
                    }
                    
                    html += '<div class="step-actions">';
                    step.actions.forEach(action => {
                        const buttonClass = action.type === 'NEXT' || action.type === 'CONFIRM' || action.type === 'FINISH' ? 'gfs-button' : 'gfs-button secondary';
                        html += '<button class="' + buttonClass + '" onclick="handleAction(\'' + action.type + '\')">' + action.label + '</button>';
                    });
                    html += '</div>';
                    html += '</div>';
                    
                    return html;
                }
                
                function handleAction(actionType) {
                    switch (actionType) {
                        case 'NEXT':
                            if (currentStepIndex < gfsData.steps.length - 1) {
                                currentStepIndex++;
                                loadStep(currentStepIndex);
                            }
                            break;
                        case 'PREVIOUS':
                            if (currentStepIndex > 0) {
                                currentStepIndex--;
                                loadStep(currentStepIndex);
                            }
                            break;
                        case 'MEASURE':
                            startMeasurement();
                            break;
                        case 'CONFIRM':
                            if (currentStepIndex < gfsData.steps.length - 1) {
                                currentStepIndex++;
                                loadStep(currentStepIndex);
                            }
                            break;
                        case 'FINISH':
                            if (window.GFS) {
                                window.GFS.onGfsCompleted();
                            }
                            break;
                        case 'CANCEL':
                            if (window.GFS) {
                                window.GFS.onGfsCancelled();
                            }
                            break;
                    }
                }
                
                function startMeasurement() {
                    if (window.GFS) {
                        window.GFS.startMeasurement();
                    }
                    
                    // Simulate measurement progress
                    let progress = 0;
                    const interval = setInterval(() => {
                        progress += 10;
                        const valueElement = document.getElementById('measurementValue');
                        if (valueElement) {
                            valueElement.textContent = progress + '%';
                        }
                        
                        if (progress >= 100) {
                            clearInterval(interval);
                            setTimeout(() => {
                                if (currentStepIndex < gfsData.steps.length - 1) {
                                    currentStepIndex++;
                                    loadStep(currentStepIndex);
                                }
                            }, 1000);
                        }
                    }, 500);
                }
                
                // Initialize first step
                loadStep(0);
            </script>
        </body>
        </html>
        """.trimIndent()
    }
    
    inner class GfsJavaScriptInterface {
        @JavascriptInterface
        fun onStepChanged(stepJson: String) {
            try {
                val stepData = JSONObject(stepJson)
                val step = GfsStep(
                    id = stepData.getString("id"),
                    title = stepData.getString("title"),
                    description = stepData.getString("description"),
                    type = StepType.valueOf(stepData.getString("type")),
                    content = stepData.toString(),
                    actions = parseActions(stepData.getJSONArray("actions"))
                )
                _currentStep.value = step
            } catch (e: Exception) {
                Log.e(tag, "Error parsing step data", e)
            }
        }
        
        @JavascriptInterface
        fun startMeasurement() {
            Log.d(tag, "Starting measurement...")
            // Implement actual measurement logic here
        }
        
        @JavascriptInterface
        fun onGfsCompleted() {
            Log.d(tag, "GFS completed")
            _currentStep.value = null
        }
        
        @JavascriptInterface
        fun onGfsCancelled() {
            Log.d(tag, "GFS cancelled")
            _currentStep.value = null
            _error.value = "Guided function cancelled"
        }
        
        private fun parseActions(actionsArray: JSONArray): List<GfsAction> {
            val actions = mutableListOf<GfsAction>()
            for (i in 0 until actionsArray.length()) {
                val actionObj = actionsArray.getJSONObject(i)
                actions.add(
                    GfsAction(
                        id = actionObj.getString("id"),
                        label = actionObj.getString("label"),
                        type = ActionType.valueOf(actionObj.getString("type"))
                    )
                )
            }
            return actions
        }
    }
}
