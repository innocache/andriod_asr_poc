package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecordingStateTest {

    @Test
    fun `RecordingState Idle is singleton`() {
        val state1 = RecordingState.Idle
        val state2 = RecordingState.Idle
        assertEquals(state1, state2)
    }

    @Test
    fun `RecordingState Recording is singleton`() {
        val state1 = RecordingState.Recording
        val state2 = RecordingState.Recording
        assertEquals(state1, state2)
    }

    @Test
    fun `RecordingState Initializing is singleton`() {
        val state1 = RecordingState.Initializing
        val state2 = RecordingState.Initializing
        assertEquals(state1, state2)
    }

    @Test
    fun `RecordingState Error contains message`() {
        val errorMessage = "Test error message"
        val errorState = RecordingState.Error(errorMessage)
        assertEquals(errorMessage, errorState.message)
    }

    @Test
    fun `RecordingState Error equality based on message`() {
        val error1 = RecordingState.Error("error")
        val error2 = RecordingState.Error("error")
        val error3 = RecordingState.Error("different")

        assertEquals(error1, error2)
        assertNotEquals(error1, error3)
    }

    @Test
    fun `different states are not equal`() {
        assertNotEquals(RecordingState.Idle, RecordingState.Recording)
        assertNotEquals(RecordingState.Recording, RecordingState.Initializing)
        assertNotEquals(RecordingState.Idle, RecordingState.Error("error"))
    }
}
