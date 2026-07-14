package com.neomods.tools

import android.app.Application
import com.neomods.tools.crash.CrashHandler
import com.neomods.tools.data.CategoryRepository
import com.neomods.tools.data.DefaultCategoryRepository
import com.neomods.tools.data.DefaultPermissionRepository
import com.neomods.tools.data.DefaultToolRepository
import com.neomods.tools.data.PermissionRepository
import com.neomods.tools.data.ToolRepository
import com.neomods.tools.onboarding.DefaultOnboardingRepository
import com.neomods.tools.onboarding.OnboardingRepository

class NeoToolsApplication : Application() {

    val categoryRepository: CategoryRepository by lazy { DefaultCategoryRepository() }
    val toolRepository: ToolRepository by lazy { DefaultToolRepository() }
    val permissionRepository: PermissionRepository by lazy { DefaultPermissionRepository() }
    val onboardingRepository: OnboardingRepository by lazy {
        DefaultOnboardingRepository(getSharedPreferences("neo_onboarding", MODE_PRIVATE))
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
