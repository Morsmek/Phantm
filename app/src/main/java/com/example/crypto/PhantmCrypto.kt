package com.example.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object PhantmCrypto {
    
    fun deriveIdentity(mnemonic: String): String {
        // BIP39 seed derivation (2048 iterations PBKDF2-SHA512)
        val password = mnemonic.trim().lowercase().replace("\\s+".toRegex(), " ").toByteArray(Charsets.UTF_8)
        val salt = "mnemonic".toByteArray(Charsets.UTF_8)
        val generator = PKCS5S2ParametersGenerator(SHA512Digest())
        generator.init(password, salt, 2048)
        val keyParam = generator.generateDerivedParameters(512) as KeyParameter
        val seed = keyParam.key
        
        // Derive Ed25519 public key from first 32 bytes of the seed
        val privateKeySeed = seed.copyOfRange(0, 32)
        val privateKey = Ed25519PrivateKeyParameters(privateKeySeed, 0)
        val publicKey = privateKey.generatePublicKey()
        
        return publicKey.encoded.joinToString("") { "%02x".format(it) }
    }

    fun truncateKey(key: String, length: Int = 6): String {
        if (key.length <= length * 2) return key
        return "${key.substring(0, length)}...${key.substring(key.length - length)}"
    }

    private fun deriveKeys(passphrase: String, salt: String): Pair<SecretKeySpec, SecretKeySpec> {
        val passwordBytes = passphrase.toByteArray(Charsets.UTF_8)
        val saltBytes = salt.toByteArray(Charsets.UTF_8)
        val generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(passwordBytes, saltBytes, 1000)
        val keyParam = generator.generateDerivedParameters(512) as KeyParameter
        val keyBytes = keyParam.key
        val aesKeyBytes = keyBytes.copyOfRange(0, 32)
        val hmacKeyBytes = keyBytes.copyOfRange(32, 64)
        return Pair(
            SecretKeySpec(aesKeyBytes, "AES"),
            SecretKeySpec(hmacKeyBytes, "HmacSHA256")
        )
    }

    fun encrypt(content: String, passphrase: String, salt: String): EncryptedData {
        val (aesKey, hmacKey) = deriveKeys(passphrase, salt)
        
        // Generate random IV
        val random = SecureRandom()
        val ivBytes = ByteArray(16)
        random.nextBytes(ivBytes)
        val ivSpec = IvParameterSpec(ivBytes)
        
        // AES CBC Encryption
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec)
        val ciphertext = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
        
        // Compute HMAC (IV + Ciphertext)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        mac.update(ivBytes)
        val hmacBytes = mac.doFinal(ciphertext)
        
        return EncryptedData(
            ciphertext = PhantmBase64.encode(ciphertext),
            iv = PhantmBase64.encode(ivBytes),
            hmac = PhantmBase64.encode(hmacBytes)
        )
    }

    fun decrypt(encryptedBase64: String, ivBase64: String, hmacBase64: String, passphrase: String, salt: String): String {
        val (aesKey, hmacKey) = deriveKeys(passphrase, salt)
        
        val ciphertext = PhantmBase64.decode(encryptedBase64)
        val ivBytes = PhantmBase64.decode(ivBase64)
        val receivedHmac = PhantmBase64.decode(hmacBase64)
        
        // Verify HMAC first (Encrypt-then-MAC)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        mac.update(ivBytes)
        val computedHmac = mac.doFinal(ciphertext)
        
        if (!computedHmac.contentEquals(receivedHmac)) {
            throw SecurityException("MAC verification failed - incorrect passphrase or tampered payload")
        }
        
        // Decrypt
        val ivSpec = IvParameterSpec(ivBytes)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

data class EncryptedData(
    val ciphertext: String,
    val iv: String,
    val hmac: String
)
