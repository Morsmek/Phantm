package com.example.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "phantm_db_key_alias"
    private const val PREFS_NAME = "phantm_secure_prefs"
    private const val ENCRYPTED_KEY_PREF = "encrypted_db_passphrase"
    private const val IV_PREF = "encrypted_db_iv"

    @Synchronized
    fun getOrGenerateDatabaseKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(ENCRYPTED_KEY_PREF, null)
        val ivBase64 = prefs.getString(IV_PREF, null)

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            try {
                val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                return decryptKey(encryptedKey, iv)
            } catch (e: Exception) {
                e.printStackTrace()
                // If decryption fails (e.g. keystore cleared), we must fall back to generating a new key
            }
        }

        // Generate a new random database key
        val newRawKey = generateRandomKey()
        try {
            val encryptionResult = encryptKey(newRawKey)
            prefs.edit()
                .putString(ENCRYPTED_KEY_PREF, Base64.encodeToString(encryptionResult.ciphertext, Base64.DEFAULT))
                .putString(IV_PREF, Base64.encodeToString(encryptionResult.iv, Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: return the generated key raw if keystore fails, but this should be rare
        }
        return newRawKey
    }

    private fun generateRandomKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        // Generate new AES key inside AndroidKeyStore
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encryptKey(rawKey: String): EncryptionResult {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(rawKey.toByteArray(Charsets.UTF_8))
        return EncryptionResult(ciphertext, cipher.iv)
    }

    private fun decryptKey(encryptedKey: ByteArray, iv: ByteArray): String {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(encryptedKey)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private class EncryptionResult(val ciphertext: ByteArray, val iv: ByteArray)
}
