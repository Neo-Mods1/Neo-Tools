package com.neomods.tools.encoding

import android.util.Base64

object Base64Utils {

    fun encode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP)
    }

    fun decode(input: String): ByteArray {
        return try {
            Base64.decode(input, Base64.DEFAULT)
        } catch (_: Exception) {
            ByteArray(0)
        }
    }
}
