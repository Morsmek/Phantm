package com.morsmek.phantm.crypto

import org.junit.Assert.*
import org.junit.Test
class PhantmCryptoTest {

    @Test
    fun testIdentityDerivation() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val identity = PhantmCrypto.deriveIdentity(mnemonic)
        
        // Assert identity is a 64-character hex string
        assertEquals(64, identity.length)
        assertTrue(identity.all { it.isDigit() || it in 'a'..'f' })
        
        // Assert that the same mnemonic derives the same identity key
        val identity2 = PhantmCrypto.deriveIdentity(mnemonic)
        assertEquals(identity, identity2)
    }

    @Test
    fun testEncryptDecrypt() {
        val content = "Cyberpunk secure communications active."
        val passphrase = "super-secret-passphrase"
        val salt = "contact-id-12345"

        val encrypted = PhantmCrypto.encrypt(content, passphrase, salt)
        assertNotNull(encrypted.ciphertext)
        assertNotNull(encrypted.iv)
        assertNotNull(encrypted.hmac)

        val decrypted = PhantmCrypto.decrypt(
            encrypted.ciphertext,
            encrypted.iv,
            encrypted.hmac,
            passphrase,
            salt
        )
        assertEquals(content, decrypted)
    }

    @Test
    fun testDecryptWithWrongPassphrase() {
        val content = "Cyberpunk secure communications active."
        val passphrase = "super-secret-passphrase"
        val salt = "contact-id-12345"

        val encrypted = PhantmCrypto.encrypt(content, passphrase, salt)

        try {
            PhantmCrypto.decrypt(
                encrypted.ciphertext,
                encrypted.iv,
                encrypted.hmac,
                "wrong-passphrase",
                salt
            )
            fail("Should throw SecurityException due to HMAC mismatch")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("MAC verification failed"))
        }
    }

    @Test
    fun testDecryptWithWrongSalt() {
        val content = "Cyberpunk secure communications active."
        val passphrase = "super-secret-passphrase"
        val salt = "contact-id-12345"

        val encrypted = PhantmCrypto.encrypt(content, passphrase, salt)

        try {
            PhantmCrypto.decrypt(
                encrypted.ciphertext,
                encrypted.iv,
                encrypted.hmac,
                passphrase,
                "wrong-salt"
            )
            fail("Should throw SecurityException due to HMAC mismatch")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("MAC verification failed"))
        }
    }
}
