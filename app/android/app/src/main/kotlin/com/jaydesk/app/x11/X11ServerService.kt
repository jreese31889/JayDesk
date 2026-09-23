package com.jaydesk.app.x11

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.Log
import com.termux.x11.CmdEntryPoint
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * Owns the native X server in the dedicated :x11 process.
 *
 * CmdEntryPoint must never be referenced by an Activity or Flutter platform view. The only
 * objects crossing back to the UI process are ParcelFileDescriptors marshalled by Binder.
 */
class X11ServerService : Service() {
    private lateinit var serverThread: HandlerThread
    private lateinit var serverHandler: Handler

    private val stateLock = Any()
    private var startLatch: CountDownLatch? = null
    @Volatile private var started = false
    @Volatile private var startSucceeded = false
    private val cmdEntryPoint = CmdEntryPoint()

    private val binder = object : IX11Service.Stub() {
        override fun startServer(): Boolean = ensureServerStarted()

        override fun getXConnection(): ParcelFileDescriptor? {
            if (!ensureServerStarted()) return null
            return cmdEntryPoint.xConnection
        }

        override fun getLogcatOutput(): ParcelFileDescriptor? {
            if (!ensureServerStarted()) return null
            return cmdEntryPoint.logcatOutput
        }
    }

    override fun onCreate() {
        super.onCreate()
        serverThread = HandlerThread("X11Server")
        serverThread.start()
        serverHandler = Handler(serverThread.looper)
        Log.i(TAG, "X11 service created in pid=${android.os.Process.myPid()}")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The Linux foreground service owns user-visible session persistence.
        // Do not resurrect X11 by itself after Stop Server or an app shutdown.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Stopping dedicated X11 service process")
        serverThread.quitSafely()
        super.onDestroy()
        // libXlorie owns native threads that are not stopped by destroying the
        // Android Service object. This process hosts X11 only, so terminate it
        // to guarantee the next launch receives a clean server and socket.
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun ensureServerStarted(): Boolean {
        if (started) return startSucceeded

        val latch: CountDownLatch
        synchronized(stateLock) {
            if (started) return startSucceeded
            val existingLatch = startLatch
            if (existingLatch != null) {
                latch = existingLatch
            } else {
                latch = CountDownLatch(1)
                startLatch = latch
                serverHandler.post {
                    startSucceeded = try {
                        configureEnvironment()
                        Log.i(TAG, "Starting native X server in pid=${android.os.Process.myPid()}")
                        CmdEntryPoint.start(arrayOf(":0", "-nolock"))
                    } catch (error: Throwable) {
                        Log.e(TAG, "Native X server failed to start", error)
                        false
                    } finally {
                        started = true
                        latch.countDown()
                    }
                    if (!startSucceeded) captureServerFailureLog()
                    Log.i(TAG, "Native X server start result=$startSucceeded")
                }
            }
        }

        try {
            latch.await()
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            return false
        }
        return startSucceeded
    }

    /**
     * The xkeyboard-config package installs usr/share/X11/xkb as an absolute
     * symlink to Termux's original prefix, which does not exist here. The X
     * server in this :x11 process has no path-rewriting hook, so repair the
     * symlink before the server starts.
     */
    private fun repairXkbSymlink() {
        val xkbLink = File(filesDir, "usr/share/X11/xkb")
        if (xkbLink.exists()) return  // correct link or real directory already
        val xkbData = File(filesDir, "usr/share/xkeyboard-config-2")
        if (!xkbData.isDirectory) return  // nothing to point at
        try {
            val target = Os.readlink(xkbLink.absolutePath)
            val termuxPrefix = "/data/data/com.termux/files/usr"
            if (target.startsWith(termuxPrefix)) {
                val newTarget = File(filesDir, "usr").absolutePath +
                    target.substring(termuxPrefix.length)
                if (File(newTarget).exists()) {
                    xkbLink.delete()
                    Os.symlink(newTarget, xkbLink.absolutePath)
                    Log.i(TAG, "Repaired XKB symlink to $newTarget")
                } else {
                    Log.w(TAG, "XKB target missing even after rewrite: $newTarget")
                }
            }
        } catch (e: Exception) {
            // readlink throws when the link is absent entirely — create it so
            // conventional consumers (and libXlorie's chroot-case fallback,
            // which resolves dirname(TMPDIR)/usr/share/X11/xkb) find the data.
            try {
                xkbLink.parentFile?.mkdirs()
                Os.symlink(xkbData.absolutePath, xkbLink.absolutePath)
                Log.i(TAG, "Created XKB symlink -> ${xkbData.absolutePath}")
            } catch (e2: Exception) {
                Log.w(TAG, "Could not create XKB symlink: ${e2.message}")
            }
        }
    }

    /**
     * When the native X server fails, its stderr and the :x11 process logcat
     * hold the real reason (missing xkb files, socket errors, linker failures).
     * Copy them into app files so the failure is diagnosable from the UI side,
     * where the loading screen can surface it.
     */
    private fun captureServerFailureLog() {
        try {
            // Written inside usr/ so the in-app terminal (cwd = usr/) can read it
            // with a plain `cat x11-server-failure.log`.
            val logFile = File(filesDir, "usr/x11-server-failure.log")
            val process = ProcessBuilder("logcat", "-d", "--pid=${android.os.Process.myPid()}")
                .redirectErrorStream(true)
                .start()
            val text = process.inputStream.bufferedReader().readText()
            logFile.writeText(text.takeLast(20_000))
            Log.w(TAG, "Saved X11 failure log to ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not capture X11 failure log: ${e.message}")
        }
    }

    private fun configureEnvironment() {
        val appTmpDir = File(filesDir, "tmp").apply { mkdirs() }
        File(appTmpDir, ".X11-unix").mkdirs()

        Os.setenv("TMPDIR", appTmpDir.absolutePath, true)
        Os.setenv("XDG_RUNTIME_DIR", appTmpDir.absolutePath, true)
        Os.setenv("PREFIX", File(filesDir, "usr").absolutePath, true)
        Os.setenv("HOME", filesDir.absolutePath, true)

        repairXkbSymlink()

        // The xkb data itself lives at usr/share/xkeyboard-config-2 (installed
        // by the xkeyboard-config package). usr/share/X11/xkb is only a symlink
        // to it that dpkg may extract as a dangling absolute link or not at
        // all, and libXlorie's own fallbacks check paths that never exist in
        // this app (/usr/share/..., /data/data/com.termux/...). Prefer the real
        // data directory directly so the X server always finds its keymaps.
        val xkbDataDir = File(filesDir, "usr/share/xkeyboard-config-2")
        val installedXkbRoot = File(filesDir, "usr/share/X11/xkb")
        val rootfsXkbRoot = File(filesDir, "rootfs/usr/share/X11/xkb")
        val xkbRoot = when {
            xkbDataDir.isDirectory -> xkbDataDir
            installedXkbRoot.exists() -> installedXkbRoot
            rootfsXkbRoot.exists() -> rootfsXkbRoot
            else -> null
        }
        if (xkbRoot != null) {
            Os.setenv("XKB_CONFIG_ROOT", xkbRoot.absolutePath, true)
            Log.i(TAG, "XKB_CONFIG_ROOT=${xkbRoot.absolutePath}")
        } else {
            Log.w(TAG, "XKB config root not found — X server will fail to start")
        }

        val staleSocket = File(appTmpDir, ".X11-unix/X0")
        if (staleSocket.exists() && !staleSocket.delete()) {
            Log.w(TAG, "Could not remove stale X11 socket ${staleSocket.absolutePath}")
        }
    }

    private companion object {
        const val TAG = "X11ServerService"
    }
}
