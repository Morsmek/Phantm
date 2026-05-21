package com.example.crypto

object PhantmBase64 {
    private val isAndroid: Boolean by lazy {
        try {
            Class.forName("android.util.Base64")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun encode(bytes: ByteArray): String {
        return if (isAndroid) {
            try {
                val clazz = Class.forName("android.util.Base64")
                val method = clazz.getMethod("encodeToString", ByteArray::class.java, Int::class.javaPrimitiveType)
                method.invoke(null, bytes, 2) as String // 2 is android.util.Base64.NO_WRAP
            } catch (e: Exception) {
                java.util.Base64.getEncoder().encodeToString(bytes)
            }
        } else {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    fun decode(str: String): ByteArray {
        return if (isAndroid) {
            try {
                val clazz = Class.forName("android.util.Base64")
                val method = clazz.getMethod("decode", String::class.java, Int::class.javaPrimitiveType)
                method.invoke(null, str, 2) as ByteArray // 2 is android.util.Base64.NO_WRAP
            } catch (e: Exception) {
                java.util.Base64.getDecoder().decode(str)
            }
        } else {
            java.util.Base64.getDecoder().decode(str)
        }
    }
}
