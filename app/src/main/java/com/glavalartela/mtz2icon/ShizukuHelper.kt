package com.glavalartela.mtz2icon

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import rikka.shizuku.Shizuku
import kotlin.system.exitProcess

/** Runs in a separate process with shell privileges (like `adb shell`); started by Shizuku. */
class UserService : IUserService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String = try {
        val p = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.trim()
    } catch (e: Exception) {
        "error: ${e.message}"
    }
}

object ShizukuHelper {
    private const val REQUEST_CODE = 1001

    enum class Connect { NOT_RUNNING, ASKED, CONNECTING, CONNECTED }

    @Volatile
    private var service: IUserService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = if (binder != null && binder.pingBinder()) IUserService.Stub.asInterface(binder) else null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private fun args(ctx: Context) =
        Shizuku.UserServiceArgs(ComponentName(ctx.packageName, UserService::class.java.name))
            .processNameSuffix("shell")
            .daemon(false)
            .version(1)

    fun isRunning(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun hasPermission(): Boolean =
        isRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun isConnected(): Boolean = service != null

    fun connect(ctx: Context): Connect {
        if (!isRunning()) return Connect.NOT_RUNNING
        if (!hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
            return Connect.ASKED
        }
        if (service != null) return Connect.CONNECTED
        Shizuku.bindUserService(args(ctx), connection)
        return Connect.CONNECTING
    }

    /** Must not be called on the main thread. */
    fun exec(command: String): String = service?.exec(command) ?: "Shizuku service not connected"

    /** Xiaomi: the "Home screen shortcuts" permission is appop 10017. True if it ended up allowed. */
    fun grantShortcutPermission(ctx: Context): Boolean {
        val pkg = ctx.packageName
        val out = exec("appops set $pkg 10017 allow; appops get $pkg 10017")
        return out.contains("allow", ignoreCase = true)
    }

    fun openSettings(ctx: Context) {
        val pkg = ctx.packageName
        val miui = Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", pkg)
        try {
            ctx.startActivity(miui)
        } catch (_: Exception) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)))
        }
    }
}
