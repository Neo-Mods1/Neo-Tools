package com.neomods.tools.onboarding

import android.content.SharedPreferences

/**
 * Tracks which onboarding steps the user has already completed so the app can
 * skip them on subsequent launches and go straight to the Home screen.
 */
interface OnboardingRepository {
    fun isWelcomeSeen(): Boolean
    fun setWelcomeSeen()
    fun isCrashOptInDecided(): Boolean
    fun setCrashOptInDecided()
}

internal class DefaultOnboardingRepository(
    private val prefs: SharedPreferences
) : OnboardingRepository {

    override fun isWelcomeSeen(): Boolean = prefs.getBoolean(KEY_WELCOME_SEEN, false)

    override fun setWelcomeSeen() {
        prefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply()
    }

    override fun isCrashOptInDecided(): Boolean =
        prefs.getBoolean(KEY_CRASH_OPT_IN_DECIDED, false)

    override fun setCrashOptInDecided() {
        prefs.edit().putBoolean(KEY_CRASH_OPT_IN_DECIDED, true).apply()
    }

    private companion object {
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_CRASH_OPT_IN_DECIDED = "crash_opt_in_decided"
    }
}
