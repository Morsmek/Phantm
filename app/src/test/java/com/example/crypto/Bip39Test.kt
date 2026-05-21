package com.example.crypto

import org.junit.Assert.*
import org.junit.Test

class Bip39Test {

    @Test
    fun testGenerateMnemonic() {
        val mnemonic = Bip39.generateMnemonic()
        assertNotNull(mnemonic)
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)
        
        // Assert all words generated are in the official word list
        val wordSet = Bip39WordList.BIP39_WORDS.toSet()
        assertTrue(words.all { wordSet.contains(it) })
    }

    @Test
    fun testValidMnemonic() {
        val validMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        assertTrue(Bip39.isValid(validMnemonic))
    }

    @Test
    fun testInvalidMnemonicWord() {
        val invalidMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon invalidword"
        assertFalse(Bip39.isValid(invalidMnemonic))
    }

    @Test
    fun testInvalidLength() {
        val invalidLength = "abandon abandon abandon"
        assertFalse(Bip39.isValid(invalidLength))
    }
}
