package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Test

class MainUiStateTest {

    @Test
    fun `default MainUiState has Initializing state`() {
        val state = MainUiState()
        assertEquals(RecordingState.Initializing, state.recordingState)
    }

    @Test
    fun `default MainUiState has empty transcript`() {
        val state = MainUiState()
        assertEquals(emptyList<TranscriptLine>(), state.transcriptLines)
    }

    @Test
    fun `default MainUiState has Moonshine engine selected`() {
        val state = MainUiState()
        assertEquals(EngineType.MOONSHINE, state.selectedEngine)
    }

    @Test
    fun `default MainUiState has all engines available`() {
        val state = MainUiState()
        assertEquals(EngineType.values().toList(), state.availableEngines)
    }

    @Test
    fun `MainUiState copy works correctly`() {
        val initial = MainUiState()
        val updated = initial.copy(
            recordingState = RecordingState.Recording,
            selectedEngine = EngineType.VOSK
        )

        assertEquals(RecordingState.Recording, updated.recordingState)
        assertEquals(EngineType.VOSK, updated.selectedEngine)
        // Original unchanged
        assertEquals(RecordingState.Initializing, initial.recordingState)
    }
}
