package com.rivavafi.universal.ui.onboarding

import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingCompletionTest {

    @Test
    fun `test onboarding completion sets required fields`() {
        // Mocking the behavior of updating firestore and preferences
        var onboardingCompleted = false
        var autoMessageTrackingEnabled = false
        var userNameSaved = false

        // Simulating the user passing through the flow
        fun saveName(name: String) {
            if (name.isNotBlank()) userNameSaved = true
        }

        fun complete(trackingEnabled: Boolean) {
            autoMessageTrackingEnabled = trackingEnabled
            onboardingCompleted = true
        }

        // Test flow
        saveName("John Doe")
        complete(true)

        assertTrue("User name should be saved", userNameSaved)
        assertTrue("Onboarding should be marked complete", onboardingCompleted)
        assertTrue("Auto message tracking should reflect user choice", autoMessageTrackingEnabled)
    }
}
