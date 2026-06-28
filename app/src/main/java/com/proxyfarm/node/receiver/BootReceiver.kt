package com.proxyfarm.node.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot receiver — registered in the manifest with RECEIVE_BOOT_COMPLETED.
 *
 * On-demand design philosophy:
 *   The proxy service is intentionally NOT auto-started on boot.
 *   The device should remain completely idle and cool until an FCM
 *   "START_PROXY" command arrives from the orchestration server.
 *
 *   This receiver exists solely to ensure Firebase re-registers the
 *   FCM token after a device reboot, so the orchestration server can
 *   still reach this node via push notifications.
 *
 * To enable auto-start on boot (not recommended for battery life),
 * uncomment the service start block below.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Boot/update received — node standing by for FCM commands")
                // Firebase SDK automatically refreshes the FCM token after boot.
                // No explicit action needed unless you maintain a custom token
                // registration flow with your orchestration server.

                // ── Optional: auto-start proxy on boot ────────────
                // Uncomment ONLY if your use-case requires the proxy
                // to be active immediately after device restart:
                //
                // val serviceIntent = Intent(
                //     context,
                //     com.proxyfarm.node.service.ProxyService::class.java
                // ).apply {
                //     action = ProxyService.ACTION_START
                //     putExtra(ProxyService.EXTRA_JOB_ID, "boot-autostart")
                // }
                // context.startForegroundService(serviceIntent)
            }
            else -> Log.w(TAG, "Unexpected action: ${intent.action}")
        }
    }
}