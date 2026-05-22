package com.example.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

object PhantmLinkCode {

    // Base32 alphabet — uppercase, no padding, no ambiguous chars (0/O 1/I removed → custom)
    private val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Derives a short 8-char link code from a public key, valid for one 10-minute window.
     * Both devices can independently derive the same code without any server.
     *
     * @param publicKey  64-char hex Ed25519 public key
     * @param windowOverride  optional override for testing; pass null to use current time
     */
    fun generate(publicKey: String, windowOverride: Long? = null): String {
        val window = windowOverride ?: (System.currentTimeMillis() / 1000L / 600L)
        val password = publicKey.toByteArray(Charsets.UTF_8)
        val salt = window.toString().toByteArray(Charsets.UTF_8)

        val generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(password, salt, 1000)
        val keyParam = generator.generateDerivedParameters(64) as KeyParameter
        val bytes = keyParam.key // 8 bytes

        // Encode 8 bytes → 8 Base32 chars (5 bits each, 40 bits from first 5 bytes)
        val raw = buildString {
            var bits = 0L
            var bitCount = 0
            for (i in 0 until 5) {
                bits = (bits shl 8) or (bytes[i].toLong() and 0xFF)
                bitCount += 8
                while (bitCount >= 5) {
                    bitCount -= 5
                    append(ALPHABET[((bits shr bitCount) and 0x1F).toInt()])
                }
            }
        }
        // Format as XXXX-XXXX
        return "${raw.substring(0, 4)}-${raw.substring(4, 8)}"
    }

    /**
     * Checks whether a given code matches the public key for the current window
     * or the immediately preceding window (to handle clock skew / boundary edge).
     */
    fun validate(publicKey: String, code: String): Boolean {
        val normalised = code.trim().uppercase().replace("-", "")
        val currentWindow = System.currentTimeMillis() / 1000L / 600L
        return listOf(currentWindow, currentWindow - 1).any { window ->
            val expected = generate(publicKey, window).replace("-", "")
            expected == normalised
        }
    }

    /**
     * Returns how many seconds remain in the current 10-minute window.
     */
    fun secondsRemaining(): Int {
        val epochSec = System.currentTimeMillis() / 1000L
        return (600 - (epochSec % 600)).toInt()
    }
}
