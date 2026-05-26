package com.morsmek.phantm.crypto

import java.security.SecureRandom

object Bip39 {
    val WORD_LIST = Bip39WordList.BIP39_WORDS

    fun generateMnemonic(): String {
        val random = SecureRandom()
        val words = mutableListOf<String>()
        val size = WORD_LIST.size
        repeat(12) {
            val index = random.nextInt(size)
            words.add(WORD_LIST[index])
        }
        return words.joinToString(" ")
    }

    fun isValid(phrase: String): Boolean {
        val words = phrase.trim().lowercase().split("\\s+".toRegex())
        if (words.size != 12) return false
        // Validate each word exists in the standard BIP39 wordlist
        val wordSet = WORD_LIST.toSet()
        return words.all { wordSet.contains(it) }
    }
}
