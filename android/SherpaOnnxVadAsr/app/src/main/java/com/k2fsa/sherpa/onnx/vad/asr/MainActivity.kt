package com.k2fsa.sherpa.onnx.vad.asr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.k2fsa.sherpa.onnx.R
import kotlinx.coroutines.launch

private const val TAG = "sherpa-onnx"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SKIP_NATIVE_INIT = "com.k2fsa.sherpa.onnx.extra.SKIP_NATIVE_INIT"
        const val EXTRA_FORCE_ASR_ENGINE = "com.k2fsa.sherpa.onnx.extra.FORCE_ASR_ENGINE"
    }

    private lateinit var recordButton: Button
    private lateinit var textView: TextView
    private lateinit var engineSpinner: Spinner
    private lateinit var audioLevelBar: ProgressBar
    private lateinit var statsText: TextView
    private lateinit var clearButton: ImageButton
    private lateinit var shareButton: ImageButton

    private val viewModel: MainViewModel by viewModels()

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {
                Log.i(TAG, "Audio record permission granted")
                viewModel.initialize()
            } else {
                Log.e(TAG, "Audio record permission denied")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val skipNativeInit = intent?.getBooleanExtra(EXTRA_SKIP_NATIVE_INIT, false) ?: false
        val forcedEngineRaw = intent?.getStringExtra(EXTRA_FORCE_ASR_ENGINE)

        // Initialize views
        recordButton = findViewById(R.id.record_button)
        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()
        engineSpinner = findViewById(R.id.engine_spinner)
        audioLevelBar = findViewById(R.id.audio_level)
        statsText = findViewById(R.id.stats_text)
        clearButton = findViewById(R.id.clear_button)
        shareButton = findViewById(R.id.share_button)

        // Handle forced engine from intent
        if (!forcedEngineRaw.isNullOrBlank()) {
            val forced = runCatching {
                EngineType.valueOf(forcedEngineRaw.trim().uppercase())
            }.getOrNull()

            if (forced != null) {
                viewModel.forceEngine(forced)
                Log.i(TAG, "Forced ASR engine via intent: ${forced.displayName}")
            } else {
                Log.w(TAG, "Unknown forced engine: $forcedEngineRaw")
            }
        }

        setupEngineSpinner()
        setupRecordButton()
        setupActionButtons()
        observeUiState()

        if (!skipNativeInit) {
            requestAudioPermissionAndInitialize()
        } else {
            Log.w(TAG, "Skipping native init (instrumentation/test mode)")
        }
    }

    private fun setupEngineSpinner() {
        val labels = EngineType.values().map { it.displayName }
        engineSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ArrayList(labels)
        )

        engineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedType = EngineType.values().getOrNull(position) ?: return
                viewModel.selectEngine(selectedType)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupActionButtons() {
        clearButton.setOnClickListener {
            viewModel.clearTranscript()
        }

        shareButton.setOnClickListener {
            val intent = viewModel.createShareIntent()
            if (intent != null) {
                startActivity(intent)
            }
        }
    }

    private fun setupRecordButton() {
        recordButton.setOnClickListener {
            if (AudioRecorder.hasPermission(this)) {
                @Suppress("MissingPermission")
                viewModel.toggleRecording()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_RECORD_AUDIO_PERMISSION
                )
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: MainUiState) {
        // Update spinner selection
        val spinnerPosition = state.selectedEngine.ordinal
        if (engineSpinner.selectedItemPosition != spinnerPosition) {
            engineSpinner.setSelection(spinnerPosition)
        }

        // Update audio level bar
        val isRecording = state.recordingState == RecordingState.Recording
        audioLevelBar.visibility = if (isRecording) View.VISIBLE else View.GONE
        audioLevelBar.progress = (state.audioLevel * 100).toInt()

        // Update stats text
        statsText.text = if (state.transcriptLines.isNotEmpty()) {
            state.sessionStats.formatted()
        } else {
            getString(R.string.no_stats)
        }

        // Update action buttons visibility
        val hasTranscript = state.transcriptLines.isNotEmpty()
        clearButton.visibility = if (hasTranscript && !isRecording) View.VISIBLE else View.GONE
        shareButton.visibility = if (hasTranscript && !isRecording) View.VISIBLE else View.GONE

        // Update button text and state
        when (state.recordingState) {
            is RecordingState.Initializing -> {
                recordButton.setText(R.string.start)
                recordButton.isEnabled = false
                engineSpinner.isEnabled = false
            }
            is RecordingState.Idle -> {
                recordButton.setText(R.string.start)
                recordButton.isEnabled = true
                engineSpinner.isEnabled = true
            }
            is RecordingState.Recording -> {
                recordButton.setText(R.string.stop)
                recordButton.isEnabled = true
                engineSpinner.isEnabled = false
            }
            is RecordingState.Error -> {
                recordButton.setText(R.string.start)
                recordButton.isEnabled = true
                engineSpinner.isEnabled = true
            }
        }

        // Update transcript text
        val text = when (val recordingState = state.recordingState) {
            is RecordingState.Error -> recordingState.message
            else -> state.transcriptLines.joinToString("\n") { it.formatted() }
        }
        textView.text = text.ifEmpty { getString(R.string.hint) }
    }

    private fun requestAudioPermissionAndInitialize() {
        if (AudioRecorder.hasPermission(this)) {
            viewModel.initialize()
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }
}
