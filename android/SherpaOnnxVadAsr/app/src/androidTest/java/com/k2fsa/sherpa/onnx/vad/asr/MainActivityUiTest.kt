package com.k2fsa.sherpa.onnx.vad.asr

import android.Manifest
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.k2fsa.sherpa.onnx.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityUiTest {

    @get:Rule
    val grantAudioPermission: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO
    )

    @get:Rule
    val scenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule<MainActivity>(
        Intent(
            ApplicationProvider.getApplicationContext<android.content.Context>(),
            MainActivity::class.java
        ).putExtra(MainActivity.EXTRA_SKIP_NATIVE_INIT, true)
    )

    @Test
    fun showsRecordButtonAndTextView() {
        scenarioRule.scenario.onActivity { activity ->
            val recordButton = activity.findViewById<Button>(R.id.record_button)
            val textView = activity.findViewById<TextView>(R.id.my_text)

            assertNotNull(recordButton)
            assertNotNull(textView)
            assertEquals(activity.getString(R.string.start), recordButton.text.toString())
        }
    }
}
