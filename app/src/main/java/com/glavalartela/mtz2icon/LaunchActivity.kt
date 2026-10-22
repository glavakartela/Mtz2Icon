package com.glavalartela.mtz2icon

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle

/** Invisible trampoline: launches the target app like a regular launcher does, then finishes. */
class LaunchActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(ShortcutPinner.EXTRA_COMPONENT)
            ?.let { ComponentName.unflattenFromString(it) }
        if (target != null) {
            val launch = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                startActivity(launch)
            } catch (_: Exception) {
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
