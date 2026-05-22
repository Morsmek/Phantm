package com.example.crypto

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import com.example.screens.parsePhantmSyncUri

/**
 * Manages NFC host-card-emulation-style push for Phantm contact exchange.
 * Uses Android Beam replacement: foreground dispatch + NDEF push via setNdefPushMessage.
 *
 * Works on Android 10+ without Android Beam via foreground NDEF dispatch.
 */
object PhantmNfc {

    fun isAvailable(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    fun isEnabled(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    /**
     * Enables NFC foreground dispatch so this activity receives NFC intents
     * before any other app when in foreground.
     */
    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val intent = android.content.Intent(activity, activity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            activity, 0, intent,
            android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableForegroundDispatch(activity)
    }

    /**
     * Builds an NDEF message carrying the phantm://sync URI.
     * The receiving device reads this message and parses the URI.
     */
    fun buildNdefMessage(publicKey: String, displayName: String): NdefMessage {
        val uri = try {
            "phantm://sync?key=$publicKey&name=${java.net.URLEncoder.encode(displayName, "UTF-8")}"
        } catch (e: Exception) {
            "phantm://sync?key=$publicKey&name=$displayName"
        }
        val record = NdefRecord.createUri(uri)
        return NdefMessage(arrayOf(record))
    }

    /**
     * Parses an incoming NFC NDEF intent and extracts the phantm://sync URI if present.
     * Returns Pair(publicKey, displayName) or null.
     */
    fun parseNfcIntent(intent: android.content.Intent): Pair<String, String>? {
        if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) return null

        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null

        for (raw in rawMessages) {
            val msg = raw as? NdefMessage ?: continue
            for (record in msg.records) {
                val payload = String(record.payload, Charsets.UTF_8)
                // NdefRecord URI payload has a 1-byte prefix for URI abbreviation code
                val uri = if (payload.isNotEmpty()) payload.drop(1) else payload
                val fullUri = if (uri.startsWith("phantm://")) uri
                             else "phantm://$uri"
                val result = parsePhantmSyncUri(fullUri)
                if (result != null) return result
            }
        }
        return null
    }
}
